package ru.eridian.justpay.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.time.LocalDate;
import java.util.Locale;

/** Регистрация, вход и удаление локальных аккаунтов. */
public final class AccountRepository {
    private static AccountRepository instance;

    private final JustPayDbHelper helper;

    private AccountRepository(Context context) {
        helper = JustPayDatabase.helper(context);
    }

    public static synchronized AccountRepository get(Context context) {
        if (instance == null) instance = new AccountRepository(context);
        return instance;
    }

    /** Ошибка с текстом, который можно показать пользователю. */
    public static class AccountException extends Exception {
        public AccountException(String message) {
            super(message);
        }
    }

    public static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public Account register(String name, String email, String password) throws AccountException {
        String mail = normalizeEmail(email);
        if (exists(mail)) throw new AccountException("Аккаунт с такой почтой уже есть на этом устройстве");

        String salt = PasswordHasher.newSalt();
        ContentValues v = new ContentValues();
        v.put(JustPayDbHelper.A_NAME, name.trim());
        v.put(JustPayDbHelper.A_EMAIL, mail);
        v.put(JustPayDbHelper.A_SALT, salt);
        v.put(JustPayDbHelper.A_ITER, PasswordHasher.ITERATIONS);
        v.put(JustPayDbHelper.A_HASH, PasswordHasher.hash(password, salt, PasswordHasher.ITERATIONS));
        v.put(JustPayDbHelper.A_CREATED, LocalDate.now().toString());
        long id = helper.getWritableDatabase().insert(JustPayDbHelper.T_ACCOUNTS, null, v);
        if (id < 0) throw new AccountException("Не удалось создать аккаунт");
        return find(id);
    }

    /** Возвращает аккаунт, если почта и пароль верны, иначе null. */
    public Account login(String email, String password) {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.query(JustPayDbHelper.T_ACCOUNTS, null,
                JustPayDbHelper.A_EMAIL + " = ?", new String[]{normalizeEmail(email)}, null, null, null)) {
            if (!c.moveToFirst()) {
                // Считаем хеш и для несуществующей почты, чтобы ответ не выдавал, есть ли такой аккаунт
                PasswordHasher.hash(password, PasswordHasher.newSalt(), PasswordHasher.ITERATIONS);
                return null;
            }
            String salt = c.getString(c.getColumnIndexOrThrow(JustPayDbHelper.A_SALT));
            int iter = c.getInt(c.getColumnIndexOrThrow(JustPayDbHelper.A_ITER));
            String hash = c.getString(c.getColumnIndexOrThrow(JustPayDbHelper.A_HASH));
            if (!PasswordHasher.matches(password, salt, iter, hash)) return null;
            return read(c);
        }
    }

    public Account find(long id) {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.query(JustPayDbHelper.T_ACCOUNTS, null,
                JustPayDbHelper.A_ID + " = ?", new String[]{String.valueOf(id)}, null, null, null)) {
            return c.moveToFirst() ? read(c) : null;
        }
    }

    public boolean exists(String normalizedEmail) {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.query(JustPayDbHelper.T_ACCOUNTS, new String[]{JustPayDbHelper.A_ID},
                JustPayDbHelper.A_EMAIL + " = ?", new String[]{normalizedEmail}, null, null, null)) {
            return c.moveToFirst();
        }
    }

    /** Удаляет аккаунт вместе с его подписками и статистикой (каскадом). */
    public void delete(long id) {
        helper.getWritableDatabase().delete(JustPayDbHelper.T_ACCOUNTS,
                JustPayDbHelper.A_ID + " = ?", new String[]{String.valueOf(id)});
    }

    public void setAvatar(long id, AvatarSpec avatar) {
        updateColumn(id, JustPayDbHelper.A_AVATAR, avatar.toString());
    }

    public void setCurrency(long id, Currency currency) {
        updateColumn(id, JustPayDbHelper.A_CURRENCY, currency.name());
    }

    /**
     * Меняет валюту аккаунта и, если factor не равен 1, умножает на него цены всех его подписок.
     * Всё выполняется одной транзакцией: либо меняется и валюта, и цены, либо ничего.
     * Цена округляется до копеек и не опускается ниже одной копейки, чтобы остаться больше нуля.
     */
    public void changeCurrency(long id, Currency currency, double factor) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            setCurrency(id, currency);
            if (factor != 1.0) {
                db.execSQL("UPDATE " + JustPayDbHelper.TABLE + " SET " + JustPayDbHelper.COL_PRICE
                                + " = MAX(0.01, ROUND(" + JustPayDbHelper.COL_PRICE + " * ?, 2)) WHERE "
                                + JustPayDbHelper.COL_ACCOUNT + " = ?",
                        new Object[]{factor, id});
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private void updateColumn(long id, String column, String value) {
        ContentValues v = new ContentValues();
        v.put(column, value);
        helper.getWritableDatabase().update(JustPayDbHelper.T_ACCOUNTS, v,
                JustPayDbHelper.A_ID + " = ?", new String[]{String.valueOf(id)});
    }

    private static Account read(Cursor c) {
        Account a = new Account();
        a.id = c.getLong(c.getColumnIndexOrThrow(JustPayDbHelper.A_ID));
        a.name = c.getString(c.getColumnIndexOrThrow(JustPayDbHelper.A_NAME));
        a.email = c.getString(c.getColumnIndexOrThrow(JustPayDbHelper.A_EMAIL));
        a.avatar = c.getString(c.getColumnIndexOrThrow(JustPayDbHelper.A_AVATAR));
        a.currency = c.getString(c.getColumnIndexOrThrow(JustPayDbHelper.A_CURRENCY));
        return a;
    }
}
