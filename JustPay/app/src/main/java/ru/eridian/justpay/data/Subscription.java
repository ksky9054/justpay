package ru.eridian.justpay.data;

import java.time.LocalDate;

/** Подписка, строка таблицы subscriptions */
public class Subscription {
    public static final String ACTIVE = "active";
    public static final String PAUSED = "paused";

    public static final String CYCLE_WEEK = "w";
    public static final String CYCLE_MONTH = "m";
    public static final String CYCLE_YEAR = "y";

    public long id;
    public String name;
    /** Две буквы названия */
    public String mark;
    public double price;
    public String cycle = CYCLE_MONTH;
    /** Следующее списание, у паузы его нет */
    public LocalDate next;
    /** Последнее списание перед паузой */
    public LocalDate prev;
    public String category;
    public String status = ACTIVE;
    /** Способ оплаты, например «Основная карта» */
    public String card = "";
    public String since;
    public long accountId;
    /** Приложение сервиса, откуда берём статистику, или null */
    public String appPackage;
    public LocalDate createdAt = LocalDate.now();
    /** Когда напомнить: дней до списания, REMIND_DEFAULT или REMIND_OFF */
    public int remindDays = REMIND_DEFAULT;

    public static final int REMIND_DEFAULT = -1;
    public static final int REMIND_OFF = -2;

    public boolean isActive() {
        return ACTIVE.equals(status);
    }

    public boolean hasCard() {
        return card != null && !card.trim().isEmpty();
    }

    /** Дней до списания или REMIND_OFF */
    public int effectiveRemindDays(Prefs prefs) {
        if (remindDays == REMIND_DEFAULT) return prefs.isRemind() ? prefs.remindDays() : REMIND_OFF;
        return remindDays;
    }

    /** Дата напоминания или null */
    public LocalDate remindDate(Prefs prefs) {
        if (!isActive() || next == null) return null;
        int days = effectiveRemindDays(prefs);
        return days == REMIND_OFF ? null : next.minusDays(days);
    }

    /** Цена в месяц: год делим на 12, неделю умножаем на 52/12 */
    public double monthly() {
        return monthly(price, cycle);
    }

    public static double monthly(double price, String cycle) {
        if (CYCLE_YEAR.equals(cycle)) return price / 12.0;
        if (CYCLE_WEEK.equals(cycle)) return price * 52.0 / 12.0;
        return price;
    }

    /** Сдвигает дату на k периодов */
    public static LocalDate shift(LocalDate base, String cycle, int k) {
        return shift(base, cycle, k, base != null ? base.getDayOfMonth() : 0);
    }

    /** Сдвигает дату на k периодов с сохранением желаемого дня месяца (например, 31 числа) */
    public static LocalDate shift(LocalDate base, String cycle, int k, int anchorDay) {
        if (CYCLE_YEAR.equals(cycle)) {
            LocalDate target = base.plusYears(k);
            if (anchorDay > 0) {
                int dim = target.lengthOfMonth();
                return target.withDayOfMonth(Math.min(anchorDay, dim));
            }
            return target;
        }
        if (CYCLE_WEEK.equals(cycle)) return base.plusWeeks(k);
        LocalDate target = base.plusMonths(k);
        if (anchorDay > 0) {
            int dim = target.lengthOfMonth();
            return target.withDayOfMonth(Math.min(anchorDay, dim));
        }
        return target;
    }

    /** Первая дата списания после today (с inclusive и сама today) */
    public static LocalDate advance(LocalDate base, String cycle, LocalDate today, boolean inclusive) {
        return advance(base, cycle, today, inclusive, base != null ? base.getDayOfMonth() : 0);
    }

    /** Первая дата списания после today с сохранением желаемого дня месяца (например, 31 числа) */
    public static LocalDate advance(LocalDate base, String cycle, LocalDate today, boolean inclusive, int anchorDay) {
        LocalDate d = base;
        int k = 0;
        while (inclusive ? d.isBefore(today) : !d.isAfter(today)) {
            k++;
            d = shift(base, cycle, k, anchorDay);
            if (k > 5000) break;
        }
        return d;
    }
}
