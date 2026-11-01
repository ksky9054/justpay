package ru.eridian.justpay.util;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Фотография аватара. Выбирается системным окном выбора изображения (разрешения не нужны),
 * обрезается до квадрата, уменьшается и сохраняется в приватной папке приложения. Исходный файл
 * не копируется, данные съёмки (место, время) при перекодировании теряются. Фото не покидает телефон.
 */
public final class AvatarPhoto {
    private AvatarPhoto() {}

    private static final int SIZE = 320;
    private static final int QUALITY = 90;
    private static final ExecutorService IO = Executors.newSingleThreadExecutor();

    public static File file(Context context, long accountId) {
        return new File(new File(context.getFilesDir(), "avatars"), accountId + ".jpg");
    }

    public static boolean exists(Context context, long accountId) {
        return file(context, accountId).isFile();
    }

    /** Сохранённое фото или null. */
    public static Bitmap load(Context context, long accountId) {
        File f = file(context, accountId);
        return f.isFile() ? BitmapFactory.decodeFile(f.getAbsolutePath()) : null;
    }

    public static void delete(Context context, long accountId) {
        File f = file(context, accountId);
        if (f.exists()) f.delete();
    }

    /** Обрабатывает выбранную картинку в фоне; onDone(true) в главном потоке, если фото сохранено. */
    public static void saveAsync(Context context, long accountId, Uri uri, Consumer<Boolean> onDone) {
        Context app = context.getApplicationContext();
        Handler main = new Handler(Looper.getMainLooper());
        IO.execute(() -> {
            boolean ok = save(app, accountId, uri);
            main.post(() -> onDone.accept(ok));
        });
    }

    static boolean save(Context context, long accountId, Uri uri) {
        ContentResolver cr = context.getContentResolver();
        try {
            // Сначала только размеры: гигантскую фотографию целиком в память не грузим
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            try (InputStream in = cr.openInputStream(uri)) {
                if (in == null) return false;
                BitmapFactory.decodeStream(in, null, bounds);
            }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return false; // не картинка

            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight);
            Bitmap src;
            try (InputStream in = cr.openInputStream(uri)) {
                if (in == null) return false;
                src = BitmapFactory.decodeStream(in, null, opts);
            }
            if (src == null) return false;

            int orientation = ExifInterface.ORIENTATION_NORMAL;
            try (InputStream in = cr.openInputStream(uri)) {
                if (in != null) {
                    orientation = new ExifInterface(in).getAttributeInt(
                            ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                }
            } catch (IOException ignored) {
                // Без данных об ориентации просто берём картинку как есть
            }

            // Квадрат по центру (поворот не меняет центр квадрата), затем нужный размер
            int side = Math.min(src.getWidth(), src.getHeight());
            Bitmap square = Bitmap.createBitmap(src, (src.getWidth() - side) / 2, (src.getHeight() - side) / 2,
                    side, side, matrixFor(orientation), true);
            Bitmap out = Bitmap.createScaledBitmap(square, SIZE, SIZE, true);

            File target = file(context, accountId);
            File dir = target.getParentFile();
            if (dir != null && !dir.isDirectory() && !dir.mkdirs()) return false;
            File tmp = new File(dir, accountId + ".tmp");
            try (FileOutputStream os = new FileOutputStream(tmp)) {
                if (!out.compress(Bitmap.CompressFormat.JPEG, QUALITY, os)) return false;
            }
            // Заменяем старое фото только после того, как новое полностью записано
            return tmp.renameTo(target) || (target.delete() && tmp.renameTo(target));
        } catch (IOException | SecurityException | OutOfMemoryError e) {
            return false;
        }
    }

    /** Во сколько раз уменьшить при чтении, чтобы короткая сторона осталась не меньше двух размеров аватара. */
    static int sampleSize(int width, int height) {
        int sample = 1;
        while (Math.min(width, height) / (sample * 2) >= SIZE * 2) sample *= 2;
        return sample;
    }

    /** Матрица для поворота и отражения по значению ориентации EXIF. */
    static Matrix matrixFor(int orientation) {
        Matrix m = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                m.postRotate(90);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                m.postRotate(180);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                m.postRotate(270);
                break;
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                m.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                m.postScale(1, -1);
                break;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                m.postRotate(90);
                m.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                m.postRotate(270);
                m.postScale(-1, 1);
                break;
            default:
                break;
        }
        return m;
    }
}
