package ru.eridian.justpay.util;

import java.util.List;
import java.util.Locale;

import ru.eridian.justpay.data.Currency;
import ru.eridian.justpay.data.Subscription;

/** Формирует CSV-файл с подписками текущего аккаунта. */
public final class SubscriptionCsvExporter {
    private SubscriptionCsvExporter() { }

    /** Создаёт CSV в UTF-8; BOM помогает Excel распознать кириллицу. */
    public static String build(List<Subscription> subscriptions, Currency currency) {
        StringBuilder csv = new StringBuilder();
        csv.append('\uFEFF');
        appendRow(csv, "Название", "Категория", "Статус", "Стоимость",
                "Валюта", "Период", "Следующее списание", "Последнее списание",
                "Способ оплаты");

        if (subscriptions == null) return csv.toString();
        Currency safeCurrency = currency == null ? Currency.RUB : currency;
        for (Subscription subscription : subscriptions) {
            if (subscription == null) continue;
            String price = String.format(Locale.US,
                    safeCurrency.digits == 0 ? "%.0f" : "%.2f", subscription.price);
            appendRow(csv, safeText(subscription.name), safeText(subscription.category),
                    subscription.isActive() ? "Активна" : "Приостановлена", price,
                    safeCurrency.name(), cycleLabel(subscription.cycle),
                    subscription.isActive() && subscription.next != null ? subscription.next.toString() : "",
                    subscription.prev == null ? "" : subscription.prev.toString(),
                    safeText(subscription.card));
        }
        return csv.toString();
    }

    private static String cycleLabel(String cycle) {
        if (Subscription.CYCLE_WEEK.equals(cycle)) return "Еженедельно";
        if (Subscription.CYCLE_YEAR.equals(cycle)) return "Ежегодно";
        return "Ежемесячно";
    }

    /** Защищает текстовые значения от интерпретации как формулы в табличных редакторах. */
    private static String safeText(String value) {
        if (value == null) return "";
        int first = 0;
        while (first < value.length() && Character.isWhitespace(value.charAt(first))) first++;
        if (first < value.length() && "=+-@".indexOf(value.charAt(first)) >= 0) {
            return "'" + value;
        }
        return value;
    }

    /** Экранирование CSV: все поля в кавычках, кавычки внутри удваиваются. */
    private static void appendRow(StringBuilder csv, String... values) {
        for (int i = 0; i < values.length; i++) {
            if (i > 0) csv.append(';');
            String value = values[i] == null ? "" : values[i];
            csv.append('"').append(value.replace("\"", "\"\"")).append('"');
        }
        csv.append("\r\n");
    }
}
