package ru.eridian.justpay.ui;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import ru.eridian.justpay.R;
import ru.eridian.justpay.util.Fmt;

/** Три экрана знакомства. Показываются при первом запуске и по кнопке в профиле. */
public class OnboardingActivity extends BaseActivity {
    public static final String EXTRA_REPLAY = "replay";

    private static final String[] TITLES = {
            "Все подписки — на одной орбите",
            "Ясная сумма каждый месяц",
            "Пауза — и минус лишние траты"};
    private static final String[] BODIES = {
            "JustPay показывает, за что списываются деньги, сколько это стоит и когда пройдёт следующий платёж.",
            "Считаем общие расходы на подписки и напоминаем о списании заранее — без сюрпризов.",
            "Приостановите то, чем не пользуетесь, и сразу увидите, сколько сэкономите за год."};

    // Пример для второй страницы. Это иллюстрация, а не данные пользователя:
    // знакомство показывается до входа, когда ни аккаунта, ни подписок ещё нет.
    private static final String[][] SAMPLE = {
            {"Музыка", "29 окт", "399"},
            {"Облако", "3 ноя", "299"},
            {"Видео", "8 ноя", "599"}};

    private int page = 0;
    private boolean replay;
    private View[] pages;
    private TextView title, body, next, skip;
    private LinearLayout dots;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);
        Ui.fitSystemBars(findViewById(R.id.root));
        interceptBack();
        replay = getIntent().getBooleanExtra(EXTRA_REPLAY, false);
        if (savedInstanceState != null) page = savedInstanceState.getInt("page", 0);

        pages = new View[]{findViewById(R.id.ob_page0), findViewById(R.id.ob_page1), findViewById(R.id.ob_page2)};
        title = findViewById(R.id.ob_title);
        body = findViewById(R.id.ob_body);
        next = findViewById(R.id.ob_next);
        skip = findViewById(R.id.ob_skip);
        dots = findViewById(R.id.ob_dots);

        fillSample();
        next.setOnClickListener(v -> {
            if (page < 2) {
                page++;
                render();
            } else {
                finishOnboarding();
            }
        });
        skip.setOnClickListener(v -> finishOnboarding());
        render();
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putInt("page", page);
    }

    @Override
    protected boolean onBack() {
        if (page == 0) return false;
        page--;
        render();
        return true;
    }

    private void finishOnboarding() {
        if (!replay) {
            prefs.setOnboarded(true);
            // Дальше главный экран сам отправит на вход, если аккаунта ещё нет
            startActivity(new Intent(this, MainActivity.class));
        }
        finish();
    }

    private void render() {
        for (int i = 0; i < pages.length; i++) pages[i].setVisibility(i == page ? View.VISIBLE : View.GONE);
        title.setText(TITLES[page]);
        body.setText(BODIES[page]);
        next.setText(page == 2 ? "Начать" : "Далее");
        skip.setVisibility(page == 2 ? View.INVISIBLE : View.VISIBLE);

        dots.removeAllViews();
        for (int i = 0; i < 3; i++) {
            View d = new View(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    Ui.dp(this, i == page ? 22 : 6), Ui.dp(this, 6));
            if (i > 0) lp.setMarginStart(Ui.dp(this, 6));
            d.setLayoutParams(lp);
            GradientDrawable g = new GradientDrawable();
            g.setCornerRadius(Ui.dp(this, 3));
            g.setColor(Ui.attr(this, i == page ? R.attr.jpTx : R.attr.jpTx3));
            d.setBackground(g);
            dots.addView(d);
        }
    }

    private void fillSample() {
        double total = 0;
        for (String[] s : SAMPLE) total += Double.parseDouble(s[2]);
        ((TextView) findViewById(R.id.ob_total)).setText(Fmt.moneyNum(total));
        ((TextView) findViewById(R.id.ob_cur)).setText(Fmt.symbol());
        ((TextView) findViewById(R.id.ob_saved)).setText("+" + Fmt.money(4788));

        LinearLayout mini = findViewById(R.id.ob_mini);
        for (String[] s : SAMPLE) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            View line = new View(this);
            line.setBackgroundColor(Ui.attr(this, R.attr.jpLine));
            row.addView(line, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 1)));

            LinearLayout inner = new LinearLayout(this);
            inner.setOrientation(LinearLayout.HORIZONTAL);
            inner.setPadding(0, Ui.dp(this, 11), 0, Ui.dp(this, 11));
            TextView name = new TextView(this, null, 0, R.style.Jp_Semi);
            name.setText(s[0]);
            name.setTextSize(14);
            name.setSingleLine(true);
            name.setEllipsize(TextUtils.TruncateAt.END);
            inner.addView(name, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            TextView right = new TextView(this, null, 0, R.style.Jp_Text);
            right.setText(s[1] + " · " + Fmt.price(Double.parseDouble(s[2])));
            right.setTextSize(14);
            right.setTextColor(Ui.attr(this, R.attr.jpTx2));
            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            rp.setMarginStart(Ui.dp(this, 12));
            inner.addView(right, rp);
            row.addView(inner);
            mini.addView(row);
        }
    }
}
