package com.jzby.vmwork;

import android.app.Application;
import android.os.Environment;
import android.util.Log;

import com.squareup.leakcanary.LeakCanary;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class App extends Application implements Thread.UncaughtExceptionHandler
{
    final static String TAG=App.class.getSimpleName();
    private static String LOG_PATH="";
    private Database database;

    @Override
    public void onCreate() {
        super.onCreate();
        Thread.setDefaultUncaughtExceptionHandler(this);
        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return;
        }
        LeakCanary.install(this);
        String packName=getPackageName();
        LOG_PATH= Environment.getExternalStorageDirectory().getAbsolutePath()+ File.separator+packName;
        Log.i(TAG,"=========LOG_PATH========="+LOG_PATH);
        database = new Database(this);
    }

    @Override
    public void uncaughtException(Thread t, Throwable ex)
    {
        File root=new File(LOG_PATH);
        if(!root.exists())
        {
            root.mkdirs();
        }
        Calendar mCalendar=Calendar.getInstance();
        String fileName=mCalendar.getTimeInMillis()+".log";
        File file=new File(LOG_PATH+File.separator+fileName);
        PrintWriter pw;
        try
        {
            SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            pw = new PrintWriter(new BufferedWriter(new FileWriter(file)));
            ex.printStackTrace(pw);
            pw.println("\r\n\r\n=========="+sdf.format(mCalendar.getTime())+"===========");
            pw.println("===============print by gordanxu==============");
            pw.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public Database getDatabase() {
        return database;
    }
}
