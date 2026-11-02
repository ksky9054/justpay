package ru.eridian.justpay.ui;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import ru.eridian.justpay.R;
import ru.eridian.justpay.data.Subscription;
import ru.eridian.justpay.util.Fmt;

/** Вкладка «Главная»: сумма в месяц, ближайшее списание, поиск, фильтр и список подписок. */
final class HomeTab {
    private final MainActivity a;
    private final View root;

    private final TextView totalNum, totalCur;
    private final TextView totalMeta;
    private final View savedPill;
    private final TextView savedText;
    private final View nextCard;
    private final TextView nextTag, nextName, nextSub, nextPrice;
    private final EditText search;
    private final TextView segAll, segActive, segPaused;
    private final LinearLayout rows;
    private final TextView empty;
    private final View bellDot;

    private String filter = "all";
    private String query = "";

    HomeTab(MainActivity activity, View screen) {
        a = activity;
        root = screen;
        totalNum = screen.findViewById(R.id.total_num);
        totalCur = screen.findViewById(R.id.total_cur);
        totalMeta = screen.findViewById(R.id.total_meta);
        savedPill = screen.findViewById(R.id.saved_pill);
        savedText = screen.findViewById(R.id.saved_text);
        nextCard = screen.findViewById(R.id.next_card);
        nextTag = screen.findViewById(R.id.next_tag);
        nextName = screen.findViewById(R.id.next_name);
        nextSub = screen.findViewById(R.id.next_sub);
        nextPrice = screen.findViewById(R.id.next_price);
        search = screen.findViewById(R.id.search);
        segAll = screen.findViewById(R.id.seg_all);
        segActive = screen.findViewById(R.id.seg_active);
        segPaused = screen.findViewById(R.id.seg_paused);
        rows = screen.findViewById(R.id.list_rows);
        empty = screen.findViewById(R.id.list_empty);
        bellDot = screen.findViewById(R.id.bell_dot);

        screen.findViewById(R.id.btn_bell).setOnClickListener(v -> a.openNotifications());
        segAll.setOnClickListener(v -> setFilterAndRefresh("all"));
        segActive.setOnClickListener(v -> setFilterAndRefresh("active"));
        segPaused.setOnClickListener(v -> setFilterAndRefresh("paused"));

        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                query = s.toString().trim();
                refresh();
            }
        });
        search.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                Ui.hideKeyboard(a);
                return true;
            }
            return false;
        });
    }

    String filter() {
        return filter;
    }

    void setFilter(String f) {
        filter = f;
    }

    private void setFilterAndRefresh(String f) {
        filter = f;
        refresh();
    }

    void refresh() {
        LocalDate today = LocalDate.now();
        List<Subscription> all = a.repo.all();
        List<Subscription> active = new ArrayList<>();
        List<Subscription> paused = new ArrayList<>();
        double total = 0, saved = 0;
        for (Subscription s : all) {
            if (s.isActive()) {
                active.add(s);
                total += s.monthly();
            } else {
                paused.add(s);
                saved += s.monthly();
            }
        }

        totalNum.setText(Fmt.moneyNum(total));
        totalCur.setText(Fmt.symbol());
        StringBuilder meta = new StringBuilder();
        meta.append(active.size()).append(' ')
                .append(Fmt.pl(active.size(), "активная", "активные", "активных"));
        if (!paused.isEmpty()) meta.append(" · ").append(paused.size()).append(" на паузе");
        meta.append(" · ").append(Fmt.money(total * 12)).append(" в год");
        totalMeta.setText(all.isEmpty() ? "Добавьте первую подписку, и мы посчитаем расходы" : meta);

        savedPill.setVisibility(saved > 0 ? View.VISIBLE : View.GONE);
        savedText.setText("Пауза экономит " + Fmt.money(saved) + " в месяц");

        // Ближайшее списание
        Subscription next = null;
        for (Subscription s : active) {
            if (s.next != null && !s.next.isBefore(today) && (next == null || s.next.isBefore(next.next))) next = s;
        }
        if (next == null) {
            nextCard.setVisibility(View.GONE);
        } else {
            final Subscription n = next;
            nextCard.setVisibility(View.VISIBLE);
            nextTag.setText(("Ближайшее · " + Fmt.rel(n.next, today)).toUpperCase(Locale.getDefault()));
            nextName.setText(n.name);
            nextSub.setText(Fmt.dayLong(n.next) + (n.hasCard() ? " · " + n.card : ""));
            nextPrice.setText(Fmt.price(n.price));
            nextCard.setOnClickListener(v -> a.openDetail(n.id));
        }

        bellDot.setVisibility(NotificationsActivity.hasUnread(a.prefs, all, today) ? View.VISIBLE : View.GONE);

        // Фильтры
        segAll.setText("Все · " + all.size());
        segActive.setText("Активные · " + active.size());
        segPaused.setText("Пауза · " + paused.size());
        segAll.setSelected(filter.equals("all"));
        segActive.setSelected(filter.equals("active"));
        segPaused.setSelected(filter.equals("paused"));

        // Список: сначала активные по дате списания, потом приостановленные
        List<Subscription> sorted = new ArrayList<>(all);
        sorted.sort(Comparator
                .comparing((Subscription s) -> !s.isActive())
                .thenComparing(s -> s.next == null ? LocalDate.MAX : s.next));

        String q = query.toLowerCase(Locale.getDefault());
        List<Subscription> shown = new ArrayList<>();
        for (Subscription s : sorted) {
            boolean byStatus = filter.equals("all")
                    || (filter.equals("active") && s.isActive())
                    || (filter.equals("paused") && !s.isActive());
            boolean byQuery = q.isEmpty() || s.name.toLowerCase(Locale.getDefault()).contains(q);
            if (byStatus && byQuery) shown.add(s);
        }

        rows.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(a);
        for (int i = 0; i < shown.size(); i++) {
            rows.addView(buildRow(inflater, rows, shown.get(i), i == 0, today));
        }

        if (shown.isEmpty()) {
            empty.setVisibility(View.VISIBLE);
            if (all.isEmpty()) {
                empty.setText("Здесь пока пусто\nНажмите «+», чтобы добавить первую подписку");
            } else if (!query.isEmpty()) {
                empty.setText("Ничего не найдено по запросу «" + query + "»");
            } else if (filter.equals("paused")) {
                empty.setText("Нет подписок на паузе");
            } else {
                empty.setText("Нет активных подписок");
            }
        } else {
            empty.setVisibility(View.GONE);
        }
    }

    private View buildRow(LayoutInflater inflater, ViewGroup parent, Subscription s, boolean first, LocalDate today) {
        View item = inflater.inflate(R.layout.item_sub_row, parent, false);
        item.findViewById(R.id.divider).setVisibility(first ? View.GONE : View.VISIBLE);

        TextView mk = item.findViewById(R.id.mk);
        TextView name = item.findViewById(R.id.name);
        TextView line = item.findViewById(R.id.line);
        TextView price = item.findViewById(R.id.price);
        TextView cyc = item.findViewById(R.id.cyc);

        mk.setText(s.mark);
        name.setText(s.name);
        price.setText(Fmt.price(s.price));
        cyc.setText("в " + Fmt.cycleShort(s.cycle));

        if (s.isActive() && s.next != null) {
            line.setText(Fmt.dayShort(s.next) + " · " + Fmt.rel(s.next, today));
            line.setTextColor(Ui.attr(a, R.attr.jpTx2));
        } else {
            line.setText("На паузе");
            line.setTextColor(Ui.attr(a, R.attr.jpAct));
        }
        float alpha = s.isActive() ? 1f : 0.5f;
        mk.setAlpha(alpha);
        name.setAlpha(alpha);
        item.findViewById(R.id.price_col).setAlpha(alpha);

        item.findViewById(R.id.row).setOnClickListener(v -> a.openDetail(s.id));
        return item;
    }
}
