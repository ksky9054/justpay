package ru.eridian.justpay.ui;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import ru.eridian.justpay.R;
import ru.eridian.justpay.data.Subscription;
import ru.eridian.justpay.util.Fmt;

/** Форма добавления и редактирования подписки с проверкой введённых значений. */
public class EditActivity extends BaseActivity {
    public static final String EXTRA_ID = "id";

    private static final String[] CATEGORIES = {"Медиа", "Работа", "Облако", "Связь", "Обучение"};
    private static final String[][] QUICK = {
            {"Кинопоиск", "299", "Медиа"}, {"Wink", "349", "Медиа"}, {"Notion Plus", "950", "Работа"},
            {"Apple Music", "169", "Медиа"}, {"Сбер Прайм", "399", "Медиа"}};
    private static final double MAX_PRICE = 1_000_000;

    private Subscription editing;
    private String cycle = Subscription.CYCLE_MONTH;
    private String category = CATEGORIES[0];
    private LocalDate date;
    private boolean tried;

    private static final int REMIND_PROFILE = 0;
    private static final int REMIND_CUSTOM = 1;
    private static final int REMIND_NONE = 2;
    /** Выбранный режим напоминания и число дней «за сколько напомнить» для своей даты. */
    private int remindMode = REMIND_PROFILE;
    private int remindOffset = 3;
    private TextView remDefault, remCustom, remOff, fRemind, errRemind, remindHint;

