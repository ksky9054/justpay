package ru.eridian.justpay.data;

/**
 * Аватар аккаунта: знак и цвет либо собственная фотография (знак «photo»). Хранится строкой
 * «знак:цвет». Сама фотография не в базе, а в приватной папке приложения.
 */
public final class AvatarSpec {
    public static final String INITIALS = "initials";
    public static final String PLANET = "planet";
    public static final String STAR = "star";
    public static final String MOON = "moon";
    public static final String COMET = "comet";
    public static final String RING = "ring";
    /** Фотография из галереи, лежит в приватной папке приложения (AvatarPhoto). */
    public static final String PHOTO = "photo";

    /** Порядок знаков в выборе. */
    public static final String[] SYMBOLS = {INITIALS, PLANET, STAR, MOON, COMET, RING, PHOTO};
    /** Цвета: акцент JustPay и три варианта акцента из макета, ещё два в тон им. */
    public static final int[] COLORS = {
            0xFF8B9BFF, 0xFF5FE0A8, 0xFFFFB45C, 0xFFFF8A7A, 0xFFC58BFF, 0xFF6FD3FF};
    public static final String[] COLOR_NAMES = {"Индиго", "Мята", "Янтарь", "Коралл", "Сирень", "Небо"};
    public static final String[] SYMBOL_NAMES = {"Инициалы", "Планета", "Звезда", "Луна", "Комета", "Кольцо", "Фото"};

    public static final String DEFAULT = INITIALS + ":0";

    public final String symbol;
    public final int colorIndex;

    public AvatarSpec(String symbol, int colorIndex) {
        this.symbol = indexOf(symbol) >= 0 ? symbol : INITIALS;
        this.colorIndex = colorIndex >= 0 && colorIndex < COLORS.length ? colorIndex : 0;
    }

    /** Разбирает строку из базы; при любой ошибке возвращает аватар по умолчанию. */
    public static AvatarSpec parse(String s) {
        if (s == null) return new AvatarSpec(INITIALS, 0);
        String[] p = s.split(":");
        int color = 0;
        if (p.length > 1) {
            try {
                color = Integer.parseInt(p[1]);
            } catch (NumberFormatException ignored) {
                color = 0;
            }
        }
        return new AvatarSpec(p.length > 0 ? p[0] : INITIALS, color);
    }

    public static int indexOf(String symbol) {
        for (int i = 0; i < SYMBOLS.length; i++) if (SYMBOLS[i].equals(symbol)) return i;
        return -1;
    }

    public int color() {
        return COLORS[colorIndex];
    }

    public AvatarSpec withSymbol(String s) {
        return new AvatarSpec(s, colorIndex);
    }

    public AvatarSpec withColor(int i) {
        return new AvatarSpec(symbol, i);
    }

    @Override
    public String toString() {
        return symbol + ":" + colorIndex;
    }
}
