package ru.eridian.justpay.data;

import java.time.LocalDate;

/** Использование подписки за один день */
public class UsageDay {
    public final LocalDate day;
    /** Открытия приложения */
    public int opens;
    /** Секунды на экране */
    public int seconds;
    /** Ручные отметки */
    public int manual;

    public UsageDay(LocalDate day) {
        this.day = day;
    }

    public UsageDay(LocalDate day, int opens, int seconds, int manual) {
        this.day = day;
        this.opens = opens;
        this.seconds = seconds;
        this.manual = manual;
    }

    /** Открытия плюс ручные отметки */
    public int uses() {
        return opens + manual;
    }

    public boolean isActive() {
        return uses() > 0;
    }
}
