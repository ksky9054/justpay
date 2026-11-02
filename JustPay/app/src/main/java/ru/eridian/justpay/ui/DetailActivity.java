package ru.eridian.justpay.ui;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.time.LocalDate;
import java.util.List;

import ru.eridian.justpay.R;
import ru.eridian.justpay.data.Subscription;
import ru.eridian.justpay.util.Fmt;

/** Карточка подписки: сведения, история списаний, пауза и возобновление, удаление. */
public class DetailActivity extends BaseActivity {
    public static final String EXTRA_ID = "id";

    private long id;
    private TextView pill, mk, name, cat, price, cyc, next, rel, cycle, card, year, toggle;
    private LinearLayout history;
    private UsagePanel usagePanel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        Ui.fitSystemBars(findViewById(R.id.root));
        interceptBack();
        id =getIntent().getLongExtra(EXTRA_ID, 0);

        pill = findViewById(R.id.pill_status);
        mk = findViewById(R.id.d_mk);
        name = findViewById(R.id.d_name);
        cat = findViewById(R.id.d_cat);
        price = findViewById(R.id.d_price);
        cyc = findViewById(R.id.d_cyc);
        next = findViewById(R.id.d_next);
        rel = findViewById(R.id.d_rel);
        cycle = findViewById(R.id.d_cycle);
        card = findViewById(R.id.d_card);
        year = findViewById(R.id.d_year);
        history = findViewById(R.id.d_hist);
        toggle = findViewById(R.id.btn_toggle);

