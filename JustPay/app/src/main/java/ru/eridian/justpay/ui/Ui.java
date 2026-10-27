package ru.eridian.justpay.ui;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowInsets;
import android.view.inputmethod.InputMethodManager;

/** Мелкие помощники для работы с размерами, цветами темы и системными отступами. */
public final class Ui {
    private Ui() {}

    public static int dp(Context c, float v) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                c.getResources().getDisplayMetrics()));
    }

    /** Цвет из атрибута текущей темы (например, R.attr.jpS1). */
    public static int attr(Context c, int attrId) {
        TypedValue tv = new TypedValue();
        c.getTheme().resolveAttribute(attrId, tv, true);
        return tv.data;
    }

    /** Смешивает два цвета: доля t берётся от a, остальное от b; результат непрозрачный. */
    public static int blend(int a, int b, float t) {
        int r = Math.round(Color.red(a) * t + Color.red(b) * (1 - t));
        int g = Math.round(Color.green(a) * t + Color.green(b) * (1 - t));
        int bl = Math.round(Color.blue(a) * t + Color.blue(b) * (1 - t));
        return Color.rgb(r, g, bl);
    }

    public static void tintBackground(View v, int color) {
        v.setBackgroundTintList(ColorStateList.valueOf(color));
    }

    /**
     * Приложение рисуется под системными панелями. Этот слушатель добавляет к корневому
     * контейнеру отступы под строку состояния, жесты навигации и клавиатуру.
     */
    public static void fitSystemBars(View root) {
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            int l, t, r, b;
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets i = insets.getInsets(WindowInsets.Type.systemBars()
                        | WindowInsets.Type.displayCutout() | WindowInsets.Type.ime());
                l = i.left;
                t = i.top;
                r = i.right;
                b = i.bottom;
            } else {
                l = insets.getSystemWindowInsetLeft();
                t = insets.getSystemWindowInsetTop();
                r = insets.getSystemWindowInsetRight();
                b = insets.getSystemWindowInsetBottom();
            }
            v.setPadding(l, t, r, b);
            return insets;
        });
        root.requestApplyInsets();
    }

    public static void hideKeyboard(Activity a) {
        View f = a.getCurrentFocus();
        if (f == null) return;
        InputMethodManager imm = (InputMethodManager) a.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(f.getWindowToken(), 0);
        f.clearFocus();
    }
}
