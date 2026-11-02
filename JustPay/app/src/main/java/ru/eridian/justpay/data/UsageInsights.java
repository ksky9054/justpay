package ru.eridian.justpay.data;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Выводы из статистики для аналитики и уведомлений */
public final class UsageInsights {
    private UsageInsights() {}

    /** Через сколько дней без активности подписка считается неиспользуемой */
    public static final int IDLE_DAYS = 14;

    /** Неиспользуемые подписки, дорогие первыми; новые не считаем */
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
