package ru.eridian.justpay.ui;

import android.app.Activity;
import android.app.Dialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.function.Consumer;

import ru.eridian.justpay.R;
import ru.eridian.justpay.data.Currency;

/** Выбор валюты аккаунта. */
final class CurrencySheet {
    private CurrencySheet() {}

    static void show(Activity a, Currency current, Consumer<Currency> onPick) {
        View sheet = LayoutInflater.from(a).inflate(R.layout.sheet_currency, null, false);
        LinearLayout list = sheet.findViewById(R.id.cur_list);
        Dialog[] dialog = new Dialog[1];

        for (Currency c : Currency.values()) {
            View row = LayoutInflater.from(a).inflate(R.layout.item_currency, list, false);
            ((TextView) row.findViewById(R.id.cur_symbol)).setText(c.symbol);
            ((TextView) row.findViewById(R.id.cur_title)).setText(c.title);
            ((TextView) row.findViewById(R.id.cur_code)).setText(c.name());
            row.findViewById(R.id.cur_check).setVisibility(c == current ? View.VISIBLE : View.INVISIBLE);
            row.setOnClickListener(v -> {
                dialog[0].dismiss();
                if (c != current) onPick.accept(c);
            });
            list.addView(row, new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        sheet.findViewById(R.id.cur_cancel).setOnClickListener(v -> dialog[0].dismiss());
        dialog[0] = SheetDialog.show(a, sheet);
    }
}
