package ru.eridian.justpay.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import ru.eridian.justpay.data.Subscription;

/** Тесты расчёта дней списаний в календаре */
public class CalendarTabTest {

    private Subscription createSub(String name, String cycle, LocalDate next, LocalDate createdAt) {
        Subscription s = new Subscription();
        s.name = name;
        s.cycle = cycle;
        s.next = next;
        s.status = Subscription.ACTIVE;
        s.createdAt = createdAt;
        s.price = 100.0;
        return s;
    }

    @Test
    public void weeklySubscription_showsInFutureMonths() {
        // Подписка со списанием по четвергам, следующее списание 5 ноября 2026
        LocalDate createdAt = LocalDate.of(2026, 11, 1);
        LocalDate next = LocalDate.of(2026, 11, 5);
        Subscription s = createSub("Weekly Service", Subscription.CYCLE_WEEK, next, createdAt);

        LocalDate today = LocalDate.of(2026, 11, 3);
        // Смотрим следующий месяц: декабрь 2026
        LocalDate decFirst = LocalDate.of(2026, 12, 1);

        Map<Integer, List<Subscription>> occ = CalendarTab.computeOccurrences(
                Collections.singletonList(s), decFirst, today);

        // Четверги декабря 2026: 3, 10, 17, 24, 31
        int[] expectedThursdays = {3, 10, 17, 24, 31};
        for (int day : expectedThursdays) {
            assertTrue("Четверг " + day + " декабря должен содержать списание", occ.containsKey(day));
            assertEquals(1, occ.get(day).size());
            assertEquals("Weekly Service", occ.get(day).get(0).name);
        }

        // Проверяем, что не попали лишние дни (например, среда 2 декабря)
        assertFalse(occ.containsKey(2));
        assertFalse(occ.containsKey(4));
    }

    @Test
    public void weeklySubscription_showsInCurrentMonth() {
        LocalDate createdAt = LocalDate.of(2026, 11, 1);
        LocalDate next = LocalDate.of(2026, 11, 5);
        Subscription s = createSub("Weekly Service", Subscription.CYCLE_WEEK, next, createdAt);

        LocalDate today = LocalDate.of(2026, 11, 3);
        LocalDate novFirst = LocalDate.of(2026, 11, 1);

        Map<Integer, List<Subscription>> occ = CalendarTab.computeOccurrences(
                Collections.singletonList(s), novFirst, today);

        // Четверги ноября 2026: 5, 12, 19, 26
        int[] expectedThursdays = {5, 12, 19, 26};
        for (int day : expectedThursdays) {
            assertTrue("Четверг " + day + " ноября должен содержать списание", occ.containsKey(day));
        }
    }

    @Test
    public void monthlySubscription_doesNotShowGhostPaymentsBeforeCreation() {
        // Подписка создана 10 ноября, первое списание 1 декабря
        LocalDate createdAt = LocalDate.of(2026, 11, 10);
        LocalDate next = LocalDate.of(2026, 12, 1);
        Subscription s = createSub("New Monthly", Subscription.CYCLE_MONTH, next, createdAt);

        LocalDate today = LocalDate.of(2026, 11, 10);
        LocalDate novFirst = LocalDate.of(2026, 11, 1);

        Map<Integer, List<Subscription>> occ = CalendarTab.computeOccurrences(
                Collections.singletonList(s), novFirst, today);

        // 1 ноября НЕ должно отображаться как прошедшее списание, так как подписка создана 10 ноября
        assertFalse("1 ноября не должно быть списания для подписки, созданной 10 ноября", occ.containsKey(1));
    }

    @Test
    public void yearlySubscription_showsInNextYear() {
        // Подписка с ежегодным списанием 15 января
        LocalDate createdAt = LocalDate.of(2026, 1, 15);
        LocalDate next = LocalDate.of(2027, 1, 15);
        Subscription s = createSub("Yearly Cloud", Subscription.CYCLE_YEAR, next, createdAt);

        LocalDate today = LocalDate.of(2026, 11, 3);
        LocalDate janFirst = LocalDate.of(2027, 1, 1);

        Map<Integer, List<Subscription>> occ = CalendarTab.computeOccurrences(
                Collections.singletonList(s), janFirst, today);

        assertTrue("15 января 2027 года должно быть списание", occ.containsKey(15));
        assertEquals(1, occ.get(15).size());
    }

    @Test
    public void monthlySubscription_preservesAnchorDayInFutureMonths() {
        // Подписка на 31 число: в феврале next = 28 февраля, prev = 31 января
        LocalDate createdAt = LocalDate.of(2026, 1, 31);
        LocalDate next = LocalDate.of(2026, 2, 28);
        Subscription s = createSub("End of Month Service", Subscription.CYCLE_MONTH, next, createdAt);
        s.prev = LocalDate.of(2026, 1, 31);

        LocalDate today = LocalDate.of(2026, 2, 15);

        // Проверяем март 2026 (31 день) -> списание должно быть 31 марта, а не 28
        LocalDate marFirst = LocalDate.of(2026, 3, 1);
        Map<Integer, List<Subscription>> marOcc = CalendarTab.computeOccurrences(
                Collections.singletonList(s), marFirst, today);
        assertTrue("В марте списание должно быть 31 числа", marOcc.containsKey(31));
        assertFalse("В марте не должно быть списания 28 числа", marOcc.containsKey(28));

        // Проверяем апрель 2026 (30 дней) -> списание должно быть 30 апреля, а не 28
        LocalDate aprFirst = LocalDate.of(2026, 4, 1);
        Map<Integer, List<Subscription>> aprOcc = CalendarTab.computeOccurrences(
                Collections.singletonList(s), aprFirst, today);
        assertTrue("В апреле списание должно быть 30 числа", aprOcc.containsKey(30));
        assertFalse("В апреле не должно быть списания 28 числа", aprOcc.containsKey(28));
    }

