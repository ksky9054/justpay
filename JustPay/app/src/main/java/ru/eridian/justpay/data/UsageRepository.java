package ru.eridian.justpay.data;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/** Дневная статистика использования подписок (таблица usage_days). */
public final class UsageRepository {
    private static UsageRepository instance;

    private final JustPayDbHelper helper;
    private final Prefs prefs;

    private UsageRepository(Context context) {
        helper = JustPayDatabase.helper(context);
        prefs = new Prefs(context);
    }

    public static synchronized UsageRepository get(Context context) {
        if (instance == null) instance = new UsageRepository(context.getApplicationContext());
        return instance;
    }

    /** Подписка принадлежит аккаунту, в который выполнен вход. */
    private boolean owns(SQLiteDatabase db, long subId) {
        try (Cursor c = db.query(JustPayDbHelper.TABLE, new String[]{JustPayDbHelper.COL_ID},
                JustPayDbHelper.COL_ID + " = ? AND " + JustPayDbHelper.COL_ACCOUNT + " = ?",
                new String[]{String.valueOf(subId), String.valueOf(prefs.accountId())}, null, null, null)) {
            return c.moveToFirst();
        }
    }

    public Map<LocalDate, UsageDay> days(long subId) {
        Map<LocalDate, UsageDay> out = new HashMap<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        if (!owns(db, subId)) return out;
        try (Cursor c = db.query(JustPayDbHelper.T_USAGE, null,
                JustPayDbHelper.U_SUB + " = ?", new String[]{String.valueOf(subId)}, null, null, null)) {
            while (c.moveToNext()) {
                UsageDay d = read(c);
                out.put(d.day, d);
            }
        }
        return out;
    }

    /** Статистика всех подписок аккаунта: номер подписки -> дни. */
    public Map<Long, Map<LocalDate, UsageDay>> allForAccount() {
        Map<Long, Map<LocalDate, UsageDay>> out = new HashMap<>();
        if (prefs.accountId() <= 0) return out;
        SQLiteDatabase db = helper.getReadableDatabase();
        String sql = "SELECT u.* FROM " + JustPayDbHelper.T_USAGE + " u JOIN " + JustPayDbHelper.TABLE
                + " s ON s." + JustPayDbHelper.COL_ID + " = u." + JustPayDbHelper.U_SUB
                + " WHERE s." + JustPayDbHelper.COL_ACCOUNT + " = ?";
        try (Cursor c = db.rawQuery(sql, new String[]{String.valueOf(prefs.accountId())})) {
            while (c.moveToNext()) {
                long sub = c.getLong(c.getColumnIndexOrThrow(JustPayDbHelper.U_SUB));
                UsageDay d = read(c);
                Map<LocalDate, UsageDay> m = out.get(sub);
                if (m == null) {
                    m = new HashMap<>();
                    out.put(sub, m);
                }
                m.put(d.day, d);
            }
        }
        return out;
    }

    /**
     * Меняет число ручных отметок за день на delta (но не ниже нуля).
     * Возвращает новое число отметок или -1, если подписка чужая.
     */
    public int addManual(long subId, LocalDate day, int delta) {
        SQLiteDatabase db = helper.getWritableDatabase();
        if (!owns(db, subId)) return -1;
        ensureRow(db, subId, day);
        db.execSQL("UPDATE " + JustPayDbHelper.T_USAGE + " SET " + JustPayDbHelper.U_MANUAL
                        + " = MAX(0, " + JustPayDbHelper.U_MANUAL + " + ?) WHERE " + JustPayDbHelper.U_SUB
                        + " = ? AND " + JustPayDbHelper.U_DAY + " = ?",
                new Object[]{delta, subId, day.toString()});
        try (Cursor c = db.query(JustPayDbHelper.T_USAGE, new String[]{JustPayDbHelper.U_MANUAL},
                JustPayDbHelper.U_SUB + " = ? AND " + JustPayDbHelper.U_DAY + " = ?",
                new String[]{String.valueOf(subId), day.toString()}, null, null, null)) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }

    /** Записывает данные приложения за день, полученные из статистики телефона. */
    public void saveAuto(long subId, LocalDate day, int opens, int seconds) {
        SQLiteDatabase db = helper.getWritableDatabase();
        if (!owns(db, subId)) return;
        ensureRow(db, subId, day);
        db.execSQL("UPDATE " + JustPayDbHelper.T_USAGE + " SET " + JustPayDbHelper.U_OPENS + " = ?, "
                        + JustPayDbHelper.U_SECONDS + " = ? WHERE " + JustPayDbHelper.U_SUB + " = ? AND "
                        + JustPayDbHelper.U_DAY + " = ?",
                new Object[]{opens, seconds, subId, day.toString()});
    }

    /** Сбрасывает данные приложения (при отвязке), ручные отметки остаются. */
    public void clearAuto(long subId) {
        SQLiteDatabase db = helper.getWritableDatabase();
        if (!owns(db, subId)) return;
        db.execSQL("UPDATE " + JustPayDbHelper.T_USAGE + " SET " + JustPayDbHelper.U_OPENS + " = 0, "
                + JustPayDbHelper.U_SECONDS + " = 0 WHERE " + JustPayDbHelper.U_SUB + " = ?", new Object[]{subId});
    }

    private static void ensureRow(SQLiteDatabase db, long subId, LocalDate day) {
        db.execSQL("INSERT OR IGNORE INTO " + JustPayDbHelper.T_USAGE + " (" + JustPayDbHelper.U_SUB + ", "
                + JustPayDbHelper.U_DAY + ") VALUES (?, ?)", new Object[]{subId, day.toString()});
    }

    private static UsageDay read(Cursor c) {
        return new UsageDay(
                LocalDate.parse(c.getString(c.getColumnIndexOrThrow(JustPayDbHelper.U_DAY))),
                c.getInt(c.getColumnIndexOrThrow(JustPayDbHelper.U_OPENS)),
                c.getInt(c.getColumnIndexOrThrow(JustPayDbHelper.U_SECONDS)),
                c.getInt(c.getColumnIndexOrThrow(JustPayDbHelper.U_MANUAL)));
    }
}
