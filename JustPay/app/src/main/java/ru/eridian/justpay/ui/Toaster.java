package ru.eridian.justpay.ui;

import android.app.Activity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import ru.eridian.justpay.R;

/** Короткое сообщение внизу экрана, как в дизайне. */
public final class Toaster {
    private Toaster() {}

    private static String pending;
    private static final int TAG = R.id.root;

    /** Запоминает сообщение, чтобы показать его на экране, к которому вернёмся. */
    public static void post(String message) {
        pending = message;
    }

    public static void flush(Activity a) {
        if (pending == null) return;
        String m = pending;
        pending = null;
        show(a, m);
    }

    public static void show(Activity a, String message) {
        ViewGroup content = a.findViewById(android.R.id.content);
        if (content == null) return;
        Object old = content.getTag(TAG);
        if (old instanceof TextView) content.removeView((TextView) old);

        TextView t = new TextView(a);
        t.setTextAppearance(R.style.Jp_Semi);
        t.setText(message);
        t.setTextSize(14f);
        t.setTextColor(Ui.attr(a, R.attr.jpBg));
        t.setBackgroundResource(R.drawable.d_toast);
        t.setElevation(Ui.dp(a, 12));
        int ph = Ui.dp(a, 18);
        int pv = Ui.dp(a, 14);
        t.setPadding(ph, pv, ph, pv);

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.gravity = android.view.Gravity.BOTTOM;
        int m = Ui.dp(a, 16);
        lp.setMargins(m, 0, m, Ui.dp(a, 84));
        t.setLayoutParams(lp);

        t.setAlpha(0f);
        t.setTranslationY(Ui.dp(a, 12));
        content.addView(t);
        content.setTag(TAG, t);
        t.animate().alpha(1f).translationY(0).setDuration(250).start();
        t.postDelayed(() -> t.animate().alpha(0f).translationY(Ui.dp(a, 12)).setDuration(250)
                .withEndAction(() -> {
                    if (t.getParent() == content) content.removeView(t);
                    if (content.getTag(TAG) == t) content.setTag(TAG, null);
                }).start(), 2400);
    }
}
