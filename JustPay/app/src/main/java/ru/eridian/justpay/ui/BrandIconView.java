package ru.eridian.justpay.ui;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import ru.eridian.justpay.R;
import ru.eridian.justpay.data.BrandCatalog;

/** Значок подписки: логотип сервиса на плитке его цвета, а если сервис не найден, первые буквы названия */
public class BrandIconView extends View {
    private final Paint tile = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint letters = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    private Drawable glyph;
    private String text = "";

    public BrandIconView(Context context) {
        this(context, null);
    }

    public BrandIconView(Context context, AttributeSet attrs) {
        super(context, attrs);
        letters.setTextAlign(Paint.Align.CENTER);
        letters.setTypeface(context.getResources().getFont(R.font.exo_bold));
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        setName("");
    }

    /** Подбирает значок по названию подписки */
    public void setName(String name) {
        BrandCatalog.Brand brand = BrandCatalog.get(getContext()).find(name);
        glyph = brand == null ? null : loadGlyph(brand);
        if (glyph != null) {
            int color = brand.color;
            // Совсем тёмная плитка сливается с чёрным фоном, поэтому берём цвет карточки
            if (brightness(color) < 0.12f) color = Ui.attr(getContext(), R.attr.jpS2);
            tile.setColor(color);
            glyph.mutate().setTint(brightness(color) > 0.6f ? Color.parseColor("#111216") : Color.WHITE);
        } else {
            tile.setColor(Ui.attr(getContext(), R.attr.jpS2));
            letters.setColor(Ui.attr(getContext(), R.attr.jpTx));
            text = firstLetters(name);
        }
        invalidate();
    }

    private Drawable loadGlyph(BrandCatalog.Brand brand) {
        if (brand.resId == -1) {
            Resources res = getResources();
            brand.resId = res.getIdentifier("brand_" + brand.slug, "drawable", getContext().getPackageName());
        }
        if (brand.resId == 0) return null;
        try {
            return getContext().getDrawable(brand.resId);
        } catch (Resources.NotFoundException e) {
            // Значок не разобрался, тогда покажем буквы
            brand.resId = 0;
            return null;
        }
    }

    // Яркость цвета от 0 до 1
    private static float brightness(int color) {
        return (0.299f * Color.red(color) + 0.587f * Color.green(color) + 0.114f * Color.blue(color)) / 255f;
    }

    // Первые две буквы или цифры названия
    private static String firstLetters(String name) {
        if (name == null) return "?";
        String clean = name.replaceAll("[^\\p{L}\\p{N}]", "");
        if (clean.isEmpty()) return "?";
        return clean.substring(0, Math.min(2, clean.length())).toUpperCase();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        rect.set(0, 0, w, h);
        canvas.drawRoundRect(rect, w * 0.28f, h * 0.28f, tile);

        if (glyph != null) {
            int pad = Math.round(w * 0.24f);
            glyph.setBounds(pad, pad, Math.round(w) - pad, Math.round(h) - pad);
            glyph.draw(canvas);
        } else {
            letters.setTextSize(h * 0.38f);
            float baseline = h / 2f - (letters.descent() + letters.ascent()) / 2f;
            canvas.drawText(text, w / 2f, baseline, letters);
        }
    }
}
