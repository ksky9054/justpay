package ru.eridian.justpay.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ru.eridian.justpay.R;
import ru.eridian.justpay.data.Prefs;
import ru.eridian.justpay.data.Subscription;
import ru.eridian.justpay.util.Fmt;

/** Уведомления внутри приложения: предстоящие списания и подписки на паузе. */
public class NotificationsActivity extends BaseActivity {

    /** Списания, о которых пора напомнить: дата напоминания каждой подписки уже наступила. */
    static List<Subscription> upcoming(Prefs prefs, List<Subscription> all, LocalDate today) {
        List<Subscription> out = new ArrayList<>();
        for (Subscription s : all) {
            LocalDate rd = s.remindDate(prefs);
            // Напоминание наступило (дата у каждой подписки своя), а списание ещё впереди
            if (rd != null && !rd.isAfter(today) && !s.next.isBefore(today)) out.add(s);
        }
        Collections.sort(out, (x, y) -> x.next.compareTo(y.next));
        return out;
    }

    static boolean hasUnread(Prefs prefs, List<Subscription> all, LocalDate today) {
        return !upcoming(prefs, all, today).isEmpty() && !today.toString().equals(prefs.notifSeenDate());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);
        Ui.fitSystemBars(findViewById(R.id.root));
        interceptBack();
        findViewById(R.id.btn_back).setOnClickListener(v -> finishToRight());
        build();
    }

    @Override
    protected void onStop() {
        super.onStop();
        prefs.setNotifSeenDate(today().toString());
    }

    @Override
    protected boolean onBack() {
        finishToRight();
        return true;
    }

    private void build() {
        LinearLayout container = findViewById(R.id.notif_container);
        LocalDate today = today();
        List<Subscription> all = repo.all();
        boolean unread = !today.toString().equals(prefs.notifSeenDate());
        int dotColor = unread ? getColor(R.color.jp_ac) : 0;

        List<Subscription> soon = upcoming(prefs, all, today);
        List<Subscription> paused = new ArrayList<>();
        for (Subscription s : all) if (!s.isActive()) paused.add(s);

        LayoutInflater inflater = LayoutInflater.from(this);

        if (!soon.isEmpty()) {
            LinearLayout card = group(container, inflater, "Скоро спишется");
            for (int i = 0; i < soon.size(); i++) {
                Subscription s = soon.get(i);
                long d = Fmt.diff(s.next, today);
                String when = d == 0 ? "Сегодня" : d == 1 ? "Завтра"
                        : "Через " + d + " " + Fmt.pl((int) d, "день", "дня", "дней");
                addItem(card, inflater, i, when + " — " + s.name,
                        Fmt.price(s.price) + " спишется " + Fmt.dayLong(s.next) + (s.hasCard() ? " · " + s.card : ""),
                        Fmt.dayShort(s.next), dotColor, s.id);
            }
        }

        if (!paused.isEmpty()) {
            LinearLayout card = group(container, inflater, "На паузе");
            for (int i = 0; i < paused.size(); i++) {
                Subscription s = paused.get(i);
                addItem(card, inflater, i, s.name + " на паузе",
                        "Экономия " + Fmt.money(s.monthly()) + " в месяц. Следующего списания не будет.",
                        s.prev == null ? "" : Fmt.dayShort(s.prev), 0, s.id);
            }
        }

        if (soon.isEmpty() && paused.isEmpty()) {
            TextView t = new TextView(this, null, 0, R.style.Jp_Text);
            t.setText("Уведомлений пока нет.\nМы сообщим, когда придёт дата напоминания. "
                    + "Её можно задать для каждой подписки отдельно.");
            t.setGravity(android.view.Gravity.CENTER);
            t.setTextColor(Ui.attr(this, R.attr.jpTx2));
            t.setTextSize(14);
            t.setPadding(Ui.dp(this, 32), Ui.dp(this, 64), Ui.dp(this, 32), 0);
            container.addView(t);
        }
    }

    private LinearLayout group(LinearLayout container, LayoutInflater inflater, String title) {
        TextView head = new TextView(this, null, 0, R.style.Jp_Label);
        head.setText(title);
        head.setPadding(Ui.dp(this, 20), Ui.dp(this, 18), Ui.dp(this, 20), Ui.dp(this, 10));
        container.addView(head);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.d_card);
        card.setClipToOutline(true);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMarginStart(Ui.dp(this, 16));
        lp.setMarginEnd(Ui.dp(this, 16));
        container.addView(card, lp);
        return card;
    }

    private void addItem(LinearLayout card, LayoutInflater inflater, int index, String title, String body,
                         String time, int dotColor, long subId) {
        View item = inflater.inflate(R.layout.item_notif, card, false);
        item.findViewById(R.id.divider).setVisibility(index == 0 ? View.GONE : View.VISIBLE);
        ((TextView) item.findViewById(R.id.title)).setText(title);
        ((TextView) item.findViewById(R.id.body)).setText(body);
        ((TextView) item.findViewById(R.id.time)).setText(time);
        View dot = item.findViewById(R.id.dot);
        if (dotColor == 0) dot.setVisibility(View.INVISIBLE);
        else Ui.tintBackground(dot, dotColor);
        item.findViewById(R.id.row).setOnClickListener(v -> openDetail(subId));
        card.addView(item);
    }
}
