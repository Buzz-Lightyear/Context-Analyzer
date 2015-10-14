package com.thesis.android.metrics.contextanalyzer;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

public class ContextAnalyzerActivity extends AppCompatActivity {

    ContextAnalyzerService contextAnalyzerService;
    boolean serviceBound = false;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service)
        {
            ContextAnalyzerService.MyBinder myBinder
                    = (ContextAnalyzerService.MyBinder) service;
            contextAnalyzerService = myBinder.getService();
            serviceBound = true;

            /*
                To do - Start a thread and work on it
            */
        }

        @Override
        public void onServiceDisconnected(ComponentName name)
        {
            serviceBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_context_analyzer);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_context_analyzer, menu);
        return true;
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        if(serviceBound)
        {
            unbindService(serviceConnection);
            serviceBound = false;
        }
    }
}
