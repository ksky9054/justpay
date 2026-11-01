package ru.eridian.justpay.ui;

import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ru.eridian.justpay.R;
import ru.eridian.justpay.data.Account;
import ru.eridian.justpay.data.AccountRepository;
import ru.eridian.justpay.data.AvatarSpec;
import ru.eridian.justpay.data.Currency;
import ru.eridian.justpay.data.Subscription;
import ru.eridian.justpay.net.ExchangeRates;
import ru.eridian.justpay.reminder.ReminderScheduler;
import ru.eridian.justpay.usage.UsageAccess;
import ru.eridian.justpay.util.AvatarPhoto;
import ru.eridian.justpay.util.Fmt;

/** Вкладка «Профиль»: аккаунт, тема, напоминания, доступ к статистике, способы оплаты. */
final class ProfileTab {
    private final MainActivity a;
    private final AvatarView avatar;
    private final TextView name, email, currencyText;
    private final TextView themeDark, themeLight;
    private final Switch remindSwitch, streakSwitch, idleSwitch;
    private final View remindDays;
    private final TextView day1, day3, day7, dayCustom;
    private final LinearLayout cards;
    private final TextView cardsEmpty, usageStatus;

    ProfileTab(MainActivity activity, View screen) {
        a = activity;
        avatar = screen.findViewById(R.id.pf_avatar);
        currencyText = screen.findViewById(R.id.pf_currency);
        name = screen.findViewById(R.id.pf_name);
        email = screen.findViewById(R.id.pf_email);
        themeDark = screen.findViewById(R.id.theme_dark);
        themeLight = screen.findViewById(R.id.theme_light);
        remindSwitch = screen.findViewById(R.id.remind_switch);
        streakSwitch = screen.findViewById(R.id.streak_switch);
        idleSwitch = screen.findViewById(R.id.idle_switch);
        remindDays = screen.findViewById(R.id.remind_days);
        day1 = screen.findViewById(R.id.day_1);
        day3 = screen.findViewById(R.id.day_3);
        day7 = screen.findViewById(R.id.day_7);
        dayCustom = screen.findViewById(R.id.day_custom);
        cards = screen.findViewById(R.id.pf_cards);
        cardsEmpty = screen.findViewById(R.id.pf_cards_empty);
        usageStatus = screen.findViewById(R.id.usage_status);

        avatar.setOnClickListener(v -> openAvatar());
        screen.findViewById(R.id.pf_avatar_edit).setOnClickListener(v -> openAvatar());
        screen.findViewById(R.id.currency_row).setOnClickListener(v -> openCurrency());
        themeDark.setOnClickListener(v -> setDark(true));
        themeLight.setOnClickListener(v -> setDark(false));

        screen.findViewById(R.id.remind_row).setOnClickListener(v -> {
            boolean on = !a.prefs.isRemind();
            a.prefs.setRemind(on);
            if (on) a.requestNotificationPermission();
            ReminderScheduler.reschedule(a);
            refresh();
        });
        screen.findViewById(R.id.streak_row).setOnClickListener(v -> {
            boolean on = !a.prefs.isStreakRemind();
            a.prefs.setStreakRemind(on);
            if (on) a.requestNotificationPermission();
            ReminderScheduler.reschedule(a);
            refresh();
        });
        screen.findViewById(R.id.idle_row).setOnClickListener(v -> {
            boolean on = !a.prefs.isIdleRemind();
            a.prefs.setIdleRemind(on);
            if (on) a.requestNotificationPermission();
            ReminderScheduler.reschedule(a);
            refresh();
        });
        day1.setOnClickListener(v -> setDays(1));
        day3.setOnClickListener(v -> setDays(3));
        day7.setOnClickListener(v -> setDays(7));
        dayCustom.setOnClickListener(v -> DaysSheet.show(a, a.prefs.remindDays(), this::setDays));

        screen.findViewById(R.id.usage_row).setOnClickListener(v ->
                a.startActivity(UsageAccess.settingsIntent(a)));
        screen.findViewById(R.id.row_onboarding).setOnClickListener(v ->
                a.startActivity(new Intent(a, OnboardingActivity.class)
                        .putExtra(OnboardingActivity.EXTRA_REPLAY, true)));
        screen.findViewById(R.id.row_logout).setOnClickListener(v -> signOut());
        screen.findViewById(R.id.row_delete).setOnClickListener(v -> confirmDelete());
    }

    private void openAvatar() {
        Account acc = AccountRepository.get(a).find(a.prefs.accountId());
        if (acc == null) return;
        Bitmap photo = AvatarPhoto.load(a, acc.id);
        AvatarSheet.show(a, acc, photo, spec -> {
            AccountRepository.get(a).setAvatar(acc.id, spec);
            refresh();
            Toaster.show(a, "Аватар обновлён");
        }, this::pickPhoto, () -> deletePhoto(acc));
    }

