package ru.eridian.justpay.data;

import org.junit.Test;
import static org.junit.Assert.*;

public class BudgetAnalyticsTest {
    @Test public void monthlyTotalIncludesOnlyActiveSubscriptions() {
        Subscription active = new Subscription();
        active.price = 1200d;
        active.cycle = Subscription.CYCLE_YEAR;
        active.status = Subscription.ACTIVE;
        Subscription paused = new Subscription();
        paused.price = 500d;
        paused.status = Subscription.PAUSED;
        assertEquals(100d, BudgetAnalytics.monthlyTotal(java.util.Arrays.asList(active, paused)), 0.001d);
    }

    @Test public void yearlyForecastUsesTwelveMonths() {
        assertEquals(12000d, BudgetAnalytics.yearlyForecast(1000d), 0.001d);
    }

    @Test public void dailyAverageUsesDaysInMonth() {
        assertEquals(100d, BudgetAnalytics.dailyAverage(3100d, 31), 0.001d);
    }

    @Test public void remainingNeverGoesBelowZero() {
        assertEquals(0d, BudgetAnalytics.remaining(500d, 700d), 0.001d);
    }

    @Test public void progressIsCappedAtOneHundredPercent() {
        assertEquals(100d, BudgetAnalytics.progressPercent(500d, 700d), 0.001d);
    }

    @Test public void zeroBudgetDoesNotCountAsExceeded() {
        assertFalse(BudgetAnalytics.isOverBudget(0d, 700d));
    }
}
