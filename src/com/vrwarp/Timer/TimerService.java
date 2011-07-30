package com.vrwarp.Timer;

import java.util.Calendar;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.Vibrator;
import android.provider.Settings;

public class TimerService extends Service {
	public final static String DURATION = "com.vrwarp.Timer.DURATION";
	public final static String KILLID = "com.vrwarp.Timer.KILLID";
	public final static String WAKEUP = "com.vrwarp.Timer.WAKEUP";
	
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

			// setup a wake up call
			if(duration > 2 * (MINUTE / SECOND)) {
				Context context = getApplicationContext();
				Intent wakeIntent = new Intent(context, AlarmReceiver.class);

				PendingIntent sender = PendingIntent.getBroadcast(context, 0, wakeIntent, PendingIntent.FLAG_UPDATE_CURRENT);
				
				Calendar cal = Calendar.getInstance();
				cal.add(Calendar.SECOND, (int)duration - 30);
				AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
				am.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), sender);
			}
		}
		else if(bundle.containsKey(KILLID)) {
			if(mTimer != null) {
				mTimer.abort();
				mTimer = null;
				stopForeground(true);
				stopSelf(startId);
			}
		}
		else if(bundle.containsKey(WAKEUP)) {
			if(mTimer != null) {
				mTimer.interrupt();
			}
		}

		return START_NOT_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
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
			// create a wake lock for sub 2 minute count down
			PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "delta-timer");

			long delta = mDuration - (System.currentTimeMillis() - mStart);
			while(delta > 0) {
				UpdateNotification(delta);

				try {
					if(delta > 3 * HOUR) {
						Thread.sleep(HOUR);
					}
					else if(delta > 30 * MINUTE) {
						Thread.sleep(15 * MINUTE);
					}
					else if(delta > 2 * MINUTE) {
						Thread.sleep(MINUTE);
					}
					else {
						if(!wl.isHeld())
							wl.acquire();
						
						if(delta > 15 * SECOND) {
							Thread.sleep(5 * SECOND);
						}
						else {
							Thread.sleep(200 /* milliseconds */);
						}
					}
				} catch (InterruptedException e) {
					// do nothing
				}

				// check to see if the timer was aborted
				if(!mAlive) {
					break;
				}

				delta = mDuration - (System.currentTimeMillis() - mStart);
			}
			
			if(mAlive) {
				final Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				final MediaPlayer mediaPlayer = new MediaPlayer();
				OnCompletionListener listener = new OnCompletionListener() {
					@Override
					public void onCompletion(MediaPlayer mp) {
						mp.release();
						vibrator.cancel();
	
				        Intent serviceIntent = new Intent(getApplicationContext(), TimerService.class).putExtra(TimerService.KILLID, mId);
				        startService(serviceIntent);
					}
				};
	
				try {
					mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
					mediaPlayer.setVolume(1.0f, 1.0f);
					mediaPlayer.setLooping(false);
					mediaPlayer.setOnCompletionListener(listener);
					mediaPlayer.setDataSource(getApplicationContext(), Settings.System.DEFAULT_ALARM_ALERT_URI);
					mediaPlayer.prepare();
					mediaPlayer.start();
	
					long pattern[] = {0,1000,500,1000};
					vibrator.vibrate(pattern, 2);
				} catch (Exception e) {
					listener.onCompletion(mediaPlayer);
				}
			}
			
			if(wl.isHeld())
				wl.release();
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
			if(delta > 3 * HOUR) {
				long hours = (delta + HOUR/2) / HOUR;
				msg = "About " + hours + (hours>1?" hours":" hour");
			}
			else if(delta > HOUR) {
				long hours = (delta + HOUR/2) / HOUR;
				long minutes = ((delta - hours * HOUR) + MINUTE/2) / MINUTE;
				msg = "About " + hours + (hours>1?" hours":" hour") + " and " + minutes + (minutes>1?" minutes":" minute");
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

			Notification n = new Notification(R.drawable.icon, "Timer set", System.currentTimeMillis());
			n.setLatestEventInfo(context, msg, "Timer", contentIntent);
			n.flags = Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR;
			
			mService.startForeground(mId, n);
		}
	}
}
