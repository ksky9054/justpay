package ru.eridian.justpay.reminder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** Когда в следующий раз запускать ежедневную проверку. Не зависит от Android, поэтому легко проверяется. */
final class DailySchedule {
    private DailySchedule() {}

    /**
     * @param checked день, в который проверка уже выполнялась (или null)
     * @param hour час, в который проверку нужно делать
     * @param lateUntilHour если телефон был выключен или проверку пропустили, её ещё можно выполнить
     *                      позже в тот же день, но не после этого часа
     * @return время запуска в миллисекундах
     */
    static long next(LocalDateTime now, ZoneId zone, LocalDate checked, int hour, int lateUntilHour) {
        LocalDate today = now.toLocalDate();
        // Сегодня уже проверяли (или часы переведены назад): следующая проверка завтра
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
