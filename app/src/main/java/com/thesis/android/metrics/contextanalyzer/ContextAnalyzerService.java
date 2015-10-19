package com.thesis.android.metrics.contextanalyzer;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

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

    private String recentHitsAndMisses = "Loading...";
    private Queue<String> recentHitsAndMissesQueue = new LinkedList<>();

    private String listOfProcessesInCache = "Loading...";
    private Set<String> setOfProcessesInCacheList = new HashSet<>();

    private String listOfSuggestedApplications = "Loading...";
    private Set<String> setOfSuggestedApplications = new HashSet<>();

    private String foregroundApp = "Loading...";

    private Set<String> uniqueApplicationsClicked = new HashSet<>();

    public String getNumberOfUniqueApplicationsClicked()
    {
        return String.valueOf(uniqueApplicationsClicked.size());
    }

    public String getUniqueApplicationsClicked()
    {
        return customStringFormatForSet(uniqueApplicationsClicked);
    }

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

        {
            final String filename = getString(R.string.metricsDataFileName);

            FileInputStream inputStream;
            InputStreamReader inputStreamReader;
            BufferedReader bufferedReader = null;

            try
            {
                inputStream       = openFileInput(filename);
                inputStreamReader = new InputStreamReader(inputStream);
                bufferedReader    = new BufferedReader(inputStreamReader);

                overallCacheHits                    = Integer.valueOf(bufferedReader.readLine());
                overallCacheMisses                  = Integer.valueOf(bufferedReader.readLine());
                suggestedExclusiveCacheHits         = Integer.valueOf(bufferedReader.readLine());
                cacheAndSuggestedCacheHits          = Integer.valueOf(bufferedReader.readLine());
                suggestedCacheMisses                = Integer.valueOf(bufferedReader.readLine());

                uniqueApplicationsClicked = new HashSet<>();
                String process;
                while((process = bufferedReader.readLine()) != null)
                    uniqueApplicationsClicked.add(process);
            }
            catch (FileNotFoundException e)
            {
                Log.d("Exception", "File hasn't been created yet, method called for first time");
                return;
            }
            catch (IOException e)
            {
                Log.d("Exception", "IO Exception while reading from metrics file");
                e.printStackTrace();
            }
            finally
            {
                try
                {   if(bufferedReader != null)
                        closeFileInputHandlers(bufferedReader);
                }
                catch (IOException e)
                {
                    Log.d("Exception", "Exception occurred while closing read file handler");
                }
            }
        }
    }

    private void closeFileInputHandlers(BufferedReader bufferedReader) throws IOException
    {
        bufferedReader.close();
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

        {
            final String filename                           = getString(R.string.metricsDataFileName);
            final String overallHits                        = String.valueOf(overallCacheHits);
            final String overallMisses                      = String.valueOf(overallCacheMisses);
            final String suggestedExclusiveHits             = String.valueOf(suggestedExclusiveCacheHits);
            final String cacheAndSuggestedHits              = String.valueOf(cacheAndSuggestedCacheHits);
            final String suggestedMisses                    = String.valueOf(suggestedCacheMisses);

            FileOutputStream outputStream;
            OutputStreamWriter outputStreamWriter;
            BufferedWriter bufferedWriter = null;

            try
            {
                outputStream         = openFileOutput(filename, Context.MODE_PRIVATE);
                outputStreamWriter   = new OutputStreamWriter(outputStream);
                bufferedWriter       = new BufferedWriter(outputStreamWriter);

                writeDataToFile(overallHits, overallMisses, suggestedExclusiveHits, cacheAndSuggestedHits,
                        suggestedMisses, bufferedWriter);
            }
            catch (Exception e)
            {
                e.printStackTrace();
                Log.d("Exception", "Exception occurred while writing to file");
            }
            finally
            {
                try {
                    if(bufferedWriter != null)
                        closeFileOutputHandlers(bufferedWriter);
                } catch (IOException e) {
                    Log.d("Exception", "Exception occurred while closing file handler");
                    e.printStackTrace();
                }
            }
        }
    }

    private void closeFileOutputHandlers(BufferedWriter bufferedWriter) throws IOException
    {
        bufferedWriter.flush();
        bufferedWriter.close();
    }

    private void writeDataToFile(String overallHits, String overallMisses, String suggestedExclusiveHits,
                                 String cacheAndSuggestedHits, String suggestedMisses,
                                 BufferedWriter bufferedWriter) throws IOException {
        bufferedWriter.write(overallHits);
        bufferedWriter.newLine();
        bufferedWriter.write(overallMisses);
        bufferedWriter.newLine();
        bufferedWriter.write(suggestedExclusiveHits);
        bufferedWriter.newLine();
        bufferedWriter.write(cacheAndSuggestedHits);
        bufferedWriter.newLine();
        bufferedWriter.write(suggestedMisses);
        bufferedWriter.newLine();

        writeOneAppPerLine(bufferedWriter);
    }

    private void writeOneAppPerLine(BufferedWriter bufferedWriter) throws IOException {
        for(String process : uniqueApplicationsClicked)
        {
            bufferedWriter.write(process);
            bufferedWriter.newLine();
        }
    }

    Map<String, List<String>> keywordToApplicationsMap = new HashMap<>();
    List<String> defaultSuggestedApplications = new ArrayList<>();

    public void updateListOfSuggestedApplications()
    {
        /*
            Update list of suggested applications.
            This function is called once at application start and every subsequent 4 hours.
        */

        setOfSuggestedApplications = new HashSet<String>();

        /*
            Hit the Calendar API here and update list of suggested applications
        */

        List<String> eventsList = getEventsFromCalendar();

        for(String event : eventsList)
        {
            String[] keywords = event.split(" ");
            for(String keyword : keywords)
            {
                keyword = keyword.toLowerCase();
                if(keywordToApplicationsMap.containsKey(keyword))
                    setOfSuggestedApplications.addAll(keywordToApplicationsMap.get(keyword));
            }
        }

        setOfSuggestedApplications.addAll(defaultSuggestedApplications);
        listOfSuggestedApplications = customStringFormatForSet(setOfSuggestedApplications);
    }

    private List<String> getEventsFromCalendar()
    {
        /*
            Hit the Calendar API and get the events for the next two hours
        */
        List<String> events = CalendarReader.readCalendarEvent(getApplicationContext());
        return events;
    }

    void populateDefaultSuggestedApplications()
    {
        defaultSuggestedApplications.add("com.facebook.katana");
        defaultSuggestedApplications.add("com.instagram.android");
        defaultSuggestedApplications.add("com.snapchat.android");
        defaultSuggestedApplications.add("com.google.android.apps.maps");
        defaultSuggestedApplications.add("com.facebook.orca");
        defaultSuggestedApplications.add("com.google.android.youtube");
        defaultSuggestedApplications.add("com.android.vending");
        defaultSuggestedApplications.add("com.pandora.android");
        defaultSuggestedApplications.add("com.google.android.gm");
        defaultSuggestedApplications.add("com.twitter.android");
    }

    void populateKeywordToApplicationsMap()
    {
        /*
            Keep updating this list as and when you think of keywords
        */

        final String googleDialer = getString(R.string.googleDialer);
        final String hangouts = getString(R.string.hangouts);
        final String skype = getString(R.string.skype);
        final String imo = getString(R.string.imo);
        final String contacts = getString(R.string.contacts);
        final String whatsapp = getString(R.string.whatsapp);

        keywordToApplicationsMap.put("call", Arrays.asList(googleDialer,
                                                            hangouts,
                                                            skype,
                                                            imo,
                                                            contacts,
                                                            whatsapp));

        final String email = getString(R.string.email);
        final String gmail = getString(R.string.gmail);

        keywordToApplicationsMap.put("contact", Arrays.asList(googleDialer,
                                                                hangouts,
                                                                skype,
                                                                imo,
                                                                contacts,
                                                                email,
                                                                whatsapp,
                                                                gmail));

        keywordToApplicationsMap.put("chat", Arrays.asList(googleDialer,
                                                            hangouts,
                                                            skype,
                                                            imo,
                                                            contacts,
                                                            email,
                                                            whatsapp,
                                                            gmail));

        keywordToApplicationsMap.put("ping", Arrays.asList(googleDialer,
                                                            hangouts,
                                                            skype,
                                                            imo,
                                                            contacts,
                                                            email,
                                                            whatsapp,
                                                            gmail));

        keywordToApplicationsMap.put("dial", Arrays.asList(googleDialer,
                                                            hangouts,
                                                            skype,
                                                            imo,
                                                            contacts,
                                                            whatsapp));

        final String calendar = getString(R.string.calendar);
        final String googleDocs = getString(R.string.googleDocs);
        final String googleSheets = getString(R.string.googleSheets);
        final String googleSlides = getString(R.string.googleSlides);
        final String googleDrive = getString(R.string.googleDrive);
        final String googleKeep = getString(R.string.googleKeep);

        keywordToApplicationsMap.put("meeting", Arrays.asList(googleDialer,
                                                                hangouts,
                                                                calendar,
                                                                skype,
                                                                imo,
                                                                contacts,
                                                                googleDocs,
                                                                googleSheets,
                                                                googleSlides,
                                                                email,
                                                                gmail,
                                                                googleDrive,
                                                                googleKeep));

        keywordToApplicationsMap.put("mail", Arrays.asList(contacts,
                                                            email,
                                                            gmail,
                                                            googleDrive,
                                                            googleDocs,
                                                            googleSheets,
                                                            googleSlides));

        keywordToApplicationsMap.put("skype", Arrays.asList(skype));

        keywordToApplicationsMap.put("conference", Arrays.asList(googleDialer,
                                                                    hangouts,
                                                                    skype,
                                                                    imo,
                                                                    contacts,
                                                                    whatsapp));

        keywordToApplicationsMap.put("presentation", Arrays.asList(googleDocs,
                                                                    googleSheets,
                                                                    googleDrive,
                                                                    googleKeep,
                                                                    googleSlides));

        keywordToApplicationsMap.put("report", Arrays.asList(googleDocs,
                                                                googleSheets,
                                                                googleSlides,
                                                                gmail,
                                                                googleDrive,
                                                                googleKeep));

        final String chromeProcess1 = getString(R.string.chromProcess1);
        final String chromeProcess2 = getString(R.string.chromeProcess2);
        final String chromeProcess3 = getString(R.string.chromeProcess3);
        final String clock = getString(R.string.clock);

        keywordToApplicationsMap.put("study", Arrays.asList(chromeProcess1,
                                                            chromeProcess2,
                                                            chromeProcess3,
                                                            clock));

        keywordToApplicationsMap.put("sleep", Arrays.asList(chromeProcess1,
                                                            chromeProcess2,
                                                            chromeProcess3,
                                                            clock));

        final String goSMS = getString(R.string.goSMS);

        keywordToApplicationsMap.put("message", Arrays.asList(goSMS,
                                                                email,
                                                                gmail,
                                                                hangouts,
                                                                whatsapp,
                                                                contacts));

        keywordToApplicationsMap.put("text", Arrays.asList(goSMS,
                                                            email,
                                                            gmail,
                                                            hangouts,
                                                            whatsapp,
                                                            contacts));

        final String calculator = getString(R.string.calculator);

        keywordToApplicationsMap.put("class", Arrays.asList(calculator,
                                                            chromeProcess2,
                                                            chromeProcess1,
                                                            chromeProcess3,
                                                            googleDocs,
                                                            googleSheets,
                                                            googleSlides));

        keywordToApplicationsMap.put("birthday", Arrays.asList(goSMS,
                                                                hangouts,
                                                                gmail,
                                                                email,
                                                                calendar,
                                                                googleDialer,
                                                                skype,
                                                                imo,
                                                                whatsapp));
    }

    public void updateStatistics()
    {
        /*
            This function is called every second.
            It updates all cache hit/miss statistics.
        */

        Set<String> appsThatDontAddToCacheMetrics = new HashSet<>();
        addIrrelevantAppsToSet(appsThatDontAddToCacheMetrics);

        String newForegroundApp = computeForegroundApp();

        /*
            User is using the recent apps screen to pick a foreground application.
            In this case, newForegroundApp is null
        */
        if(newForegroundApp == null)
            return;

        final boolean foregroundAppChanged = !newForegroundApp.equals(foregroundApp);

        /*
            User has switched to a new application, update Cache statistics!

            The second check in the if statement ensures that switching to
            the home screen (internally a process or switching to the Cache
            Metrics app doesn't affect the statistics.)
        */

        if(foregroundAppChanged && !appsThatDontAddToCacheMetrics.contains(newForegroundApp))
        {
            removeLastIfQueueFull();
            uniqueApplicationsClicked.add(newForegroundApp);

            boolean appPresentInCache           = setOfProcessesInCacheList.contains(newForegroundApp);
            boolean appPresentInSuggestedList   = setOfSuggestedApplications.contains(newForegroundApp);

            /*
                Increment overall cache hit, cache and suggested cache hit
            */
            if(appPresentInCache && appPresentInSuggestedList)
            {
                Log.d("Cache Hit", "App present in both suggested list and cache");
                recentHitsAndMissesQueue.offer("HIT - Present in Both Lists\nOLD - "
                        + foregroundApp
                        + "\nNEW - "
                        + newForegroundApp
                        + "\n");
                overallCacheHits++;
                cacheAndSuggestedCacheHits++;
            }

            /*
                Increment overall cache hit, suggested cache miss
            */
            else if(appPresentInCache && !appPresentInSuggestedList)
            {
                Log.d("Cache Hit", "App present in cache but not in suggested list");
                recentHitsAndMissesQueue.offer("HIT - Present only in Cache\nOLD - "
                        + foregroundApp
                        + "\nNEW - "
                        + newForegroundApp
                        + "\n");
                overallCacheHits++;
                suggestedCacheMisses++;
            }

            /*
                Increment overall cache hits, suggested exclusive cache hits
            */
            else if(!appPresentInCache && appPresentInSuggestedList)
            {
                Log.d("Cache Hit", "App present in suggested app list but not in cache");
                recentHitsAndMissesQueue.offer("HIT - Present in Suggested Apps\nOLD - "
                        + foregroundApp
                        + "\nNEW - "
                        + newForegroundApp
                        + "\n");
                overallCacheHits++;
                suggestedExclusiveCacheHits++;
            }

            /*
                Increment overall cache misses, suggested cache misses
            */
            else
            {
                Log.d("Cache Miss", "App present in neither of the lists");
                recentHitsAndMissesQueue.offer("MISS - Present in Neither List\nOLD - "
                        + foregroundApp
                        + "\nNEW - "
                        + newForegroundApp
                        + "\n");
                overallCacheMisses++;
                suggestedCacheMisses++;
            }

        }

        updateOverallCacheHitRatio();
        updateSuggestedCacheHitRatio();

        foregroundApp = newForegroundApp;

        recentHitsAndMisses = customStringFormatForQueue(recentHitsAndMissesQueue);

        setOfProcessesInCacheList = computeRunningApplications();
        listOfProcessesInCache = customStringFormatForSet(setOfProcessesInCacheList);
    }

    private void updateOverallCacheHitRatio()
    {
        final int totalHitsAndMisses = overallCacheHits + overallCacheMisses;
        overallCacheHitRatio = totalHitsAndMisses < 1 ? 0.0 : overallCacheHits * 100 / totalHitsAndMisses;
    }

    private void updateSuggestedCacheHitRatio()
    {
        final int totalHits = suggestedExclusiveCacheHits + cacheAndSuggestedCacheHits;
        final int totalHitsAndMisses = totalHits + suggestedCacheMisses;
        suggestedCacheHitRatio = totalHitsAndMisses < 1
                                    ? 0.0
                                    : totalHits * 100 / totalHitsAndMisses;
    }

    private void removeLastIfQueueFull() {
        if(recentHitsAndMissesQueue.size() > 9)
            recentHitsAndMissesQueue.poll();
    }

    /** first app user */
    public static final int AID_APP = 10000;

    /** offset for uid ranges for each user */
    public static final int AID_USER = 100000;

    private String computeForegroundApp()
    {
        File[] files = new File(getString(R.string.topLevelFilePath)).listFiles();
        int lowestOomScore = Integer.MAX_VALUE;
        String foregroundProcess = null;

        for (File file : files) {
            if (!file.isDirectory()) {
                continue;
            }

            int pid;
            try {
                pid = Integer.parseInt(file.getName());
            } catch (NumberFormatException e) {
                continue;
            }

            try {
                String cgroup = read(String.format(getString(R.string.cgroupFilePath), pid));

                String[] lines = cgroup.split("\n");

                if (lines.length != 2) {
                    continue;
                }

                String cpuSubsystem = lines[0];
                String cpuaccctSubsystem = lines[1];

                if (!cpuaccctSubsystem.endsWith(Integer.toString(pid))) {
                    // not an application process
                    continue;
                }

                if (cpuSubsystem.endsWith(getString(R.string.bg_non_interactive))) {
                    // background policy
                    continue;
                }

                String cmdline = read(String.format(getString(R.string.cmdlineFilePath), pid));

                if (cmdline.contains(getString(R.string.systemUIProcessName))) {
                    continue;
                }

                int uid = Integer.parseInt(
                        cpuaccctSubsystem.split(getString(R.string.COLON))[2].split(getString(R.string.FORWARD_SLASH))[1]
                                .replace(getString(R.string.uid_), getString(R.string.EMPTY)));
                if (uid >= 1000 && uid <= 1038) {
                    // system process
                    continue;
                }

                int appId = uid - AID_APP;
                int userId = 0;
                // loop until we get the correct user id.
                // 100000 is the offset for each user.
                while (appId > AID_USER) {
                    appId -= AID_USER;
                    userId++;
                }

                if (appId < 0) {
                    continue;
                }

                // u{user_id}_a{app_id} is used on API 17+ for multiple user account support.
                // String uidName = String.format("u%d_a%d", userId, appId);

                File oomScoreAdj = new File(String.format(getString(R.string.oomScoreAdjFilePath), pid));
                if (oomScoreAdj.canRead()) {
                    int oomAdj = Integer.parseInt(read(oomScoreAdj.getAbsolutePath()));
                    if (oomAdj != 0) {
                        continue;
                    }
                }

                int oomscore = Integer.parseInt(read(String.format(getString(R.string.oomScoreFilePath), pid)));
                if (oomscore < lowestOomScore) {
                    lowestOomScore = oomscore;
                    foregroundProcess = cmdline;
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        String result = null;

        if(foregroundProcess != null)
            result = foregroundProcess.replaceAll("[^a-z.]+", "");

        return result;
    }

    private static String read(String path) throws IOException
    {
        StringBuilder output = new StringBuilder();
        BufferedReader reader = new BufferedReader(new FileReader(path));
        output.append(reader.readLine());
        for (String line = reader.readLine(); line != null; line = reader.readLine()) {
            output.append('\n').append(line);
        }
        reader.close();
        return output.toString();
    }

    private void addIrrelevantAppsToSet(Set<String> appsThatDontAddToCacheMetrics)
    {
        appsThatDontAddToCacheMetrics.add(getString(R.string.myApp));
        appsThatDontAddToCacheMetrics.add(getString(R.string.cacheMetricsApp));
        appsThatDontAddToCacheMetrics.add(getString(R.string.homeScreen));
        appsThatDontAddToCacheMetrics.add(getString(R.string.acoreProcess));
        appsThatDontAddToCacheMetrics.add(getString(R.string.boxSearchApp));
    }

    private String customStringFormatForQueue(Queue<String> queue)
    {
        StringBuilder result = new StringBuilder();

        for(String hitOrMiss : queue)
            result.append(hitOrMiss).append("\n");

        return result.toString();
    }

    private String customStringFormatForSet(Set<String> set)
    {
        StringBuilder builder = new StringBuilder("");
        for(String item : set)
            builder.append(item).append("\n");
        return builder.toString();
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

    private Set<String> computeRunningApplications()
    {
        Set<String> runningApplicationsNames = new HashSet<>();

        List<ProcessManager.Process> runningApplications = ProcessManager.getRunningApplications();

        for(ProcessManager.Process process : runningApplications)
        {
            String processName = process.name;
            runningApplicationsNames.add(removeColonsUnderscoresAndDigits(processName));
        }

        return runningApplicationsNames;
    }

    private String removeColonsUnderscoresAndDigits(String inputString)
    {
        inputString = inputString.replace(":", "");
        inputString = inputString.replace("_", "");
        inputString = inputString.replace("0", "");
        inputString = inputString.replace("1", "");

        return inputString;
    }

    public String getListOfProcessesInCache()
    {
        return listOfProcessesInCache;
    }

    public String getListOfSuggestedApplications()
    {
        return listOfSuggestedApplications;
    }

    List<String> getAllInstalledApplications()
    {
        final PackageManager packageManager = getPackageManager();

        List<ApplicationInfo> packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);

        List<String> packageNames = new ArrayList<>();
        for(ApplicationInfo applicationPackage : packages)
            packageNames.add(applicationPackage.packageName);

        return packageNames;
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
