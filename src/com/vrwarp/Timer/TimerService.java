package com.vrwarp.Timer;

import java.util.HashMap;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.Toast;

public class TimerService extends Service {
	public final static String DURATION = "com.vrwarp.Timer.DURATION";
	public final static String KILL_ID = "com.vrwarp.Timer.KILL_ID";
	
	private Timer mTimer = null;

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Bundle bundle = intent.getExtras();
		if(bundle.containsKey(DURATION)) {
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
		// We don't provide binding, so return null
		return null;
	}

	@Override
	public void onDestroy() {
		Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show(); 
	}
	
	private class Timer extends Thread {
		private long mDuration;
		private long mStart;
		private int mId;
		private boolean mAlive;
		private NotificationManager mManager;
		private TimerService mService;

		public Timer(TimerService service, int id, long duration) {
			mDuration = duration * 1000;
			mStart = System.currentTimeMillis();
			mId = id;
			mAlive = true;
			mManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			mService = service;
		}

		@Override
		public void run() {
			long delta = mDuration - (System.currentTimeMillis() - mStart);
			while(delta > 0) {
				UpdateNotification(delta);

				try {
					if(delta > 2 /* hours */ * 60 * 60 * 1000) {
						Thread.sleep(60 /* minutes */ * 60 * 1000);
					}
					else if(delta > 2 /* minutes */ * 60 * 1000) {
						Thread.sleep(60 /* seconds */ * 60 * 1000);
					}
					else {
						Thread.sleep(200 /* milliseconds */);
					}
				} catch (InterruptedException e) {
					// do nothing
				}

				// check to see if the timer was aborted
				if(!mAlive) {
					mManager.cancel(mId);
					return;
				}

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
			if(delta > 60 /* minutes */ * 60 * 1000) {
				long hours = delta / (60 * 60 * 1000);
				msg = hours + (hours>1?" hours":" hour");
			}
			else if(delta > 60 /* seconds */ * 1000) {
				long minutes = delta / (60 * 1000);
				msg = minutes + (minutes>1?" minutes":" minute");
			}
			else {
				long seconds = delta / (1000);
				msg = seconds + (seconds>1?" seconds":" second");
			}

			Context context = getApplicationContext();
			
			Intent intent = new Intent(context, TimerService.class);
			intent.putExtra(KILL_ID, mId);

			PendingIntent contentIntent = PendingIntent.getService(context, 0, intent, 0);

			Notification n = new Notification(R.drawable.icon, msg, System.currentTimeMillis());
			n.setLatestEventInfo(context, msg, "Timer", contentIntent);
			n.flags = Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR;
			
			mService.startForeground(mId, n);
		}
	}
}
