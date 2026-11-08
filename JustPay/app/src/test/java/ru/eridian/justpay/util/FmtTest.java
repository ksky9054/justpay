package ru.eridian.justpay.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.LocalDate;

import ru.eridian.justpay.data.Currency;

/** Тесты форматирования сумм, дат, длительности и склонений */
public class FmtTest {

    @Test
    public void num_formatsThousandsWithSpace() {
        assertEquals("12 346", Fmt.num(12345.6));
        assertEquals("0", Fmt.num(0));
        assertEquals("999", Fmt.num(999));
        assertEquals("1 000 000", Fmt.num(1000000));
    }

    @Test
    public void decimals_formatsWithCents() {
        assertEquals("1 234,50", Fmt.decimals(1234.5));
        assertEquals("399", Fmt.decimals(399.0));
        assertEquals("0", Fmt.decimals(0.0));
    }

    @Test
    public void pl_pluralizesRussianWords() {
        assertEquals("день", Fmt.pl(1, "день", "дня", "дней"));
        assertEquals("дня", Fmt.pl(2, "день", "дня", "дней"));
        assertEquals("дня", Fmt.pl(4, "день", "дня", "дней"));
        assertEquals("дней", Fmt.pl(5, "день", "дня", "дней"));
        assertEquals("дней", Fmt.pl(11, "день", "дня", "дней"));
        assertEquals("дней", Fmt.pl(14, "день", "дня", "дней"));
        assertEquals("день", Fmt.pl(21, "день", "дня", "дней"));
        assertEquals("дня", Fmt.pl(22, "день", "дня", "дней"));
        assertEquals("дней", Fmt.pl(25, "день", "дня", "дней"));
    }

    @Test
    public void diff_and_rel_dateComparisons() {
        LocalDate today = LocalDate.of(2026, 11, 3);
        assertEquals(0, Fmt.diff(today, today));
        assertEquals("сегодня", Fmt.rel(today, today));

        LocalDate tomorrow = today.plusDays(1);
        assertEquals(1, Fmt.diff(tomorrow, today));
        assertEquals("завтра", Fmt.rel(tomorrow, today));

        LocalDate inThreeDays = today.plusDays(3);
        assertEquals(3, Fmt.diff(inThreeDays, today));
        assertEquals("через 3 дня", Fmt.rel(inThreeDays, today));

        LocalDate past = today.minusDays(2);
        assertTrue(Fmt.diff(past, today) < 0);
        assertEquals("списано", Fmt.rel(past, today));
    }

    @Test
    public void duration_formatsSeconds() {
        assertEquals("< 1 мин", Fmt.duration(30));
        assertEquals("5 мин", Fmt.duration(300));
        assertEquals("1 ч 2 мин", Fmt.duration(3725));
    }

    @Test
    public void currency_and_money_formatting() {
        Fmt.setCurrency(Currency.RUB);
        assertEquals("₽", Fmt.symbol());
        assertEquals("399", Fmt.moneyNum(399.0));
        assertEquals("399 ₽", Fmt.money(399.0));

        Fmt.setCurrency(Currency.USD);
        assertEquals("$", Fmt.symbol());
        assertEquals("9,99", Fmt.moneyNum(9.99));
        assertEquals("9,99 $", Fmt.money(9.99));

        // Возвращаем дефолт
        Fmt.setCurrency(Currency.RUB);
    }

    @Test
    public void cycle_labels() {
        assertEquals("мес", Fmt.cycleShort("m"));
        assertEquals("нед", Fmt.cycleShort("w"));
        assertEquals("год", Fmt.cycleShort("y"));

        assertEquals("Ежемесячно", Fmt.cycleFull("m"));
        assertEquals("Еженедельно", Fmt.cycleFull("w"));
        assertEquals("Ежегодно", Fmt.cycleFull("y"));
    }
}

