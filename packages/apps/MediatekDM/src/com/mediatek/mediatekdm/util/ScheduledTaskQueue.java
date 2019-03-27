
package com.mediatek.mediatekdm.util;

import android.util.Log;

import com.mediatek.mediatekdm.DmConst.TAG;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ScheduledTaskQueue {
    private static final int QUEUE_CAPACITY = 1;

    private ScheduledThreadPoolExecutor mThreadPool;
    private List<ScheduledFuture<?>> mQueuedTasks;
    private Object mLock;

    public ScheduledTaskQueue() {
        mThreadPool = new ScheduledThreadPoolExecutor(QUEUE_CAPACITY);
        mQueuedTasks = new ArrayList<ScheduledFuture<?>>();
        mLock = new Object();
    }

    public void addPendingTask(Runnable task, long delayMS) {
        synchronized (mLock) {
            ScheduledFuture<?> schedTask = mThreadPool.schedule(task, delayMS,
                    TimeUnit.MILLISECONDS);
            mQueuedTasks.add(schedTask);
        }
    }

    public void removeAll() {
        synchronized (mLock) {
            for (ScheduledFuture<?> schedTask : mQueuedTasks) {
                schedTask.cancel(false);
            }
            mQueuedTasks.clear();
            Log.d(TAG.DEBUG,
                    "++ [removing all] ++, thread pool size = " + mThreadPool.getTaskCount());
        }
    }

    public void dump() {
        Log.d(TAG.DEBUG, "---- dumping sched task Q ----");
        synchronized (mLock) {
            for (ScheduledFuture<?> schedTask : mQueuedTasks) {
                long delayMS = schedTask.getDelay(TimeUnit.MILLISECONDS);
                Log.d(TAG.DEBUG, "[sched-task] delays-->" + delayMS);
            }
        }
        Log.d(TAG.DEBUG, "---- dumping finished ----");
    }
}
