package ru.eridian.justpay.reminder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** Когда в следующий раз запускать ежедневную проверку */
final class DailySchedule {
    private DailySchedule() {}

    /** checked - день последней проверки, lateUntilHour - до какого часа проверку ещё можно сделать */
    static long next(LocalDateTime now, ZoneId zone, LocalDate checked, int hour, int lateUntilHour) {
        LocalDate today = now.toLocalDate();
        // Сегодня уже проверяли, следующая завтра
        LocalDate day = checked != null && !checked.isBefore(today) ? today.plusDays(1) : today;
        LocalDateTime at = day.atTime(hour, 0);
        if (!at.isAfter(now)) {
            if (day.equals(today) && now.getHour() < lateUntilHour) {
                at = now.plusSeconds(5); // пропущенное время, но день ещё не кончился
            } else {
                at = day.plusDays(1).atTime(hour, 0);
            }
        }
        return at.atZone(zone).toInstant().toEpochMilli();
    }
}
