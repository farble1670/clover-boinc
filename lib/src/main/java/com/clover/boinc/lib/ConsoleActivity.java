package com.clover.boinc.lib;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;

public class ConsoleActivity extends Activity {
  private ViewGroup consoleLayout;
  private ScrollView scrollLayout;

  private final Handler handler = new Handler(Looper.getMainLooper());

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_console);

    consoleLayout = (ViewGroup) findViewById(R.id.layout_console);
    scrollLayout = (ScrollView) findViewById(R.id.layout_scroll);
  }

  protected void message(final String format, final Object... args) {
    message(String.format(format, args));
  }

    protected void message(final String msg) {
    handler.post(new Runnable() {
      @Override
      public void run() {
        TextView tv = new TextView(ConsoleActivity.this);
        tv.setText(String.format("%s", msg));
        consoleLayout.addView(tv, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        scrollLayout.post(new Runnable() {
          @Override
          public void run() {
            scrollLayout.fullScroll(View.FOCUS_DOWN);
          }
        });
      }
    });
  }

  protected void clear() {
    handler.post(new Runnable() {
      @Override
      public void run() {
        consoleLayout.removeAllViews();
      }
    });

  }
}
