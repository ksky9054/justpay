package ru.eridian.justpay.data;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.time.LocalDate;
import java.util.Set;
import java.util.TreeSet;

/** Проверка серии дней подряд */
public class UsageMetricsTest {
    private final LocalDate today = LocalDate.of(2026, 10, 31);

    // Набор дней, в которые пользовались подпиской
    private Set<LocalDate> days(int... dayNumbers) {
        Set<LocalDate> set = new TreeSet<>();
        for (int d : dayNumbers) set.add(LocalDate.of(2026, 10, d));
        return set;
    }

    @Test
    public void streak_countsDaysInARowIncludingToday() {
        assertEquals(3, UsageMetrics.currentStreak(days(29, 30, 31), today));
    }

    @Test
    public void streak_withoutToday_countsFromYesterday() {
        assertEquals(2, UsageMetrics.currentStreak(days(29, 30), today));
    }

    @Test
    public void streak_brokenWhenYesterdayMissed() {
        assertEquals(0, UsageMetrics.currentStreak(days(20, 21), today));
    }

    @Test
    public void longestStreak_findsBestRun() {
        assertEquals(3, UsageMetrics.longestStreak(days(1, 2, 3, 10, 11, 20)));
    }

    @Test
    public void usageDay_isActiveWithManualMark() {
        UsageDay day = new UsageDay(today, 0, 0, 1);
        assertEquals(true, day.isActive());
    }
}
