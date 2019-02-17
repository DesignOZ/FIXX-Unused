package com.tistory.overimagine.voltecalllogfix;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.provider.CallLog;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.tistory.overimagine.voltecalllogfix.Util.AutoFixService;
import com.tistory.overimagine.voltecalllogfix.Util.CallLogUtil;
import com.tistory.overimagine.voltecalllogfix.Util.PhoneUtil;

import static android.widget.Toast.LENGTH_SHORT;
import static android.widget.Toast.makeText;

public class MainActivity extends PreferenceActivity implements Preference.OnPreferenceClickListener {

    private static final String TAG = "MainActivity";

    public static boolean isMultiSimEnabled = false;

    private int calllog_size;
    private int errorlog_size;          // 오류항목 개수

    private Preference btn_easteregg;
    private int egg_toast = 0;
    Toast easter = null;
    private Preference btn_number;
    private Preference btn_fix;
    private Preference btn_autofix;

    PhoneUtil mPhoneUtil;
    CallLogUtil mCallLogUtil;

    private Uri UriCalls; // 대소문자에 주의.
    private Cursor c;
    private ContentValues values;

    SharedPreferences settings;
    SharedPreferences.Editor editor;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.mainactivity);

        mPhoneUtil = new PhoneUtil(this);
        isMultiSimEnabled = mPhoneUtil.isMultiSimEnabled();

        mCallLogUtil = new CallLogUtil(this);

        settings = getSharedPreferences("setting", 0);
        editor = settings.edit();

        UriCalls = Uri.parse("content://call_log/calls"); // 대소문자에 주의.
        c = getContentResolver().query(UriCalls, null, null, null, null);
        values = new ContentValues();

        btn_easteregg = findPreference("easteregg");
        btn_easteregg.setOnPreferenceClickListener(this);

        btn_number = findPreference("number");
        btn_number.setSummary(mPhoneUtil.getSimSlotStatus(true));

        btn_fix = findPreference("fix");
        btn_fix.setOnPreferenceClickListener(this);

        btn_autofix = (Preference) findPreference("autofix");
        btn_autofix.setOnPreferenceClickListener(this);

        // 아직 미구현

        Preference btn_incoming = (Preference) findPreference("incomingcall");
        btn_incoming.setOnPreferenceClickListener(this);