    /** Системный выбор картинки: отдельное разрешение на доступ к фото не требуется. */
    private void pickPhoto() {
        Intent pick = new Intent(Intent.ACTION_GET_CONTENT)
                .setType("image/*")
                .addCategory(Intent.CATEGORY_OPENABLE);
        try {
            a.startActivityForResult(pick, MainActivity.REQ_PHOTO);
        } catch (ActivityNotFoundException e) {
            Toaster.show(a, "На телефоне нет приложения для выбора фото");
        }
    }

    /** Результат выбора картинки: обрабатываем в фоне и делаем её аватаром. */
    void onPhotoPicked(Uri uri) {
        Account acc = AccountRepository.get(a).find(a.prefs.accountId());
        if (acc == null) return;
        Toaster.show(a, "Обрабатываем фото…");
        AvatarPhoto.saveAsync(a, acc.id, uri, ok -> {
            if (a.isFinishing() || a.isDestroyed()) return;
            if (ok) {
                AccountRepository.get(a).setAvatar(acc.id,
                        new AvatarSpec(AvatarSpec.PHOTO, acc.avatarSpec().colorIndex));
                refresh();
                Toaster.show(a, "Фото обновлено");
            } else {
                Toaster.show(a, "Не удалось открыть изображение");
            }
        });
    }

    private void deletePhoto(Account acc) {
        AvatarPhoto.delete(a, acc.id);
        if (AvatarSpec.PHOTO.equals(acc.avatarSpec().symbol)) {
            AccountRepository.get(a).setAvatar(acc.id,
                    new AvatarSpec(AvatarSpec.INITIALS, acc.avatarSpec().colorIndex));
        }
        refresh();
        Toaster.show(a, "Фото удалено");
    }

    private void openCurrency() {
        Account acc = AccountRepository.get(a).find(a.prefs.accountId());
        if (acc == null) return;
        CurrencySheet.show(a, acc.currencyValue(), c -> pickCurrency(acc, c));
    }

    private boolean loadingRates;

    /**
     * Смена валюты. Если в аккаунте есть подписки, пытаемся получить курс Банка России:
     * с интернетом предлагаем пересчитать цены, без него честно говорим, что пересчёт не работает,
     * и предлагаем сменить валюту, оставив цены прежними числами.
     */
    private void pickCurrency(Account acc, Currency to) {
        Currency from = acc.currencyValue();
        List<Subscription> subs = a.repo.all();
        if (subs.isEmpty()) {
            applyCurrency(acc, to, 1.0, "Валюта: " + to.title);
            return;
        }
        if (loadingRates) return;
        loadingRates = true;
        Toaster.show(a, "Получаем курс Банка России…");
        ExchangeRates.fetchAsync(rates -> {
            loadingRates = false;
            if (a.isFinishing() || a.isDestroyed()) return;
            if (rates.supports(from, to)) confirmConversion(acc, from, to, rates, subs);
            else offerWithoutConversion(acc, to, subs);
        }, () -> {
            loadingRates = false;
            if (a.isFinishing() || a.isDestroyed()) return;
            offerWithoutConversion(acc, to, subs);
        });
    }

    private void confirmConversion(Account acc, Currency from, Currency to, ExchangeRates rates,
                                   List<Subscription> subs) {
        double factor = rates.factor(from, to);
        Subscription biggest = subs.get(0);
        for (Subscription s : subs) if (s.price > biggest.price) biggest = s;
        double converted = Math.max(0.01, Math.round(biggest.price * factor * 100) / 100.0);
        String note = "Курс Банка России на " + rates.date + ": " + rates.describe(to, from) + ". "
                + "Цены " + subs.size() + " " + Fmt.pl(subs.size(), "подписки", "подписок", "подписок")
                + " будут пересчитаны: например, " + Fmt.price(biggest.price) + " станет "
                + Fmt.priceIn(to, converted) + ". При обратном пересчёте возможны отличия на копейки из-за округления.";
        ConfirmSheet.show(a, "Пересчитать цены в валюту «" + to.title + "»?", 0, null, note,
                "Пересчитать", false,
                () -> applyCurrency(acc, to, factor, "Цены пересчитаны: " + from.symbol + " → " + to.symbol));
    }

    private void offerWithoutConversion(Account acc, Currency to, List<Subscription> subs) {
        String note = "Не удалось получить курс: нет интернета или сервер недоступен, поэтому пересчитать цены сейчас нельзя. "
                + "Можно сменить валюту без пересчёта: цены останутся прежними числами (" + Fmt.price(subs.get(0).price)
                + " станет " + Fmt.priceIn(to, subs.get(0).price) + "), и их нужно будет проверить вручную. "
                + "Или отмените и повторите, когда появится сеть.";
        ConfirmSheet.show(a, "Конвертация не работает", 0, null, note, "Сменить без пересчёта", false,
                () -> applyCurrency(acc, to, 1.0, "Валюта: " + to.title + ". Цены не пересчитаны"));
    }

    private void applyCurrency(Account acc, Currency to, double factor, String message) {
        AccountRepository.get(a).changeCurrency(acc.id, to, factor);
        Fmt.setCurrency(to);
        a.refreshAll();
        Toaster.show(a, message);
    }

