package ru.eridian.justpay.data;

/**
 * Валюта аккаунта. Суммы вводятся и показываются в ней, курсов и пересчёта нет:
 * приложение работает без интернета, а свежие курсы взять неоткуда.
 */
public enum Currency {
    RUB("₽", "Российский рубль", 0),
    USD("$", "Доллар США", 2),
    EUR("€", "Евро", 2),
    KZT("₸", "Казахстанский тенге", 0),
    BYN("Br", "Белорусский рубль", 2),
    UAH("₴", "Украинская гривна", 2);

    public final String symbol;
    public final String title;
    /** Знаков после запятой в итоговых суммах: 0 для рубля и тенге, 2 для остальных. */
    public final int digits;

    Currency(String symbol, String title, int digits) {
        this.symbol = symbol;
        this.title = title;
        this.digits = digits;
    }

    public static Currency fromCode(String code) {
        for (Currency c : values()) if (c.name().equals(code)) return c;
        return RUB;
    }
}