    @Test
    public void monthlySubscription_noGhostPaymentsWhenPrevIsNull() {
        // Создана 1 ноября, первое списание 5 декабря, prev = null
        LocalDate createdAt = LocalDate.of(2026, 11, 1);
        LocalDate next = LocalDate.of(2026, 12, 5);
        Subscription s = createSub("Trial Service", Subscription.CYCLE_MONTH, next, createdAt);
        s.prev = null;

        LocalDate today = LocalDate.of(2026, 11, 10);
        LocalDate novFirst = LocalDate.of(2026, 11, 1);

        Map<Integer, List<Subscription>> novOcc = CalendarTab.computeOccurrences(
                Collections.singletonList(s), novFirst, today);

        // 5 ноября НЕ должно отображаться, так как реальных платежей ещё не было (prev == null)
        assertFalse("5 ноября не должно отображаться как списание, если оплат еще не было", novOcc.containsKey(5));

        // В декабре списание 5 декабря должно присутствовать
        LocalDate decFirst = LocalDate.of(2026, 12, 1);
        Map<Integer, List<Subscription>> decOcc = CalendarTab.computeOccurrences(
                Collections.singletonList(s), decFirst, today);
        assertTrue("5 декабря должно быть запланированное списание", decOcc.containsKey(5));
    }

    @Test
    public void monthlySubscription_showsActualPastPaymentWhenPrevExists() {
        // Подписка создана 1 октября, списана 5 ноября (prev = 5 ноября), следующее 5 декабря
        LocalDate createdAt = LocalDate.of(2026, 10, 1);
        LocalDate next = LocalDate.of(2026, 12, 5);
        Subscription s = createSub("Regular Service", Subscription.CYCLE_MONTH, next, createdAt);
        s.prev = LocalDate.of(2026, 11, 5);

        LocalDate today = LocalDate.of(2026, 11, 10);
        LocalDate novFirst = LocalDate.of(2026, 11, 1);

        Map<Integer, List<Subscription>> novOcc = CalendarTab.computeOccurrences(
                Collections.singletonList(s), novFirst, today);

        // 5 ноября ДОЛЖНО отображаться, так как платеж реально был (prev = 5 ноября)
        assertTrue("5 ноября должно отображаться как прошедшее списание", novOcc.containsKey(5));
    }

    @Test
    public void weeklySubscription_pastAndFutureOccurrencesInSameMonth() {
        // Еженедельная подписка по средам (в ноябре 2026: 4, 11, 18, 25)
        // Создана 8 ноября (воскресенье), списана 11 ноября (prev), следующее 18 ноября (next)
        LocalDate createdAt = LocalDate.of(2026, 11, 8);
        LocalDate next = LocalDate.of(2026, 11, 18);
        Subscription s = createSub("Weekly Gym", Subscription.CYCLE_WEEK, next, createdAt);
        s.prev = LocalDate.of(2026, 11, 11);

        LocalDate today = LocalDate.of(2026, 11, 15);
        LocalDate novFirst = LocalDate.of(2026, 11, 1);

        Map<Integer, List<Subscription>> occ = CalendarTab.computeOccurrences(
                Collections.singletonList(s), novFirst, today);

        // 4 ноября НЕ должно быть (до createdAt)
        assertFalse("4 ноября не должно быть списания, подписка создана 8 ноября", occ.containsKey(4));
        // 11 ноября ДОЛЖНО быть (прошедшее фактическое списание)
        assertTrue("11 ноября должно быть списание", occ.containsKey(11));
        // 18 ноября ДОЛЖНО быть (предстоящее)
        assertTrue("18 ноября должно быть списание", occ.containsKey(18));
        // 25 ноября ДОЛЖНО быть (предстоящее)
        assertTrue("25 ноября должно быть списание", occ.containsKey(25));
    }

    @Test
    public void yearlySubscription_leapYearPreserved() {
        // Ежегодная подписка на 29 февраля (високосный год)
        LocalDate createdAt = LocalDate.of(2024, 2, 29);
        LocalDate next = LocalDate.of(2028, 2, 29);
        Subscription s = createSub("Leap Year Domain", Subscription.CYCLE_YEAR, next, createdAt);
        s.prev = LocalDate.of(2024, 2, 29);

        LocalDate today = LocalDate.of(2027, 10, 1);
        LocalDate feb2028First = LocalDate.of(2028, 2, 1);

        Map<Integer, List<Subscription>> occ = CalendarTab.computeOccurrences(
                Collections.singletonList(s), feb2028First, today);

        assertTrue("29 февраля 2028 года должно быть списание", occ.containsKey(29));
        assertFalse("28 февраля 2028 года не должно быть списания", occ.containsKey(28));
    }
}

