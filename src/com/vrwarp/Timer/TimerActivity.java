package com.vrwarp.Timer;

import kankan.wheel.widget.WheelView;
import kankan.wheel.widget.adapters.NumericWheelAdapter;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class TimerActivity extends Activity {
	private ServiceConnection mConnection;
    
    private void displayMain() {
        setContentView(R.layout.main);
        
        int wraps = 5;
        String[] sixty = new String[60*wraps];
        for(int i = 0; i < 60*wraps; i++) {
        	int n = i%60;
        	if(n < 10)
        		sixty[i] = "0"+n;
        	else
        		sixty[i] = ""+n;
        }

        final WheelView hoursView = (WheelView)findViewById(R.id.hours);
        NumericWheelAdapter hours = new NumericWheelAdapter(this, 0, 24, "%02d");
        hours.setTextSize(36);
        hoursView.setViewAdapter(hours);
        hoursView.setCyclic(true);

        final WheelView minutesView = (WheelView)findViewById(R.id.minutes);
        NumericWheelAdapter minutes = new NumericWheelAdapter(this, 0, 59, "%02d");
        minutes.setTextSize(36);
        minutesView.setViewAdapter(minutes);
        minutesView.setCyclic(true);

        final WheelView secondsView = (WheelView)findViewById(R.id.seconds);
        NumericWheelAdapter seconds = new NumericWheelAdapter(this, 0, 59, "%02d");
        seconds.setTextSize(36);
        secondsView.setViewAdapter(seconds);
        secondsView.setCyclic(true);
        
        Button startButton = (Button)findViewById(R.id.start);
        startButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				long duration = (hoursView.getCurrentItem() * 60 /*min/hour*/ * 60 /*sec/min*/) +
				                (minutesView.getCurrentItem() * 60 /*sec/min*/) +
				                (secondsView.getCurrentItem());
		        Intent serviceIntent = new Intent(getApplicationContext(), TimerService.class).putExtra(TimerService.DURATION, duration);
		        startService(serviceIntent);
		        
		        finish();
			}
		});
    }
    
    private void displayTerminator() {
        setContentView(R.layout.terminator);

        Button confirmButton = (Button)findViewById(R.id.confirm);
        confirmButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
		        Intent serviceIntent = new Intent(getApplicationContext(), TimerService.class).putExtra(TimerService.KILLID, true);
		        startService(serviceIntent);

		        displayMain();
			}
		});

        Button cancelButton = (Button)findViewById(R.id.cancel);
        cancelButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
}
    
    @Override
    protected void onDestroy() {
        unbindService(mConnection);
        super.onDestroy();
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName className,
                    IBinder service) {
                // We've bound to LocalService, cast the IBinder and get LocalService instance
                TimerService.LocalBinder binder = (TimerService.LocalBinder) service;

                if(binder.getService().isRunning())
                	displayTerminator();
                else
                	displayMain();
            }

            public void onServiceDisconnected(ComponentName arg0) {
            	finish();
            }
        };
        
        Intent intent = new Intent(this, TimerService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }
}