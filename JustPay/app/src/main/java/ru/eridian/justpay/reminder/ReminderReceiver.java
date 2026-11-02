package ru.eridian.justpay.reminder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Срабатывает от будильников: показывает нужное уведомление и ставит следующий будильник. */
public class ReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (ReminderScheduler.ACTION_REMIND.equals(action)) {
            ReminderScheduler.notifyDue(context);
        } else if (ActivityReminders.ACTION_STREAK.equals(action)) {
            ActivityReminders.notifyStreak(context);
        } else if (ActivityReminders.ACTION_IDLE.equals(action)) {
            ActivityReminders.notifyIdle(context);
        } else {
            return;
        }
        ReminderScheduler.reschedule(context);
    }
}
