package ru.eridian.justpay.ui;

import android.app.Activity;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ru.eridian.justpay.R;
import ru.eridian.justpay.data.Subscription;
import ru.eridian.justpay.data.SubscriptionRepository;
import ru.eridian.justpay.data.UsageDay;
import ru.eridian.justpay.data.UsageMetrics;
import ru.eridian.justpay.data.UsageRepository;
import ru.eridian.justpay.usage.AppCatalog;
import ru.eridian.justpay.usage.UsageAccess;
import ru.eridian.justpay.usage.UsageSync;
import ru.eridian.justpay.util.Fmt;

/** Блок «Использование» в карточке подписки: серия звёзд, цифры, ручные отметки, привязка приложения. */
final class UsagePanel {
    private static final String WEEKDAY_LETTERS = "ПВСЧПСВ";

    private final Activity a;
    private final SubscriptionRepository subs;
    private final UsageRepository usage;
    private final Runnable onChanged;

    private final ImageView star;
    private final TextView streak, streakLabel, best, uses, time, cost, hint, source, sourceHint;
    private final TextView mark, unmark, link, unlink;
    private final LinearLayout week;

    UsagePanel(Activity activity, View root, Runnable onChanged) {
        a = activity;
        this.onChanged = onChanged;
        subs = SubscriptionRepository.get(a);
        usage = UsageRepository.get(a);
        star = root.findViewById(R.id.u_star);
        streak = root.findViewById(R.id.u_streak);
        streakLabel = root.findViewById(R.id.u_streak_label);
        best = root.findViewById(R.id.u_best);
        week = root.findViewById(R.id.u_week);
        uses = root.findViewById(R.id.u_uses);
        time = root.findViewById(R.id.u_time);
        cost = root.findViewById(R.id.u_cost);
        hint = root.findViewById(R.id.u_hint);
        source = root.findViewById(R.id.u_source);
        sourceHint = root.findViewById(R.id.u_source_hint);
        mark = root.findViewById(R.id.u_mark);
        unmark = root.findViewById(R.id.u_unmark);
        link = root.findViewById(R.id.u_link);
        unlink = root.findViewById(R.id.u_unlink);
    }

    void bind(Subscription s) {
        LocalDate today = LocalDate.now();
        Map<LocalDate, UsageDay> days = usage.days(s.id);
        Set<LocalDate> active = UsageMetrics.activeDays(days);
        boolean doneToday = active.contains(today);
        int current = UsageMetrics.currentStreak(active, today);
        int record = UsageMetrics.longestStreak(active);

        // Серия
        int ac = a.getColor(R.color.jp_ac);
        int starColor = current == 0 ? Ui.attr(a, R.attr.jpTx3) : ac;
        star.setColorFilter(starColor);
        star.setAlpha(current > 0 && !doneToday ? 0.6f : 1f);
        streak.setText(String.valueOf(current));
        streakLabel.setText(Fmt.pl(current, "день подряд", "дня подряд", "дней подряд"));
        if (current > 0 && !doneToday) best.setText("Отметьте сегодня, чтобы не потерять серию");
        else if (record > 0) best.setText("Рекорд: " + record + " " + Fmt.pl(record, "день", "дня", "дней"));
        else best.setText("Отметьте сегодняшний день, чтобы начать серию");
        buildWeek(a, week, active, today);

        // Цифры за 30 дней
        boolean linked = s.appPackage != null && !s.appPackage.isEmpty();
        int uses30 = UsageMetrics.usesInLast(days, 30, today);
        int sec30 = UsageMetrics.secondsInLast(days, 30, today);
        uses.setText(String.valueOf(uses30));
        time.setText(linked && sec30 > 0 ? Fmt.duration(sec30) : "—");
        double perUse = UsageMetrics.costPerUse(s.monthly(), uses30);
        cost.setText(perUse < 0 ? "—" : Fmt.money(perUse));

        // Подсказка по последнему использованию
        LocalDate last = UsageMetrics.lastUse(active);
        StringBuilder h = new StringBuilder();
        if (last == null) {
            long age = Fmt.diff(today, s.createdAt);
            if (s.isActive() && age >= 14) {
                h.append("Использований пока нет, а подписка стоит ").append(Fmt.money(s.monthly()))
                        .append(" в месяц. Может, её пора приостановить?");
            } else {
                h.append("Пока нет ни одной отметки.");
            }
        } else {
            long ago = Fmt.diff(today, last);
            h.append("Последний раз: ").append(ago == 0 ? "сегодня" : ago == 1 ? "вчера"
                    : ago + " " + Fmt.pl((int) ago, "день", "дня", "дней") + " назад").append('.');
            if (s.isActive() && ago >= 14) {
                h.append(" Вы не пользуетесь подпиской, а она стоит ").append(Fmt.money(s.monthly()))
                        .append(" в месяц. Подумайте о паузе.");
            }
        }
        hint.setText(h);

        // Источник данных
        if (linked) {
            String label = AppCatalog.labelOf(a, s.appPackage);
            source.setText(label == null ? "Приложение удалено с телефона" : "Приложение «" + label + "»");
            sourceHint.setText(UsageAccess.isGranted(a)
                    ? "Считаем, сколько раз вы открываете приложение и сколько времени в нём проводите. Ручные отметки тоже учитываются."
                    : "Доступ к статистике телефона выключен. Включите его, и данные начнут собираться сами.");
            link.setText("Сменить приложение");
        } else {
            source.setText("Ручные отметки");
            sourceHint.setText("Если у сервиса есть приложение на телефоне, подключите его: открытия будут считаться сами. Если приложения нет, отмечайте использование вручную.");
            link.setText("Подключить приложение");
        }
        unlink.setVisibility(linked ? View.VISIBLE : View.GONE);

        // Кнопки
        int manualToday = days.containsKey(today) ? days.get(today).manual : 0;
        mark.setText(manualToday > 0 ? "Пользовался ещё раз (сегодня: " + manualToday + ")" : "Пользовался сегодня");
        unmark.setVisibility(manualToday > 0 ? View.VISIBLE : View.GONE);
        mark.setOnClickListener(v -> {
            usage.addManual(s.id, today, +1);
            Set<LocalDate> now = UsageMetrics.activeDays(usage.days(s.id));
            int streakNow = UsageMetrics.currentStreak(now, today);
            Toaster.show(a, "Отмечено · серия: " + streakNow + " " + Fmt.pl(streakNow, "день", "дня", "дней"));
            onChanged.run();
        });
        unmark.setOnClickListener(v -> {
            usage.addManual(s.id, today, -1);
            onChanged.run();
        });
        link.setOnClickListener(v -> AppPickerSheet.show(a, s.name, app -> {
            s.appPackage = app.pkg;
            subs.update(s);
            Toaster.show(a, "Подключено: " + app.label);
            if (!UsageAccess.isGranted(a)) askAccess();
            UsageSync.syncAsync(a, () -> a.runOnUiThread(onChanged));
            onChanged.run();
        }));
        unlink.setOnClickListener(v -> {
            s.appPackage = null;
            subs.update(s);
            usage.clearAuto(s.id);
            Toaster.show(a, "Приложение отвязано");
            onChanged.run();
        });
    }

