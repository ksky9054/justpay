package ru.eridian.justpay.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.window.OnBackInvokedDispatcher;

import java.time.LocalDate;

import ru.eridian.justpay.R;
import ru.eridian.justpay.data.Account;
import ru.eridian.justpay.data.AccountRepository;
import ru.eridian.justpay.data.Prefs;
import ru.eridian.justpay.data.SubscriptionRepository;
import ru.eridian.justpay.util.Fmt;

/** Общая основа экранов: тема из настроек, отрисовка под системными панелями, переходы. */
public abstract class BaseActivity extends Activity {
    protected Prefs prefs;
    protected SubscriptionRepository repo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        prefs = new Prefs(this);
        repo = SubscriptionRepository.get(this);
        // Суммы показываются в валюте аккаунта; без входа это рубль
        Account signedIn = prefs.isSignedIn() ? AccountRepository.get(this).find(prefs.accountId()) : null;
        Fmt.setCurrency(signedIn == null ? null : signedIn.currencyValue());
        setTheme(prefs.isDark() ? R.style.Theme_JustPay_Dark : R.style.Theme_JustPay_Light);
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= 30) {
            getWindow().setDecorFitsSystemWindows(false);
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | (prefs.isDark() ? 0 : View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR));
        }
    }

    // ----- кнопка и жест «Назад» -----

    /** Возвращает true, если экран сам обработал «Назад»; иначе экран просто закроется. */
    protected boolean onBack() {
        return false;
    }

    @SuppressLint("GestureBackNavigation")
    @Override
    public void onBackPressed() {
        if (!onBack()) super.onBackPressed();
    }

    /**
     * Начиная с Android 13 при targetSdk 36 жест «Назад» не вызывает onBackPressed,
     * поэтому экраны со своей обработкой регистрируют обратный вызов.
     */
    protected void interceptBack() {
        if (Build.VERSION.SDK_INT >= 33) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT, () -> {
                        if (!onBack()) finish();
                    });
        }
    }

    /** С Android 13 для показа уведомлений нужно разрешение, которое запрашивается у пользователя. */
    protected void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 100);
        }
    }

    protected LocalDate today() {
        return LocalDate.now();
    }

    // ----- переходы между экранами -----

    public void openDetail(long id) {
        startActivity(new Intent(this, DetailActivity.class).putExtra(DetailActivity.EXTRA_ID, id));
        slideInRight();
    }

    /** id = 0 открывает форму новой подписки. */
    public void openEdit(long id) {
        startActivity(new Intent(this, EditActivity.class).putExtra(EditActivity.EXTRA_ID, id));
        overridePendingTransition(R.anim.slide_in_bottom, R.anim.hold);
    }

    public void openNotifications() {
        startActivity(new Intent(this, NotificationsActivity.class));
        slideInRight();
    }

    protected void slideInRight() {
        overridePendingTransition(R.anim.slide_in_right, R.anim.hold);
    }

    /** Закрывает экран, который выехал справа. */
    protected void finishToRight() {
        finish();
        overridePendingTransition(R.anim.hold, R.anim.slide_out_right);
    }

    /** Закрывает экран, который выехал снизу. */
    protected void finishToBottom() {
        finish();
        overridePendingTransition(R.anim.hold, R.anim.slide_out_bottom);
    }
}
