package ru.eridian.justpay.data;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.time.LocalDate;

/** Проверка расчёта цены в месяц и дат списания */
public class SubscriptionTest {

    @Test
    public void monthlyPrice_forYear() {
        assertEquals(100.0, Subscription.monthly(1200, Subscription.CYCLE_YEAR), 0.001);
    }

    @Test
    public void monthlyPrice_forWeek() {
        assertEquals(100 * 52 / 12.0, Subscription.monthly(100, Subscription.CYCLE_WEEK), 0.001);
    }

    @Test
    public void monthlyPrice_forMonth() {
        assertEquals(399.0, Subscription.monthly(399, Subscription.CYCLE_MONTH), 0.001);
    }

    @Test
    public void shift_monthEndDoesNotOverflow() {
        // 31 января плюс месяц это 28 февраля, а не 3 марта
        LocalDate result = Subscription.shift(LocalDate.of(2026, 1, 31), Subscription.CYCLE_MONTH, 1);
        assertEquals(LocalDate.of(2026, 2, 28), result);
    }

    @Test
    public void advance_findsNextDateAfterToday() {
        LocalDate base = LocalDate.of(2026, 10, 5);
        LocalDate today = LocalDate.of(2026, 10, 31);
        LocalDate result = Subscription.advance(base, Subscription.CYCLE_MONTH, today, false);
        assertEquals(LocalDate.of(2026, 11, 5), result);
    }

    @Test
    public void advance_inclusiveKeepsToday() {
        LocalDate today = LocalDate.of(2026, 10, 31);
        LocalDate result = Subscription.advance(today, Subscription.CYCLE_MONTH, today, true);
        assertEquals(today, result);
    }
}
