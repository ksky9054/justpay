package ru.eridian.justpay.data;

import ru.eridian.justpay.R;

/** Цвет акцента, выбирается в профиле */
public enum Accent {
    LAVENDER("Лавандовый", 0xFF8B9BFF, R.style.Jp_Accent_Lavender),
    VIOLET("Фиолетовый", 0xFFCDBDFF, R.style.Jp_Accent_Violet),
    SKY("Голубой", 0xFF98CCF9, R.style.Jp_Accent_Sky),
    MINT("Мятный", 0xFF8DD5B2, R.style.Jp_Accent_Mint),
    LIME("Лаймовый", 0xFFB9CF83, R.style.Jp_Accent_Lime),
    AMBER("Янтарный", 0xFFF9BB72, R.style.Jp_Accent_Amber),
    ROSE("Розовый", 0xFFFEB0D2, R.style.Jp_Accent_Rose);

    public final String title;
    public final int swatch;
    public final int style;

    Accent(String title, int swatch, int style) {
        this.title = title;
        this.swatch = swatch;
        this.style = style;
    }

    /** Цвет по номеру, неверный номер даёт лавандовый */
    public static Accent of(int index) {
        Accent[] all = values();
        return index >= 0 && index < all.length ? all[index] : LAVENDER;
    }
}
