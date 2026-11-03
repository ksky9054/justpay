package ru.eridian.justpay.data;

/** Чистые расчёты для месячного бюджета и сводной аналитики. */
public final class BudgetAnalytics {
    private BudgetAnalytics() { }

    public static double monthlyTotal(Iterable<Subscription> subscriptions) {
        double total = 0d;
        for (Subscription subscription : subscriptions) {
            if (subscription != null && subscription.isActive()) total += subscription.monthly();
        }
        return total;
    }

    public static double yearlyForecast(double monthlyTotal) {
        return Math.max(0d, monthlyTotal) * 12d;
    }

    public static double dailyAverage(double monthlyTotal, int daysInMonth) {
        if (daysInMonth <= 0) return 0d;
        return Math.max(0d, monthlyTotal) / daysInMonth;
    }

    public static double remaining(double budget, double spending) {
        return Math.max(0d, budget - spending);
    }

    public static double progressPercent(double budget, double spending) {
        if (budget <= 0d) return 0d;
        return Math.min(100d, Math.max(0d, spending / budget * 100d));
    }

    public static boolean isOverBudget(double budget, double spending) {
        return budget > 0d && spending > budget;
    }
}
