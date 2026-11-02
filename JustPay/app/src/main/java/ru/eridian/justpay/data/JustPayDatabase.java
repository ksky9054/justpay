package ru.eridian.justpay.data;

import android.content.Context;

/** Один общий помощник базы на всё приложение. */
final class JustPayDatabase {
    private JustPayDatabase() {}

    private static JustPayDbHelper helper;

    static synchronized JustPayDbHelper helper(Context context) {
        if (helper == null) helper = new JustPayDbHelper(context.getApplicationContext());
        return helper;
    }
}
