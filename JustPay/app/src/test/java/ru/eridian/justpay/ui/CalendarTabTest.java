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
}

