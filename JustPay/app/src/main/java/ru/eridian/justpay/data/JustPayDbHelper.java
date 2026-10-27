package ru.eridian.justpay.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * База данных приложения: аккаунты, подписки аккаунта и дневная статистика использования.
 * Подписки и статистика принадлежат аккаунту и удаляются вместе с ним (внешние ключи).
 * Новый аккаунт пуст: никаких демо-записей приложение не создаёт.
 */
public class JustPayDbHelper extends SQLiteOpenHelper {
    public static final String DB_NAME = "justpay.db";
    public static final int DB_VERSION = 4;

    // accounts
    public static final String T_ACCOUNTS = "accounts";
    public static final String A_ID = "_id";
    public static final String A_NAME = "name";
    public static final String A_EMAIL = "email";
    public static final String A_HASH = "pwd_hash";
    public static final String A_SALT = "salt";
    public static final String A_ITER = "iterations";
    public static final String A_CREATED = "created_at";
    public static final String A_AVATAR = "avatar";
    public static final String A_CURRENCY = "currency";

    // subscriptions
    public static final String TABLE = "subscriptions";
    public static final String COL_ID = "_id";
    public static final String COL_ACCOUNT = "account_id";
    public static final String COL_NAME = "name";
    public static final String COL_MARK = "mark";
    public static final String COL_PRICE = "price";
    public static final String COL_CYCLE = "cycle";
    public static final String COL_NEXT = "next_date";
    public static final String COL_PREV = "prev_date";
    public static final String COL_CATEGORY = "category";
    public static final String COL_STATUS = "status";
    public static final String COL_CARD = "card";
    public static final String COL_SINCE = "since";
    public static final String COL_REMIND = "remind_days";
    public static final String COL_PACKAGE = "app_package";
    public static final String COL_CREATED = "created_at";

    // usage_days
    public static final String T_USAGE = "usage_days";
    public static final String U_SUB = "sub_id";
    public static final String U_DAY = "day";
    public static final String U_OPENS = "auto_opens";
    public static final String U_SECONDS = "auto_seconds";
    public static final String U_MANUAL = "manual";

    public JustPayDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createAccounts(db);
        createSubscriptions(db);
        createUsage(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 3) {
            // До версии 3 аккаунтов не было, а в таблице лежали только демо-записи.
            // Их выбрасываем: каждая подписка теперь принадлежит аккаунту.
            db.execSQL("DROP TABLE IF EXISTS " + T_USAGE);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE);
            createAccounts(db);
            createSubscriptions(db);
            createUsage(db);
        } else if (oldVersion < 4) {
            // Версия 4: аватар и валюта аккаунта. У существующих аккаунтов остаются значения по умолчанию.
            db.execSQL("ALTER TABLE " + T_ACCOUNTS + " ADD COLUMN " + A_AVATAR
                    + " TEXT NOT NULL DEFAULT 'initials:0'");
            db.execSQL("ALTER TABLE " + T_ACCOUNTS + " ADD COLUMN " + A_CURRENCY
                    + " TEXT NOT NULL DEFAULT 'RUB'");
        }
    }

    private static void createAccounts(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + T_ACCOUNTS + " ("
                + A_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + A_NAME + " TEXT NOT NULL, "
                + A_EMAIL + " TEXT NOT NULL UNIQUE, "
                + A_HASH + " TEXT NOT NULL, "
                + A_SALT + " TEXT NOT NULL, "
                + A_ITER + " INTEGER NOT NULL, "
                + A_CREATED + " TEXT NOT NULL, "
                + A_AVATAR + " TEXT NOT NULL DEFAULT 'initials:0', "
                + A_CURRENCY + " TEXT NOT NULL DEFAULT 'RUB')");
    }

    private static void createSubscriptions(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE + " ("
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_ACCOUNT + " INTEGER NOT NULL REFERENCES " + T_ACCOUNTS + "(" + A_ID + ") ON DELETE CASCADE, "
                + COL_NAME + " TEXT NOT NULL, "
                + COL_MARK + " TEXT NOT NULL, "
                + COL_PRICE + " REAL NOT NULL, "
                + COL_CYCLE + " TEXT NOT NULL, "
                + COL_NEXT + " TEXT, "
                + COL_PREV + " TEXT, "
                + COL_CATEGORY + " TEXT NOT NULL, "
                + COL_STATUS + " TEXT NOT NULL, "
                + COL_CARD + " TEXT NOT NULL DEFAULT '', "
                + COL_SINCE + " TEXT NOT NULL, "
                + COL_REMIND + " INTEGER NOT NULL DEFAULT -1, "
                + COL_PACKAGE + " TEXT, "
                + COL_CREATED + " TEXT NOT NULL)");
        db.execSQL("CREATE INDEX idx_sub_account ON " + TABLE + "(" + COL_ACCOUNT + ")");
    }

    private static void createUsage(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + T_USAGE + " ("
                + U_SUB + " INTEGER NOT NULL REFERENCES " + TABLE + "(" + COL_ID + ") ON DELETE CASCADE, "
                + U_DAY + " TEXT NOT NULL, "
                + U_OPENS + " INTEGER NOT NULL DEFAULT 0, "
                + U_SECONDS + " INTEGER NOT NULL DEFAULT 0, "
                + U_MANUAL + " INTEGER NOT NULL DEFAULT 0, "
                + "PRIMARY KEY (" + U_SUB + ", " + U_DAY + "))");
    }
}
