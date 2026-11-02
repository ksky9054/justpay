package ru.eridian.justpay.data;

import java.time.LocalDate;

/** Использование одной подписки за один день: данные приложения и ручные отметки. */
public class UsageDay {
    public final LocalDate day;
    /** Сколько раз за день открывали приложение сервиса (по статистике телефона). */
    public int opens;
    /** Сколько секунд приложение было на экране. */
    public int seconds;
    /** Ручные отметки «сегодня пользовался». */
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

    /** Сколько раз за день пользовались: открытия плюс ручные отметки. */
    public int uses() {
        return opens + manual;
    }

    public boolean isActive() {
        return uses() > 0;
    }
}
