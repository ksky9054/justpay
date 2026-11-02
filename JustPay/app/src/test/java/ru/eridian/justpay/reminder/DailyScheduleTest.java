package ru.eridian.justpay.reminder;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** Проверка времени ежедневного напоминания */
public class DailyScheduleTest {
    private static final ZoneId UTC = ZoneId.of("UTC");

    // Переводит дату и время в миллисекунды, как это делает сам класс
    private long millis(LocalDateTime time) {
        return time.atZone(UTC).toInstant().toEpochMilli();
    }

    @Test
    public void morning_waitsForEvening() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 27, 10, 0);
        long result = DailySchedule.next(now, UTC, null, 20, 23);
        assertEquals(millis(LocalDateTime.of(2026, 10, 27, 20, 0)), result);
    }

    @Test
    public void missedEvening_runsAlmostImmediately() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 27, 21, 0);
        long result = DailySchedule.next(now, UTC, null, 20, 23);
        assertEquals(millis(now.plusSeconds(5)), result);
    }

    @Test
    public void tooLate_movesToTomorrow() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 27, 23, 30);
        long result = DailySchedule.next(now, UTC, null, 20, 23);
        assertEquals(millis(LocalDateTime.of(2026, 10, 28, 20, 0)), result);
    }

    @Test
    public void alreadyChecked_movesToTomorrow() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 27, 10, 0);
        long result = DailySchedule.next(now, UTC, LocalDate.of(2026, 10, 27), 20, 23);
        assertEquals(millis(LocalDateTime.of(2026, 10, 28, 20, 0)), result);
    }
}
