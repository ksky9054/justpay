package ru.eridian.justpay.ui;

import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ru.eridian.justpay.R;
import ru.eridian.justpay.data.Subscription;
import ru.eridian.justpay.data.UsageDay;
import ru.eridian.justpay.data.UsageInsights;
import ru.eridian.justpay.data.UsageMetrics;
import ru.eridian.justpay.data.UsageRepository;
import ru.eridian.justpay.util.Fmt;

/** Вкладка «Аналитика»: траты в год, разбивка по категориям, самые дорогие подписки, экономия. */
final class StatsTab {
    private final MainActivity a;
    private final TextView year, sub, insight, pausedN, saved, catsEmpty;
    private final LinearLayout bar, catList, top;
    private final ImageView star;
    private final TextView streak, streakLabel, best, unusedSub;
    private final LinearLayout week, unusedList;
    private final View unusedCard;

    StatsTab(MainActivity activity, View screen) {
        a = activity;
        star = screen.findViewById(R.id.st_star);
        streak = screen.findViewById(R.id.st_streak);
        streakLabel = screen.findViewById(R.id.st_streak_label);
        best = screen.findViewById(R.id.st_best);
        week = screen.findViewById(R.id.st_week);
        unusedCard = screen.findViewById(R.id.st_unused_card);
        unusedSub = screen.findViewById(R.id.st_unused_sub);
        unusedList = screen.findViewById(R.id.st_unused_list);
        year = screen.findViewById(R.id.st_year);
        sub = screen.findViewById(R.id.st_sub);
        insight = screen.findViewById(R.id.st_insight);
        pausedN = screen.findViewById(R.id.st_paused_n);
        saved = screen.findViewById(R.id.st_saved);
        catsEmpty = screen.findViewById(R.id.st_cats_empty);
        bar = screen.findViewById(R.id.st_bar);
        catList = screen.findViewById(R.id.st_cat_list);
        top = screen.findViewById(R.id.st_top);

        GradientDrawable clip = new GradientDrawable();
        clip.setCornerRadius(Ui.dp(a, 7));
        bar.setBackground(clip);
        bar.setClipToOutline(true);
    }

    private static final class Cat {
        final String name;
        int n;
        double sum;

        Cat(String name) {
            this.name = name;
        }
    }

    /** Общая серия и подписки, которыми давно не пользовались. */
    private void bindActivity(List<Subscription> all) {
        LocalDate today = LocalDate.now();
        Map<Long, Map<LocalDate, UsageDay>> usage = UsageRepository.get(a).allForAccount();
        Set<LocalDate> any = UsageMetrics.activeDaysAny(usage.values());
        boolean doneToday = any.contains(today);
        int current = UsageMetrics.currentStreak(any, today);
        int record = UsageMetrics.longestStreak(any);

        star.setColorFilter(current == 0 ? Ui.attr(a, R.attr.jpTx3) : a.getColor(R.color.jp_ac));
        star.setAlpha(current > 0 && !doneToday ? 0.6f : 1f);
        streak.setText(String.valueOf(current));
        streakLabel.setText(Fmt.pl(current, "день подряд", "дня подряд", "дней подряд"));
        if (all.isEmpty()) best.setText("Добавьте подписку, чтобы вести серию");
        else if (current > 0 && !doneToday) best.setText("Отметьте использование сегодня, чтобы не потерять серию");
        else if (record > 0) best.setText("Рекорд: " + record + " " + Fmt.pl(record, "день", "дня", "дней"));
        else best.setText("Пока нет ни одной отметки");
        UsagePanel.buildWeek(a, week, any, today);

        // Подписки без активности больше двух недель (то же правило, что у уведомления)
        List<Subscription> idle = UsageInsights.idle(all, usage, today, UsageInsights.IDLE_DAYS);
        double idleSum = UsageInsights.monthlySum(idle);
        unusedList.removeAllViews();
        unusedCard.setVisibility(idle.isEmpty() ? View.GONE : View.VISIBLE);
        if (idle.isEmpty()) return;
        unusedSub.setText("Они стоят " + Fmt.money(idleSum) + " в месяц. Если подписки не нужны, поставьте их на паузу.");
        LayoutInflater inflater = LayoutInflater.from(a);
        for (int i = 0; i < idle.size(); i++) {
            Subscription s = idle.get(i);
            View row = inflater.inflate(R.layout.item_top_row, unusedList, false);
            row.findViewById(R.id.divider).setVisibility(i == 0 ? View.GONE : View.VISIBLE);
            row.findViewById(R.id.rank).setVisibility(View.GONE);
            TextView since = row.findViewById(R.id.since);
            since.setVisibility(View.VISIBLE);
            since.setText("Подключена " + Fmt.dayFull(s.createdAt));
            ((TextView) row.findViewById(R.id.name)).setText(s.name);
            ((TextView) row.findViewById(R.id.price)).setText(Fmt.money(s.monthly()) + " / мес");
            row.findViewById(R.id.row).setOnClickListener(v -> a.openDetail(s.id));
            unusedList.addView(row);
        }
    }

