package uz.ramazon.qarshi;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;

import java.util.Calendar;

public class AlarmReceiver extends BroadcastReceiver {

    static final String[] PRAYER_NAMES = {"Bomdod", "Quyosh", "Peshin", "Asr", "Shom", "Xufton"};
    static final String[] PRAYER_ICONS = {"🌙", "☀️", "🌤️", "🌅", "🌆", "🌃"};
    static final String CH_PRAYER = "namoz_kanal";
    static final String CH_RAMAZON = "ramazon_kanal";

    @Override
    public void onReceive(Context ctx, Intent intent) {
        int prayerIndex = intent.getIntExtra("prayer_index", -1);
        int alarmType   = intent.getIntExtra("alarm_type", 0);
        // 0=namoz vaqti, 1=sahar30, 2=sahar10, 3=iftor15, 4=iftor5

        createChannels(ctx);

        switch (alarmType) {
            case 0: // Namoz vaqti
                if (prayerIndex >= 0 && prayerIndex < 6) {
                    String icon  = PRAYER_ICONS[prayerIndex];
                    String name  = PRAYER_NAMES[prayerIndex];
                    int[] times  = PrayerTimes.getTodayTimes();
                    String vaqt  = PrayerTimes.formatTime(times[prayerIndex]);
                    sendNotif(ctx, 100+prayerIndex,
                        icon + " " + name + " namozi kirdi!",
                        vaqt + " · Alloh qabul qilsin 🤲",
                        CH_PRAYER, Color.parseColor("#17857e"), false);
                }
                break;
            case 1: // Saharlikka 30 daqiqa
                sendNotif(ctx, 200,
                    "⏰ Saharlikka 30 daqiqa qoldi!",
                    "Uxlab qolmang · Saharlik vaqti yaqinlashmoqda",
                    CH_RAMAZON, Color.parseColor("#f0cc70"), true);
                break;
            case 2: // Saharlikka 10 daqiqa
                sendNotif(ctx, 201,
                    "⚠️ Saharlik tugaydi!",
                    "10 daqiqa qoldi · Tezroq!",
                    CH_RAMAZON, Color.parseColor("#f0cc70"), true);
                break;
            case 3: // Iftorlikka 15 daqiqa
                sendNotif(ctx, 202,
                    "🌙 Iftorlikka 15 daqiqa qoldi!",
                    "Tayyorlaning · Iftor duo: Allohuma laka sumtu...",
                    CH_RAMAZON, Color.parseColor("#d4a843"), true);
                break;
            case 4: // Iftorlikka 5 daqiqa
                sendNotif(ctx, 203,
                    "🌙 Iftorlikka 5 daqiqa qoldi!",
                    "Og'iz ochishga tayyorlaning!",
                    CH_RAMAZON, Color.parseColor("#d4a843"), true);
                break;
        }

        // Ertangi alarmlarni rejalashtirish
        PrayerScheduler.scheduleNextDay(ctx);
    }

    void sendNotif(Context ctx, int id, String title, String body, String channel, int color, boolean bigVibrate) {
        try {
            NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            Intent openApp = new Intent(ctx, MainActivity.class);
            openApp.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            int piFlags = Build.VERSION.SDK_INT >= 23
                ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                : PendingIntent.FLAG_UPDATE_CURRENT;
            PendingIntent pi = PendingIntent.getActivity(ctx, id, openApp, piFlags);

            Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(ctx, channel)
                : new Notification.Builder(ctx);

            b.setSmallIcon(android.R.drawable.ic_dialog_info)
             .setContentTitle(title)
             .setContentText(body)
             .setStyle(new Notification.BigTextStyle().bigText(body))
             .setContentIntent(pi)
             .setAutoCancel(true)
             .setWhen(System.currentTimeMillis())
             .setShowWhen(true);

            if (Build.VERSION.SDK_INT >= 21) b.setColor(color);
            if (Build.VERSION.SDK_INT < 26) {
                b.setPriority(Notification.PRIORITY_MAX);
                b.setVibrate(bigVibrate
                    ? new long[]{0, 300, 150, 300, 150, 600}
                    : new long[]{0, 200, 100, 200, 100, 400});
            }

            nm.notify(id, b.build());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void createChannels(Context ctx) {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);

            NotificationChannel c1 = new NotificationChannel(CH_PRAYER, "Namoz Vaqtlari", NotificationManager.IMPORTANCE_HIGH);
            c1.enableLights(true); c1.setLightColor(Color.parseColor("#17857e"));
            c1.enableVibration(true); c1.setVibrationPattern(new long[]{0,200,100,200,100,400});
            c1.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

            NotificationChannel c2 = new NotificationChannel(CH_RAMAZON, "Ramazon Eslatmalari", NotificationManager.IMPORTANCE_HIGH);
            c2.enableLights(true); c2.setLightColor(Color.parseColor("#f0cc70"));
            c2.enableVibration(true); c2.setVibrationPattern(new long[]{0,300,150,300,150,600});
            c2.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

            nm.createNotificationChannel(c1);
            nm.createNotificationChannel(c2);
        }
    }
}
