package ru.eridian.justpay.ui;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;

import ru.eridian.justpay.R;
import ru.eridian.justpay.usage.AppCatalog;

/** Нижняя шторка со списком установленных приложений для привязки к подписке. */
public final class AppPickerSheet {
    private AppPickerSheet() {}

    public interface OnPick {
        void picked(AppCatalog.AppInfo app);
    }

    public static void show(Activity a, String subscriptionName, OnPick onPick) {
        List<AppCatalog.AppInfo> apps = AppCatalog.launchable(a, subscriptionName);
        Dialog dialog = new Dialog(a);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View sheet = LayoutInflater.from(a).inflate(R.layout.sheet_apps, null, false);

        TextView sub = sheet.findViewById(R.id.ap_sub);
        sub.setText(apps.isEmpty()
                ? "На телефоне не нашлось приложений для выбора."
                : "Выберите приложение «" + subscriptionName + "». JustPay будет считать, сколько раз вы его открываете.");

        ListView list = sheet.findViewById(R.id.ap_list);
        list.setAdapter(new ArrayAdapter<AppCatalog.AppInfo>(a, 0, apps) {
            @Override
            public View getView(int position, View convert, ViewGroup parent) {
                View v = convert != null ? convert
                        : LayoutInflater.from(a).inflate(R.layout.item_app, parent, false);
                AppCatalog.AppInfo app = getItem(position);
                ((ImageView) v.findViewById(R.id.app_icon)).setImageDrawable(app.icon);
                ((TextView) v.findViewById(R.id.app_label)).setText(app.label);
                v.findViewById(R.id.app_tag).setVisibility(app.suggested ? View.VISIBLE : View.GONE);
                return v;
            }
        });
        list.setOnItemClickListener((parent, view, position, id) -> {
            dialog.dismiss();
            onPick.picked(apps.get(position));
        });
        sheet.findViewById(R.id.ap_cancel).setOnClickListener(v -> dialog.dismiss());

        dialog.setContentView(sheet);
        Window w = dialog.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            w.setGravity(Gravity.BOTTOM);
            w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            w.setDimAmount(0.55f);
            w.setWindowAnimations(R.style.Jp_SheetAnim);
        }
        dialog.show();
    }
}
