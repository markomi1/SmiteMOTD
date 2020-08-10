package com.marko.smitemotd.debug;

import android.os.Environment;
import android.util.Log;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CrashHandler implements Thread.UncaughtExceptionHandler{
    //From https://stackoverflow.com/a/36910473/13154944
    private final Thread.UncaughtExceptionHandler defaultUEH;
    private final String localPath;

    public CrashHandler(String localPath) {
        this.localPath = localPath;
        //Getting the the default exception handler
        //that's executed when uncaught exception terminates a thread
        this.defaultUEH = Thread.getDefaultUncaughtExceptionHandler();
    }

    public void uncaughtException(Thread t, Throwable e) {

        //Write a printable representation of this Throwable
        //The StringWriter gives the lock used to synchronize access to this writer.
        final Writer stringBuffSync = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(stringBuffSync);
        e.printStackTrace(printWriter);
        String stacktrace = stringBuffSync.toString();
        printWriter.close();

        if (localPath != null) {
            writeToFile(stacktrace);
        }

        //Used only to prevent from any code getting executed.
        // Not needed in this example
        defaultUEH.uncaughtException(t, e);
    }

    private void writeToFile(String currentStacktrace) {
        try {

            //Gets the Android external storage directory & Create new folder Crash_Reports
            File sdCard = Environment.getExternalStorageDirectory();
            File dir = new File (sdCard.getAbsolutePath() + "//Crash_Reports");
            if (!dir.exists()) {
                dir.mkdirs();
            }
            SimpleDateFormat dateFormat = new SimpleDateFormat(
                    "yyyy_MM_dd_HH_mm_ss");
            Date date = new Date();
            String filename = dateFormat.format(date) + ".STACKTRACE";

            // Write the file into the folder
            File reportFile = new File(dir, filename);
            FileOutputStream stream = new FileOutputStream(reportFile);
            stream.write(currentStacktrace.getBytes());
            stream.close();
        } catch (Exception e) {
            Log.e("ExceptionHandler", e.getMessage());
        }
    }

}
