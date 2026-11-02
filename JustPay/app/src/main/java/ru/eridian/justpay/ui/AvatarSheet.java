package ru.eridian.justpay.ui;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.function.Consumer;

import ru.eridian.justpay.R;
import ru.eridian.justpay.data.Account;
import ru.eridian.justpay.data.AvatarSpec;

/**
 * Выбор аватара: цвет, знак или фотография, с крупным предпросмотром.
 * Сохраняется кнопкой «Сохранить»; выбор и удаление фото выполняют переданные обработчики.
 */
final class AvatarSheet {
    private AvatarSheet() {}

    private static final class State {
        AvatarSpec spec;
    }

    /**
     * @param photo сохранённое фото аккаунта или null
     * @param onPickPhoto открыть системный выбор картинки
     * @param onDeletePhoto удалить сохранённое фото
     */
    static void show(Activity a, Account account, Bitmap photo, Consumer<AvatarSpec> onSave,
                     Runnable onPickPhoto, Runnable onDeletePhoto) {
        View sheet = LayoutInflater.from(a).inflate(R.layout.sheet_avatar, null, false);
        State st = new State();
        st.spec = account.avatarSpec();
        // Фото могли удалить файлом: тогда «Фото» нельзя оставить выбранным
        if (AvatarSpec.PHOTO.equals(st.spec.symbol) && photo == null) st.spec = st.spec.withSymbol(AvatarSpec.INITIALS);

        AvatarView preview = sheet.findViewById(R.id.av_preview);
        TextView caption = sheet.findViewById(R.id.av_caption);
        LinearLayout colors = sheet.findViewById(R.id.av_colors);
        FlowLayout symbols = sheet.findViewById(R.id.av_symbols);
        String initials = account.initials();

        FrameLayout[] colorCells = new FrameLayout[AvatarSpec.COLORS.length];
        FrameLayout[] symbolCells = new FrameLayout[AvatarSpec.SYMBOLS.length];
        AvatarView[] symbolTiles = new AvatarView[AvatarSpec.SYMBOLS.length];
        Dialog[] dialog = new Dialog[1];

        Runnable refresh = () -> {
            preview.setAvatar(st.spec, initials, photo);
            caption.setText(AvatarSpec.SYMBOL_NAMES[AvatarSpec.indexOf(st.spec.symbol)]
                    + (AvatarSpec.PHOTO.equals(st.spec.symbol) ? "" : " · " + AvatarSpec.COLOR_NAMES[st.spec.colorIndex]));
            for (int i = 0; i < colorCells.length; i++) {
                colorCells[i].setBackground(ring(a, i == st.spec.colorIndex));
            }
            for (int i = 0; i < symbolCells.length; i++) {
                symbolCells[i].setBackground(ring(a, AvatarSpec.SYMBOLS[i].equals(st.spec.symbol)));
                symbolTiles[i].setAvatar(st.spec.withSymbol(AvatarSpec.SYMBOLS[i]), initials, photo);
            }
        };

        // Цвета
        for (int i = 0; i < AvatarSpec.COLORS.length; i++) {
            final int index = i;
            FrameLayout cell = new FrameLayout(a);
            cell.setPadding(Ui.dp(a, 4), Ui.dp(a, 4), Ui.dp(a, 4), Ui.dp(a, 4));
            cell.setClickable(true);
            cell.setContentDescription(AvatarSpec.COLOR_NAMES[i]);
            View dot = new View(a);
            GradientDrawable g = new GradientDrawable();
            g.setShape(GradientDrawable.OVAL);
            g.setColor(AvatarSpec.COLORS[i]);
            dot.setBackground(g);
            cell.addView(dot, new FrameLayout.LayoutParams(Ui.dp(a, 34), Ui.dp(a, 34)));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.dp(a, 42), Ui.dp(a, 42));
            if (i > 0) lp.setMarginStart(Ui.dp(a, 6));
            colors.addView(cell, lp);
            colorCells[i] = cell;
            cell.setOnClickListener(v -> {
                st.spec = st.spec.withColor(index);
                refresh.run();
            });
        }

        // Знаки и фото
        for (int i = 0; i < AvatarSpec.SYMBOLS.length; i++) {
            final String symbol = AvatarSpec.SYMBOLS[i];
            FrameLayout cell = new FrameLayout(a);
            cell.setPadding(Ui.dp(a, 4), Ui.dp(a, 4), Ui.dp(a, 4), Ui.dp(a, 4));
            cell.setClickable(true);
            cell.setContentDescription(AvatarSpec.SYMBOL_NAMES[i]);
            AvatarView tile = new AvatarView(a);
            cell.addView(tile, new FrameLayout.LayoutParams(Ui.dp(a, 52), Ui.dp(a, 52), Gravity.CENTER));
            symbols.addView(cell, new ViewGroup.LayoutParams(Ui.dp(a, 60), Ui.dp(a, 60)));
            symbolCells[i] = cell;
            symbolTiles[i] = tile;
            cell.setOnClickListener(v -> {
                if (AvatarSpec.PHOTO.equals(symbol) && photo == null) {
                    // Фото ещё нет: закрываем шторку и открываем системный выбор картинки
                    dialog[0].dismiss();
                    onPickPhoto.run();
                    return;
                }
                st.spec = st.spec.withSymbol(symbol);
                refresh.run();
            });
        }

        // Действия с уже сохранённым фото
        sheet.findViewById(R.id.av_photo_actions).setVisibility(photo != null ? View.VISIBLE : View.GONE);
        sheet.findViewById(R.id.av_photo_change).setOnClickListener(v -> {
            dialog[0].dismiss();
            onPickPhoto.run();
        });
        sheet.findViewById(R.id.av_photo_delete).setOnClickListener(v -> {
            dialog[0].dismiss();
            onDeletePhoto.run();
        });
        refresh.run();

        sheet.findViewById(R.id.av_cancel).setOnClickListener(v -> dialog[0].dismiss());
        sheet.findViewById(R.id.av_save).setOnClickListener(v -> {
            dialog[0].dismiss();
            onSave.accept(st.spec);
        });
        dialog[0] = SheetDialog.show(a, sheet);
    }

    /** Кольцо вокруг выбранного варианта. */
    private static GradientDrawable ring(Activity a, boolean selected) {
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        if (selected) g.setStroke(Ui.dp(a, 2), Ui.attr(a, R.attr.jpTx));
        return g;
    }
}
