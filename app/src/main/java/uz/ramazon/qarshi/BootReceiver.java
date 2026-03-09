package uz.ramazon.qarshi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Telefon yoqilganda alarmlarni qayta o'rnatadi
 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context ctx, Intent intent) {
        String action = intent.getAction();
        if (action == null) return;

        if (action.equals(Intent.ACTION_BOOT_COMPLETED)
            || action.equals("android.intent.action.QUICKBOOT_POWERON")
            || action.equals(Intent.ACTION_MY_PACKAGE_REPLACED)) {

            Log.d("BootReceiver", "Telefon yoqildi — alarmlar qayta o'rnatilmoqda");
            AlarmReceiver.createChannels(ctx);
            PrayerScheduler.scheduleAll(ctx);
        }
    }
}
