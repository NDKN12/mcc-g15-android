package com.services;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import androidVNC.VncCanvasActivity;
import fi.aalto.openoranges.project1.mcc.MainActivity;


public class MyService extends Service {

    IBinder mBinder;
    private BroadcastReceiver mReceiver = null;

    @Override
    public void onCreate(){
        //Initializing SCREEN_OFF Listener
        final IntentFilter mfilter = new IntentFilter(Intent.ACTION_SCREEN_OFF);

        mReceiver = new ScreenReceiver();
        registerReceiver(mReceiver, mfilter);

        super.onCreate();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {

        //close notification
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(1);

        //stop service
        stopSelf();

        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onDestroy(){
        //close notification
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(1);

        super.onDestroy();
    }

    public class ScreenReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(final Context context, final Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                mNotificationManager.cancel(1);
            } else {
            }
        }

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
}
