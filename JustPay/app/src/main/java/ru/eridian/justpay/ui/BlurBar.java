package ru.eridian.justpay.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import ru.eridian.justpay.R;

/** Нижняя панель с размытым фоном: под ней рисуется размытая копия содержимого экрана */
public class BlurBar extends LinearLayout {
    // Во сколько раз уменьшаем картинку перед размытием
    private static final int SCALE = 8;
    // Радиус размытия в пикселях уменьшенной картинки
    private static final int RADIUS = 3;

    private View source;
    private Bitmap small;
    private int[] pixels;
    private int[] temp;
    private final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);

    public BlurBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /** Вид, содержимое которого размываем (то, что лежит под панелью) */
    public void setSource(View view) {
        source = view;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);
        int smallW = Math.max(1, w / SCALE);
        int smallH = Math.max(1, h / SCALE);
        small = Bitmap.createBitmap(smallW, smallH, Bitmap.Config.ARGB_8888);
        pixels = new int[smallW * smallH];
        temp = new int[smallW * smallH];
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (source != null && small != null) {
            int bg = Ui.attr(getContext(), R.attr.jpBg);

            // Рисуем кусок экрана под панелью в маленькую картинку
            Canvas c = new Canvas(small);
            c.drawColor(bg);
            c.scale(1f / SCALE, 1f / SCALE);
            c.translate(-getLeft(), -(getTop() - source.getTop()));
            source.draw(c);

            // Размываем её и растягиваем на всю панель, сверху лёгкая дымка цвета фона
            blur();
            canvas.drawBitmap(small, null, new Rect(0, 0, getWidth(), getHeight()), paint);
            canvas.drawColor((bg & 0x00FFFFFF) | 0x99000000);
        }
        super.dispatchDraw(canvas);
    }

    // Размытие в два захода: по строкам и по столбцам, потом ещё раз, чтобы края стали мягкими
    private void blur() {
        int w = small.getWidth();
        int h = small.getHeight();
        small.getPixels(pixels, 0, w, 0, 0, w, h);
        for (int i = 0; i < 2; i++) {
            average(pixels, temp, w, h, true);
            average(temp, pixels, w, h, false);
        }
        small.setPixels(pixels, 0, w, 0, 0, w, h);
    }

    // Каждый пиксель становится средним цветом соседей по строке или по столбцу
    private void average(int[] from, int[] to, int w, int h, boolean horizontal) {
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int a = 0, r = 0, g = 0, b = 0, count = 0;
                for (int k = -RADIUS; k <= RADIUS; k++) {
                    int nx = horizontal ? x + k : x;
                    int ny = horizontal ? y : y + k;
                    if (nx < 0 || nx >= w || ny < 0 || ny >= h) continue;
                    int color = from[ny * w + nx];
                    a += color >>> 24;
                    r += (color >> 16) & 0xFF;
                    g += (color >> 8) & 0xFF;
                    b += color & 0xFF;
                    count++;
                }
                to[y * w + x] = ((a / count) << 24) | ((r / count) << 16) | ((g / count) << 8) | (b / count);
            }
        }
    }
}
