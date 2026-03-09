package uz.ramazon.qarshi;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.os.IBinder;

/**
 * Fonda ishlaydigan xizmat
 * Ilova yopilsa ham alarmlar ishlaydi
 */
public class PrayerService extends Service {

    static final int SERVICE_ID = 999;

    @Override
    public void onCreate() {
        super.onCreate();
        AlarmReceiver.createChannels(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Foreground notification (foydalanuvchi bilishi uchun)
        Notification notif = buildServiceNotification();
        startForeground(SERVICE_ID, notif);

        // Alarmlarni o'rnatamiz
        PrayerScheduler.scheduleAll(this);

        // Xizmat o'ldirilsa qayta ishga tushadi
        return START_STICKY;
    }

    Notification buildServiceNotification() {
        Intent openApp = new Intent(this, MainActivity.class);
        openApp.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        android.app.PendingIntent pi;
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            pi = android.app.PendingIntent.getActivity(this, 0, openApp,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE);
        } else {
            pi = android.app.PendingIntent.getActivity(this, 0, openApp,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT);
        }

        Notification.Builder b;
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            b = new Notification.Builder(this, AlarmReceiver.CH_PRAYER);
        } else {
            b = new Notification.Builder(this);
            b.setPriority(Notification.PRIORITY_MIN);
        }

        b.setSmallIcon(android.R.drawable.ic_dialog_info)
         .setContentTitle("🕌 Namoz Vaqtlari — Qarshi")
         .setContentText("Namoz bildirganomalar yoqilgan ✅")
         .setContentIntent(pi)
         .setOngoing(true)
         .setShowWhen(false);

        if (android.os.Build.VERSION.SDK_INT >= 21) {
            b.setColor(Color.parseColor("#17857e"));
        }

        return b.build();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Qayta ishga tushirish
        Intent restart = new Intent(this, PrayerService.class);
        startService(restart);
    }
}
