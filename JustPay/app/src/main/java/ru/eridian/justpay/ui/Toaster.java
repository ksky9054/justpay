package ru.eridian.justpay.ui;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.ViewGroup;
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

        TextView t = (TextView) LayoutInflater.from(a).inflate(R.layout.view_toast, content, false);
        t.setText(message);
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
