package id.ac.ukdw.securedoor.adapters;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;

import id.ac.ukdw.securedoor.MainActivity;
import id.ac.ukdw.securedoor.R;

public class NotificationHelper {
    public static int PIN_REQUEST = 1;

    private Context mContext;
    private NotificationManager mNotificationManger;
    private NotificationCompat.Builder mBuilder;
    private int notificationId;
    public static final String NOTIFICATION_CHANNEL_ID = "80000";

    public NotificationHelper(Context mContext, int notificationId) {
        this.mContext = mContext;
        this.notificationId = notificationId;
    }

    /**
     * Building the notification
     *
     * @param title String
     * @param body String
     */
    public void buildNotification(String title, String body){
        //Create an intent as the intent when the notification is opened
        Intent resultIntent = new Intent(mContext, MainActivity.class);
        resultIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        //Put the created intent to the pending intent
        PendingIntent resultPendingIntent = PendingIntent.getActivity(mContext, 0, resultIntent, PendingIntent.FLAG_ONE_SHOT);

        mBuilder = new NotificationCompat.Builder(mContext, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setAutoCancel(true)
                .setSound(Settings.System.DEFAULT_NOTIFICATION_URI)
                .setContentIntent(resultPendingIntent);

        mNotificationManger = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "Notification", importance);
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.GREEN);
            mBuilder.setChannelId(NOTIFICATION_CHANNEL_ID);

            mNotificationManger.createNotificationChannel(notificationChannel);
        }
    }

    /**
     * Send the build notification
     *
     */
    public void send() {
        mNotificationManger.notify(notificationId, mBuilder.build());
    }
}
