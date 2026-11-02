package ru.eridian.justpay.data;

import java.util.Locale;

/** Локальный аккаунт, хранится только на телефоне */
public class Account {
    public long id;
    public String name;
    public String email;
    public String avatar = AvatarSpec.DEFAULT;
    public String currency = Currency.RUB.name();
    /** Дата согласия на обработку данных, у старых аккаунтов пусто */
    public String consentDate = "";

    public AvatarSpec avatarSpec() {
        return AvatarSpec.parse(avatar);
    }

    public Currency currencyValue() {
        return Currency.fromCode(currency);
    }

    /** Инициалы для аватарки */
    public String initials() {
        String[] parts = name.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (!p.isEmpty() && sb.length() < 2) sb.append(p.substring(0, 1));
        }
        if (sb.length() == 1 && name.trim().length() > 1) sb.append(name.trim().charAt(1));
        return sb.toString().toUpperCase(Locale.getDefault());
    }
}
