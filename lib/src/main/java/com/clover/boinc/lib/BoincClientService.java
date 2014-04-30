package com.clover.boinc.lib;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class BoincClientService extends Service {

  public class BoincClientBinder extends Binder {
    BoincClientService getService() {
      return BoincClientService.this;
    }
  }

  private final Executor clientExec = Executors.newSingleThreadExecutor();

  private final IBinder binder = new BoincClientBinder();

  private Process process = null;

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    start();
    return START_NOT_STICKY;
  }

  public void start() {
    if (process != null) {
      return;
    }

    try {
      process = new ProcessBuilder().command("./boinc_client").directory(new File(getFilesDir(), "boinc")).redirectErrorStream(true).start();
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }

    Notification n = new Notification.Builder(this)
        .setSmallIcon(android.R.drawable.stat_notify_sync)
        .setContentTitle("Boinc client")
        .setContentText("Boinc client is running ...")
        .build();

    startForeground(100, n);
  }

  public void stop() {
    if (process != null) {
      process.destroy();
      process = null;
    }

    stopForeground(true);
  }

  @Override
  public IBinder onBind(Intent intent) {
    return binder;
  }


  public boolean isRunning() {
    return process != null;
  }

  @Override
  public void onDestroy() {
    stop();
    super.onDestroy();
  }
}
