package com.thesis.android.metrics.contextanalyzer;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;

/**
 * Created by Srinivas on 10/14/2015.
 */
public class ContextAnalyzerService extends Service
{
    public class MyBinder extends Binder {
        ContextAnalyzerService getService() {
            return ContextAnalyzerService.this;
        }
    }

    private IBinder binder = new MyBinder();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
}
