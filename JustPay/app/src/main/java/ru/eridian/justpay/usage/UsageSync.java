package ru.eridian.justpay.usage;

import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import ru.eridian.justpay.data.Subscription;
import ru.eridian.justpay.data.SubscriptionRepository;
import ru.eridian.justpay.data.UsageRepository;

/** Берёт из статистики телефона открытия и время в приложениях; Android хранит события около недели, поэтому копим в базе */
public final class UsageSync {
    private UsageSync() {}

    private static final int WINDOW_DAYS = 7;
    /** Возврат быстрее этого срока считаем тем же открытием */
    private static final long SAME_LAUNCH_GAP_MS = 15_000;
    // События: 1 - приложение на экране, 2 - ушло с экрана
    private static final int EVENT_FOREGROUND = 1;
    private static final int EVENT_BACKGROUND = 2;

    private static final Executor IO = Executors.newSingleThreadExecutor();

    /** Работает в фоне, экран обновлять через runOnUiThread */
    public static void syncAsync(Context context, Runnable onDone) {
        Context app = context.getApplicationContext();
        IO.execute(() -> {
            try {
                sync(app);
            } catch (RuntimeException ignored) {
                // Сбой чтения статистики не должен ронять приложение
            }
            if (onDone != null) onDone.run();
        });
    }

    static final class Counter {
        long fgSince = -1;
        long lastPause = -1;
        final Map<LocalDate, int[]> perDay = new HashMap<>(); // день -> {открытия, секунды}
    }

    public static void sync(Context context) {
        if (!UsageAccess.isGranted(context)) return;

        List<Subscription> linked = new ArrayList<>();
        for (Subscription s : SubscriptionRepository.get(context).all()) {
            if (s.appPackage != null && !s.appPackage.isEmpty()) linked.add(s);
        }
        if (linked.isEmpty()) return;

        Set<String> packages = new HashSet<>();
        for (Subscription s : linked) packages.add(s.appPackage);

        UsageStatsManager usm = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        if (usm == null) return;
        ZoneId zone = ZoneId.systemDefault();
        long end = System.currentTimeMillis();
        long begin = LocalDate.now().minusDays(WINDOW_DAYS - 1).atStartOfDay(zone).toInstant().toEpochMilli();

        Map<String, Counter> counters = new HashMap<>();
        UsageEvents events = usm.queryEvents(begin, end);
        UsageEvents.Event e = new UsageEvents.Event();
        while (events.hasNextEvent()) {
            events.getNextEvent(e);
            String pkg = e.getPackageName();
            if (!packages.contains(pkg)) continue;
            Counter c = counters.get(pkg);
            if (c == null) {
                c = new Counter();
                counters.put(pkg, c);
            }
            apply(c, e.getEventType(), e.getTimeStamp(), zone);
        }
        for (Counter c : counters.values()) finish(c, end, zone);

        UsageRepository usage = UsageRepository.get(context);
        for (Subscription s : linked) {
            Counter c = counters.get(s.appPackage);
            if (c == null) continue;
            for (Map.Entry<LocalDate, int[]> d : c.perDay.entrySet()) {
                int opens = d.getValue()[0], seconds = d.getValue()[1];
                // Нулевые дни не пишем: старые события могли удалиться, пустой ответ не должен затирать накопленное
                if (opens > 0 || seconds > 0) usage.saveAuto(s.id, d.getKey(), opens, seconds);
            }
        }
    }

    /** Учитывает одно событие */
    static void apply(Counter c, int type, long ts, ZoneId zone) {
        if (type == EVENT_FOREGROUND) {
            if (c.fgSince < 0) {
                boolean newLaunch = c.lastPause < 0 || ts - c.lastPause > SAME_LAUNCH_GAP_MS;
                if (newLaunch) bucket(c, ts, zone)[0]++;
                c.fgSince = ts;
            }
        } else if (type == EVENT_BACKGROUND) {
            if (c.fgSince >= 0) {
                bucket(c, c.fgSince, zone)[1] += (int) ((ts - c.fgSince) / 1000);
                c.fgSince = -1;
                c.lastPause = ts;
            }
        }
    }

    static void finish(Counter c, long end, ZoneId zone) {
        // Приложение всё ещё на экране
        if (c.fgSince >= 0) {
            bucket(c, c.fgSince, zone)[1] += (int) ((end - c.fgSince) / 1000);
            c.fgSince = -1;
        }
    }

    private static int[] bucket(Counter c, long ts, ZoneId zone) {
        LocalDate day = Instant.ofEpochMilli(ts).atZone(zone).toLocalDate();
        int[] v = c.perDay.get(day);
        if (v == null) {
            v = new int[2];
            c.perDay.put(day, v);
        }
        return v;
    }
}