    private void setDark(boolean dark) {
        if (a.prefs.isDark() == dark) return;
        a.prefs.setDark(dark);
        a.recreate();
    }

    private void setDays(int n) {
        a.prefs.setRemindDays(n);
        ReminderScheduler.reschedule(a);
        refresh();
    }

    private void signOut() {
        a.prefs.clearAccount();
        endSession();
    }

    private void confirmDelete() {
        Account acc = AccountRepository.get(a).find(a.prefs.accountId());
        if (acc == null) return;
        ConfirmSheet.show(a, "Удалить аккаунт?", 0, null,
                "Аккаунт «" + acc.email + "», все его подписки и статистика будут удалены с телефона. Восстановить их будет нельзя.",
                "Удалить", true, () -> {
                    AccountRepository.get(a).delete(acc.id);
                    AvatarPhoto.delete(a, acc.id);
                    a.prefs.wipeAccountSettings(acc.id);
                    a.prefs.clearAccount();
                    endSession();
                });
    }

    /** После выхода или удаления: убираем напоминания и возвращаемся на экран входа. */
    private void endSession() {
        ReminderScheduler.reschedule(a);
        NotificationManager nm = (NotificationManager) a.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.cancelAll();
        a.startActivity(new Intent(a, AuthActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        a.finish();
    }

    void refresh() {
        Account acc = AccountRepository.get(a).find(a.prefs.accountId());
        if (acc != null) {
            AvatarSpec spec = acc.avatarSpec();
            Bitmap photo = AvatarSpec.PHOTO.equals(spec.symbol) ? AvatarPhoto.load(a, acc.id) : null;
            // Файл с фото могли удалить: тогда вместо «Фото» показываем инициалы
            if (AvatarSpec.PHOTO.equals(spec.symbol) && photo == null) spec = spec.withSymbol(AvatarSpec.INITIALS);
            avatar.setAvatar(spec, acc.initials(), photo);
            currencyText.setText(acc.currencyValue().title + ", " + acc.currencyValue().symbol);
            name.setText(acc.name);
            email.setText(acc.email);
        }

        boolean dark = a.prefs.isDark();
        themeDark.setSelected(dark);
        themeLight.setSelected(!dark);

        boolean remind = a.prefs.isRemind();
        remindSwitch.setChecked(remind);
        streakSwitch.setChecked(a.prefs.isStreakRemind());
        idleSwitch.setChecked(a.prefs.isIdleRemind());
        remindDays.setAlpha(remind ? 1f : 0.35f);
        dayCustom.setAlpha(remind ? 1f : 0.35f);
        int n = a.prefs.remindDays();
        day1.setSelected(n == 1);
        day3.setSelected(n == 3);
        day7.setSelected(n == 7);
        boolean custom = n != 1 && n != 3 && n != 7;
        dayCustom.setSelected(custom);
        dayCustom.setText(custom ? (n == 0 ? "В день списания" : "За " + n + " " + Fmt.pl(n, "день", "дня", "дней")) + " · изменить" : "Свой срок…");
        for (View d : new View[]{day1, day3, day7, dayCustom}) d.setEnabled(remind);

        boolean granted = UsageAccess.isGranted(a);
        usageStatus.setText(granted ? "Включён" : "Выключен · нажмите, чтобы включить");
        usageStatus.setTextColor(granted ? Ui.attr(a, R.attr.jpAct) : Ui.attr(a, R.attr.jpTx2));

        // Способы оплаты берутся из подписок: сколько подписок оплачивается каждым из них
        Map<String, Integer> counts = new LinkedHashMap<>();
        List<Subscription> all = a.repo.all();
        for (Subscription s : all) {
            if (!s.hasCard()) continue;
            String key = s.card.trim();
            counts.put(key, counts.containsKey(key) ? counts.get(key) + 1 : 1);
        }
        cards.removeAllViews();
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            View line = new View(a);
            line.setBackgroundColor(Ui.attr(a, R.attr.jpLine));
            cards.addView(line, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(a, 1)));

            LinearLayout row = new LinearLayout(a);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(Ui.dp(a, 16), Ui.dp(a, 16), Ui.dp(a, 16), Ui.dp(a, 16));
            TextView label = new TextView(a, null, 0, R.style.Jp_Semi);
            label.setText(e.getKey());
            label.setTextSize(15);
            row.addView(label, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            TextView count = new TextView(a, null, 0, R.style.Jp_Text);
            int c = e.getValue();
            count.setText(c + " " + Fmt.pl(c, "подписка", "подписки", "подписок"));
            count.setTextSize(15);
            count.setTextColor(Ui.attr(a, R.attr.jpTx2));
            row.addView(count);
            cards.addView(row);
        }
        cardsEmpty.setVisibility(counts.isEmpty() ? View.VISIBLE : View.GONE);
        // У строки с подсказкой отступ сверху, пока способов оплаты нет
        cardsEmpty.setPadding(cardsEmpty.getPaddingLeft(), counts.isEmpty() ? 0 : Ui.dp(a, 4),
                cardsEmpty.getPaddingRight(), cardsEmpty.getPaddingBottom());
    }
}