//        if (isExistApp("com.tistory.overimagine.fixxdonate")) {
//            PreferenceCategory category_AD = (PreferenceCategory) findPreference("Category_AD");
//            Preference AD = findPreference("AD");
//            category_AD.removePreference(AD);
//        }

    }


    @Override
    public void onResume() {
        egg_toast = 0;

        calllog_size = mCallLogUtil.getCallLogSize();
        errorlog_size = mCallLogUtil.getErrorLog();

        if (calllog_size == 0)
            btn_fix.setSummary(R.string.calllog_null);
        else if (calllog_size > 0 & errorlog_size != 0)
            btn_fix.setSummary(String.format(getString(R.string.calllog_summary), calllog_size, errorlog_size));
        else
            btn_fix.setSummary(R.string.calllog_notfound);

        if (!isServiceRunningCheck())
            btn_autofix.setTitle(getString(R.string.auto_fix));

        else
            btn_autofix.setTitle(getString(R.string.auto_fix) + " (실행중)");
        super.onResume();
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        switch (preference.getKey()) {
            case "easteregg":
                if (egg_toast < 6) {
                    egg_toast++;
                    if (easter != null)
                        easter.cancel();
                    easter = makeText(this, "이스터에그를 열기위해선 " + (7 - egg_toast) + "회 더 눌러야합니다.", LENGTH_SHORT);
                    easter.show();
                } else {
                    startActivity(new Intent(this, EasterEgg.class));
                    easter.cancel();
                }

                break;
            case "fix":
                if (errorlog_size != 0) {
                    AlertDialog.Builder Line1builder = new AlertDialog.Builder(this);

                    // 여기서 부터는 알림창의 속성 설정
                    Line1builder.setTitle("주의")        // 제목 설정
                            .setMessage(getString(R.string.caution_summary))        // 메세지 설정
                            .setCancelable(true)        // 뒤로 버튼 클릭시 취소 가능 설정
                            .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                                // 확인 버튼 클릭시 설정
                                public void onClick(DialogInterface dialog, int whichButton) {
//                                    mCallLogUtil.run(true);
                                    FixTask fixTask = new FixTask();
                                    fixTask.execute();
                                }
                            })
                            .setNegativeButton("취소", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    dialog.cancel();
                                }
                            });

                    AlertDialog Line1dialog = Line1builder.create();    // 알림창 객체 생성
                    Line1dialog.show();    // 알림창 띄우기
                } else makeText(this, "통화목록 중 오류 항목이 없습니다.", LENGTH_SHORT).show();
                break;
            case "autofix":
                if (!isServiceRunningCheck()) {
                    Intent serviceIntent = new Intent(this, AutoFixService.class);
                    startService(serviceIntent);
                    StartNotification();
                    btn_autofix.setTitle(getString(R.string.auto_fix) + " (실행중)");
                    btn_autofix.setEnabled(true);
                    editor.putBoolean("auto_service", true);
                    editor.commit();
                } else makeText(this, "서비스가 이미 실행중입니다.", LENGTH_SHORT).show();
                break;
            case "incomingcall":
                makeText(this, "아직 구현되지 않았습니다.", LENGTH_SHORT).show();
                break;
        }

        return false;
    }

    private boolean isExistApp(String packageName) {
        PackageManager pm = getPackageManager();
        try {
            pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public boolean isServiceRunningCheck() {
        ActivityManager manager = (ActivityManager) this.getSystemService(Activity.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if ("com.tistory.overimagine.voltecalllogfix.Util.AutoFixService".equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

//    출처:http://itmir.tistory.com/326 [미르의 IT 정복기]
//    public void getErrorLog() {
//        c = getContentResolver().query(UriCalls, null, null, null, null);
//        errorlog_size = 0;
//        if (calllog_size > 0)
//            if (c.moveToFirst()) {
//                do {
//                    String num = c.getString(c.getColumnIndex(CallLog.Calls.NUMBER));
//                    if (num.length() >= 21 & num.contains(defaultSimNumber))
//                        errorlog_size++;
//                } while (c.moveToNext());
//            }
//
//        if (calllog_size == 0)
//            btn_fix.setSummary(R.string.calllog_null);
//        else if (calllog_size > 0 & errorlog_size != 0)
//            btn_fix.setSummary(String.format(getString(R.string.calllog_summary), calllog_size, errorlog_size));
//        else
//            btn_fix.setSummary(R.string.calllog_notfound);
//
//    }

//    public void getNumber() {
//        // 전화번호 설정.
//
////        getApplicationContext();
//        TelephonyManager telephonyManager = (TelephonyManager) getApplicationContext().getSystemService(TELEPHONY_SERVICE);
//
//        subInfoList = mSubscriptionManager.getActiveSubscriptionInfoList();
//        if (subInfoList.size() > 1) {
//            isMultiSimEnabled = true;
//        }
//
//        for (SubscriptionInfo subscriptionInfo : subInfoList) {
//            Numbers.add(subscriptionInfo.getNumber());
//            SimSlots.add("SIM " + (subscriptionInfo.getSimSlotIndex() + 1));
//            IMEIs.add(telephonyManager.getDeviceId());
//
//        }
//        // (String) num_sim1 : SIM1 전화번호
//        // (String) sim1 : SIM1  -  SIM1 전화번호
//
//        if (isMultiSimEnabled) {
//
//            if (Numbers.get(0).length() > 11)
//                num_sim1 = "010" + Numbers.get(0).substring(Numbers.get(0).length() - 8, Numbers.get(0).length());
//            else
//                num_sim1 = Numbers.get(0);
//
//
//            if (Numbers.get(1).length() > 11)
//                num_sim2 = "010" + Numbers.get(1).substring(Numbers.get(1).length() - 8, Numbers.get(1).length());
//            else
//                num_sim2 = Numbers.get(1);
//
//            sim1 = SimSlots.get(0) + "  -  " + num_sim1;
//            sim2 = SimSlots.get(1) + "  -  " + num_sim2;
//            Log.i(TAG, "getNumber: " + sim1);
//            Log.i(TAG, "getNumber: " + sim2);
//
//            btn_number.setSummary(sim1 + "\n" + sim2);
////
////            if (telephonyManager.getDeviceId().equals(IMEIs.get(0))) {
////                defaultSim = sim1;
////                defaultSimNumber = num_sim1;
////                btn_default.setSummary(sim1);
////                btn_number.setSummary(sim1 + "\n" + sim2);
////                Log.d(TAG, "defaultSIM: " + defaultSim);
////            } else {
////                defaultSim = sim2;
////                defaultSimNumber = num_sim2;
////                btn_default.setSummary(sim2);
////                btn_number.setSummary(sim1 + "\n" + sim2);
////                Log.d(TAG, "defaultSIM: " + defaultSim);
////            }
//
//        } else {
//            // 싱글심일 경우
//            if (Numbers.get(0).length() > 11)
//                defaultSimNumber = "010" + Numbers.get(0).substring(Numbers.get(0).length() - 8, Numbers.get(0).length());
//            else
//                defaultSimNumber = Numbers.get(0);
//
//            defaultSim = SimSlots.get(0) + "  -  " + defaultSimNumber;
//            btn_number.setSummary(defaultSim);
//            Log.d(TAG, "defaultSIM: " + defaultSim);
//
//        }
//
//    }

//    private void deleteLog(Cursor cursor) {
//        String _ID = cursor.getString(cursor.getColumnIndex(CallLog.Calls._ID));
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
//            // TODO: Consider calling
//            //    ActivityCompat#requestPermissions
//            // here to request the missing permissions, and then overriding
//            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//            //                                          int[] grantResults)
//            // to handle the case where the user grants the permission. See the documentation
//            // for ActivityCompat#requestPermissions for more details.
//            return;
//        }
//        getContentResolver().delete(
//                CallLog.Calls.CONTENT_URI,
//                CallLog.Calls._ID + " =" + _ID, null);
//    }
//
//    private class FixTask extends AsyncTask {
//        int fixlogs = 0;
//        ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);
//
//        @Override
//        protected void onPreExecute() {
//
//            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
//            progressDialog.setMessage("진행중입니다");
//            progressDialog.setCancelable(false);
//            progressDialog.setMax(errorlog_size);
//            progressDialog.show();
//
//            super.onPreExecute();
//        }
//
//        @Override
//        protected Object doInBackground(Object[] objects) {
//            try {
//                if (c.moveToFirst())
//                    do {
//                        String Number = c.getString(c.getColumnIndex(CallLog.Calls.NUMBER));
//                        if (Number.length() >= 21 & Number.contains(defaultSimNumber)) {
//                            String date = c.getString(c.getColumnIndex(CallLog.Calls.DATE));
//                            String Duration = c.getString(c.getColumnIndex(CallLog.Calls.DURATION));
//                            String New = c.getString(c.getColumnIndex(CallLog.Calls.NEW));
//                            String Type = c.getString(c.getColumnIndex(CallLog.Calls.TYPE));
//                            String ID = c.getString(c.getColumnIndex(CallLog.Calls._ID));
//
//                            Log.d(TAG, "Num : " + Number);
//                            deleteLog(c);
//
//                            values.put(CallLog.Calls.DATE, date);
//                            values.put(CallLog.Calls.DURATION, Duration);
//                            values.put(CallLog.Calls.NEW, New);
//                            values.put(CallLog.Calls.NUMBER, Number.replace(defaultSimNumber, ""));
//                            values.put(CallLog.Calls.TYPE, Type);
//                            values.put(CallLog.Calls._ID, ID);
//
//                            if (ActivityCompat.checkSelfPermission(getBaseContext(), Manifest.permission.WRITE_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
//                                // TODO: Consider calling
//                                //    ActivityCompat#requestPermissions
//                                // here to request the missing permissions, and then overriding
//                                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//                                //                                          int[] grantResults)
//                                // to handle the case where the user grants the permission. See the documentation
//                                // for ActivityCompat#requestPermissions for more details.
//
//                                // return;
//                            }
//                            getContentResolver().insert(CallLog.Calls.CONTENT_URI, values);
//
//                            progressDialog.setProgress(fixlogs++);
//                            Thread.sleep(500);
//                        }
//                    } while (c.moveToNext());
//
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//
//            return null;
//        }
//
//        @Override
//        protected void onPostExecute(Object o) {
//            progressDialog.dismiss();
//            AlertDialog.Builder Line1builder = new AlertDialog.Builder(MainActivity.this);
//
//            // 여기서 부터는 알림창의 속성 설정
//            Line1builder.setTitle("완료되었습니다.")        // 제목 설정
//                    .setCancelable(true)        // 뒤로 버튼 클릭시 취소 가능 설정
//                    .setPositiveButton("확인", new DialogInterface.OnClickListener() {
//                        // 확인 버튼 클릭시 설정
//                        public void onClick(DialogInterface dialog, int whichButton) {
//                            c.close();
//                            getErrorLog();
//                        }
//                    });
//
//            AlertDialog Line1dialog = Line1builder.create();    // 알림창 객체 생성
//            Line1dialog.show();    // 알림창 띄우기
//            super.onPostExecute(o);
//        }
//    }

//    // 미사용
//    public void Fix() {
//        if (c.moveToFirst())
//            do {
//                String Number = c.getString(c.getColumnIndex(CallLog.Calls.NUMBER));
//                if (Number.length() >= 21 & Number.contains(num_sim1)) {
//                    String date = c.getString(c.getColumnIndex(CallLog.Calls.DATE));
//                    String Duration = c.getString(c.getColumnIndex(CallLog.Calls.DURATION));
//                    String New = c.getString(c.getColumnIndex(CallLog.Calls.NEW));
//                    String Type = c.getString(c.getColumnIndex(CallLog.Calls.TYPE));
//                    String ID = c.getString(c.getColumnIndex(CallLog.Calls._ID));
//
//                    Log.d(TAG, "Num : " + Number);
//                    deleteLog(c);
//
//                    values.put(CallLog.Calls.DATE, date);
//                    values.put(CallLog.Calls.DURATION, Duration);
//                    values.put(CallLog.Calls.NEW, New);
//                    values.put(CallLog.Calls.NUMBER, Number.replace(defaultSimNumber, ""));
//                    values.put(CallLog.Calls.TYPE, Type);
//                    values.put(CallLog.Calls._ID, ID);
//
//                    if (ActivityCompat.checkSelfPermission(getBaseContext(), Manifest.permission.WRITE_CALL_LOG) != PackageManager.PERMISSION_GRANTED)
//                        ;
//                    getContentResolver().insert(CallLog.Calls.CONTENT_URI, values);
//                }
//            }
//            while (c.moveToNext());
//    }

//    private class AutoFixTask extends AsyncTask {
//
//        @Override
//        protected Object doInBackground(Object[] objects) {
//            CallLogUtil();
//            return null;
//        }
//
//        @Override
//        protected void onPostExecute(Object o) {
//
//            NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext());
//
//            builder.setContentTitle(errorlog_size + "개의 자동 변환을 마쳤습니다.")
////                .setContentText("상태바 드래그시 보이는 서브타이틀")
////                .setTicker("상태바 한줄 메시지")
//                    .setSmallIcon(R.drawable.noti_icon)
//                    .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
//                    .setAutoCancel(true)
//                    .setWhen(System.currentTimeMillis())
//                    .setDefaults(Notification.DEFAULT_ALL)
//                    .setCategory(Notification.CATEGORY_MESSAGE)
//                    .setPriority(Notification.PRIORITY_HIGH)
//                    .setVisibility(Notification.VISIBILITY_PUBLIC);
//
//            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//            nm.notify(0001, builder.build());
//            super.onPostExecute(o);
//        }
//    }
//
//    public class AutoFixService extends Service {
//        public AutoFixService() {
//        }
//
//        @Override
//        public void onCreate() {
//            NotificationCompat.Builder builder = new NotificationCompat.Builder(MainActivity.this);
//
//            builder.setContentTitle("자동 변환 서비스가 시작되었습니다.")
////                .setContentText("상태바 드래그시 보이는 서브타이틀")
////                .setTicker("상태바 한줄 메시지")
//                    .setSmallIcon(R.drawable.noti_icon)
//                    .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
//                    .setAutoCancel(true)
//                    .setWhen(System.currentTimeMillis())
//                    .setDefaults(Notification.DEFAULT_ALL)
//                    .setCategory(Notification.CATEGORY_MESSAGE)
//                    .setPriority(Notification.PRIORITY_HIGH)
//                    .setVisibility(Notification.VISIBILITY_PUBLIC);
//
//            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//            nm.notify(0002, builder.build());
//
//            manager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
//            super.onCreate();
//        }
//
//        @Nullable
//        @Override
//        public IBinder onBind(Intent intent) {
//            return null;
//        }
//    }

}

