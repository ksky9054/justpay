package ru.eridian.justpay.reminder;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

import ru.eridian.justpay.R;
import ru.eridian.justpay.data.Prefs;
import ru.eridian.justpay.data.Subscription;
import ru.eridian.justpay.data.SubscriptionRepository;
import ru.eridian.justpay.ui.DetailActivity;
import ru.eridian.justpay.util.Fmt;

/**
 * Напоминания о предстоящих списаниях. Заводится один будильник на ближайшее напоминание;
 * когда он срабатывает, показываются уведомления и будильник переставляется на следующее.
 * Точные будильники не нужны, поэтому специальное разрешение не требуется.
 */
public final class ReminderScheduler {
    public static final String ACTION_REMIND = "ru.eridian.justpay.action.REMIND";

    private static final String CHANNEL_ID = "payments";
    private static final int REMIND_HOUR = 9;
    private static final int ALARM_CODE = 1;

    private ReminderScheduler() {}

    /** Пересчитывает время ближайшего напоминания и переставляет будильник. */
    public static void reschedule(Context context) {
        Context app = context.getApplicationContext();
        AlarmManager am = (AlarmManager) app.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        PendingIntent pi = alarmIntent(app);
        am.cancel(pi);
        long at = nextTrigger(app);
        if (at > 0) am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
        // Вечернее напоминание про серию и еженедельное про неиспользуемые подписки
        ActivityReminders.schedule(app);
    }

    private static PendingIntent alarmIntent(Context app) {
        Intent i = new Intent(app, ReminderReceiver.class).setAction(ACTION_REMIND);
        return PendingIntent.getBroadcast(app, ALARM_CODE, i,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    static String key(Subscription s) {
        return s.id + ":" + s.next;
    }

    /** Напоминание ещё не отправлено, а списание впереди или сегодня. */
    private static boolean pending(Subscription s, Prefs prefs, Set<String> done, LocalDate today) {
        LocalDate rd = s.remindDate(prefs);
        return rd != null && !s.next.isBefore(today) && !done.contains(key(s));
    }

    /** Время ближайшего напоминания в миллисекундах или -1, если напоминать не о чем. */
    static long nextTrigger(Context app) {
        Prefs prefs = new Prefs(app);
        Set<String> done = prefs.notified();
        LocalDate today = LocalDate.now();
        long now = System.currentTimeMillis();
        long best = -1;
        for (Subscription s : SubscriptionRepository.get(app).all()) {
            if (!s.isActive() || s.next == null || !pending(s, prefs, done, today)) continue;
            long ms = s.remindDate(prefs).atTime(REMIND_HOUR, 0)
                    .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            // Время напоминания уже прошло (например, подписку добавили поздно): шлём сразу.
            if (ms <= now) ms = now + 5000;
            if (best < 0 || ms < best) best = ms;
        }
        return best;
    }

    /** Показывает уведомления по всем подпискам, у которых напоминание наступило. */
    static void notifyDue(Context app) {
        Prefs prefs = new Prefs(app);
        // Приёмник запускается без экранов: валюту аккаунта подставляем сами
        ru.eridian.justpay.data.Account acc = ru.eridian.justpay.data.AccountRepository.get(app).find(prefs.accountId());
        Fmt.setCurrency(acc == null ? null : acc.currencyValue());
        Set<String> done = prefs.notified();
        LocalDate today = LocalDate.now();
        List<Subscription> all = SubscriptionRepository.get(app).all();
        NotificationManager nm = (NotificationManager) app.getSystemService(Context.NOTIFICATION_SERVICE);
        boolean canPost = nm != null && nm.areNotificationsEnabled();
        if (canPost) ensureChannel(nm);

        for (Subscription s : all) {
            if (!s.isActive() || s.next == null || !pending(s, prefs, done, today)) continue;
            if (s.remindDate(prefs).isAfter(today)) continue;
            // Отмечаем в любом случае, иначе при выключенных уведомлениях будильник сработал бы повторно.
            done.add(key(s));
            if (canPost) nm.notify((int) s.id, build(app, s, today));
        }
        prefs.setNotified(done);
    }

    private static Notification build(Context app, Subscription s, LocalDate today) {
        long d = Fmt.diff(s.next, today);
        String when = d == 0 ? "Сегодня" : d == 1 ? "Завтра"
                : "Через " + d + " " + Fmt.pl((int) d, "день", "дня", "дней");

        Intent open = new Intent(app, DetailActivity.class)
                .putExtra(DetailActivity.EXTRA_ID, s.id)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent tap = PendingIntent.getActivity(app, (int) s.id, open,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        return new Notification.Builder(app, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_pay)
                .setContentTitle(when + " спишется " + s.name)
                .setContentText(Fmt.price(s.price) + " · " + Fmt.dayLong(s.next) + (s.hasCard() ? " · " + s.card : ""))
                .setContentIntent(tap)
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_REMINDER)
                .build();
    }

    private static void ensureChannel(NotificationManager nm) {
        NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "Напоминания о платежах",
                NotificationManager.IMPORTANCE_DEFAULT);
        ch.setDescription("Уведомления о предстоящих списаниях по подпискам");
        nm.createNotificationChannel(ch);
    }
}