    private void askAccess() {
        ConfirmSheet.show(a, "Включите доступ к статистике", 0, null,
                "Без вашего разрешения Android не позволяет читать активность приложений. В настройках найдите JustPay и включите доступ. "
                        + "JustPay увидит только открытия и время тех приложений, которые вы привязали к подпискам. Данные остаются на телефоне.",
                "Открыть настройки", false, () -> a.startActivity(UsageAccess.settingsIntent(a)));
    }

    /** Семь последних дней: заполненный круг со звездой там, где пользовались. */
    static void buildWeek(Activity a, LinearLayout week, Set<LocalDate> active, LocalDate today) {
        week.removeAllViews();
        List<Boolean> flags = UsageMetrics.lastWeek(active, today);
        int ink = a.getColor(R.color.jp_ink);
        int ac = a.getColor(R.color.jp_ac);
        for (int i = 0; i < 7; i++) {
            LocalDate d = today.minusDays(6 - i);
            boolean on = flags.get(i);

            LinearLayout cell = new LinearLayout(a);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER_HORIZONTAL);
            cell.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            FrameLayout circle = new FrameLayout(a);
            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.OVAL);
            if (on) bg.setColor(ac);
            else if (d.equals(today)) bg.setStroke(Ui.dp(a, 1.5f), Ui.attr(a, R.attr.jpTx3));
            else bg.setColor(Ui.attr(a, R.attr.jpS2));
            circle.setBackground(bg);
            circle.setLayoutParams(new LinearLayout.LayoutParams(Ui.dp(a, 34), Ui.dp(a, 34)));
            if (on) {
                ImageView s = new ImageView(a);
                s.setImageResource(R.drawable.ic_star);
                s.setColorFilter(ink);
                s.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
                FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(Ui.dp(a, 18), Ui.dp(a, 18), Gravity.CENTER);
                circle.addView(s, lp);
            }
            circle.setContentDescription(Fmt.dayLong(d) + (on ? ": пользовались" : ": нет активности"));
            cell.addView(circle);

            TextView label = new TextView(a, null, 0, R.style.Jp_Caption);
            label.setText(String.valueOf(WEEKDAY_LETTERS.charAt(d.getDayOfWeek().getValue() - 1)));
            label.setTextSize(11);
            label.setPadding(0, Ui.dp(a, 6), 0, 0);
            if (d.equals(today)) label.setTextColor(Ui.attr(a, R.attr.jpTx));
            cell.addView(label);
            week.addView(cell);
        }
    }
}
