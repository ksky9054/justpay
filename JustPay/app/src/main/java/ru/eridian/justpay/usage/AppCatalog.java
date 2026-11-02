package ru.eridian.justpay.usage;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Приложения на телефоне, которые можно привязать к подписке. */
public final class AppCatalog {
    private AppCatalog() {}

    public static final class AppInfo {
        public final String pkg;
        public final String label;
        public final Drawable icon;
        /** Название похоже на подписку: такие приложения показываются первыми. */
        public boolean suggested;

        AppInfo(String pkg, String label, Drawable icon) {
            this.pkg = pkg;
            this.label = label;
            this.icon = icon;
        }
    }

    /** Известные сервисы: слово в названии подписки -> пакет их приложения. */
    private static final Map<String, String> KNOWN = new LinkedHashMap<>();

    static {
        KNOWN.put("netflix", "com.netflix.mediaclient");
        KNOWN.put("youtube", "com.google.android.youtube");
        KNOWN.put("spotify", "com.spotify.music");
        KNOWN.put("telegram", "org.telegram.messenger");
        KNOWN.put("кинопоиск", "ru.kinopoisk");
        KNOWN.put("okko", "ru.more.play");
        KNOWN.put("иви", "ru.ivi.client");
        KNOWN.put("wink", "ru.rt.video.app.mobile");
        KNOWN.put("vk музыка", "com.vkontakte.android");
        KNOWN.put("яндекс музыка", "ru.yandex.music");
        KNOWN.put("chatgpt", "com.openai.chatgpt");
        KNOWN.put("figma", "com.figma.mobile");
        KNOWN.put("notion", "notion.id");
        KNOWN.put("duolingo", "com.duolingo");
        KNOWN.put("литрес", "ru.litres.android");
        KNOWN.put("apple music", "com.apple.android.music");
        KNOWN.put("twitch", "tv.twitch.android.app");
        KNOWN.put("яндекс 360", "ru.yandex.disk");
    }

    private static final Set<String> STOP_WORDS = new HashSet<>();

    static {
        Collections.addAll(STOP_WORDS, "premium", "plus", "pro", "max", "professional", "music", "музыка",
                "плюс", "прайм", "prime");
    }

    /** Все приложения с значком запуска, кроме самого JustPay. Подходящие по названию идут первыми. */
    public static List<AppInfo> launchable(Context context, String subscriptionName) {
        PackageManager pm = context.getPackageManager();
        Intent main = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> found = pm.queryIntentActivities(main, 0);

        String name = subscriptionName == null ? "" : subscriptionName.toLowerCase(Locale.getDefault());
        Set<String> knownPackages = new HashSet<>();
        for (Map.Entry<String, String> k : KNOWN.entrySet()) {
            if (name.contains(k.getKey())) knownPackages.add(k.getValue());
        }
        String token = firstToken(name);

        List<AppInfo> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (ResolveInfo ri : found) {
            String pkg = ri.activityInfo.packageName;
            if (pkg.equals(context.getPackageName()) || !seen.add(pkg)) continue;
            String label = String.valueOf(ri.loadLabel(pm));
            AppInfo a = new AppInfo(pkg, label, ri.loadIcon(pm));
            a.suggested = knownPackages.contains(pkg)
                    || (!token.isEmpty() && label.toLowerCase(Locale.getDefault()).contains(token));
            out.add(a);
        }
        Collections.sort(out, (x, y) -> {
            if (x.suggested != y.suggested) return x.suggested ? -1 : 1;
            return x.label.compareToIgnoreCase(y.label);
        });
        return out;
    }

    /** Первое значимое слово названия подписки: «Telegram Premium» -> «telegram». */
    static String firstToken(String name) {
        for (String t : name.split("[^\\p{L}\\p{N}]+")) {
            if (t.length() >= 3 && !STOP_WORDS.contains(t)) return t;
        }
        return "";
    }

    /** Название приложения или null, если оно удалено с телефона. */
    public static String labelOf(Context context, String pkg) {
        PackageManager pm = context.getPackageManager();
        Intent main = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(pkg);
        List<ResolveInfo> r = pm.queryIntentActivities(main, 0);
        return r.isEmpty() ? null : String.valueOf(r.get(0).loadLabel(pm));
    }
}
