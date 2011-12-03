package com.vrwarp.Timer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AlarmReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
        Intent serviceIntent = new Intent(context, TimerService.class).putExtra(TimerService.WAKEUP, intent.getAction());
        context.startService(serviceIntent);
	}

}