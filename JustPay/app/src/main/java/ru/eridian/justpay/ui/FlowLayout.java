package ru.eridian.justpay.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

/** Раскладка с переносом дочерних элементов на новую строку (для чипов категорий). */
public class FlowLayout extends ViewGroup {
    private final int gap;

    public FlowLayout(Context context) {
        this(context, null);
    }

    public FlowLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        gap = Ui.dp(context, 8);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int maxWidth = MeasureSpec.getSize(widthSpec);
        int x = 0, y = 0, lineH = 0;
        for (int i = 0; i < getChildCount(); i++) {
            View c = getChildAt(i);
            if (c.getVisibility() == GONE) continue;
            measureChild(c, MeasureSpec.makeMeasureSpec(maxWidth, MeasureSpec.AT_MOST), heightSpec);
            int w = c.getMeasuredWidth(), h = c.getMeasuredHeight();
            if (x > 0 && x + w > maxWidth) {
                x = 0;
                y += lineH + gap;
                lineH = 0;
            }
            x += w + gap;
            lineH = Math.max(lineH, h);
        }
        setMeasuredDimension(maxWidth, y + lineH);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int maxWidth = r - l;
        int x = 0, y = 0, lineH = 0;
        for (int i = 0; i < getChildCount(); i++) {
            View c = getChildAt(i);
            if (c.getVisibility() == GONE) continue;
            int w = c.getMeasuredWidth(), h = c.getMeasuredHeight();
            if (x > 0 && x + w > maxWidth) {
                x = 0;
                y += lineH + gap;
                lineH = 0;
            }
            c.layout(x, y, x + w, y + h);
            x += w + gap;
            lineH = Math.max(lineH, h);
        }
    }
}
