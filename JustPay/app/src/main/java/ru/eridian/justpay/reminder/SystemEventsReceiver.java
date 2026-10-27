package ru.eridian.justpay.reminder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Будильники пропадают после перезагрузки, смены часового пояса и обновления приложения,
 * поэтому в этих случаях напоминание заводится заново.
 */
public class SystemEventsReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String a = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(a)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(a)
                || Intent.ACTION_TIMEZONE_CHANGED.equals(a)
                || Intent.ACTION_TIME_CHANGED.equals(a)) {
            ReminderScheduler.reschedule(context);
        }
    }
}
