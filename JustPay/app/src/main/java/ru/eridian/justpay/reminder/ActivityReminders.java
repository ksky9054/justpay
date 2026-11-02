package ru.eridian.justpay.reminder;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ru.eridian.justpay.R;
import ru.eridian.justpay.data.Account;
import ru.eridian.justpay.data.AccountRepository;
import ru.eridian.justpay.data.Prefs;
import ru.eridian.justpay.data.Subscription;
import ru.eridian.justpay.data.SubscriptionRepository;
import ru.eridian.justpay.data.UsageDay;
import ru.eridian.justpay.data.UsageInsights;
import ru.eridian.justpay.data.UsageMetrics;
import ru.eridian.justpay.data.UsageRepository;
import ru.eridian.justpay.ui.MainActivity;
import ru.eridian.justpay.usage.UsageSync;
import ru.eridian.justpay.util.Fmt;

/**
 * Два напоминания об активности:
 * <ul>
 *   <li>вечером (20:00), если сегодня нет ни одной отметки и серию ещё можно потерять;</li>
 *   <li>раз в неделю в 12:00 о подписках, которыми не пользуются больше двух недель.</li>
 * </ul>
 * Условия проверяются в момент срабатывания, а не при планировании: к вечеру отметка могла появиться.
 * Для каждого напоминания свой неточный будильник; после срабатывания он ставится на следующий день.
 */
public final class ActivityReminders {
    public static final String ACTION_STREAK = "ru.eridian.justpay.action.STREAK";
    public static final String ACTION_IDLE = "ru.eridian.justpay.action.IDLE";

    private static final String CHANNEL_ID = "activity";
    private static final int STREAK_CODE = 2;
    private static final int IDLE_CODE = 3;
    private static final int STREAK_NOTIFICATION = 9001;
    private static final int IDLE_NOTIFICATION = 9002;

    private static final int STREAK_HOUR = 20;
    private static final int STREAK_LATE_UNTIL = 23;
    private static final int IDLE_HOUR = 12;
    private static final int IDLE_LATE_UNTIL = 20;
    /** Не чаще одного напоминания о неиспользуемых подписках в неделю. */
    private static final int IDLE_EVERY_DAYS = 7;

    private ActivityReminders() {}

    /** Ставит или снимает оба будильника в зависимости от входа в аккаунт и настроек. */
    static void schedule(Context context) {
        Context app = context.getApplicationContext();
        AlarmManager am = (AlarmManager) app.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        Prefs prefs = new Prefs(app);
        set(am, app, STREAK_CODE, ACTION_STREAK, prefs.isSignedIn() && prefs.isStreakRemind(),
                prefs.streakChecked(), STREAK_HOUR, STREAK_LATE_UNTIL);
        set(am, app, IDLE_CODE, ACTION_IDLE, prefs.isSignedIn() && prefs.isIdleRemind(),
                prefs.idleChecked(), IDLE_HOUR, IDLE_LATE_UNTIL);
    }

    private static void set(AlarmManager am, Context app, int code, String action, boolean enabled,
                            String checkedIso, int hour, int lateUntil) {
        PendingIntent pi = alarm(app, code, action);
        if (!enabled) {
            am.cancel(pi);
            return;
        }
        LocalDate checked = checkedIso.isEmpty() ? null : LocalDate.parse(checkedIso);
        long at = DailySchedule.next(LocalDateTime.now(), ZoneId.systemDefault(), checked, hour, lateUntil);
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
    }

