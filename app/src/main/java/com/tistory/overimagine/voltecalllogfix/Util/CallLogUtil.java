package com.tistory.overimagine.voltecalllogfix.Util;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.CallLog;
import android.provider.Contacts;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.tistory.overimagine.voltecalllogfix.R;

/**
 * Created by Horyeong Park on 2017-06-12.
 */

public class CallLogUtil {
    private static final String TAG = "FIX!";

    private Context mContext;

    private Uri UriCalls; // 대소문자에 주의.
    private Cursor c;
    private ContentValues values;

    private PhoneUtil mPhoneUtil;
    private String PhoneNumber_1 = null;            //Line1Number
    private String PhoneNumber_2 = null;            //Line2Number
    private boolean isMultiSimEnabled = false;
    private boolean isMissedCall = false;
    private int errorlog_size;
    private String Name = "";

    public CallLogUtil(Context context) {
        mContext = context;

        UriCalls = Uri.parse("content://call_log/calls"); // 대소문자에 주의.
        c = context.getContentResolver().query(UriCalls, null, null, null, null);
        values = new ContentValues();

        mPhoneUtil = new PhoneUtil(context);
        isMultiSimEnabled = mPhoneUtil.isMultiSimEnabled();

        if (isMultiSimEnabled) {
            PhoneNumber_1 = mPhoneUtil.getSIM1Number();
            PhoneNumber_2 = mPhoneUtil.getSIM2Number();
        } else
            PhoneNumber_1 = mPhoneUtil.getLine1Number();


    }

    public int getCallLogSize() {
        return c.getCount();
    }

    public int getErrorLog() {
        errorlog_size = 0;
        c = mContext.getContentResolver().query(UriCalls, null, null, null, null);

        if (c.moveToFirst()) {
            do {
                String num = c.getString(c.getColumnIndex(CallLog.Calls.NUMBER));

                if (num.length() >= 21 && num.contains(PhoneNumber_1))
                    errorlog_size++;

                if (isMultiSimEnabled && num.length() >= 21 && num.contains(PhoneNumber_2))
                    errorlog_size++;

            } while (c.moveToNext());
        }
        Log.i(TAG, "getErrorLog: " + errorlog_size);
        return errorlog_size;
    }


    void run(boolean isMissedCall) {
        this.isMissedCall = isMissedCall;

        AutoFixTask autoFixTask = new AutoFixTask();
        autoFixTask.execute();
    }


    private class AutoFixTask extends AsyncTask {
        int countlogs = 0;

        @Override
        protected Object doInBackground(Object[] objects) {
            if (c.moveToFirst())
                do {
                    String num = c.getString(c.getColumnIndex(CallLog.Calls.NUMBER));
                    if (num.length() >= 21 && num.contains(PhoneNumber_1)) {
                        Fix(c, PhoneNumber_1);
                        countlogs++;
                    }
                    if (isMultiSimEnabled && num.length() >= 21 && num.contains(PhoneNumber_2)) {
                        Fix(c, PhoneNumber_2);
                        countlogs++;
                    }

                }
                while (c.moveToNext());
            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext);
            if (countlogs == 1)
                builder.setContentTitle("부재중 전화")
                        .setContentText(Name)
                        .setSmallIcon(R.drawable.noti_icon)
                        .setLargeIcon(BitmapFactory.decodeResource(mContext.getResources(), R.mipmap.ic_launcher))
                        .setAutoCancel(true)
                        .setWhen(System.currentTimeMillis())
                        .setDefaults(Notification.DEFAULT_ALL)
                        .setCategory(Notification.CATEGORY_MESSAGE)
                        .setPriority(Notification.PRIORITY_HIGH)
                        .setVisibility(Notification.VISIBILITY_PUBLIC);
            else
                builder.setContentTitle(errorlog_size + "개의 자동변환을 마쳤습니다.")
                        .setSmallIcon(R.drawable.noti_icon)
                        .setLargeIcon(BitmapFactory.decodeResource(mContext.getResources(), R.mipmap.ic_launcher))
                        .setAutoCancel(true)
                        .setWhen(System.currentTimeMillis())
                        .setDefaults(Notification.DEFAULT_ALL)
                        .setCategory(Notification.CATEGORY_MESSAGE)
                        .setPriority(Notification.PRIORITY_HIGH)
                        .setVisibility(Notification.VISIBILITY_PUBLIC);
            NotificationManager nm = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
            nm.notify(0001, builder.build());
            super.onPostExecute(o);
        }

    }

    public void Fix(Cursor cursor, String number) {
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.WRITE_CALL_LOG) != PackageManager.PERMISSION_GRANTED)
            ;
        String Number = cursor.getString(cursor.getColumnIndex(CallLog.Calls.NUMBER));
        String date = cursor.getString(cursor.getColumnIndex(CallLog.Calls.DATE));
        String Duration = cursor.getString(cursor.getColumnIndex(CallLog.Calls.DURATION));
        String New = cursor.getString(cursor.getColumnIndex(CallLog.Calls.NEW));
        String Type = cursor.getString(cursor.getColumnIndex(CallLog.Calls.TYPE));
        String ID = cursor.getString(cursor.getColumnIndex(CallLog.Calls._ID));

        Log.d(TAG, "Num: " + Number);
        Log.d(TAG, "New: " + New);