    private EditText fName, fPrice, fCard;
    private TextView fDate, errName, errPrice, errDate, preview;
    private TextView cycW, cycM, cycY;
    private final TextView[] catChips = new TextView[CATEGORIES.length];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);
        Ui.fitSystemBars(findViewById(R.id.root));
        interceptBack();

        long id = getIntent().getLongExtra(EXTRA_ID, 0);
        editing = id > 0 ? repo.find(id) : null;

        fName = findViewById(R.id.f_name);
        fCard = findViewById(R.id.f_card);
        fPrice = findViewById(R.id.f_price);
        ((TextView) findViewById(R.id.label_price)).setText("Стоимость, " + Fmt.symbol());
        fDate = findViewById(R.id.f_date);
        errName = findViewById(R.id.err_name);
        errPrice = findViewById(R.id.err_price);
        errDate = findViewById(R.id.err_date);
        preview = findViewById(R.id.preview);
        cycW = findViewById(R.id.cyc_w);
        cycM = findViewById(R.id.cyc_m);
        cycY = findViewById(R.id.cyc_y);

        date = today().plusMonths(1);

        if (editing != null) {
            ((TextView) findViewById(R.id.title)).setText("Редактирование");
            ((TextView) findViewById(R.id.btn_save)).setText("Сохранить изменения");
            findViewById(R.id.quick_block).setVisibility(View.GONE);
            fName.setText(editing.name);
            fCard.setText(editing.card);
            fPrice.setText(trimZeros(editing.price));
            cycle = editing.cycle;
            category = editing.category;
            if (editing.next != null) date = editing.next;
            else {
                // У подписки на паузе нет ни даты списания, ни напоминания
                findViewById(R.id.date_block).setVisibility(View.GONE);
                findViewById(R.id.remind_block).setVisibility(View.GONE);
            }
            if (editing.remindDays == Subscription.REMIND_OFF) remindMode = REMIND_NONE;
            else if (editing.remindDays >= 0) {
                remindMode = REMIND_CUSTOM;
                remindOffset = editing.remindDays;
            }
        } else if (savedInstanceState != null) {
            cycle = savedInstanceState.getString("cycle", cycle);
            category = savedInstanceState.getString("category", category);
            String d = savedInstanceState.getString("date");
            if (d != null) date = LocalDate.parse(d);
        }

        if (editing == null || editing.remindDays == Subscription.REMIND_DEFAULT) {
            remindOffset = prefs.remindDays();
        }
        if (savedInstanceState != null) {
            remindMode = savedInstanceState.getInt("rmode", remindMode);
            remindOffset = savedInstanceState.getInt("roffset", remindOffset);
        }

        remDefault = findViewById(R.id.rem_default);
        remCustom = findViewById(R.id.rem_custom);
        remOff = findViewById(R.id.rem_off);
        fRemind = findViewById(R.id.f_remind);
        errRemind = findViewById(R.id.err_remind);
        remindHint = findViewById(R.id.remind_hint);
        remDefault.setOnClickListener(v -> setRemindMode(REMIND_PROFILE));
        remCustom.setOnClickListener(v -> setRemindMode(REMIND_CUSTOM));
        remOff.setOnClickListener(v -> setRemindMode(REMIND_NONE));
        fRemind.setOnClickListener(v -> pickRemindDate());

        buildQuickChips();
        buildCategoryChips();

        findViewById(R.id.btn_close).setOnClickListener(v -> finishToBottom());
        cycW.setOnClickListener(v -> setCycle(Subscription.CYCLE_WEEK));
        cycM.setOnClickListener(v -> setCycle(Subscription.CYCLE_MONTH));
        cycY.setOnClickListener(v -> setCycle(Subscription.CYCLE_YEAR));
        fDate.setOnClickListener(v -> pickDate());
        findViewById(R.id.btn_save).setOnClickListener(v -> save());

        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                if (tried) validate(false);
                updatePreview();
            }
        };
        fName.addTextChangedListener(watcher);
        fPrice.addTextChangedListener(watcher);

        updateCycleUi();
        updateDateUi();
        updateRemindUi();
        updatePreview();
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putString("cycle", cycle);
        out.putString("category", category);
        out.putString("date", date.toString());
        out.putInt("rmode", remindMode);
        out.putInt("roffset", remindOffset);
    }

    @Override
    protected boolean onBack() {
        finishToBottom();
        return true;
    }

    // ----- элементы формы -----

    private void buildQuickChips() {
        LinearLayout row = findViewById(R.id.quick_row);
        for (String[] q : QUICK) {
            TextView chip = new TextView(this, null, 0, R.style.Jp_Chip);
            chip.setText(q[0]);
            chip.setBackgroundResource(R.drawable.d_chip_quick);
            chip.setTextColor(Ui.attr(this, R.attr.jpTx));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(Ui.dp(this, 8));
            chip.setLayoutParams(lp);
            chip.setOnClickListener(v -> {
                fName.setText(q[0]);
                fPrice.setText(q[1]);
                category = q[2];
                updateCategoryUi();
                Ui.hideKeyboard(this);
            });
            row.addView(chip);
        }
    }

    private void buildCategoryChips() {
        FlowLayout flow = findViewById(R.id.cat_flow);
        for (int i = 0; i < CATEGORIES.length; i++) {
            final String c = CATEGORIES[i];
            TextView chip = new TextView(this, null, 0, R.style.Jp_Chip);
            chip.setText(c);
            chip.setOnClickListener(v -> {
                category = c;
                updateCategoryUi();
            });
            catChips[i] = chip;
            flow.addView(chip, new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        updateCategoryUi();
    }

    private void updateCategoryUi() {
        for (int i = 0; i < CATEGORIES.length; i++) catChips[i].setSelected(CATEGORIES[i].equals(category));
    }

    private void setCycle(String c) {
        cycle = c;
        updateCycleUi();
        updatePreview();
    }

    private void updateCycleUi() {
        cycW.setSelected(Subscription.CYCLE_WEEK.equals(cycle));
        cycM.setSelected(Subscription.CYCLE_MONTH.equals(cycle));
        cycY.setSelected(Subscription.CYCLE_YEAR.equals(cycle));
    }

    private void updateDateUi() {
        fDate.setText(Fmt.dayFull(date));
    }

    private void pickDate() {
        Ui.hideKeyboard(this);
        DatePickerDialog d = new DatePickerDialog(this, (view, y, m, day) -> {
            date = LocalDate.of(y, m + 1, day);
            updateDateUi();
            updateRemindUi();
            if (tried) validate(false);
        }, date.getYear(), date.getMonthValue() - 1, date.getDayOfMonth());
        long min = today().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long max = today().plusYears(5).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        d.getDatePicker().setMinDate(min);
        d.getDatePicker().setMaxDate(max);
        d.show();
    }

    // ----- напоминание -----

    private boolean hasRemindBlock() {
        return editing == null || editing.next != null;
    }

    private LocalDate customRemindDate() {
        return date.minusDays(remindOffset);
    }

    private void setRemindMode(int mode) {
        remindMode = mode;
        if (mode == REMIND_CUSTOM) {
            // Первое включение: предлагаем дату по настройке профиля, но не раньше сегодняшнего дня
            long max = Math.max(0, Fmt.diff(date, today()));
            if (remindOffset > max) remindOffset = (int) max;
        }
        updateRemindUi();
        if (tried) validate(false);
    }

    private void pickRemindDate() {
        Ui.hideKeyboard(this);
        LocalDate shown = customRemindDate();
        if (shown.isBefore(today())) shown = today();
        DatePickerDialog d = new DatePickerDialog(this, (view, y, m, day) -> {
            remindOffset = (int) Fmt.diff(date, LocalDate.of(y, m + 1, day));
            updateRemindUi();
            if (tried) validate(false);
        }, shown.getYear(), shown.getMonthValue() - 1, shown.getDayOfMonth());
        ZoneId zone = ZoneId.systemDefault();
        d.getDatePicker().setMinDate(today().atStartOfDay(zone).toInstant().toEpochMilli());
        d.getDatePicker().setMaxDate(date.atStartOfDay(zone).toInstant().toEpochMilli());
        d.show();
    }

    private static String beforeText(int days) {
        return days == 0 ? "в день списания" : "за " + days + " " + Fmt.pl(days, "день", "дня", "дней") + " до списания";
    }

    private void updateRemindUi() {
        remDefault.setSelected(remindMode == REMIND_PROFILE);
        remCustom.setSelected(remindMode == REMIND_CUSTOM);
        remOff.setSelected(remindMode == REMIND_NONE);
        fRemind.setVisibility(remindMode == REMIND_CUSTOM ? View.VISIBLE : View.GONE);
        if (remindMode != REMIND_CUSTOM) showError(fRemind, errRemind, null);

        String hint;
        if (remindMode == REMIND_CUSTOM) {
            LocalDate rd = customRemindDate();
            fRemind.setText(Fmt.dayFull(rd));
            hint = "Напомним " + Fmt.dayLong(rd) + " в 09:00, " + beforeText(remindOffset) + ".";
        } else if (remindMode == REMIND_NONE) {
            hint = "Уведомления по этой подписке не придут.";
        } else if (prefs.isRemind()) {
            int n = prefs.remindDays();
            hint = "Напомним " + Fmt.dayLong(date.minusDays(n)) + " в 09:00, " + beforeText(n)
                    + ". Срок задан в профиле.";
        } else {
            hint = "В профиле напоминания выключены, поэтому уведомление не придёт. Выберите «Своя дата», чтобы включить его для этой подписки.";
        }
        remindHint.setText(hint);
    }

    // ----- проверка и расчёт -----

    /** Возвращает сумму или -1, если поле пустое либо содержит не число. */
    private double parsePrice() {
        String raw = fPrice.getText().toString().trim().replace(',', '.');
        if (raw.isEmpty() || raw.equals(".")) return -1;
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private void updatePreview() {
        double p = parsePrice();
        if (p <= 0 || p > MAX_PRICE) {
            preview.setText("Укажите стоимость — посчитаем, сколько это в месяц и в год.");
            return;
        }
        double month = Subscription.monthly(p, cycle);
        StringBuilder sb = new StringBuilder("≈ " + Fmt.money(month) + " в месяц · " + Fmt.money(month * 12) + " в год.");
        if (editing == null || editing.isActive()) {
            double others = 0;
            List<Subscription> all = repo.all();
            for (Subscription s : all) {
                if (s.isActive() && (editing == null || s.id != editing.id)) others += s.monthly();
            }
            sb.append(" Общие расходы станут ").append(Fmt.money(others + month)).append(" в месяц.");
        }
        preview.setText(sb);
    }

    private static void showError(View field, TextView error, String message) {
        boolean bad = message != null;
        if (field != null) field.setActivated(bad);
        error.setVisibility(bad ? View.VISIBLE : View.GONE);
        if (bad) error.setText(message);
    }

    /** Показывает ошибки у полей; возвращает true, если всё заполнено верно. */
    private boolean validate(boolean focusFirst) {
        String name = fName.getText().toString().trim();
        String nameErr = name.isEmpty() ? "Введите название сервиса" : null;

        double p = parsePrice();
        String priceErr = null;
        if (p <= 0) priceErr = "Укажите сумму больше нуля";
        else if (p > MAX_PRICE) priceErr = "Сумма слишком большая: не больше " + Fmt.price(MAX_PRICE);

        String dateErr = null;
        boolean needsDate = editing == null || editing.next != null;
        // Дату, которую не меняли, не ругаем: за время редактирования она могла стать вчерашней
        boolean dateUnchanged = editing != null && date.equals(editing.next);
        if (needsDate && !dateUnchanged && date.isBefore(today())) dateErr = "Дата списания не может быть в прошлом";

        String remindErr = null;
        if (hasRemindBlock() && remindMode == REMIND_CUSTOM && customRemindDate().isBefore(today())) {
            remindErr = "Дата напоминания уже прошла: выберите день от сегодняшнего до даты списания";
        }

        showError(fName, errName, nameErr);
        showError(fPrice, errPrice, priceErr);
        showError(fDate, errDate, dateErr);
        showError(fRemind, errRemind, remindErr);

        if (focusFirst) {
            if (nameErr != null) fName.requestFocus();
            else if (priceErr != null) fPrice.requestFocus();
        }
        return nameErr == null && priceErr == null && dateErr == null && remindErr == null;
    }

    private int remindValue() {
        if (remindMode == REMIND_NONE) return Subscription.REMIND_OFF;
        if (remindMode == REMIND_CUSTOM) return remindOffset;
        return Subscription.REMIND_DEFAULT;
    }

    private static String trimZeros(double v) {
        return v == Math.floor(v) ? String.valueOf((long) v) : String.valueOf(v);
    }

    private static String makeMark(String name) {
        String clean = name.replaceAll("[^A-Za-zА-Яа-яЁё0-9]", "");
        if (clean.isEmpty()) return "—";
        return clean.substring(0, Math.min(2, clean.length()));
    }

    private void save() {
        tried = true;
        if (!validate(true)) return;
        Ui.hideKeyboard(this);

        String name = fName.getText().toString().trim();
        double price = parsePrice();

        if (editing == null) {
            Subscription s = new Subscription();
            s.name = name;
            s.mark = makeMark(name);
            s.price = price;
            s.cycle = cycle;
            s.next = date;
            s.category = category;
            s.status = Subscription.ACTIVE;
            s.card = fCard.getText().toString().trim();
            s.since = Fmt.monthYear(today());
            s.remindDays = remindValue();
            repo.insert(s);
            Toaster.post("«" + name + "» добавлена · " + Fmt.money(Subscription.monthly(price, cycle)) + " в месяц");
        } else {
            if (!name.equals(editing.name)) editing.mark = makeMark(name);
            editing.name = name;
            editing.price = price;
            editing.cycle = cycle;
            editing.category = category;
            editing.card = fCard.getText().toString().trim();
            if (editing.next != null) {
                // Сменили дату списания или напоминания: уведомление может прийти заново
                if (!date.equals(editing.next) || editing.remindDays != remindValue()) {
                    prefs.forgetNotified(editing.id);
                }
                editing.next = date;
                editing.remindDays = remindValue();
            }
            repo.update(editing);
            Toaster.post("Изменения сохранены");
        }
        finishToBottom();
    }
}
