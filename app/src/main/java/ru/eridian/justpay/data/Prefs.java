package ru.eridian.justpay.data;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

/**
 * Простые настройки в SharedPreferences. Общие для устройства: тема, знакомство, текущий вход.
 * Настройки напоминаний хранятся отдельно для каждого аккаунта.
 */
public final class Prefs {
    private static final String FILE = "justpay_prefs";
    private static final String K_ONBOARDED = "onboarded";
    private static final String K_DARK = "dark";
    private static final String K_ACCOUNT = "account_id";
    private static final String K_PERM_ASKED = "notif_permission_asked";

    // Ключи, к которым добавляется номер аккаунта
    private static final String K_REMIND = "remind";
    private static final String K_REMIND_DAYS = "remind_days";
    private static final String K_SEEN_DATE = "notif_seen_date";
    private static final String K_NOTIFIED = "notified_keys";
    private static final String K_STREAK_REMIND = "streak_remind";
    private static final String K_IDLE_REMIND = "idle_remind";
    private static final String K_STREAK_CHECKED = "streak_checked";
    private static final String K_IDLE_CHECKED = "idle_checked";
    private static final String K_IDLE_SENT = "idle_sent";

    private final SharedPreferences sp;

    public Prefs(Context context) {
        sp = context.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    private String k(String base) {
        return base + "_" + accountId();
    }

    // ----- устройство -----

    public boolean isOnboarded() { return sp.getBoolean(K_ONBOARDED, false); }
    public void setOnboarded(boolean v) { sp.edit().putBoolean(K_ONBOARDED, v).apply(); }

    public boolean isDark() { return sp.getBoolean(K_DARK, true); }
    public void setDark(boolean v) { sp.edit().putBoolean(K_DARK, v).apply(); }

    /** Уже спрашивали разрешение на уведомления (Android 13+). */
    public boolean notifPermissionAsked() { return sp.getBoolean(K_PERM_ASKED, false); }
    public void setNotifPermissionAsked(boolean v) { sp.edit().putBoolean(K_PERM_ASKED, v).apply(); }

    // ----- вход -----

    /** Номер аккаунта, в который выполнен вход, или 0, если входа нет. */
    public long accountId() { return sp.getLong(K_ACCOUNT, 0); }
    public void setAccountId(long id) { sp.edit().putLong(K_ACCOUNT, id).apply(); }
    public void clearAccount() { sp.edit().remove(K_ACCOUNT).apply(); }
    public boolean isSignedIn() { return accountId() > 0; }

    // ----- напоминания (на аккаунт) -----

    public boolean isRemind() { return sp.getBoolean(k(K_REMIND), true); }
    public void setRemind(boolean v) { sp.edit().putBoolean(k(K_REMIND), v).apply(); }

    public int remindDays() { return sp.getInt(k(K_REMIND_DAYS), 3); }
    public void setRemindDays(int v) { sp.edit().putInt(k(K_REMIND_DAYS), v).apply(); }

    /** Ключи «подписка:дата списания», о которых уже отправлено уведомление. */
    public Set<String> notified() {
        return new HashSet<>(sp.getStringSet(k(K_NOTIFIED), new HashSet<>()));
    }

    public void setNotified(Set<String> keys) {
        sp.edit().putStringSet(k(K_NOTIFIED), new HashSet<>(keys)).apply();
    }

    // ----- напоминания об активности (на аккаунт) -----

    /** Вечернее напоминание, что серия может прерваться. */
    public boolean isStreakRemind() { return sp.getBoolean(k(K_STREAK_REMIND), true); }
    public void setStreakRemind(boolean v) { sp.edit().putBoolean(k(K_STREAK_REMIND), v).apply(); }

    /** Еженедельное напоминание о подписках без активности. */
    public boolean isIdleRemind() { return sp.getBoolean(k(K_IDLE_REMIND), true); }
    public void setIdleRemind(boolean v) { sp.edit().putBoolean(k(K_IDLE_REMIND), v).apply(); }

    /** Дата (ISO) последней проверки серии: нужна, чтобы проверять один раз в день. */
    public String streakChecked() { return sp.getString(k(K_STREAK_CHECKED), ""); }
    public void setStreakChecked(String iso) { sp.edit().putString(k(K_STREAK_CHECKED), iso).apply(); }

    public String idleChecked() { return sp.getString(k(K_IDLE_CHECKED), ""); }
    public void setIdleChecked(String iso) { sp.edit().putString(k(K_IDLE_CHECKED), iso).apply(); }

    /** Дата (ISO), когда последний раз отправлено напоминание о неиспользуемых подписках. */
    public String idleSent() { return sp.getString(k(K_IDLE_SENT), ""); }
    public void setIdleSent(String iso) { sp.edit().putString(k(K_IDLE_SENT), iso).apply(); }

    /** Забывает отправленные уведомления по подписке, чтобы новое напоминание могло прийти. */
    public void forgetNotified(long subscriptionId) {
        Set<String> keys = notified();
        keys.removeIf(key -> key.startsWith(subscriptionId + ":"));
        setNotified(keys);
    }

    /** Дата (ISO), когда пользователь последний раз открывал уведомления. */
    public String notifSeenDate() { return sp.getString(k(K_SEEN_DATE), ""); }
    public void setNotifSeenDate(String iso) { sp.edit().putString(k(K_SEEN_DATE), iso).apply(); }

    /** Стирает все настройки аккаунта: нужно при его удалении. */
    public void wipeAccountSettings(long id) {
        String suffix = "_" + id;
        SharedPreferences.Editor e = sp.edit();
        for (String key : new HashSet<>(sp.getAll().keySet())) {
            if (key.endsWith(suffix) && !key.equals(K_ACCOUNT)) e.remove(key);
        }
        e.apply();
    }
}
