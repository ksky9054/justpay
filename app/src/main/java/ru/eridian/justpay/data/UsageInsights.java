package ru.eridian.justpay.data;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Выводы из статистики, которые нужны и экрану «Аналитика», и уведомлениям. */
public final class UsageInsights {
    private UsageInsights() {}

    /** Сколько дней без активности считается «давно не используется». */
    public static final int IDLE_DAYS = 14;

    /**
     * Активные подписки, которым не меньше days дней и по которым столько же дней нет активности.
     * Самые дорогие в месяц идут первыми. Только что добавленную подписку неиспользуемой не считаем.
     */
    public static List<Subscription> idle(List<Subscription> subs,
                                          Map<Long, Map<LocalDate, UsageDay>> usage,
                                          LocalDate today, int days) {
        List<Subscription> out = new ArrayList<>();
        for (Subscription s : subs) {
            if (!s.isActive() || ChronoUnit.DAYS.between(s.createdAt, today) < days) continue;
            Map<LocalDate, UsageDay> m = usage.containsKey(s.id) ? usage.get(s.id) : new HashMap<>();
            Set<LocalDate> active = UsageMetrics.activeDays(m);
            LocalDate last = UsageMetrics.lastUse(active);
            if (last == null || ChronoUnit.DAYS.between(last, today) >= days) out.add(s);
        }
        Collections.sort(out, (x, y) -> Double.compare(y.monthly(), x.monthly()));
        return out;
    }

    public static double monthlySum(List<Subscription> subs) {
        double sum = 0;
        for (Subscription s : subs) sum += s.monthly();
        return sum;
    }
}
