package com.example.wilson.pushnotification;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import java.io.IOException;

/**
 * Created by Wilson on 2015/5/14.
 */
public class MagicLenGCM {
    // ----------類別常數(須自行修改)----------
    /**
     * Google Developers Console 的 Project Number
     */
    public final static String SENDER_ID = "Please enter your Project Number";

    // ----------類別列舉----------
    public static enum PlayServicesState {
        SUPPROT, NEED_PLAY_SERVICE, UNSUPPORT;
    }

    public static enum GCMState {
        PLAY_SERVICES_NEED_PLAY_SERVICE, PLAY_SERVICES_UNSUPPORT, NEED_REGISTER, AVAILABLE;
    }

    // ----------類別介面----------
    public static interface MagicLenGCMListener {
        /**
         * GCM註冊結束
         *
         * @param successfull
         *            是否註冊成功
         * @param regID
         *            傳回註冊到的regID
         */
        public void gcmRegistered(boolean successfull, String regID);

        /**
         * GCM註冊成功，將結果寫入App Server
         *
         * @param regID
         *            傳回註冊到的regID
         * @return 是否傳送App Server成功
         */
        public boolean gcmSendRegistrationIdToAppServer(String regID);

    }

    // ----------類別常數----------
    /**
     * 用來當作SharedPreferences的Key.
     */
    private static final String PROPERTY_REG_ID = "registration_id";
    private static final String PROPERTY_APP_VERSION = "appVersion";
    /**
     * 使用MagicLenGCM的Activity可以實作這個ActivityResult號碼
     */
    public final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    // ----------類別方法----------
    /**
     * 發出Local端的通知(顯示在通知欄上)
     *
     * @param context
     *            Context
     * @param notifyID
     *            通知ID(重複會被覆蓋)
     * @param drawableSmallIcon
     *            小圖示(用Drawable ID來設定)
     * @param title
     *            標題
     * @param msg
     *            訊息
     * @param info
     *            附加文字
     * @param autoCancel
     *            是否按下後就消失
     * @param pendingIntent
     *            按下後要使用什麼Intent
     */
    public static void sendLocalNotification(Context context, int notifyID,
                                             int drawableSmallIcon, String title, String msg, String info,
                                             boolean autoCancel, PendingIntent pendingIntent) {
        NotificationManager mNotificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
                context).setSmallIcon(drawableSmallIcon).setContentTitle(title)
                .setContentText(msg).setAutoCancel(autoCancel)
                .setContentInfo(info).setDefaults(Notification.DEFAULT_ALL);

        if (msg.length() > 10) {
            mBuilder.setStyle(new NotificationCompat.BigTextStyle()
                    .bigText(msg));
        }
        mBuilder.setContentIntent(pendingIntent);
        mNotificationManager.notify(notifyID, mBuilder.build());
    }

    // ----------物件變數----------
    private Activity activity;
    private MagicLenGCMListener listener;

    // ----------建構子----------
    public MagicLenGCM(Activity activity) {
        this(activity, null);
    }

    public MagicLenGCM(Activity activity, MagicLenGCMListener listener) {
        this.activity = activity;
        setMagicLenGCMListener(listener);
    }

    // ----------物件方法----------
    /**
     * 取得Activity
     *
     * @return 傳回Activity
     */
    public Activity getActivity() {
        return activity;
    }

    public void setMagicLenGCMListener(MagicLenGCMListener listener) {
        this.listener = listener;
    }

    /**
     * 開始接上GCM
     *
     * @return 傳回GCM狀態
     */
    public GCMState startGCM() {
        return openGCM();
    }

    /**
     * 開始接上GCM
     *
     * @return 傳回GCM狀態
     */
    public GCMState openGCM() {
        switch (checkPlayServices()) {
            case SUPPROT:
                String regid = getRegistrationId();
                if (regid.isEmpty()) {
                    registerInBackground();
                    return GCMState.NEED_REGISTER;
                } else {
                    return GCMState.AVAILABLE;
                }
            case NEED_PLAY_SERVICE:
                return GCMState.PLAY_SERVICES_NEED_PLAY_SERVICE;
            default:
                return GCMState.PLAY_SERVICES_UNSUPPORT;
        }
    }

    public String getRegistrationId() {
        final SharedPreferences prefs = getGCMPreferences();
        String registrationId = prefs.getString(PROPERTY_REG_ID, "");
        if (registrationId.isEmpty()) {
            return "";
        }
        // 檢查程式是否有更新過
        int registeredVersion = prefs.getInt(MagicLenGCM.PROPERTY_APP_VERSION,
                Integer.MIN_VALUE);
        int currentVersion = getAppVersion();
        if (registeredVersion != currentVersion) {
            return "";
        }
        return registrationId;
    }

    public int getAppVersion() {
        try {
            PackageInfo packageInfo = activity.getPackageManager()
                    .getPackageInfo(activity.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            // 不可能會發生
            throw new RuntimeException("Could not get package name: " + e);
        }
    }

    private SharedPreferences getGCMPreferences() {
        return activity.getSharedPreferences(activity.getClass()
                .getSimpleName(), Context.MODE_PRIVATE);
    }

    /**
     * 檢查Google Play Service可用狀態
     *
     * @return 傳回Google Play Service可用狀態
     */
    public PlayServicesState checkPlayServices() {
        int resultCode = GooglePlayServicesUtil
                .isGooglePlayServicesAvailable(activity);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, activity,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
                return PlayServicesState.NEED_PLAY_SERVICE;
            } else {
                return PlayServicesState.UNSUPPORT;
            }
        }
        return PlayServicesState.SUPPROT;
    }

    /**
     * 在背景註冊GCM
     */
    private void registerInBackground() {
        new AsyncTaskRegister().execute();
    }

    private final class AsyncTaskRegister extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... params) {
            String regid = "";
            try {
                GoogleCloudMessaging gcm = GoogleCloudMessaging
                        .getInstance(activity);
                regid = gcm.register(SENDER_ID);

                if (regid == null || regid.isEmpty()) {
                    return "";
                }

                // 儲存regID
                storeRegistrationId(regid);

                if (listener != null) {
                    if (!listener.gcmSendRegistrationIdToAppServer(regid)) {
                        storeRegistrationId("");
                        return "";
                    }
                }
            } catch (IOException ex) {

            }
            return regid;
        }

        @Override
        protected void onPostExecute(String msg) {
            if (listener != null) {
                listener.gcmRegistered(!msg.isEmpty(), msg.toString());
            }
        }
    }

    private void storeRegistrationId(String regId) {
        final SharedPreferences prefs = getGCMPreferences();
        int appVersion = getAppVersion();
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_REG_ID, regId);
        editor.putInt(PROPERTY_APP_VERSION, appVersion);
        editor.commit();
    }
}
