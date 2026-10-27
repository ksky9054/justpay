package ru.eridian.justpay.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import ru.eridian.justpay.R;
import ru.eridian.justpay.data.AvatarSpec;

/** Круглый аватар: цветной круг с инициалами или знаком в стиле «Эридиан», либо фотография. */
public class AvatarView extends FrameLayout {
    private final TextView initials;
    private final ImageView glyph;
    private final ImageView photoView;

    public AvatarView(Context context) {
        this(context, null);
    }

    public AvatarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setClipToOutline(true); // контур берётся из круглого фона, фото обрезается по кругу

        initials = new TextView(context);
        initials.setGravity(Gravity.CENTER);
        initials.setIncludeFontPadding(false);
        initials.setTextColor(context.getColor(R.color.jp_ink));
        initials.setTypeface(context.getResources().getFont(R.font.unbounded_medium));
        addView(initials, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        glyph = new ImageView(context);
        glyph.setColorFilter(context.getColor(R.color.jp_ink));
        glyph.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        addView(glyph, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        photoView = new ImageView(context);
        photoView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        photoView.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        addView(photoView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        setAvatar(new AvatarSpec(AvatarSpec.INITIALS, 0), "", null);
    }

    public void setAvatar(AvatarSpec spec, String initialsText) {
        setAvatar(spec, initialsText, null);
    }

    /**
     * Рисует аватар. Для знака «Фото» нужна сама картинка; если её нет, показывается значок фото
     * (так выглядит плитка выбора, пока фото ещё не выбрано).
     */
    public void setAvatar(AvatarSpec spec, String initialsText, Bitmap photo) {
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(spec.color());
        setBackground(bg);

        boolean letters = AvatarSpec.INITIALS.equals(spec.symbol);
        boolean showPhoto = AvatarSpec.PHOTO.equals(spec.symbol) && photo != null;
        initials.setVisibility(letters ? VISIBLE : GONE);
        glyph.setVisibility(letters || showPhoto ? GONE : VISIBLE);
        photoView.setVisibility(showPhoto ? VISIBLE : GONE);
        initials.setText(initialsText);
        if (showPhoto) photoView.setImageBitmap(photo);
        else if (!letters) glyph.setImageResource(glyphFor(spec.symbol));
        updateSizes();
    }

    private static int glyphFor(String symbol) {
        switch (symbol) {
            case AvatarSpec.PLANET: return R.drawable.ic_av_planet;
            case AvatarSpec.MOON: return R.drawable.ic_av_moon;
            case AvatarSpec.COMET: return R.drawable.ic_av_comet;
            case AvatarSpec.RING: return R.drawable.ic_av_ring;
            case AvatarSpec.PHOTO: return R.drawable.ic_av_photo;
            default: return R.drawable.ic_star;
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateSizes();
    }

    private void updateSizes() {
        int size = Math.min(getWidth(), getHeight());
        if (size <= 0) return;
        initials.setTextSize(TypedValue.COMPLEX_UNIT_PX, size * 0.32f);
        int pad = Math.round(size * 0.24f);
        glyph.setPadding(pad, pad, pad, pad);
    }
}
