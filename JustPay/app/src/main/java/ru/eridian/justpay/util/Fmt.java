package ru.eridian.justpay.util;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import ru.eridian.justpay.data.Currency;

/** Форматирование сумм, дат и склонений по-русски. */
public final class Fmt {
    private Fmt() {}

    private static final String NBSP = " ";
    private static final String[] MS = {"янв", "фев", "мар", "апр", "мая", "июн", "июл", "авг", "сен", "окт", "ноя", "дек"};
    private static final String[] MG = {"января", "февраля", "марта", "апреля", "мая", "июня", "июля", "августа", "сентября", "октября", "ноября", "декабря"};
    private static final String[] MN = {"Январь", "Февраль", "Март", "Апрель", "Май", "Июнь", "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"};
    // индекс = DayOfWeek.getValue() % 7, воскресенье = 0
    private static final String[] WD = {"воскресенье", "понедельник", "вторник", "среда", "четверг", "пятница", "суббота"};

    /** 12345.6 -> «12 346» с неразрывными пробелами между разрядами. */
    public static String num(double n) {
        String s = String.valueOf(Math.abs(Math.round(n)));
        StringBuilder sb = new StringBuilder();
        if (n < 0 && Math.round(n) != 0) sb.append('-');
        int len = s.length();
        for (int i = 0; i < len; i++) {
            if (i > 0 && (len - i) % 3 == 0) sb.append(NBSP);
            sb.append(s.charAt(i));
        }
        return sb.toString();
    }

    // ----- деньги в валюте аккаунта -----

    private static Currency currency = Currency.RUB;

    /** Валюта, в которой сейчас показываются все суммы (валюта аккаунта, в который выполнен вход). */
    public static void setCurrency(Currency c) {
        currency = c == null ? Currency.RUB : c;
    }

    public static Currency currency() {
        return currency;
    }

    public static String symbol() {
        return currency.symbol;
    }

    /** 1234.5 -> «1 234,50», 399 -> «399»: копейки видны, только если они есть. */
    public static String decimals(double n) {
        double r = Math.round(n * 100) / 100.0;
        if (Math.abs(r - Math.rint(r)) < 0.005) return num(r);
        long cents = Math.round(Math.abs(r) * 100);
        long frac = cents % 100;
        return (r < 0 ? "-" : "") + num((double) (cents / 100)) + "," + (frac < 10 ? "0" : "") + frac;
    }

    /** Число для итоговых сумм: целое для рубля и тенге, с копейками для остальных валют. */
    public static String moneyNum(double n) {
        return currency.digits == 0 ? num(n) : decimals(n);
    }

    /** Итоговая сумма (в месяц, в год и т. п.) с обозначением валюты. */
    public static String money(double n) {
        return moneyNum(n) + NBSP + currency.symbol;
    }

    /** Сумма в указанной валюте, а не в текущей (нужно для примера пересчёта). */
    public static String priceIn(Currency c, double n) {
        return decimals(n) + NBSP + c.symbol;
    }

    /** Цена подписки так, как её ввёл пользователь: копейки показываются, если они есть. */
    public static String price(double n) {
        return decimals(n) + NBSP + currency.symbol;
    }

    /** Склонение: 1 день, 2 дня, 5 дней. */
    public static String pl(int n, String one, String few, String many) {
        int t = n % 10, h = n % 100;
        if (t == 1 && h != 11) return one;
        if (t >= 2 && t <= 4 && (h < 10 || h >= 20)) return few;
        return many;
    }

    public static long diff(LocalDate d, LocalDate today) {
        return ChronoUnit.DAYS.between(today, d);
    }

    public static String rel(LocalDate d, LocalDate today) {
        long n = diff(d, today);
        if (n < 0) return "списано";
        if (n == 0) return "сегодня";
        if (n == 1) return "завтра";
        return "через " + n + " " + pl((int) n, "день", "дня", "дней");
    }

    /** 29 окт */
    public static String dayShort(LocalDate d) {
        return d.getDayOfMonth() + " " + MS[d.getMonthValue() - 1];
    }

    /** 29 октября */
    public static String dayLong(LocalDate d) {
        return d.getDayOfMonth() + " " + MG[d.getMonthValue() - 1];
    }

    /** 29 октября 2026 */
    public static String dayFull(LocalDate d) {
        return dayLong(d) + " " + d.getYear();
    }

    /** Октябрь 2026 */
    public static String monthTitle(LocalDate d) {
        return MN[d.getMonthValue() - 1] + " " + d.getYear();
    }

    /** 3725 с -> «1 ч 2 мин»; меньше минуты -> «менее мин». */
    public static String duration(int seconds) {
        int minutes = seconds / 60;
        if (minutes < 1) return "< 1 мин";
        if (minutes < 60) return minutes + " мин";
        return (minutes / 60) + " ч " + (minutes % 60) + " мин";
    }

    /** окт 2026 */
    public static String monthYear(LocalDate d) {
        return MS[d.getMonthValue() - 1] + " " + d.getYear();
    }

    public static String weekday(LocalDate d) {
        return WD[d.getDayOfWeek().getValue() % 7];
    }

    public static String cycleShort(String cycle) {
        switch (cycle) {
            case "y": return "год";
            case "w": return "нед";
            default: return "мес";
        }
    }

    public static String cycleFull(String cycle) {
        switch (cycle) {
            case "y": return "Ежегодно";
            case "w": return "Еженедельно";
            default: return "Ежемесячно";
        }
    }
}
