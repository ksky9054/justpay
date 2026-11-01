package ru.eridian.justpay.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Patterns;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ru.eridian.justpay.R;
import ru.eridian.justpay.data.Account;
import ru.eridian.justpay.data.AccountRepository;

/** Вход в локальный аккаунт и регистрация нового. Аккаунты хранятся только на этом устройстве. */
public class AuthActivity extends BaseActivity {
    private static final int MAX_FAILS = 5;
    private static final long LOCK_MS = 30_000;
    // Защита от перебора пароля: считаем неудачные входы и временно блокируем кнопку
    private static int fails = 0;
    private static long lockedUntil = 0;

    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor();

    private boolean register = false;
    private boolean passwordShown = false;
    private boolean busy = false;

    private View blockName, blockPass2;
    private EditText aName, aEmail, aPass, aPass2;
    private TextView errName, errEmail, errPass, errPass2, authError, submit, title, subtitle;
    private TextView tabLogin, tabRegister, btnShow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (prefs.isSignedIn()) {
            openMain();
            return;
        }
        setContentView(R.layout.activity_auth);
        Ui.fitSystemBars(findViewById(R.id.root));

        blockName = findViewById(R.id.block_name);
        blockPass2 = findViewById(R.id.block_pass2);
        aName = findViewById(R.id.a_name);
        aEmail = findViewById(R.id.a_email);
        aPass = findViewById(R.id.a_pass);
        aPass2 = findViewById(R.id.a_pass2);
        errName = findViewById(R.id.err_name);
        errEmail = findViewById(R.id.err_email);
        errPass = findViewById(R.id.err_pass);
        errPass2 = findViewById(R.id.err_pass2);
        authError = findViewById(R.id.auth_error);
        submit = findViewById(R.id.auth_submit);
        title = findViewById(R.id.auth_title);
        subtitle = findViewById(R.id.auth_subtitle);
        tabLogin = findViewById(R.id.tab_login);
        tabRegister = findViewById(R.id.tab_register);
        btnShow = findViewById(R.id.btn_show);

        if (savedInstanceState != null) register = savedInstanceState.getBoolean("register", false);

        tabLogin.setOnClickListener(v -> setMode(false));
        tabRegister.setOnClickListener(v -> setMode(true));
        submit.setOnClickListener(v -> submit());
        btnShow.setOnClickListener(v -> togglePassword());
        aPass.setOnEditorActionListener((v, id, e) -> {
            if (!register && id == EditorInfo.IME_ACTION_DONE) {
                submit();
                return true;
            }
            return false;
        });
        aPass2.setOnEditorActionListener((v, id, e) -> {
            if (id == EditorInfo.IME_ACTION_DONE) {
                submit();
                return true;
            }
            return false;
        });
        // Для входа на клавиатуре у пароля кнопка «Готово»
        setMode(register);
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putBoolean("register", register);
    }

    private void openMain() {
        startActivity(new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }

    private void setMode(boolean reg) {
        register = reg;
        tabLogin.setSelected(!reg);
        tabRegister.setSelected(reg);
        blockName.setVisibility(reg ? View.VISIBLE : View.GONE);
        blockPass2.setVisibility(reg ? View.VISIBLE : View.GONE);
        title.setText(reg ? "Создайте аккаунт" : "С возвращением");
        subtitle.setText(reg
                ? "Новый аккаунт пуст: подписки, платежи и отметки вы добавите сами."
                : "Войдите, чтобы увидеть свои подписки.");
        submit.setText(reg ? "Создать аккаунт" : "Войти");
        aPass.setImeOptions(reg ? EditorInfo.IME_ACTION_NEXT : EditorInfo.IME_ACTION_DONE);
        clearErrors();
    }

    private void togglePassword() {
        passwordShown = !passwordShown;
        int pos = aPass.getSelectionEnd();
        aPass.setTransformationMethod(passwordShown
                ? HideReturnsTransformationMethod.getInstance() : PasswordTransformationMethod.getInstance());
        aPass2.setTransformationMethod(aPass.getTransformationMethod());
        aPass.setSelection(Math.max(0, pos));
        btnShow.setText(passwordShown ? "Скрыть" : "Показать");
    }

    private void clearErrors() {
        show(aName, errName, null);
        show(aEmail, errEmail, null);
        show(aPass, errPass, null);
        show(aPass2, errPass2, null);
        authError.setVisibility(View.GONE);
    }

    private static void show(View field, TextView err, String msg) {
        field.setActivated(msg != null);
        err.setVisibility(msg != null ? View.VISIBLE : View.GONE);
        if (msg != null) err.setText(msg);
    }

    private void submit() {
        if (busy) return;
        clearErrors();

        String name = aName.getText().toString().trim();
        String email = aEmail.getText().toString().trim();
        String pass = aPass.getText().toString();
        String pass2 = aPass2.getText().toString();

        boolean ok = true;
        if (register && name.length() < 2) {
            show(aName, errName, "Введите имя, минимум 2 буквы");
            ok = false;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            show(aEmail, errEmail, "Введите почту в виде name@example.com");
            ok = false;
        }
        if (register && pass.length() < 6) {
            show(aPass, errPass, "Пароль не короче 6 символов");
            ok = false;
        } else if (!register && pass.isEmpty()) {
            show(aPass, errPass, "Введите пароль");
            ok = false;
        }
        if (register && ok && !pass.equals(pass2)) {
            show(aPass2, errPass2, "Пароли не совпадают");
            ok = false;
        }
        if (!ok) return;

        long wait = lockedUntil - SystemClock.elapsedRealtime();
        if (!register && wait > 0) {
            authError.setText("Слишком много попыток. Подождите " + ((wait + 999) / 1000) + " с.");
            authError.setVisibility(View.VISIBLE);
            return;
        }

        setBusy(true);
        Ui.hideKeyboard(this);
        AccountRepository accounts = AccountRepository.get(this);
        // Хеширование пароля занимает заметное время, поэтому выполняется не в главном потоке
        WORKER.execute(() -> {
            Account account = null;
            String error = null;
            try {
                if (register) account = accounts.register(name, email, pass);
                else {
                    account = accounts.login(email, pass);
                    if (account == null) error = "Неверная почта или пароль";
                }
            } catch (AccountRepository.AccountException e) {
                error = e.getMessage();
            }
            final Account result = account;
            final String message = error;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                setBusy(false);
                if (result != null) {
                    fails = 0;
                    prefs.setAccountId(result.id);
                    openMain();
                    return;
                }
                if (!register) {
                    fails++;
                    if (fails >= MAX_FAILS) {
                        fails = 0;
                        lockedUntil = SystemClock.elapsedRealtime() + LOCK_MS;
                    }
                }
                authError.setText(message);
                authError.setVisibility(View.VISIBLE);
            });
        });
    }

    private void setBusy(boolean b) {
        busy = b;
        submit.setAlpha(b ? 0.6f : 1f);
        submit.setText(b ? "Проверяем…" : (register ? "Создать аккаунт" : "Войти"));
    }
}
