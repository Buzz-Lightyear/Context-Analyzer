package com.thesis.android.metrics.contextanalyzer;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.widget.TextView;

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

            populateNumberOfInstalledApplications();
            contextAnalyzerService.populateKeywordToApplicationsMap();
            contextAnalyzerService.populateDefaultSuggestedApplications();

            Thread t = new Thread() {

                @Override
                public void run() {
                    try {

                        /*
                            If the activity is closed or hung for some reason and users end up
                            reopening it, we need to get the previous values from the database.

                            Initially the values in the file are 0 hits and 0 misses.
                        */

                        contextAnalyzerService.getCurrentMetricsFromFile();

                        int iteration = 0;

                        while (!isInterrupted()) {

                            /*
                                Update metrics file every minute, so that if the app crashes
                                and the update statement in onDestroy isn't called, we'll always
                                have accurate data from the last minute.
                            */
                            if(iteration % 60 == 0)
                            {
                                Log.d("Minute Update", "Saving latest metrics to file");
                                contextAnalyzerService.updateMetricsFile();
                            }

                            /*
                                Every two hours, get the latest list of suggested applications by parsing the Calendar
                            */
                            if(iteration % 14400 == 0)
                            {
                                Log.d("4 Hour Update", "Update list of suggested applications by parsing the user's Calendar");
                                contextAnalyzerService.updateListOfSuggestedApplications();
                            }

                            iteration++;

                            /*
                                Wait for a second before checking for a new foreground app
                            */
                            Thread.sleep(1000);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    populateTextViews();
                                    contextAnalyzerService.updateStatistics();
                                }

                                private void populateTextViews() {
                                    populateTextView(contextAnalyzerService.getOverallCacheHits(), R.id.overallCacheHits);
                                    populateTextView(contextAnalyzerService.getOverallCacheMisses(), R.id.overallCacheMisses);
                                    populateTextView(contextAnalyzerService.getOverallCacheHitRatio(), R.id.overallCacheHitRatio);
                                    populateTextView(contextAnalyzerService.getSuggestedExclusiveCacheHits(), R.id.suggestedExclusiveCacheHits);
                                    populateTextView(contextAnalyzerService.getCacheAndSuggestedCacheHits(), R.id.cacheAndSuggestedCacheHits);
                                    populateTextView(contextAnalyzerService.getSuggestedCacheMisses(), R.id.suggestedCacheMisses);
                                    populateTextView(contextAnalyzerService.getSuggestedCacheHitRatio(), R.id.suggestedCacheHitRatio);
                                    populateTextView(contextAnalyzerService.getNumberOfUniqueApplicationsClicked(), R.id.numberOfUniqueApplicationsClicked);
                                    populateTextView(contextAnalyzerService.getUniqueApplicationsClicked(), R.id.uniqueApplicationsClicked);
                                    populateTextView(contextAnalyzerService.getRecentHitsAndMisses(), R.id.recentHitsAndMisses);
                                    populateTextView(contextAnalyzerService.getListOfProcessesInCache(), R.id.listOfProcessesInCache);
                                    populateTextView(contextAnalyzerService.getListOfSuggestedApplications(), R.id.listOfSuggestedApplications);
                                }

                            });
                        }
                    } catch (InterruptedException e) {
                    }
                }
            };

            t.start();
        }

        private void populateNumberOfInstalledApplications() {
            final int numberOfInstalledApplications =
                    contextAnalyzerService.getAllInstalledApplications().size();
            populateTextView(String.valueOf(numberOfInstalledApplications) + "\n", R.id.numberOfInstalledApplications);
        }

        @Override
        public void onServiceDisconnected(ComponentName name)
        {
            serviceBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_context_analyzer);

        populateSystemAndDeviceInformation();

        Intent intent = new Intent(this, ContextAnalyzerService.class);
        startService(intent);

        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void populateSystemAndDeviceInformation()
    {
        StringBuilder systemAndDeviceInformation = new StringBuilder();

        appendSystemInformation(systemAndDeviceInformation);
        appendDeviceInformation(systemAndDeviceInformation);

        populateTextView(systemAndDeviceInformation.toString(), R.id.systemAndDeviceInformation);
    }

    private void appendDeviceInformation(StringBuilder systemAndDeviceInformation) {
        systemAndDeviceInformation.append("\nModel: ").append(android.os.Build.MODEL)
                .append(" (").append(android.os.Build.PRODUCT).append(")\n");
    }

    private void appendSystemInformation(StringBuilder systemAndDeviceInformation) {
        systemAndDeviceInformation.append("API Level: ").append(Build.VERSION.SDK_INT);
    }

    private void populateTextView(String content, int viewId) {
        TextView textView = (TextView) findViewById(viewId);
        textView.setText(content);
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
            /*
                If the activity is manually closed,
                update internal storage with cache metrics
            */
            contextAnalyzerService.updateMetricsFile();

            unbindService(serviceConnection);
            serviceBound = false;
        }
    }
}
