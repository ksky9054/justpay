package ru.eridian.justpay.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import ru.eridian.justpay.reminder.ReminderScheduler;

/**
 * Вся работа с таблицей подписок. Видны и изменяются только подписки аккаунта,
 * в который выполнен вход; без входа список пуст.
 */
public final class SubscriptionRepository {
    private static SubscriptionRepository instance;

    private final Context appContext;
    private final JustPayDbHelper helper;
    private final Prefs prefs;

    private SubscriptionRepository(Context context) {
        appContext = context.getApplicationContext();
        helper = JustPayDatabase.helper(appContext);
        prefs = new Prefs(appContext);
    }

    public static synchronized SubscriptionRepository get(Context context) {
        if (instance == null) instance = new SubscriptionRepository(context);
        return instance;
    }

    private long account() {
        return prefs.accountId();
    }

    public List<Subscription> all() {
        List<Subscription> out = new ArrayList<>();
        if (account() <= 0) return out;
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.query(JustPayDbHelper.TABLE, null,
                JustPayDbHelper.COL_ACCOUNT + " = ?", new String[]{String.valueOf(account())},
                null, null, JustPayDbHelper.COL_ID)) {
            while (c.moveToNext()) out.add(read(c));
        }
        return out;
    }

    public Subscription find(long id) {
        if (account() <= 0) return null;
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.query(JustPayDbHelper.TABLE, null,
                JustPayDbHelper.COL_ID + " = ? AND " + JustPayDbHelper.COL_ACCOUNT + " = ?",
                new String[]{String.valueOf(id), String.valueOf(account())}, null, null, null)) {
            return c.moveToFirst() ? read(c) : null;
        }
    }

    // После любого изменения данных пересчитываем время ближайшего напоминания.

    public long insert(Subscription s) {
        s.accountId = account();
        long id = helper.getWritableDatabase().insert(JustPayDbHelper.TABLE, null, values(s));
        ReminderScheduler.reschedule(appContext);
        return id;
    }

    public void update(Subscription s) {
        write(s);
        ReminderScheduler.reschedule(appContext);
    }

    public void delete(long id) {
        // Статистика использования удаляется каскадом
        helper.getWritableDatabase().delete(JustPayDbHelper.TABLE,
                JustPayDbHelper.COL_ID + " = ? AND " + JustPayDbHelper.COL_ACCOUNT + " = ?",
                new String[]{String.valueOf(id), String.valueOf(account())});
        ReminderScheduler.reschedule(appContext);
    }

    private void write(Subscription s) {
        helper.getWritableDatabase().update(JustPayDbHelper.TABLE, values(s),
                JustPayDbHelper.COL_ID + " = ? AND " + JustPayDbHelper.COL_ACCOUNT + " = ?",
                new String[]{String.valueOf(s.id), String.valueOf(account())});
    }

    /** Если дата списания уже прошла, переносит её на следующий период. */
    public void rollForward(LocalDate today) {
        boolean changed = false;
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            for (Subscription s : all()) {
                if (s.isActive() && s.next != null && s.next.isBefore(today)) {
                    s.prev = s.next;
                    s.next = Subscription.advance(s.next, s.cycle, today, true);
                    write(s);
                    changed = true;
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        if (changed) ReminderScheduler.reschedule(appContext);
    }

    private static ContentValues values(Subscription s) {
        ContentValues v = new ContentValues();
        v.put(JustPayDbHelper.COL_ACCOUNT, s.accountId);
        v.put(JustPayDbHelper.COL_NAME, s.name);
        v.put(JustPayDbHelper.COL_MARK, s.mark);
        v.put(JustPayDbHelper.COL_PRICE, s.price);
        v.put(JustPayDbHelper.COL_CYCLE, s.cycle);
        v.put(JustPayDbHelper.COL_NEXT, s.next == null ? null : s.next.toString());
        v.put(JustPayDbHelper.COL_PREV, s.prev == null ? null : s.prev.toString());
        v.put(JustPayDbHelper.COL_CATEGORY, s.category);
        v.put(JustPayDbHelper.COL_STATUS, s.status);
        v.put(JustPayDbHelper.COL_CARD, s.card == null ? "" : s.card);
        v.put(JustPayDbHelper.COL_SINCE, s.since);
        v.put(JustPayDbHelper.COL_REMIND, s.remindDays);
        v.put(JustPayDbHelper.COL_PACKAGE, s.appPackage);
        v.put(JustPayDbHelper.COL_CREATED, s.createdAt.toString());
        return v;
    }

    private static Subscription read(Cursor c) {
        Subscription s = new Subscription();
        s.id = c.getLong(c.getColumnIndexOrThrow(JustPayDbHelper.COL_ID));
        s.accountId = c.getLong(c.getColumnIndexOrThrow(JustPayDbHelper.COL_ACCOUNT));
        s.name = c.getString(c.getColumnIndexOrThrow(JustPayDbHelper.COL_NAME));
        s.mark = c.getString(c.getColumnIndexOrThrow(JustPayDbHelper.COL_MARK));
        s.price = c.getDouble(c.getColumnIndexOrThrow(JustPayDbHelper.COL_PRICE));
        s.cycle = c.getString(c.getColumnIndexOrThrow(JustPayDbHelper.COL_CYCLE));
        s.next = date(c, JustPayDbHelper.COL_NEXT);
        s.prev = date(c, JustPayDbHelper.COL_PREV);
        s.category = c.getString(c.getColumnIndexOrThrow(JustPayDbHelper.COL_CATEGORY));
        s.status = c.getString(c.getColumnIndexOrThrow(JustPayDbHelper.COL_STATUS));
        s.card = c.getString(c.getColumnIndexOrThrow(JustPayDbHelper.COL_CARD));
        s.since = c.getString(c.getColumnIndexOrThrow(JustPayDbHelper.COL_SINCE));
        s.remindDays = c.getInt(c.getColumnIndexOrThrow(JustPayDbHelper.COL_REMIND));
        int pk = c.getColumnIndexOrThrow(JustPayDbHelper.COL_PACKAGE);
        s.appPackage = c.isNull(pk) ? null : c.getString(pk);
        LocalDate created = date(c, JustPayDbHelper.COL_CREATED);
        s.createdAt = created == null ? LocalDate.now() : created;
        return s;
    }

    private static LocalDate date(Cursor c, String col) {
        int i = c.getColumnIndexOrThrow(col);
        return c.isNull(i) ? null : LocalDate.parse(c.getString(i));
    }
}
