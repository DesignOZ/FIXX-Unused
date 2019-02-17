package com.tistory.overimagine.voltecalllogfix.Util;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.tistory.overimagine.voltecalllogfix.R;

public class AutoStart extends BroadcastReceiver {
    private static final String TAG = "AutoStart";

    Context context;
    PhoneUtil mPhoneUtil;

    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;

        mPhoneUtil = new PhoneUtil(context);

        String action = intent.getAction();
        Log.d(TAG, "onReceive: " + action);
        //수신된 action값이 시스템의 '부팅 완료'가 맞는지 확인..

        if (action.equals("android.intent.action.BOOT_COMPLETED")) {
            Intent serviceIntent = new Intent(context, AutoFixService.class);
            context.startService(serviceIntent);
            Log.d(TAG, "onReceive: AutoStart!");
            StartNotification();
        }
//        throw new UnsupportedOperationException("Not yet implemented");
    }

    private void StartNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);

        builder.setContentTitle("자동 변환 서비스가 시작되었습니다.")
                .setContentText(mPhoneUtil.getSimSlotStatus(false))
                .setSmallIcon(R.drawable.noti_icon)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher))
                .setAutoCancel(true)
                .setWhen(System.currentTimeMillis())
                .setDefaults(Notification.DEFAULT_ALL)
                .setCategory(Notification.CATEGORY_MESSAGE)
                .setPriority(Notification.PRIORITY_HIGH)
                .setVisibility(Notification.VISIBILITY_PUBLIC);
        Log.d(TAG, "onCreate: Create Notification");

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(0002, builder.build());
        Log.d(TAG, "onCreate: Notify");

    }
}