        findViewById(R.id.btn_back).setOnClickListener(v -> finishToRight());
        findViewById(R.id.btn_edit).setOnClickListener(v -> openEdit(id));
        findViewById(R.id.d_remind_row).setOnClickListener(v -> openEdit(id));
        toggle.setOnClickListener(v -> confirmToggle());
        findViewById(R.id.btn_delete).setOnClickListener(v -> confirmDelete());
        usagePanel = new UsagePanel(this, findViewById(R.id.root), this::bind);
    }

    @Override
    protected void onResume() {
        super.onResume();
        bind();
        Toaster.flush(this);
    }

    @Override
    protected boolean onBack() {
        finishToRight();
        return true;
    }

    private void bind() {
        Subscription s = repo.find(id);
        if (s == null) {
            finish();
            return;
        }
        LocalDate today = today();
        boolean active = s.isActive();

        GradientDrawable pillBg = new GradientDrawable();
        pillBg.setCornerRadius(Ui.dp(this, 16));
        pillBg.setColor(active ? Ui.attr(this, R.attr.jpS1) : getColor(R.color.jp_ac));
        pill.setBackground(pillBg);
        pill.setTextColor(active ? Ui.attr(this, R.attr.jpTx) : getColor(R.color.jp_ink));
        pill.setText(active ? "Активна" : "На паузе");

        mk.setText(s.mark);
        name.setText(s.name);
        cat.setText(s.category + " · с " + s.since);
        price.setText(Fmt.price(s.price));
        cyc.setText("/ " + Fmt.cycleShort(s.cycle));

        next.setText(active && s.next != null ? Fmt.dayLong(s.next) : "—");
        rel.setText(active && s.next != null ? Fmt.rel(s.next, today) : "приостановлена");
        cycle.setText(Fmt.cycleFull(s.cycle));
        card.setText(s.hasCard() ? s.card : "Не указан");
        year.setText(Fmt.money(s.monthly() * 12));
        usagePanel.bind(s);

        // Напоминание: дата, когда придёт уведомление, и откуда она взята
        TextView remind = findViewById(R.id.d_remind);
        TextView remindSub = findViewById(R.id.d_remind_sub);
        LocalDate rd = s.remindDate(prefs);
        if (!active) {
            remind.setText("—");
            remindSub.setText("Подписка на паузе");
        } else if (rd == null) {
            remind.setText("Выключено");
            remindSub.setText(s.remindDays == Subscription.REMIND_OFF
                    ? "Для этой подписки" : "Выключено в профиле");
        } else {
            remind.setText(Fmt.dayLong(rd) + " · 09:00");
            int n = s.effectiveRemindDays(prefs);
            String before = n == 0 ? "в день списания"
                    : "за " + n + " " + Fmt.pl(n, "день", "дня", "дней") + " до списания";
            remindSub.setText(before + (s.remindDays == Subscription.REMIND_DEFAULT ? " · как в профиле" : ""));
        }

        // История: три последних списания по расписанию
        history.removeAllViews();
        LocalDate base = s.next != null ? s.next : (s.prev != null ? s.prev : today);
        LayoutInflater inflater = LayoutInflater.from(this);
        int count = 0;
        for (int k = 1; count < 3 && k < 40; k++) {
            LocalDate t = Subscription.shift(base, s.cycle, -k);
            if (!t.isBefore(today)) continue;
            View row = inflater.inflate(R.layout.item_history, history, false);
            row.findViewById(R.id.divider).setVisibility(count == 0 ? View.GONE : View.VISIBLE);
            ((TextView) row.findViewById(R.id.date)).setText(Fmt.dayFull(t));
            ((TextView) row.findViewById(R.id.amt)).setText(Fmt.price(s.price));
            history.addView(row);
            count++;
        }

        if (active) {
            toggle.setText("Приостановить подписку");
            toggle.setBackgroundResource(R.drawable.d_btn_outline);
            toggle.setTextColor(Ui.attr(this, R.attr.jpTx));
        } else {
            toggle.setText("Возобновить подписку");
            toggle.setBackgroundResource(R.drawable.d_btn_primary);
            toggle.setTextColor(getColor(R.color.jp_ink));
        }
    }

    private double totalMonthly() {
        double total = 0;
        List<Subscription> all = repo.all();
        for (Subscription s : all) if (s.isActive()) total += s.monthly();
        return total;
    }

    private void confirmToggle() {
        Subscription s = repo.find(id);
        if (s == null) return;
        double month = s.monthly();
        double total = totalMonthly();

        if (s.isActive()) {
            ConfirmSheet.show(this, "Приостановить «" + s.name + "»?", R.layout.sheet_body_confirm, body -> {
                ((TextView) body.findViewById(R.id.b_title)).setText("Вы сэкономите");
                TextView monthTv = body.findViewById(R.id.b_month);
                monthTv.setText(Fmt.money(month));
                monthTv.setTextColor(Ui.attr(this, R.attr.jpAct));
                TextView yearTv = body.findViewById(R.id.b_year);
                yearTv.setVisibility(View.VISIBLE);
                yearTv.setText(Fmt.money(month * 12) + " за год");
                body.findViewById(R.id.b_row_change).setVisibility(View.VISIBLE);
                body.findViewById(R.id.b_row_date).setVisibility(View.GONE);
                TextView before = body.findViewById(R.id.b_before);
                before.setText(Fmt.money(total));
                before.setPaintFlags(before.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
                ((TextView) body.findViewById(R.id.b_after)).setText(Fmt.money(total - month));
            }, "Статус изменится в JustPay. Чтобы деньги не списались, отключите автоплатёж в настройках самого сервиса.",
                    "Приостановить", false, () -> {
                        s.status = Subscription.PAUSED;
                        s.prev = s.next;
                        s.next = null;
                        repo.update(s);
                        bind();
                        Toaster.show(this, "«" + s.name + "» на паузе · −" + Fmt.money(month) + " в месяц");
                    });
        } else {
            LocalDate base = s.prev != null ? s.prev : today();
            LocalDate resume = Subscription.advance(base, s.cycle, today(), false);
            ConfirmSheet.show(this, "Возобновить «" + s.name + "»?", R.layout.sheet_body_confirm, body -> {
                ((TextView) body.findViewById(R.id.b_title)).setText("Добавится к расходам");
                TextView monthTv = body.findViewById(R.id.b_month);
                monthTv.setText("+" + Fmt.money(month));
                monthTv.setTextColor(Ui.attr(this, R.attr.jpTx));
                body.findViewById(R.id.b_year).setVisibility(View.GONE);
                body.findViewById(R.id.b_row_change).setVisibility(View.GONE);
                body.findViewById(R.id.b_row_date).setVisibility(View.VISIBLE);
                ((TextView) body.findViewById(R.id.b_date)).setText(Fmt.dayLong(resume));
            }, null, "Возобновить", false, () -> {
                s.status = Subscription.ACTIVE;
                s.next = resume;
                repo.update(s);
                bind();
                Toaster.show(this, "«" + s.name + "» снова активна");
            });
        }
    }

    private void confirmDelete() {
        Subscription s = repo.find(id);
        if (s == null) return;
        ConfirmSheet.show(this, "Удалить «" + s.name + "»?", 0, null,
                "Подписка и её история исчезнут из JustPay. Отменить удаление будет нельзя.",
                "Удалить", true, () -> {
                    repo.delete(s.id);
                    Toaster.post("«" + s.name + "» удалена из JustPay");
                    finishToRight();
                });
    }
}
