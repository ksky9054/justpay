package ru.eridian.justpay.util;

import java.util.List;
import java.util.Locale;

import ru.eridian.justpay.data.Currency;
import ru.eridian.justpay.data.Subscription;

/** Формирует CSV-резервную копию списка подписок для экспорта из профиля. */
public final class SubscriptionCsvExporter {
    private SubscriptionCsvExporter() { }

    public static String build(List<Subscription> subscriptions, Currency currency) {
        StringBuilder csv = new StringBuilder();
        // BOM помогает Excel корректно распознать UTF-8 и русские заголовки.
        csv.append('\uFEFF');
        csv.append("Название;Категория;Статус;Стоимость;Валюта;Период;Следующее списание;Способ оплаты\r\n");

        for (Subscription subscription : subscriptions) {
            appendRow(csv,
                    safeText(subscription.name),
                    safeText(subscription.category),
                    subscription.isActive() ? "Активна" : "Приостановлена",
                    String.format(Locale.US, "%.2f", subscription.price),
                    currency.name(),
                    cycleLabel(subscription.cycle),
                    subscription.next == null ? "" : subscription.next.toString(),
                    safeText(subscription.card));
        }
        return csv.toString();
    }

    private static String cycleLabel(String cycle) {
        if (Subscription.CYCLE_WEEK.equals(cycle)) return "Еженедельно";
        if (Subscription.CYCLE_YEAR.equals(cycle)) return "Ежегодно";
        return "Ежемесячно";
    }

    private static void appendRow(StringBuilder csv, String... values) {
        for (int i = 0; i < values.length; i++) {
            if (i > 0) csv.append(';');
            csv.append(escape(values[i]));
        }
        csv.append("\r\n");
    }

    /** Защита табличных редакторов от интерпретации пользовательского текста как формулы. */
    private static String safeText(String value) {
        if (value == null) return "";
        int first = 0;
        while (first < value.length() && Character.isWhitespace(value.charAt(first))) first++;
        if (first < value.length() && "=+-@".indexOf(value.charAt(first)) >= 0) return "'" + value;
        return value;
    }

    /** Экранирование по правилам CSV: кавычки удваиваются, поля с разделителем берутся в кавычки. */
    private static String escape(String value) {
        if (value == null) return "";
        boolean quote = value.contains(";") || value.contains("\"")
                || value.contains("\n") || value.contains("\r");
        String escaped = value.replace("\"", "\"\"");
        return quote ? "\"" + escaped + "\"" : escaped;
    }
}
