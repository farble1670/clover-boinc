package com.clover.boinc.lib;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import edu.berkeley.boinc.rpc.AccountIn;
import edu.berkeley.boinc.rpc.AccountOut;
import edu.berkeley.boinc.rpc.Project;
import edu.berkeley.boinc.rpc.Result;
import edu.berkeley.boinc.rpc.RpcClient;
import edu.berkeley.boinc.rpc.Workunit;
import edu.berkeley.boinc.utils.Logging;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.List;

public class SetupActivity extends ConsoleActivity implements ServiceConnection {
  private BoincClientService boincClientService = null;
  private final Handler handler = new Handler(Looper.getMainLooper());
  private final RpcClient rpcClient = new RpcClient();
  private Menu menu = null;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    copyBinaries();
  }

  private void startBoincClient() {
    Intent svcIntent = new Intent(this, BoincClientService.class);
    bindService(svcIntent, this, Service.BIND_AUTO_CREATE);
  }

  private void copyBinaries() {
    new AsyncTask<Void, Void, Boolean>() {
      @Override
      protected void onPreExecute() {
        message("Copying boinc binaries into place ...");
      }

      @Override
      protected Boolean doInBackground(Void... params) {
        File boincDir = new File(getFilesDir(), "boinc");
        if (!boincDir.exists()) {
          boincDir.mkdir();
        }

        File boincClientFile = new File(boincDir, "boinc_client");

        if (!copyRaw(R.raw.boinc_client, boincClientFile)) {
          return false;
        }
        boincClientFile.setExecutable(true, false);
        boincClientFile.setReadable(true, false);

        File boincCmdFile = new File(boincDir, "boinccmd");

        if (!copyRaw(R.raw.boinccmd, boincCmdFile)) {
          return false;
        }
        boincCmdFile.setExecutable(true, false);
        boincCmdFile.setReadable(true, false);

        return true;
      }

      @Override
      protected void onPostExecute(Boolean result) {
        if (result) {
          message("Copied boinc binaries");
        } else {
          message("Failed to copy boinc binaries");
        }
        message("Starting boinc client ...");
        startBoincClient();
      }
    }.execute();
  }

  private boolean copyRaw(int rawId, File destFile) {
    InputStream is = getResources().openRawResource(rawId);
    OutputStream os = null;

    byte[] buf = new byte[8192];
    int count;

    try {
      os = new FileOutputStream(destFile);
      while ((count = is.read(buf)) != -1) {
        os.write(buf, 0, count);
      }
      return true;
    } catch (IOException e) {
      e.printStackTrace();
      return false;
    } finally {
      if (is != null) {
        try {
          is.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
      if (os != null) {
        try {
          os.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }

  private void connect() {
    new AsyncTask<Void, Void, Boolean>() {
      @Override
      protected Boolean doInBackground(Void... params) {
        int count = 0;
        while (count < 600) {
          if (rpcClient.open("localhost", 31416)) {
            return true;
          }
          SystemClock.sleep(100);
          count++;
        }
        return false;
      }

      @Override
      protected void onPostExecute(Boolean result) {
        MenuItem startStopMenuItem = menu.findItem(R.id.action_start_stop);
        startStopMenuItem.setVisible(true);

        if (!result) {
          message("Open failed");
          startStopMenuItem.setTitle("Start");
        } else {
          message("Open succeeded");
          startStopMenuItem.setTitle("Stop");
          authorize();
        }
      }
    }.execute();
  }

  private void authorize() {
    new AsyncTask<Void, Void, Boolean>() {
      @Override
      protected Boolean doInBackground(Void... params) {
        String pw = readPassword();
        if (!rpcClient.authorize(pw)) {
          return false;
        }

        return true;
      }

      @Override
      protected void onPostExecute(Boolean result) {
        if (!result) {
          message("Authorize failed");
        } else {
          message("Authorize succeeded");
          lookupCredentials(getProjectUrl(), getProjectUser(), getProjectPassword(), false);
        }
      }
    }.execute();
  }

  @Override
  public void onServiceConnected(ComponentName name, IBinder service) {
    message("Service connected");
    BoincClientService.BoincClientBinder binder = (BoincClientService.BoincClientBinder) service;
    boincClientService = binder.getService();

    boincClientService.start();
    message("Boinc client started ... wating ");

    connect();
  }

  @Override
  public void onServiceDisconnected(ComponentName name) {
    message("Service disconnected");
  }

  public void lookupCredentials(final String url, final String id, final String pwd, final Boolean usesName) {
    new AsyncTask<Void, Void, AccountOut>() {
      @Override
      protected AccountOut doInBackground(Void... params) {
        AccountOut auth = null;
        AccountIn credentials = new AccountIn();
        if (usesName) credentials.user_name = id;
        else credentials.email_addr = id;
        credentials.passwd = pwd;
        credentials.url = url;
        Boolean success = rpcClient.lookupAccount(credentials); //asynch
        if (success) { //only continue if lookupAccount command did not fail
          //get authentication token from lookupAccountPoll
          Integer counter = 0;
          Integer maxLoops = 100;
          Boolean loop = true;
          while (loop && (counter < maxLoops)) {
            loop = false;
            try {
              Thread.sleep(100);
            } catch (Exception e) {
            }
            counter++;
            auth = rpcClient.lookupAccountPoll();
            if (auth == null) {
              if (Logging.DEBUG) Log.d(Logging.TAG, "error in rpc.lookupAccountPoll.");
              return null;
            }
            if (auth.error_num == -204) {
              loop = true; //no result yet, keep looping
            } else {
              //final result ready
              if (auth.error_num == 0)
                if (Logging.DEBUG) Log.d(Logging.TAG, "credentials verification result, retrieved authenticator.");
                else Log.d(Logging.TAG, "credentials verification result, error: " + auth.error_num);
            }
          }
        } else if (Logging.DEBUG) Log.d(Logging.TAG, "rpc.lookupAccount failed.");

        return auth;
      }

      @Override
      protected void onPostExecute(AccountOut accountOut) {
        if (accountOut == null) {
          message("Account lookup failed");
        } else {
          message("Account lookup succeeded, authenticator: " + accountOut.authenticator);
          attachProject(getProjectUrl(), accountOut.authenticator);
        }
      }
    }.execute();
  }

  private void attachProject(final String url, final String authenticator) {
    new AsyncTask<Void, Void, Boolean>() {
      @Override
      protected Boolean doInBackground(Void... params) {
        if (!rpcClient.projectAttach(url, authenticator, url)) {
          return false;
        }

        return true;
      }

      @Override
      protected void onPostExecute(Boolean result) {
        if (!result) {
          message("Attach failed");
        } else {
          message("Attach successful");
        }
      }
    }.execute();
  }

  protected String getProjectUrl() {
    return getMetaData("com.clover.boinc.project_url");
  }

  protected String getProjectUser() {
    return getMetaData("com.clover.boinc.project_email");
  }

  protected String getProjectPassword() {
    return getMetaData("com.clover.boinc.project_password");
  }

  @Override
  protected void onDestroy() {
    if (boincClientService != null) {
      unbindService(this);
      boincClientService = null;
    }

    super.onDestroy();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    this.menu = menu;

    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.menu_setup, menu);

    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle presses on the action bar items
    if (item.getItemId() == R.id.action_start_stop) {
      final MenuItem startStopMenuItem = menu.findItem(R.id.action_start_stop);

      if (rpcClient.isConnected()) {
        quit();

        handler.postDelayed(new Runnable() {
          @Override
          public void run() {
            rpcClient.close();
            boincClientService.stop();
            if (boincClientService != null) {
              unbindService(SetupActivity.this);
              boincClientService = null;
            }
            startStopMenuItem.setTitle("Start");
          }
        }, 2000);
      } else {
        startBoincClient();
      }
      return true;
    } else if (item.getItemId() == R.id.action_status) {
      getProjectStatus();
      return true;
    } else if (item.getItemId() == R.id.action_workunits) {
      getWorkUnits();
      return true;
    } else if (item.getItemId() == R.id.action_results) {
      getResults();
      return true;
    } else if (item.getItemId() == R.id.action_clear) {
      clear();
    }
    return super.onOptionsItemSelected(item);
  }

  private void getWorkUnits() {
    new AsyncTask<Void, Void, List<Workunit>>() {
      @Override
      protected List<Workunit> doInBackground(Void... params) {
        List<Workunit> workunits = rpcClient.getState().workunits;
        return workunits;
      }

      @Override
      protected void onPostExecute(List<Workunit> workunits) {
        if (workunits == null) {
          message("Failed getting work units");
          return;
        }

        for (Workunit wu : workunits) {
          message("[WORKUNIT] %s/%s: %s=%s", wu.project.project_name, wu.name, "version_num", wu.version_num);
        }
      }
    }.execute();


  }

  private String getMetaData(String key) {
    try {
      ApplicationInfo ai = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
      Bundle bundle = ai.metaData;
      return bundle.getString(key);
    } catch (PackageManager.NameNotFoundException e) {
      throw new RuntimeException(e);
    } catch (NullPointerException e) {
      throw new RuntimeException(e);
    }
  }

  private String readPassword() {
    File guiRpcAuthFile = new File(new File(getFilesDir(), "boinc"), "gui_rpc_auth.cfg");
    if (!guiRpcAuthFile.exists()) {
      return null;
    }

    try {
      InputStreamReader isr = new InputStreamReader(new FileInputStream(guiRpcAuthFile));
      BufferedReader br = new BufferedReader(isr);

      String line = br.readLine();
      return line;
    } catch (IOException e) {
      e.printStackTrace();
    }

    return null;
  }

  private void quit() {
    new AsyncTask<Void, Void, Void>() {
      @Override
      protected Void doInBackground(Void... params) {
        if (!rpcClient.quit()) {
          message("Quit failed");
        } else {
          message("Quit successful");
        }
        return null;
      }
    }.execute();
  }

  private void getProjectStatus() {
    new AsyncTask<Void, Void, Void>() {

      @Override
      protected Void doInBackground(Void... params) {
        List<Project> projects = rpcClient.getProjectStatus();
        for (Project project : projects) {
          message(String.format("[PROJECT] %s: %s=%s", project.project_name, "user_total_credit", project.user_total_credit));
        }

        return null;
      }
    }.execute();
  }

  private void getResults() {
    new AsyncTask<Void, Void, List<Result>>() {

      @Override
      protected List<Result> doInBackground(Void... params) {
        List<Result> results = rpcClient.getActiveResults();
        return results;
      }

      @Override
      protected void onPostExecute(List<Result> results) {
        if (results == null) {
          message("Failed to get results");
        } else {
          for (Result result : results) {
            message("[RESULT] %s-%s: %s=%s", result.project_url, result.wu_name, "elapsed_time", getTimeString(result.elapsed_time));
            message("[RESULT] %s-%s: %s=%s", result.project_url, result.wu_name, "estimated_cpu_time_remaining", getTimeString(result.estimated_cpu_time_remaining));
          }
        }

      }
    }.execute();
  }

  private static String getTimeString(double totalSecs) {
    return getTimeString((int)totalSecs);
  }

  private static String getTimeString(int totalSecs) {
    int hours = totalSecs / 3600;
    int minutes = (totalSecs % 3600) / 60;
    int seconds = totalSecs % 60;

    String timeString = String.format("%02d:%02d:%02d", hours, minutes, seconds);
    return timeString;
  }
}
