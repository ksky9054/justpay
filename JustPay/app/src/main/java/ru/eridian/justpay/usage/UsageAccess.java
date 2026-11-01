package ru.eridian.justpay.usage;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Process;
import android.provider.Settings;

/** Доступ к статистике: особое разрешение, пользователь включает его сам */
public final class UsageAccess {
    private UsageAccess() {}

    @SuppressWarnings("deprecation")
    public static boolean isGranted(Context context) {
        AppOpsManager ops = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        if (ops == null) return false;
        int mode = Build.VERSION.SDK_INT >= 29
                ? ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.getPackageName())
                : ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    /** Системные настройки доступа */
    public static Intent settingsIntent(Context context) {
        Intent i = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
        if (Build.VERSION.SDK_INT >= 29) i.setData(Uri.parse("package:" + context.getPackageName()));
        return i;
    }
}
