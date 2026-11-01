package ru.eridian.justpay.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import ru.eridian.justpay.R;
import ru.eridian.justpay.data.AccountRepository;
import ru.eridian.justpay.reminder.ReminderScheduler;
import ru.eridian.justpay.usage.UsageSync;

/** Главный экран с четырьмя вкладками: список, календарь, аналитика, профиль. */
public class MainActivity extends BaseActivity {
    private static final String STATE_TAB = "tab";
    private static final String STATE_FILTER = "filter";
    /** Код запроса системного выбора фото для аватара. */
    static final int REQ_PHOTO = 41;
    /** Какую вкладку открыть (из уведомления): 0 главная, 1 календарь, 2 аналитика, 3 профиль. */
    public static final String EXTRA_TAB = "tab";

    private View[] screens;
    private View[] navs;
    private View fab;
    private int tab = 0;

    private HomeTab home;
    private CalendarTab calendar;
    private StatsTab stats;
    private ProfileTab profile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!prefs.isOnboarded()) {
            startActivity(new Intent(this, OnboardingActivity.class));
            finish();
            return;
        }
        // Без входа в аккаунт данных нет: сначала экран входа (или регистрации)
        if (!prefs.isSignedIn() || AccountRepository.get(this).find(prefs.accountId()) == null) {
            prefs.clearAccount();
            startActivity(new Intent(this, AuthActivity.class));
            finish();
            return;
        }
        setContentView(R.layout.activity_main);
        Ui.fitSystemBars(findViewById(R.id.root));

        home = new HomeTab(this, findViewById(R.id.screen_home));
        calendar = new CalendarTab(this, findViewById(R.id.screen_cal));
        stats = new StatsTab(this, findViewById(R.id.screen_stats));
        profile = new ProfileTab(this, findViewById(R.id.screen_profile));

        screens = new View[]{
                findViewById(R.id.screen_home), findViewById(R.id.screen_cal),
                findViewById(R.id.screen_stats), findViewById(R.id.screen_profile)};
        navs = new View[]{
                findViewById(R.id.nav_home), findViewById(R.id.nav_cal),
                findViewById(R.id.nav_stats), findViewById(R.id.nav_profile)};
        for (int i = 0; i < navs.length; i++) {
            final int index = i;
            navs[i].setOnClickListener(v -> selectTab(index));
        }

        fab = findViewById(R.id.fab);
        fab.setOnClickListener(v -> openEdit(0));

        if (savedInstanceState != null) {
            tab = savedInstanceState.getInt(STATE_TAB, 0);
            home.setFilter(savedInstanceState.getString(STATE_FILTER, "all"));
        } else {
            tab = clampTab(getIntent().getIntExtra(EXTRA_TAB, 0));
        }
        selectTab(tab);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        // Приложение уже открыто, а пользователь нажал уведомление: переходим на нужную вкладку
        if (screens != null && intent.hasExtra(EXTRA_TAB)) selectTab(clampTab(intent.getIntExtra(EXTRA_TAB, 0)));
    }

    private static int clampTab(int t) {
        return t < 0 || t > 3 ? 0 : t;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (home == null) return;
        repo.rollForward(today());
        ReminderScheduler.reschedule(this);
        if (!prefs.notifPermissionAsked()) {
            // Один раз объясняем необходимость разрешения системным окном при первом запуске
            prefs.setNotifPermissionAsked(true);
            requestNotificationPermission();
        }
        refreshAll();
        Toaster.flush(this);
        // Статистика из приложений сервисов читается в фоне и обновляет экраны, когда готова
        UsageSync.syncAsync(this, () -> runOnUiThread(() -> {
            if (!isFinishing() && !isDestroyed()) stats.refresh();
        }));
    }

    void refreshAll() {
        home.refresh();
        calendar.refresh();
        stats.refresh();
        profile.refresh();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_PHOTO && resultCode == RESULT_OK && data != null
                && data.getData() != null && profile != null) {
            profile.onPhotoPicked(data.getData());
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putInt(STATE_TAB, tab);
        if (home != null) out.putString(STATE_FILTER, home.filter());
    }

    private void selectTab(int index) {
        tab = index;
        Ui.hideKeyboard(this);
        for (int i = 0; i < screens.length; i++) {
            screens[i].setVisibility(i == index ? View.VISIBLE : View.GONE);
            navs[i].setSelected(i == index);
        }
        fab.setVisibility(index == 0 ? View.VISIBLE : View.GONE);
    }
}
