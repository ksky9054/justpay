package ru.eridian.justpay.data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Расчёты по статистике: серия дней подряд, число использований, стоимость одного использования.
 * Класс не зависит от Android, поэтому его можно проверять обычным запуском Java.
 */
public final class UsageMetrics {
    private UsageMetrics() {}

    /** Дни, в которые подпиской пользовались. */
    public static Set<LocalDate> activeDays(Map<LocalDate, UsageDay> days) {
        Set<LocalDate> out = new TreeSet<>();
        for (UsageDay d : days.values()) if (d.isActive()) out.add(d.day);
        return out;
    }

    /** Дни, в которые пользовались хотя бы одной из подписок. */
    public static Set<LocalDate> activeDaysAny(Collection<Map<LocalDate, UsageDay>> all) {
        Set<LocalDate> out = new TreeSet<>();
        for (Map<LocalDate, UsageDay> m : all) out.addAll(activeDays(m));
        return out;
    }

    /**
     * Текущая серия: сколько дней подряд была активность. Если сегодня отметки ещё нет,
     * серия не обрывается и считается от вчерашнего дня, как в Duolingo: у пользователя
     * есть весь сегодняшний день, чтобы её продолжить.
     */
    public static int currentStreak(Set<LocalDate> active, LocalDate today) {
        LocalDate d = active.contains(today) ? today : today.minusDays(1);
        int n = 0;
        while (active.contains(d)) {
            n++;
            d = d.minusDays(1);
        }
        return n;
    }

    /** Самая длинная серия за всё время. */
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

    /** Использований за последние n дней, включая сегодня. */
    public static int usesInLast(Map<LocalDate, UsageDay> days, int n, LocalDate today) {
        int sum = 0;
        for (UsageDay d : days.values()) if (inWindow(d.day, n, today)) sum += d.uses();
        return sum;
    }

    /** Время в приложении за последние n дней, секунды. */
    public static int secondsInLast(Map<LocalDate, UsageDay> days, int n, LocalDate today) {
        int sum = 0;
        for (UsageDay d : days.values()) if (inWindow(d.day, n, today)) sum += d.seconds;
        return sum;
    }

    private static boolean inWindow(LocalDate d, int n, LocalDate today) {
        return !d.isAfter(today) && d.isAfter(today.minusDays(n));
    }

    /** Последний день с активностью или null, если её ещё не было. */
    public static LocalDate lastUse(Set<LocalDate> active) {
        LocalDate last = null;
        for (LocalDate d : active) if (last == null || d.isAfter(last)) last = d;
        return last;
    }

    /** Активность за последние 7 дней: первый элемент — шесть дней назад, последний — сегодня. */
    public static List<Boolean> lastWeek(Set<LocalDate> active, LocalDate today) {
        List<Boolean> out = new ArrayList<>();
        for (int i = 6; i >= 0; i--) out.add(active.contains(today.minusDays(i)));
        return out;
    }

    /** Во сколько обходится одно использование за месяц, или -1, если использований не было. */
    public static double costPerUse(double monthlyPrice, int usesIn30Days) {
        return usesIn30Days <= 0 ? -1 : monthlyPrice / usesIn30Days;
    }
}
