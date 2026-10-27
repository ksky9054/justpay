package ru.eridian.justpay.data;

import java.time.LocalDate;

/** Запись о подписке: то, что хранится в таблице subscriptions. */
public class Subscription {
    public static final String ACTIVE = "active";
    public static final String PAUSED = "paused";

    public static final String CYCLE_WEEK = "w";
    public static final String CYCLE_MONTH = "m";
    public static final String CYCLE_YEAR = "y";

    public long id;
    public String name;
    /** Две буквы для плитки в списке. */
    public String mark;
    public double price;
    public String cycle = CYCLE_MONTH;
    /** Дата следующего списания; у подписки на паузе её нет. */
    public LocalDate next;
    /** Дата последнего списания перед паузой. */
    public LocalDate prev;
    public String category;
    public String status = ACTIVE;
    /** Подпись способа оплаты, например «Основная карта». Номер карты вводить не нужно. */
    public String card = "";
    public String since;
    public long accountId;
    /** Приложение сервиса на телефоне, из активности которого берётся статистика, или null. */
    public String appPackage;
    public LocalDate createdAt = LocalDate.now();
    /**
     * Когда напоминать: за сколько дней до списания (0 и больше), либо REMIND_DEFAULT
     * (как в настройках профиля), либо REMIND_OFF (не напоминать). Хранится смещение, а не
     * дата, поэтому напоминание само переезжает вместе с датой следующего списания.
     */
    public int remindDays = REMIND_DEFAULT;

    public static final int REMIND_DEFAULT = -1;
    public static final int REMIND_OFF = -2;

    public boolean isActive() {
        return ACTIVE.equals(status);
    }

    public boolean hasCard() {
        return card != null && !card.trim().isEmpty();
    }

    /** Фактическое число дней до списания или REMIND_OFF, если напоминать не нужно. */
    public int effectiveRemindDays(Prefs prefs) {
        if (remindDays == REMIND_DEFAULT) return prefs.isRemind() ? prefs.remindDays() : REMIND_OFF;
        return remindDays;
    }

    /** Дата напоминания или null: подписка на паузе либо напоминания выключены. */
    public LocalDate remindDate(Prefs prefs) {
        if (!isActive() || next == null) return null;
        int days = effectiveRemindDays(prefs);
        return days == REMIND_OFF ? null : next.minusDays(days);
    }

    /** Сколько подписка стоит в месяц: год делится на 12, неделя умножается на 52/12. */
    public double monthly() {
        return monthly(price, cycle);
    }

    public static double monthly(double price, String cycle) {
        if (CYCLE_YEAR.equals(cycle)) return price / 12.0;
        if (CYCLE_WEEK.equals(cycle)) return price * 52.0 / 12.0;
        return price;
    }

    /** Сдвигает дату на k периодов (k может быть отрицательным). */
    public static LocalDate shift(LocalDate base, String cycle, int k) {
        if (CYCLE_YEAR.equals(cycle)) return base.plusYears(k);
        if (CYCLE_WEEK.equals(cycle)) return base.plusWeeks(k);
        return base.plusMonths(k);
    }

    /**
     * Первая дата списания от base с шагом в один период, которая позже today
     * (или не раньше today, если inclusive).
     */
    public static LocalDate advance(LocalDate base, String cycle, LocalDate today, boolean inclusive) {
        LocalDate d = base;
        int k = 0;
        while (inclusive ? d.isBefore(today) : !d.isAfter(today)) {
            k++;
            d = shift(base, cycle, k);
            if (k > 5000) break;
        }
        return d;
    }
}