//        deleteLog(c);
        String _ID = cursor.getString(cursor.getColumnIndex(CallLog.Calls._ID));
        mContext.getContentResolver().delete(CallLog.Calls.CONTENT_URI, CallLog.Calls._ID + " =" + _ID, null);
        Log.d(TAG, "Fix: Deleted");

        values.put(CallLog.Calls.DATE, date);
        values.put(CallLog.Calls.DURATION, Duration);
        values.put(CallLog.Calls.NEW, New);
        values.put(CallLog.Calls.NUMBER, Number.replace(number, ""));
        values.put(CallLog.Calls.TYPE, Type);
        values.put(CallLog.Calls._ID, ID);

        mContext.getContentResolver().insert(CallLog.Calls.CONTENT_URI, values);

        if (errorlog_size == 1 & isMissedCall)
            this.Name = getContactName(Number.replace(number, ""));

    }

    private String getContactName(final String phoneNumber) {
        Uri uri;
        String[] projection;
        Uri mBaseUri = Contacts.Phones.CONTENT_FILTER_URL;
        projection = new String[]{android.provider.Contacts.People.NAME};
        try {
            Class<?> c = Class.forName("android.provider.ContactsContract$PhoneLookup");
            mBaseUri = (Uri) c.getField("CONTENT_FILTER_URI").get(mBaseUri);
            projection = new String[]{"display_name"};
        } catch (Exception e) {
        }


        uri = Uri.withAppendedPath(mBaseUri, Uri.encode(phoneNumber));
        Cursor cursor = mContext.getContentResolver().query(uri, projection, null, null, null);

        String contactName = "";

        if (cursor.moveToFirst() & cursor.getCount() != 0) {
            contactName = cursor.getString(0);
        } else contactName = phoneNumber;

        cursor.close();
        cursor = null;

        return contactName;
    }

//    private class FixTask extends AsyncTask {
//        int fixlogs = 0;
//        ProgressDialog progressDialog;
//
//        @Override
//        protected void onPreExecute() {
//            progressDialog = new ProgressDialog(mContext);
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
//            if (c.moveToFirst())
//                do {
//                    String num = c.getString(c.getColumnIndex(CallLog.Calls.NUMBER));
//                    if (num.length() >= 21 && num.contains(PhoneNumber_1)) {
//                        Fix(c, PhoneNumber_1);
//                        progressDialog.setProgress(fixlogs++);
//                        try {
//                            Thread.sleep(300);
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
//                    }
//
//                    if (isMultiSimEnabled && num.length() >= 21 && num.contains(PhoneNumber_2)) {
//                        Fix(c, PhoneNumber_2);
//                        progressDialog.setProgress(fixlogs++);
//                        try {
//                            Thread.sleep(300);
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
//                    }
//                }
//                while (c.moveToNext());
//            return null;
//        }
//
//        @Override
//        protected void onPostExecute(Object o) {
//            progressDialog.dismiss();
//            AlertDialog.Builder Line1builder = new AlertDialog.Builder(mContext);
//
//            // 여기서 부터는 알림창의 속성 설정
//            Line1builder.setTitle("완료되었습니다.")        // 제목 설정
//                    .setCancelable(true)        // 뒤로 버튼 클릭시 취소 가능 설정
//                    .setPositiveButton("확인", new DialogInterface.OnClickListener() {
//                        // 확인 버튼 클릭시 설정
//                        public void onClick(DialogInterface dialog, int whichButton) {
//                            c.close();
//                        }
//                    });
//
//            AlertDialog Line1dialog = Line1builder.create();    // 알림창 객체 생성
//            Line1dialog.show();    // 알림창 띄우기
//
//            savedFixLog(fixlogs);
//            super.onPostExecute(o);
//        }
//    }

//    private void deleteLog(Cursor cursor) {
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
//
//        String _ID = cursor.getString(cursor.getColumnIndex(CallLog.Calls._ID));
//        getContentResolver().delete(CallLog.Calls.CONTENT_URI, CallLog.Calls._ID + " =" + _ID, null);
//    }
}