    private static PendingIntent alarm(Context app, int code, String action) {
        Intent i = new Intent(app, ReminderReceiver.class).setAction(action);
        return PendingIntent.getBroadcast(app, code, i,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    // ----- вечернее напоминание про серию -----

    static void notifyStreak(Context app) {
        Prefs prefs = new Prefs(app);
        if (!prefs.isSignedIn() || !prefs.isStreakRemind()) return;
        LocalDate today = LocalDate.now();
        // Отмечаем проверку сразу: иначе при отсутствии причин для уведомления будильник сработал бы снова
        prefs.setStreakChecked(today.toString());

        refreshUsage(app);
        Map<Long, Map<LocalDate, UsageDay>> usage = UsageRepository.get(app).allForAccount();
        Set<LocalDate> any = UsageMetrics.activeDaysAny(usage.values());
        if (any.contains(today)) return; // сегодня уже пользовались, серия продолжена
        int streak = UsageMetrics.currentStreak(any, today);
        if (streak <= 0) return; // терять нечего

        applyCurrency(app, prefs);
        post(app, STREAK_NOTIFICATION, new Notification.Builder(app, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_pay)
                .setContentTitle("Серия под угрозой: " + streak + " " + Fmt.pl(streak, "день", "дня", "дней"))
                .setContentText("Отметьте, что сегодня пользовались подпиской, иначе серия прервётся.")
                .setContentIntent(openApp(app, 2, STREAK_NOTIFICATION))
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_REMINDER));
    }

    // ----- напоминание о неиспользуемых подписках -----

    static void notifyIdle(Context app) {
        Prefs prefs = new Prefs(app);
        if (!prefs.isSignedIn() || !prefs.isIdleRemind()) return;
        LocalDate today = LocalDate.now();
        prefs.setIdleChecked(today.toString());

        String sent = prefs.idleSent();
        if (!sent.isEmpty() && ChronoUnit.DAYS.between(LocalDate.parse(sent), today) < IDLE_EVERY_DAYS) return;

        refreshUsage(app);
        List<Subscription> idle = UsageInsights.idle(SubscriptionRepository.get(app).all(),
                UsageRepository.get(app).allForAccount(), today, UsageInsights.IDLE_DAYS);
        if (idle.isEmpty()) return;

        applyCurrency(app, prefs);
        StringBuilder names = new StringBuilder();
        for (int i = 0; i < Math.min(3, idle.size()); i++) {
            if (i > 0) names.append(", ");
            names.append(idle.get(i).name);
        }
        if (idle.size() > 3) names.append(" и ещё ").append(idle.size() - 3);
        String text = "Больше двух недель без активности: " + names + ". Они стоят "
                + Fmt.money(UsageInsights.monthlySum(idle)) + " в месяц. Подумайте о паузе.";

        prefs.setIdleSent(today.toString());
        post(app, IDLE_NOTIFICATION, new Notification.Builder(app, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_pay)
                .setContentTitle("Подписки без активности: " + idle.size())
                .setContentText(text)
                .setStyle(new Notification.BigTextStyle().bigText(text))
                .setContentIntent(openApp(app, 2, IDLE_NOTIFICATION))
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_RECOMMENDATION));
    }

    // ----- общее -----

    /** Подтягиваем свежую статистику приложений: вдруг сегодня уже пользовались, а JustPay не открывали. */
    private static void refreshUsage(Context app) {
        try {
            UsageSync.sync(app);
        } catch (RuntimeException ignored) {
            // Статистика необязательна: сбой чтения не должен отменять напоминание
        }
    }

    private static void applyCurrency(Context app, Prefs prefs) {
        Account acc = AccountRepository.get(app).find(prefs.accountId());
        Fmt.setCurrency(acc == null ? null : acc.currencyValue());
    }

    private static PendingIntent openApp(Context app, int tab, int requestCode) {
        Intent open = new Intent(app, MainActivity.class)
                .putExtra(MainActivity.EXTRA_TAB, tab)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(app, requestCode, open,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private static void post(Context app, int id, Notification.Builder builder) {
        NotificationManager nm = (NotificationManager) app.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null || !nm.areNotificationsEnabled()) return;
        NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "Серия и активность",
                NotificationManager.IMPORTANCE_DEFAULT);
        ch.setDescription("Напоминания про серию использования и подписки, которыми давно не пользуются");
        nm.createNotificationChannel(ch);
        nm.notify(id, builder.build());
    }
}
