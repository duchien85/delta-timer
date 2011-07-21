package com.vrwarp.Timer;

import kankan.wheel.widget.WheelView;
import kankan.wheel.widget.adapters.NumericWheelAdapter;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

public class TimerActivity extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
}