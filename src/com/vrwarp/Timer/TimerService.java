package com.vrwarp.Timer;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.Toast;

public class TimerService extends Service {
	public final static String DURATION = "com.vrwarp.Timer.DURATION";
	public final static String KILL_ID = "com.vrwarp.Timer.KILL_ID";
	
	private final static long SECOND = 1000;
	private final static long MINUTE = 60 * SECOND;
	private final static long HOUR = 60 * MINUTE;
	
	private Timer mTimer = null;
    private final IBinder mBinder = new LocalBinder();

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Bundle bundle = intent.getExtras();
		if(bundle.containsKey(DURATION)) {
			if(mTimer != null)
				mTimer.abort();
			long duration = intent.getExtras().getLong(DURATION);
			mTimer = new Timer(this, startId, duration);
			mTimer.setPriority(Thread.MIN_PRIORITY);
			mTimer.start();
		}
		else if(bundle.containsKey(KILL_ID)) {
			mTimer.abort();
			mTimer = null;
			stopForeground(true);
		}

		return START_NOT_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public void onDestroy() {
		Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show(); 
	}
	
	public boolean isRunning() {
		return mTimer != null;
	}
	
	public class LocalBinder extends Binder {
        TimerService getService() {
            // Return this instance of LocalService so clients can call public methods
            return TimerService.this;
        }
    }
	
	private class Timer extends Thread {
		private long mDuration;
		private long mStart;
		private int mId;
		private boolean mAlive;
		private TimerService mService;

		public Timer(TimerService service, int id, long duration) {
			mDuration = duration * 1000;
			mStart = System.currentTimeMillis();
			mId = id;
			mAlive = true;
			mService = service;
		}

		@Override
		public void run() {
			long delta = mDuration - (System.currentTimeMillis() - mStart);
			while(delta > 0) {
				UpdateNotification(delta);

				try {
					if(delta > 2 * HOUR) {
						Thread.sleep(HOUR);
					}
					else if(delta > 2 * MINUTE) {
						Thread.sleep(MINUTE);
					}
					else {
						Thread.sleep(200 /* milliseconds */);
					}
				} catch (InterruptedException e) {
					// do nothing
				}

				// check to see if the timer was aborted
				if(!mAlive)
					return;

				delta = mDuration - (System.currentTimeMillis() - mStart);
			}
		}

		public void abort() {
			mAlive = false;
			interrupt();
			while(isAlive()) {
				try {
					join();
				} catch (InterruptedException e) {
					// do nothing
				}
			}
		}

		private void UpdateNotification(long delta) {
			String msg;
			if(delta > HOUR) {
				long hours = (delta + HOUR/2) / HOUR;
				msg = "About " + hours + (hours>1?" hours":" hour");
			}
			else if(delta > MINUTE) {
				long minutes = (delta + MINUTE/2) / MINUTE;
				msg = "About " + minutes + (minutes>1?" minutes":" minute");
			}
			else {
				long seconds = delta / (1000);
				msg = seconds + (seconds>1?" seconds":" second");
			}

			Context context = getApplicationContext();
			
			Intent intent = new Intent(context, TimerActivity.class);
			PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intent, 0);

			Notification n = new Notification(R.drawable.icon, msg, System.currentTimeMillis());
			n.setLatestEventInfo(context, msg, "Timer", contentIntent);
			n.flags = Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR;
			
			mService.startForeground(mId, n);
		}
	}
}
