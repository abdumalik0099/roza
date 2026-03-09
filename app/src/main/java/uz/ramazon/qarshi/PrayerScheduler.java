package uz.ramazon.qarshi;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import java.util.Calendar;

public class PrayerScheduler {

    static final String TAG = "PrayerScheduler";
    static final String PREFS = "namoz_prefs";

    /**
     * Bugungi va ertangi namoz alarmlarini o'rnatadi
     */
    public static void scheduleAll(Context ctx) {
        cancelAll(ctx);

        Calendar now = Calendar.getInstance();
        int today[] = PrayerTimes.getTodayTimes();

        // Bugungi qolgan namozlar uchun alarm
        setAlarmsForDay(ctx, now, today, false);

        // Ertangi namozlar
        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DAY_OF_YEAR, 1);
        int[] tmrTimes = PrayerTimes.getTimesForDate(
            tomorrow.get(Calendar.YEAR),
            tomorrow.get(Calendar.MONTH)+1,
            tomorrow.get(Calendar.DAY_OF_MONTH)
        );
        setAlarmsForDay(ctx, tomorrow, tmrTimes, true);

        Log.d(TAG, "Alarmlar o'rnatildi");
    }

    static void setAlarmsForDay(Context ctx, Calendar day, int[] times, boolean isTomorrow) {
        Calendar now = Calendar.getInstance();
        int nowMin = now.get(Calendar.HOUR_OF_DAY)*60 + now.get(Calendar.MINUTE);

        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        // Har 6 namoz uchun alarm
        String[] prefKeys = {"notif_bomdod","notif_quyosh","notif_peshin","notif_asr","notif_shom","notif_xufton"};
        for (int i = 0; i < 6; i++) {
            if (!prefs.getBoolean(prefKeys[i], true)) continue; // o'chirilgan bo'lsa skip

            int prayerMin = times[i];
            if (!isTomorrow && prayerMin <= nowMin) continue; // o'tib ketgan

            Calendar alarmTime = (Calendar) day.clone();
            alarmTime.set(Calendar.HOUR_OF_DAY, prayerMin / 60);
            alarmTime.set(Calendar.MINUTE, prayerMin % 60);
            alarmTime.set(Calendar.SECOND, 0);
            alarmTime.set(Calendar.MILLISECOND, 0);

            setAlarm(ctx, alarmTime.getTimeInMillis(), i*10 + (isTomorrow?100:0), 0, i);
        }

        // Saharlikka 30 daqiqa (faqat Ramazon oyida)
        Calendar cal = day;
        int mo = cal.get(Calendar.MONTH)+1;
        int d  = cal.get(Calendar.DAY_OF_MONTH);
        boolean isRamazon = (mo==2 && d>=19) || (mo==3 && d<=20);

        if (isRamazon) {
            if (prefs.getBoolean("notif_sahar30", true)) {
                int s30 = times[0] - 30;
                if (s30 > 0 && (isTomorrow || s30 > nowMin)) {
                    Calendar t = (Calendar) day.clone();
                    t.set(Calendar.HOUR_OF_DAY, s30/60); t.set(Calendar.MINUTE, s30%60);
                    t.set(Calendar.SECOND, 0); t.set(Calendar.MILLISECOND, 0);
                    setAlarm(ctx, t.getTimeInMillis(), 200+(isTomorrow?10:0), 1, -1);
                }
            }
            if (prefs.getBoolean("notif_sahar10", true)) {
                int s10 = times[0] - 10;
                if (s10 > 0 && (isTomorrow || s10 > nowMin)) {
                    Calendar t = (Calendar) day.clone();
                    t.set(Calendar.HOUR_OF_DAY, s10/60); t.set(Calendar.MINUTE, s10%60);
                    t.set(Calendar.SECOND, 0); t.set(Calendar.MILLISECOND, 0);
                    setAlarm(ctx, t.getTimeInMillis(), 201+(isTomorrow?10:0), 2, -1);
                }
            }
            if (prefs.getBoolean("notif_iftor15", true)) {
                int i15 = times[4] - 15;
                if (isTomorrow || i15 > nowMin) {
                    Calendar t = (Calendar) day.clone();
                    t.set(Calendar.HOUR_OF_DAY, i15/60); t.set(Calendar.MINUTE, i15%60);
                    t.set(Calendar.SECOND, 0); t.set(Calendar.MILLISECOND, 0);
                    setAlarm(ctx, t.getTimeInMillis(), 202+(isTomorrow?10:0), 3, -1);
                }
            }
            if (prefs.getBoolean("notif_iftor5", true)) {
                int i5 = times[4] - 5;
                if (isTomorrow || i5 > nowMin) {
                    Calendar t = (Calendar) day.clone();
                    t.set(Calendar.HOUR_OF_DAY, i5/60); t.set(Calendar.MINUTE, i5%60);
                    t.set(Calendar.SECOND, 0); t.set(Calendar.MILLISECOND, 0);
                    setAlarm(ctx, t.getTimeInMillis(), 203+(isTomorrow?10:0), 4, -1);
                }
            }
        }
    }

    static void setAlarm(Context ctx, long timeMs, int reqCode, int alarmType, int prayerIndex) {
        try {
            AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
            Intent i = new Intent(ctx, AlarmReceiver.class);
            i.putExtra("alarm_type", alarmType);
            i.putExtra("prayer_index", prayerIndex);
            int flags = Build.VERSION.SDK_INT >= 23
                ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                : PendingIntent.FLAG_UPDATE_CURRENT;
            PendingIntent pi = PendingIntent.getBroadcast(ctx, reqCode, i, flags);

            if (Build.VERSION.SDK_INT >= 23) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeMs, pi);
            } else if (Build.VERSION.SDK_INT >= 19) {
                am.setExact(AlarmManager.RTC_WAKEUP, timeMs, pi);
            } else {
                am.set(AlarmManager.RTC_WAKEUP, timeMs, pi);
            }
        } catch (Exception e) {
            Log.e(TAG, "Alarm o'rnatishda xato: " + e.getMessage());
        }
    }

    public static void cancelAll(Context ctx) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        // Barcha mumkin bo'lgan alarm ID larini bekor qilamiz
        for (int id = 0; id < 250; id++) {
            try {
                Intent i = new Intent(ctx, AlarmReceiver.class);
                int flags = Build.VERSION.SDK_INT >= 23
                    ? PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
                    : PendingIntent.FLAG_NO_CREATE;
                PendingIntent pi = PendingIntent.getBroadcast(ctx, id, i, flags);
                if (pi != null) am.cancel(pi);
            } catch (Exception ignored) {}
        }
    }

    /**
     * Ertangi alarmlarni o'rnatish (har kuni kechqurun chaqiriladi)
     */
    public static void scheduleNextDay(Context ctx) {
        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DAY_OF_YEAR, 1);
        int[] times = PrayerTimes.getTimesForDate(
            tomorrow.get(Calendar.YEAR),
            tomorrow.get(Calendar.MONTH)+1,
            tomorrow.get(Calendar.DAY_OF_MONTH)
        );
        setAlarmsForDay(ctx, tomorrow, times, true);
    }
}
