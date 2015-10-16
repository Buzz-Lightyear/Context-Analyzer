package com.thesis.android.metrics.contextanalyzer;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;

import java.util.List;

/**
 * Created by Srinivas on 10/14/2015.
 */
public class ContextAnalyzerService extends Service
{
    private int overallCacheHits;
    private int overallCacheMisses;
    private double overallCacheHitRatio = -1;
    private int suggestedExclusiveCacheHits;
    private int cacheAndSuggestedCacheHits;
    private int suggestedCacheMisses;
    private double suggestedCacheHitRatio = 1;
    private String recentHitsAndMisses;
    private String listOfProcessesInCache;
    private String listOfSuggestedApplications;

    public void getCurrentMetricsFromFile()
    {
        /*
            Retrieve 5 values from file
                Overall CH
                Overall CM
                Suggested Exclusive CH
                Suggested and Cache CH
                Suggested CM
        */
    }

    public void updateMetricsFile()
    {
        /*
            Update 5 values to file
            Overall CH
            Overall CM
            Suggested Exclusive CH
            Suggested and Cache CH
            Suggested CM
        */
    }

    public void updateSuggestedApplications()
    {
        /*
            Update list of suggested applications.
            This function is called once at application start and every subsequent hour.
        */
    }

    public void updateStatistics()
    {
        /*
            This function is called every second.
            It updates all cache hit/miss statistics.
        */
    }

    public String getOverallCacheHits()
    {
        return String.valueOf(overallCacheHits);
    }

    public String getOverallCacheMisses()
    {
        return String.valueOf(overallCacheMisses);
    }

    public String getOverallCacheHitRatio()
    {
        return overallCacheHitRatio <= 0.0 ? "N/A" : String.valueOf(overallCacheHitRatio) + "%";
    }

    public String getSuggestedExclusiveCacheHits()
    {
        return String.valueOf(suggestedExclusiveCacheHits);
    }

    public String getCacheAndSuggestedCacheHits()
    {
        return String.valueOf(cacheAndSuggestedCacheHits);
    }

    public String getSuggestedCacheMisses()
    {
        return String.valueOf(suggestedCacheMisses);
    }

    public String getSuggestedCacheHitRatio()
    {
        return suggestedCacheHitRatio <= 0.0 ? "N/A" : String.valueOf(suggestedCacheHitRatio) + "%";
    }

    public String getRecentHitsAndMisses()
    {
        return recentHitsAndMisses;
    }

    public String getListOfProcessesInCache()
    {
        return listOfProcessesInCache;
    }

    public String getListOfSuggestedApplications()
    {
        return listOfSuggestedApplications;
    }

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
