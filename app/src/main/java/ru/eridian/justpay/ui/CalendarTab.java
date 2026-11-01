package ru.eridian.justpay.ui;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.eridian.justpay.R;
import ru.eridian.justpay.data.Subscription;
import ru.eridian.justpay.util.Fmt;

/** Вкладка «Календарь»: списания по дням на ближайшие шесть месяцев. */
final class CalendarTab {
    private static final int MONTHS_AHEAD = 5;
    private static final String[] WEEKDAYS = {"Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"};

    private final MainActivity a;
    private final TextView title, sum, left, selTitle, empty;
    private final View prev, next;
    private final GridLayout grid;
    private final LinearLayout list;

    private int monthOffset = 0;
    /** Выбранный день; применяется только к тому месяцу, для которого выбран. */
    private int selectedDay = -1;
    private int selectedMonthOffset = -1;

    CalendarTab(MainActivity activity, View screen) {
        a = activity;
        title = screen.findViewById(R.id.cal_title);
        prev = screen.findViewById(R.id.cal_prev);
        next = screen.findViewById(R.id.cal_next);
        sum = screen.findViewById(R.id.cal_sum);
        left = screen.findViewById(R.id.cal_left);
        selTitle = screen.findViewById(R.id.cal_sel_title);
        empty = screen.findViewById(R.id.cal_empty);
        grid = screen.findViewById(R.id.cal_grid);
        list = screen.findViewById(R.id.cal_list);

        LinearLayout weekdays = screen.findViewById(R.id.cal_weekdays);
        for (String w : WEEKDAYS) {
            TextView t = new TextView(a);
            t.setText(w);
            t.setGravity(android.view.Gravity.CENTER);
            t.setTextSize(12);
            t.setTextColor(Ui.attr(a, R.attr.jpTx3));
            t.setTypeface(a.getResources().getFont(R.font.manrope_semibold));
            weekdays.addView(t, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        }

        prev.setOnClickListener(v -> {
            if (monthOffset > 0) {
                monthOffset--;
                refresh();
            }
        });
        next.setOnClickListener(v -> {
            if (monthOffset < MONTHS_AHEAD) {
                monthOffset++;
                refresh();
            }
        });
    }

    void refresh() {
        LocalDate today = LocalDate.now();
        LocalDate first = today.withDayOfMonth(1).plusMonths(monthOffset);
        int y = first.getYear(), m = first.getMonthValue(), dim = first.lengthOfMonth();

        Map<Integer, List<Subscription>> occ = new HashMap<>();
        for (Subscription s : a.repo.all()) {
            if (!s.isActive() || s.next == null) continue;
            LocalDate nd = s.next;
            if (Subscription.CYCLE_MONTH.equals(s.cycle)) {
                int day = Math.min(nd.getDayOfMonth(), dim);
                LocalDate dt = LocalDate.of(y, m, day);
                // До даты ближайшего списания в будущем месяце ещё не списывали
                if (!(!dt.isBefore(today) && dt.isBefore(nd))) add(occ, day, s);
            } else if (Subscription.CYCLE_YEAR.equals(s.cycle)) {
                if (nd.getMonthValue() == m && nd.getYear() == y) add(occ, nd.getDayOfMonth(), s);
            } else {
                LocalDate t = nd;
                while (!t.isBefore(first)) t = t.minusDays(7);
                t = t.plusDays(7);
                while (t.getMonthValue() == m && t.getYear() == y) {
                    if (!t.isBefore(nd) || t.isBefore(today)) add(occ, t.getDayOfMonth(), s);
                    t = t.plusDays(7);
                }
            }
        }

        // Какой день выбран: сохранённый для этого месяца, иначе ближайший с платежом
        List<Integer> days = new ArrayList<>(occ.keySet());
        java.util.Collections.sort(days);
        int sel;
        if (selectedMonthOffset == monthOffset && selectedDay > 0) {
            sel = selectedDay;
        } else {
            sel = days.isEmpty() ? 1 : days.get(0);
            for (int d : days) {
                if (!LocalDate.of(y, m, d).isBefore(today)) {
                    sel = d;
                    break;
                }
            }
        }

        title.setText(Fmt.monthTitle(first));
        prev.setAlpha(monthOffset > 0 ? 1f : 0.35f);
        next.setAlpha(monthOffset < MONTHS_AHEAD ? 1f : 0.35f);

        double monthSum = 0, monthLeft = 0;
        for (int d : days) {
            for (Subscription s : occ.get(d)) {
                monthSum += s.price;
                if (!LocalDate.of(y, m, d).isBefore(today)) monthLeft += s.price;
            }
        }
        sum.setText(Fmt.money(monthSum));
        left.setText(Fmt.money(monthLeft));

        buildGrid(first, today, occ, sel);

        LocalDate selDate = LocalDate.of(y, m, sel);
        boolean past = selDate.isBefore(today);
        selTitle.setText(Fmt.dayLong(selDate) + ", " + Fmt.weekday(selDate));
        list.removeAllViews();
        List<Subscription> items = occ.get(sel);
        LayoutInflater inflater = LayoutInflater.from(a);
        if (items != null) {
            for (int i = 0; i < items.size(); i++) {
                Subscription s = items.get(i);
                View row = inflater.inflate(R.layout.item_cal_row, list, false);
                row.findViewById(R.id.divider).setVisibility(i == 0 ? View.GONE : View.VISIBLE);
                ((TextView) row.findViewById(R.id.mk)).setText(s.mark);
                ((TextView) row.findViewById(R.id.name)).setText(s.name);
                ((TextView) row.findViewById(R.id.price)).setText(Fmt.price(s.price));
                ((TextView) row.findViewById(R.id.status)).setText(
                        (past ? "Списано" : "Запланировано") + (s.hasCard() ? " · " + s.card : ""));
                row.findViewById(R.id.row).setOnClickListener(v -> a.openDetail(s.id));
                list.addView(row);
            }
        }
        empty.setVisibility(items == null || items.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private static void add(Map<Integer, List<Subscription>> occ, int day, Subscription s) {
        List<Subscription> l = occ.get(day);
        if (l == null) {
            l = new ArrayList<>();
            occ.put(day, l);
        }
        l.add(s);
    }

    private void buildGrid(LocalDate first, LocalDate today, Map<Integer, List<Subscription>> occ, int sel) {
        grid.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(a);
        int lead = first.getDayOfWeek().getValue() - 1; // понедельник = 0
        int dim = first.lengthOfMonth();
        int ink = a.getColor(R.color.jp_ink);
        int ac = a.getColor(R.color.jp_ac);
        int tx = Ui.attr(a, R.attr.jpTx);
        int tx3 = Ui.attr(a, R.attr.jpTx3);
        int act = Ui.attr(a, R.attr.jpAct);

        for (int i = 0; i < lead; i++) {
            View blank = inflater.inflate(R.layout.item_cal_cell, grid, false);
            blank.setVisibility(View.INVISIBLE);
            grid.addView(blank);
        }
        for (int d = 1; d <= dim; d++) {
            LocalDate date = first.withDayOfMonth(d);
            boolean past = date.isBefore(today);
            boolean isToday = date.isEqual(today);
            boolean on = d == sel;
            int n = occ.containsKey(d) ? occ.get(d).size() : 0;

            View cell = inflater.inflate(R.layout.item_cal_cell, grid, false);
            TextView day = cell.findViewById(R.id.day);
            LinearLayout dots = cell.findViewById(R.id.dots);
            day.setText(String.valueOf(d));

            int textColor = on ? ink : past ? tx3 : tx;
            int dotColor = on ? ink : past ? tx3 : act;
            day.setTextColor(textColor);

            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(Ui.dp(a, 14));
            if (on) bg.setColor(ac);
            else if (isToday) bg.setStroke(Ui.dp(a, 1.5f), tx);
            cell.setBackground(bg);

            for (int k = 0; k < Math.min(n, 3); k++) {
                View dot = new View(a);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.dp(a, 4), Ui.dp(a, 4));
                if (k > 0) lp.setMarginStart(Ui.dp(a, 3));
                dot.setLayoutParams(lp);
                dot.setBackgroundResource(R.drawable.d_dot);
                Ui.tintBackground(dot, dotColor);
                dots.addView(dot);
            }

            final int dayNumber = d;
            cell.setContentDescription(Fmt.dayLong(date) + (n > 0 ? ", платежей: " + n : ""));
            cell.setOnClickListener(v -> {
                selectedDay = dayNumber;
                selectedMonthOffset = monthOffset;
                refresh();
            });
            grid.addView(cell);
        }
    }
}
