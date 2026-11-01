package ru.eridian.justpay.data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/** Расчёты по статистике: серия, число использований, цена одного */
public final class UsageMetrics {
    private UsageMetrics() {}

    /** Дни, когда пользовались подпиской */
    public static Set<LocalDate> activeDays(Map<LocalDate, UsageDay> days) {
        Set<LocalDate> out = new TreeSet<>();
        for (UsageDay d : days.values()) if (d.isActive()) out.add(d.day);
        return out;
    }

    /** Дни, когда пользовались любой подпиской */
    public static Set<LocalDate> activeDaysAny(Collection<Map<LocalDate, UsageDay>> all) {
        Set<LocalDate> out = new TreeSet<>();
        for (Map<LocalDate, UsageDay> m : all) out.addAll(activeDays(m));
        return out;
    }

    /** Серия дней подряд; если сегодня отметки нет, считаем от вчера */
    public static int currentStreak(Set<LocalDate> active, LocalDate today) {
        LocalDate d = active.contains(today) ? today : today.minusDays(1);
        int n = 0;
        while (active.contains(d)) {
            n++;
            d = d.minusDays(1);
        }
        return n;
    }

    /** Самая длинная серия */
    public static int longestStreak(Set<LocalDate> active) {
        int best = 0, run = 0;
        LocalDate prev = null;
        for (LocalDate d : new TreeSet<>(active)) {
            run = prev != null && prev.plusDays(1).equals(d) ? run + 1 : 1;
            best = Math.max(best, run);
            prev = d;
        }
        return best;
    }

    /** Использований за n дней */
    public static int usesInLast(Map<LocalDate, UsageDay> days, int n, LocalDate today) {
        int sum = 0;
        for (UsageDay d : days.values()) if (inWindow(d.day, n, today)) sum += d.uses();
        return sum;
    }

    /** Секунд в приложении за n дней */
    public static int secondsInLast(Map<LocalDate, UsageDay> days, int n, LocalDate today) {
        int sum = 0;
        for (UsageDay d : days.values()) if (inWindow(d.day, n, today)) sum += d.seconds;
        return sum;
    }

    private static boolean inWindow(LocalDate d, int n, LocalDate today) {
        return !d.isAfter(today) && d.isAfter(today.minusDays(n));
    }

    /** Последний день с активностью или null */
    public static LocalDate lastUse(Set<LocalDate> active) {
        LocalDate last = null;
        for (LocalDate d : active) if (last == null || d.isAfter(last)) last = d;
        return last;
    }

    /** Последние 7 дней, последний элемент - сегодня */
    public static List<Boolean> lastWeek(Set<LocalDate> active, LocalDate today) {
        List<Boolean> out = new ArrayList<>();
        for (int i = 6; i >= 0; i--) out.add(active.contains(today.minusDays(i)));
        return out;
    }

    /** Цена одного использования за месяц или -1 */
    public static double costPerUse(double monthlyPrice, int usesIn30Days) {
        return usesIn30Days <= 0 ? -1 : monthlyPrice / usesIn30Days;
    }
}
