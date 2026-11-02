package ru.eridian.justpay.ui;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

import ru.eridian.justpay.R;

/** Нижняя шторка с подтверждением. Закрывается кнопкой «Отмена», тапом вне шторки и кнопкой «Назад». */
public final class ConfirmSheet {
    private ConfirmSheet() {}

    public interface BodyBinder {
        void bind(View body);
    }

    /**
     * @param bodyLayout макет блока с расчётом или 0, если он не нужен
     * @param destructive красная кнопка действия (для удаления)
     */
    public static void show(Activity a, String title, int bodyLayout, BodyBinder binder,
                            String note, String cta, boolean destructive, Runnable onConfirm) {
        Dialog dialog = new Dialog(a);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        View sheet = LayoutInflater.from(a).inflate(R.layout.sheet_confirm, null, false);
        ((TextView) sheet.findViewById(R.id.sh_title)).setText(title);

        FrameLayout body = sheet.findViewById(R.id.sh_body);
        if (bodyLayout != 0) {
            View b = LayoutInflater.from(a).inflate(bodyLayout, body, false);
            if (binder != null) binder.bind(b);
            body.addView(b);
        }

        TextView noteView = sheet.findViewById(R.id.sh_note);
        if (note == null || note.isEmpty()) noteView.setVisibility(View.GONE);
        else noteView.setText(note);

        TextView ok = sheet.findViewById(R.id.sh_ok);
        ok.setText(cta);
        if (destructive) ok.setBackgroundResource(R.drawable.d_btn_danger);
        ok.setOnClickListener(v -> {
            dialog.dismiss();
            onConfirm.run();
        });
        sheet.findViewById(R.id.sh_cancel).setOnClickListener(v -> dialog.dismiss());

        dialog.setContentView(sheet);
        Window w = dialog.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            w.setGravity(android.view.Gravity.BOTTOM);
            w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            w.setDimAmount(0.55f);
            w.setWindowAnimations(R.style.Jp_SheetAnim);
        }
        // Отступ под жестовую панель навигации.
        sheet.setOnApplyWindowInsetsListener((v, insets) -> {
            int bottom = android.os.Build.VERSION.SDK_INT >= 30
                    ? insets.getInsets(android.view.WindowInsets.Type.systemBars()).bottom
                    : insets.getSystemWindowInsetBottom();
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(),
                    Ui.dp(a, 20) + bottom);
            return insets;
        });
        dialog.show();
    }
}