    void refresh() {
        List<Subscription> all = a.repo.all();
        List<Subscription> active = new ArrayList<>();
        List<Subscription> paused = new ArrayList<>();
        double total = 0, savedSum = 0;
        for (Subscription s : all) {
            if (s.isActive()) {
                active.add(s);
                total += s.monthly();
            } else {
                paused.add(s);
                savedSum += s.monthly();
            }
        }

        bindActivity(all);

        year.setText(Fmt.money(total * 12));
        sub.setText(Fmt.money(total) + " в месяц · " + Fmt.money(total * 12 / 365) + " в день");

        // Категории
        Map<String, Cat> map = new LinkedHashMap<>();
        for (Subscription s : active) {
            Cat c = map.get(s.category);
            if (c == null) {
                c = new Cat(s.category);
                map.put(s.category, c);
            }
            c.n++;
            c.sum += s.monthly();
        }
        List<Cat> cats = new ArrayList<>(map.values());
        Collections.sort(cats, (x, y) -> Double.compare(y.sum, x.sum));

        int ac = a.getColor(R.color.jp_ac);
        int s1 = Ui.attr(a, R.attr.jpS1);
        int tx = Ui.attr(a, R.attr.jpTx);
        int[] colors = {
                ac,
                Ui.blend(ac, s1, 0.60f),
                Ui.blend(ac, s1, 0.34f),
                Ui.blend(tx, s1, 0.32f),
                Ui.blend(tx, s1, 0.16f)};

        bar.removeAllViews();
        catList.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(a);
        for (int i = 0; i < cats.size(); i++) {
            Cat c = cats.get(i);
            int color = colors[Math.min(i, colors.length - 1)];

            View seg = new View(a);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    0, android.view.ViewGroup.LayoutParams.MATCH_PARENT, (float) c.sum);
            if (i < cats.size() - 1) lp.setMarginEnd(Ui.dp(a, 3));
            seg.setLayoutParams(lp);
            seg.setBackgroundColor(color);
            bar.addView(seg);

            View row = inflater.inflate(R.layout.item_cat_row, catList, false);
            row.findViewById(R.id.divider).setVisibility(i == 0 ? View.GONE : View.VISIBLE);
            Ui.tintBackground(row.findViewById(R.id.swatch), color);
            ((TextView) row.findViewById(R.id.name)).setText(c.name);
            ((TextView) row.findViewById(R.id.count)).setText(
                    c.n + " " + Fmt.pl(c.n, "подписка", "подписки", "подписок"));
            ((TextView) row.findViewById(R.id.sum)).setText(Fmt.money(c.sum) + " / мес");
            ((TextView) row.findViewById(R.id.pct)).setText(Math.round(c.sum / total * 100) + "%");
            catList.addView(row);
        }
        bar.setVisibility(cats.isEmpty() ? View.GONE : View.VISIBLE);
        catsEmpty.setVisibility(cats.isEmpty() ? View.VISIBLE : View.GONE);

        // Вывод по самой затратной категории
        if (cats.isEmpty()) {
            insight.setVisibility(View.GONE);
        } else {
            Cat c = cats.get(0);
            String head = c.name + " — " + Math.round(c.sum / total * 100) + "% расходов.";
            String body = " Это " + c.n + " " + Fmt.pl(c.n, "подписка", "подписки", "подписок")
                    + " на " + Fmt.money(c.sum * 12) + " в год. Проверьте, всеми ли вы пользуетесь.";
            SpannableStringBuilder sb = new SpannableStringBuilder(head + body);
            sb.setSpan(new StyleSpan(Typeface.BOLD), 0, head.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            insight.setText(sb);
            insight.setVisibility(View.VISIBLE);
        }

        // Самые дорогие
        List<Subscription> byPrice = new ArrayList<>(active);
        Collections.sort(byPrice, (x, y) -> Double.compare(y.monthly(), x.monthly()));
        top.removeAllViews();
        for (int i = 0; i < Math.min(3, byPrice.size()); i++) {
            Subscription s = byPrice.get(i);
            View row = inflater.inflate(R.layout.item_top_row, top, false);
            row.findViewById(R.id.divider).setVisibility(i == 0 ? View.GONE : View.VISIBLE);
            ((TextView) row.findViewById(R.id.rank)).setText("0" + (i + 1));
            ((TextView) row.findViewById(R.id.name)).setText(s.name);
            ((TextView) row.findViewById(R.id.price)).setText(Fmt.money(s.monthly()) + " / мес");
            row.findViewById(R.id.row).setOnClickListener(v -> a.openDetail(s.id));
            top.addView(row);
        }
        top.setVisibility(byPrice.isEmpty() ? View.GONE : View.VISIBLE);

        // Экономия на паузах
        saved.setText(Fmt.money(savedSum) + " / мес");
        if (paused.isEmpty()) {
            pausedN.setText("Нет подписок на паузе");
        } else {
            StringBuilder names = new StringBuilder();
            for (Subscription s : paused) {
                if (names.length() > 0) names.append(", ");
                names.append(s.name);
            }
            pausedN.setText(names + " · " + Fmt.money(savedSum * 12) + " в год");
        }
    }
}
