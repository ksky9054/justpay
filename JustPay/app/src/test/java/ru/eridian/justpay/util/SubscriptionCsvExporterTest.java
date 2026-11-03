package ru.eridian.justpay.util;

import org.junit.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;

import ru.eridian.justpay.data.Currency;
import ru.eridian.justpay.data.Subscription;

import static org.junit.Assert.assertTrue;

public class SubscriptionCsvExporterTest {
    @Test
    public void exportsHeaderForEmptyList() {
        String csv = SubscriptionCsvExporter.build(Collections.emptyList(), Currency.RUB);
        assertTrue(csv.startsWith("\uFEFF\"Название\";"));
        assertTrue(csv.contains("\"Способ оплаты\""));
    }

    @Test
    public void exportsSubscriptionFieldsAndDates() {
        Subscription s = new Subscription();
        s.name = "Музыка";
        s.category = "Развлечения";
        s.price = 199.0;
        s.cycle = Subscription.CYCLE_MONTH;
        s.next = LocalDate.of(2026, 12, 1);
        s.prev = LocalDate.of(2026, 11, 1);
        s.card = "Основная карта";

        String csv = SubscriptionCsvExporter.build(Arrays.asList(s), Currency.RUB);
        assertTrue(csv.contains("\"Музыка\""));
        assertTrue(csv.contains("\"199\""));
        assertTrue(csv.contains("\"2026-12-01\""));
        assertTrue(csv.contains("\"2026-11-01\""));
    }

    @Test
    public void escapesQuotesAndSeparators() {
        Subscription s = new Subscription();
        s.name = "Сервис; \"Плюс\"";
        String csv = SubscriptionCsvExporter.build(Arrays.asList(s), Currency.RUB);
        assertTrue(csv.contains("\"Сервис; \"\"Плюс\"\"\""));
    }

    @Test
    public void neutralizesFormulaLikeUserText() {
        Subscription s = new Subscription();
        s.name = "=HYPERLINK(\"https://example.com\")";
        String csv = SubscriptionCsvExporter.build(Arrays.asList(s), Currency.RUB);
        assertTrue(csv.contains("\"'=HYPERLINK"));
    }

    @Test
    public void handlesPausedSubscriptionAndNullCurrency() {
        Subscription s = new Subscription();
        s.status = Subscription.PAUSED;
        String csv = SubscriptionCsvExporter.build(Arrays.asList(s), null);
        assertTrue(csv.contains("\"Приостановлена\""));
        assertTrue(csv.contains("\"RUB\""));
    }
}
