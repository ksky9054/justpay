package ru.eridian.justpay.ui;

import android.app.Activity;
import android.app.Dialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import java.time.LocalDate;
import java.util.function.Consumer;

import ru.eridian.justpay.R;
import ru.eridian.justpay.util.Fmt;

/** Выбор своего срока напоминания: за сколько дней до списания присылать уведомление. */
final class DaysSheet {
    private DaysSheet() {}

    static final int MIN_DAYS = 0;
    static final int MAX_DAYS = 60;

    static void show(Activity a, int current, Consumer<Integer> onSave) {
        View sheet = LayoutInflater.from(a).inflate(R.layout.sheet_days, null, false);
        int[] days = {Math.max(MIN_DAYS, Math.min(MAX_DAYS, current))};
        TextView value = sheet.findViewById(R.id.dy_value);
        TextView unit = sheet.findViewById(R.id.dy_unit);
        TextView example = sheet.findViewById(R.id.dy_example);
        View minus = sheet.findViewById(R.id.dy_minus);
        View plus = sheet.findViewById(R.id.dy_plus);

        Runnable refresh = () -> {
            int n = days[0];
            value.setText(String.valueOf(n));
            unit.setText(n == 0 ? "в день списания"
                    : Fmt.pl(n, "день", "дня", "дней") + " до списания");
            LocalDate charge = LocalDate.now().plusDays(n + 10L);
            example.setText("Например, списание " + Fmt.dayLong(charge) + ": уведомление придёт "
                    + Fmt.dayLong(charge.minusDays(n)) + " в 09:00");
            minus.setAlpha(n > MIN_DAYS ? 1f : 0.35f);
            plus.setAlpha(n < MAX_DAYS ? 1f : 0.35f);
        };
        minus.setOnClickListener(v -> {
            if (days[0] > MIN_DAYS) days[0]--;
            refresh.run();
        });
        plus.setOnClickListener(v -> {
            if (days[0] < MAX_DAYS) days[0]++;
            refresh.run();
        });
        refresh.run();

        Dialog[] dialog = new Dialog[1];
        sheet.findViewById(R.id.dy_cancel).setOnClickListener(v -> dialog[0].dismiss());
        sheet.findViewById(R.id.dy_save).setOnClickListener(v -> {
            dialog[0].dismiss();
            onSave.accept(days[0]);
        });
        dialog[0] = SheetDialog.show(a, sheet);
    }
}
