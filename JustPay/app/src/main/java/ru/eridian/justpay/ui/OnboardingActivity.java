package ru.eridian.justpay.ui;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

import ru.eridian.justpay.R;
import ru.eridian.justpay.util.Fmt;

/** Три экрана знакомства */
public class OnboardingActivity extends BaseActivity {
    public static final String EXTRA_REPLAY = "replay";

    private static final String[] TITLES = {
            "Все подписки на одной орбите",
            "Ясная сумма каждый месяц",
            "Пауза и минус лишние траты"};
    private static final String[] BODIES = {
            "Видно, за что списываются деньги, сколько и когда.",
            "Считаем расходы в месяц и заранее напоминаем о списании.",
            "Поставьте на паузу ненужное и увидите экономию за год."};

    // Пример для страницы 2, это не данные пользователя
    private static final String[][] SAMPLE = {
            {"Музыка", "29 окт", "399"},
            {"Облако", "3 ноя", "299"},
            {"Видео", "8 ноя", "599"}};

    private int page = 0;
    private boolean replay;
    private View[] pages;
    private TextView title, body, next, skip;
    private LinearLayout dots;
    // Бесконечные анимации, останавливаем когда экран не виден
    private final ArrayList<ObjectAnimator> loops = new ArrayList<>();

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
    protected void onPause() {
        super.onPause();
        stopLoops();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (loops.isEmpty()) startLoops();
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
            // Без аккаунта главный экран сам отправит на вход
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
        playPage();
    }

    // Анимации при показе страницы
    private void playPage() {
        showUp(title, 0);
        showUp(body, 120);
        if (page == 0) playOrbit();
        if (page == 1) playMoney();
        if (page == 2) playSavings();
        stopLoops();
        startLoops();
    }

    // Страница 1: кольца, ядро и логотип
    private void playOrbit() {
        grow(findViewById(R.id.ob_ring_outer), 0);
        grow(findViewById(R.id.ob_ring_inner), 150);
        popIn(findViewById(R.id.ob_core), 300);

        View logo = findViewById(R.id.ob_logo);
        popIn(logo, 450);
        // Логотип крутится при появлении
        logo.setRotation(-180f);
        logo.animate().rotation(0f).setStartDelay(450).setDuration(900)
                .setInterpolator(new DecelerateInterpolator()).start();

        fadeIn(findViewById(R.id.ob_orbit_outer), 900);
        fadeIn(findViewById(R.id.ob_orbit_inner), 1100);
    }

    // Страница 2: сумма набирается, строки по очереди
    private void playMoney() {
        showUp(findViewById(R.id.ob_page1), 150);
        countUp((TextView) findViewById(R.id.ob_total), 1297, false);

        LinearLayout mini = findViewById(R.id.ob_mini);
        for (int i = 0; i < mini.getChildCount(); i++) {
            View row = mini.getChildAt(i);
            row.setAlpha(0f);
            row.setTranslationX(Ui.dp(this, 40));
            row.animate().alpha(1f).translationX(0f).setStartDelay(500 + i * 200L)
                    .setDuration(500).setInterpolator(new DecelerateInterpolator()).start();
        }
    }

    // Страница 3: карточки и число экономии
    private void playSavings() {
        View pause = findViewById(R.id.ob_pause_card);
        pause.setAlpha(0f);
        pause.setTranslationX(-Ui.dp(this, 60));
        pause.animate().alpha(1f).translationX(0f).setStartDelay(150).setDuration(600)
                .setInterpolator(new DecelerateInterpolator()).start();

        popIn(findViewById(R.id.ob_saved_card), 500);
        countUp((TextView) findViewById(R.id.ob_saved), 4788, true);
    }

    // Появление снизу
    private void showUp(View v, long delay) {
        v.animate().cancel();
        v.setAlpha(0f);
        v.setTranslationY(Ui.dp(this, 24));
        v.animate().alpha(1f).translationY(0f).setStartDelay(delay).setDuration(500)
                .setInterpolator(new DecelerateInterpolator()).start();
    }

