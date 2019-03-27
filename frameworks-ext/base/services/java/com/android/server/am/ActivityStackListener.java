package com.android.server.am;
/**
* Use to dump activityStack when ActivityStack has changed.
*/
import java.util.ArrayList;
import android.util.Log;

public class ActivityStackListener{
    private static final String TAG = "ActivityStackListener";
    final ArrayList<ArrayList> mHistorys = new ArrayList<ArrayList>(15);
    private volatile boolean isRun = true;

    public class DumpHistoryThread implements Runnable{
        public DumpHistoryThread(){}
        @Override
        public void run(){
            while(isRun){
                dumpHistory();
            }
        }
    }

    public ActivityStackListener(){
        new Thread(new DumpHistoryThread()).start();
    }

    public void closeStackListener(){
        isRun = false;
    }

    public synchronized void dumpHistory(){
        while(mHistorys.size() == 0){
            try{
                wait();
            }catch(Exception e){
                e.printStackTrace();
            }
        }
        Log.v(TAG,"Dump History Start:");
        ArrayList mHistory = mHistorys.get(0);
        for(int i=mHistory.size()-1; i>=0; i--){
            ActivityRecord ar = (ActivityRecord)mHistory.get(i);
            Log.v(TAG,"realActivity: "+ar.realActivity);
            Log.v(TAG,"packageName: "+ar.packageName);
            Log.v(TAG,"task: "+ar.task.realActivity);
            Log.v(TAG,"intent: "+ar.intent);
            Log.v(TAG,"launchMode: "+ar.launchMode);
            Log.v(TAG,"state: "+ar.state);
            Log.v(TAG,"taskAffinity: "+ar.taskAffinity);
            Log.v(TAG,"haveState: "+ar.haveState);
            Log.v(TAG,"finishing: "+ar.finishing);
            Log.v(TAG,"visible: "+ar.visible);
            Log.v(TAG,"waitingVisible: "+ar.waitingVisible);
            Log.v(TAG,"nowVisible: "+ar.nowVisible);
        }
        Log.v(TAG,"Dump History End.");
        mHistorys.remove(0);
    }

    public synchronized void dumpStack(ArrayList mHistory)
    {
        mHistorys.add(mHistory);
        if(mHistorys.size() == 1){
            notifyAll();
        }
    }
}
