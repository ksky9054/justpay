package ru.eridian.justpay.data;

import java.util.Locale;

/** Локальный аккаунт: существует только на этом устройстве. */
public class Account {
    public long id;
    public String name;
    public String email;
    /** Аватар в виде «знак:цвет», см. AvatarSpec. */
    public String avatar = AvatarSpec.DEFAULT;
    /** Код валюты из Currency. */
    public String currency = Currency.RUB.name();

    public AvatarSpec avatarSpec() {
        return AvatarSpec.parse(avatar);
    }

    public Currency currencyValue() {
        return Currency.fromCode(currency);
    }

    /** Две заглавные буквы для круглой аватарки. */
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