    // Появление с отскоком
    private void popIn(View v, long delay) {
        v.animate().cancel();
        v.setScaleX(0f);
        v.setScaleY(0f);
        v.animate().scaleX(1f).scaleY(1f).setStartDelay(delay).setDuration(700)
                .setInterpolator(new OvershootInterpolator(2f)).start();
    }

    // Кольцо растёт из центра
    private void grow(View v, long delay) {
        v.animate().cancel();
        v.setAlpha(0f);
        v.setScaleX(0.3f);
        v.setScaleY(0.3f);
        v.animate().alpha(1f).scaleX(1f).scaleY(1f).setStartDelay(delay).setDuration(800)
                .setInterpolator(new DecelerateInterpolator()).start();
    }

    private void fadeIn(View v, long delay) {
        v.animate().cancel();
        v.setAlpha(0f);
        v.animate().alpha(1f).setStartDelay(delay).setDuration(600).start();
    }

    // Число набирается от нуля
    private void countUp(TextView view, int target, boolean withPlus) {
        ValueAnimator counter = ValueAnimator.ofFloat(0f, 1f);
        counter.setStartDelay(400);
        counter.setDuration(1200);
        counter.setInterpolator(new DecelerateInterpolator());
        counter.addUpdateListener(animation -> {
            float part = (float) animation.getAnimatedValue();
            int now = Math.round(target * part);
            view.setText(withPlus ? "+" + Fmt.money(now) : Fmt.moneyNum(now));
        });
        view.setText(withPlus ? "+" + Fmt.money(0) : Fmt.moneyNum(0));
        counter.start();
    }

    // Бесконечные движения: орбиты, дыхание, парение, пульс кнопки
    private void startLoops() {
        // Анимации отключены в настройках, не крутим
        if (!ValueAnimator.areAnimatorsEnabled()) return;

        if (page == 0) {
            spin(findViewById(R.id.ob_orbit_outer), 9000, 360f);
            spin(findViewById(R.id.ob_orbit_inner), 14000, -360f);
            breathe(findViewById(R.id.ob_core), 1.12f, 2000);
            hover(findViewById(R.id.ob_logo), 2600);
        }
        if (page == 2) {
            breathe(next, 1.05f, 900);
        }
    }

    private void stopLoops() {
        for (ObjectAnimator loop : loops) loop.cancel();
        loops.clear();
        next.setScaleX(1f);
        next.setScaleY(1f);
    }

    private void spin(View v, long ms, float degrees) {
        ObjectAnimator loop = ObjectAnimator.ofFloat(v, View.ROTATION, 0f, degrees);
        loop.setDuration(ms);
        loop.setInterpolator(new LinearInterpolator());
        loop.setRepeatCount(ValueAnimator.INFINITE);
        loop.start();
        loops.add(loop);
    }

    private void breathe(View v, float size, long ms) {
        ObjectAnimator loop = ObjectAnimator.ofPropertyValuesHolder(v,
                PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, size),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, size));
        loop.setDuration(ms);
        loop.setInterpolator(new AccelerateDecelerateInterpolator());
        loop.setRepeatCount(ValueAnimator.INFINITE);
        loop.setRepeatMode(ValueAnimator.REVERSE);
        loop.start();
        loops.add(loop);
    }

    private void hover(View v, long ms) {
        ObjectAnimator loop = ObjectAnimator.ofFloat(v, View.TRANSLATION_Y, 0f, -Ui.dp(this, 8));
        loop.setDuration(ms);
        loop.setInterpolator(new AccelerateDecelerateInterpolator());
        loop.setRepeatCount(ValueAnimator.INFINITE);
        loop.setRepeatMode(ValueAnimator.REVERSE);
        loop.start();
        loops.add(loop);
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
            right.setText(s[1] + " - " + Fmt.price(Double.parseDouble(s[2])));
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
