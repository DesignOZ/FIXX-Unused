package com.tistory.overimagine.voltecalllogfix.Util;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.tistory.overimagine.voltecalllogfix.R;


public class AutoFixService extends Service {
    public AutoFixService() {
    }

    private static final String TAG = "AutoFixService";
    private TelephonyManager manager;

    CallLogUtil mCallLogUtil;
    PhoneUtil mPhoneUtil;

    @Override
    public void onCreate() {
        manager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        manager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        Log.d(TAG, "onCreate: Start PhoneStateListener");

        mCallLogUtil = new CallLogUtil(this);
        mPhoneUtil = new PhoneUtil(this);
//        StartNotification();
        startForeground(0, new Notification());
        super.onCreate();
    }

//    private void StartNotification() {
//        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext());
//
//        builder.setContentTitle("자동 변환 서비스가 시작되었습니다.")
//                .setContentText(mPhoneUtil.getSimSlotStatus(false))
//                .setSmallIcon(R.drawable.noti_icon)
//                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
//                .setAutoCancel(true)
//                .setWhen(System.currentTimeMillis())
//                .setDefaults(Notification.DEFAULT_ALL)
//                .setCategory(Notification.CATEGORY_MESSAGE)
//                .setPriority(Notification.PRIORITY_HIGH)
//                .setVisibility(Notification.VISIBILITY_PUBLIC);
//        Log.d(TAG, "onCreate: Create Notification");
//
//        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//        nm.notify(0002, builder.build());
//        Log.d(TAG, "onCreate: Notify");
//
//        if (mCallLogUtil.getErrorLog() != 0)
//            mCallLogUtil.run(false);
//    }


    private PhoneStateListener phoneStateListener = new PhoneStateListener() {
        public void onCallStateChanged(int state, String incomingNumber) {
            Log.i(TAG, "onCallStateChanged: " + incomingNumber);
            if (state == TelephonyManager.CALL_STATE_IDLE)
                if (!incomingNumber.isEmpty()) {
                    try {
                        Thread.sleep(3000);
                        if (mCallLogUtil.getErrorLog() != 0)
                            mCallLogUtil.run(true);

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Log.d(TAG, "onCallStateChanged: " + incomingNumber);

                }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

}
