package ru.eridian.justpay.ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Patterns;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ru.eridian.justpay.R;
import ru.eridian.justpay.data.Account;
import ru.eridian.justpay.data.AccountRepository;

/** Вход и регистрация, аккаунты хранятся только на телефоне */
public class AuthActivity extends BaseActivity {
    private static final int MAX_FAILS = 5;
    private static final long LOCK_MS = 30_000;

    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor();

    private boolean register = false;
    private boolean passwordShown = false;
    private boolean busy = false;
    private CountDownTimer lockTimer; // отсчёт до следующей попытки входа

    private View blockName, blockPass2, blockConsent;
    private CheckBox cbTerms, cbPrivacy, cbConsent;
    private EditText aName, aEmail, aPass, aPass2;
    private TextView errName, errEmail, errPass, errPass2, errConsent, authError, submit, title, subtitle;
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
        blockConsent = findViewById(R.id.block_consent);
        cbTerms = findViewById(R.id.cb_terms);
        cbPrivacy = findViewById(R.id.cb_privacy);
        cbConsent = findViewById(R.id.cb_consent);
        errConsent = findViewById(R.id.err_consent);
        setConsentText(cbTerms, "Принимаю ", "пользовательское соглашение", LegalActivity.TERMS);
        setConsentText(cbPrivacy, "Ознакомился с ", "политикой обработки персональных данных", LegalActivity.PRIVACY);
        setConsentText(cbConsent, "Даю ", "согласие на обработку персональных данных", LegalActivity.CONSENT);
        // Ошибка про отметки пропадает, как только пользователь что-то отметил
        CompoundButton.OnCheckedChangeListener hideError = (button, checked) -> errConsent.setVisibility(View.GONE);
        cbTerms.setOnCheckedChangeListener(hideError);
        cbPrivacy.setOnCheckedChangeListener(hideError);
        cbConsent.setOnCheckedChangeListener(hideError);

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
        // Кнопка «Готово» на клавиатуре
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
        blockConsent.setVisibility(reg ? View.VISIBLE : View.GONE);
        title.setText(reg ? "Создайте аккаунт" : "С возвращением");
        subtitle.setText(reg
                ? "Новый аккаунт пуст."
                : "Войдите в аккаунт.");
        submit.setText(reg ? "Создать аккаунт" : "Войти");
        aPass.setImeOptions(reg ? EditorInfo.IME_ACTION_NEXT : EditorInfo.IME_ACTION_DONE);
        clearErrors();
        startLockTimer();
    }

    // Пока вход закрыт, показываем сколько секунд осталось и не даём нажать кнопку
    private void startLockTimer() {
        stopLockTimer();
        long wait = prefs.lockedUntil() - System.currentTimeMillis();
        if (register || wait <= 0) return;
        submit.setEnabled(false);
        submit.setAlpha(0.6f);
        lockTimer = new CountDownTimer(wait, 1000) {
            @Override
            public void onTick(long left) {
                long seconds = (left + 999) / 1000;
                authError.setText("Много попыток. Следующая через " + seconds + " с");
                authError.setVisibility(View.VISIBLE);
                submit.setText("Войти через " + seconds + " с");
            }

            @Override
            public void onFinish() {
                stopLockTimer();
                authError.setVisibility(View.GONE);
                submit.setText("Войти");
            }
        }.start();
    }

    private void stopLockTimer() {
        if (lockTimer != null) {
            lockTimer.cancel();
            lockTimer = null;
        }
        submit.setEnabled(true);
        submit.setAlpha(1f);
    }

    @Override
    protected void onDestroy() {
        if (lockTimer != null) lockTimer.cancel();
        super.onDestroy();
    }

    // Название документа в тексте флажка это ссылка, остальной текст ставит или снимает отметку
    private void setConsentText(CheckBox box, String before, String link, int doc) {
        SpannableString text = new SpannableString(before + link);
        text.setSpan(new ClickableSpan() {
            @Override
            public void onClick(View v) {
                LegalActivity.open(AuthActivity.this, doc);
            }

            @Override
            public void updateDrawState(TextPaint paint) {
                paint.setColor(Ui.attr(AuthActivity.this, R.attr.jpAct));
                paint.setUnderlineText(true);
            }
        }, before.length(), text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        box.setText(text);
        box.setMovementMethod(LinkMovementMethod.getInstance());
        box.setHighlightColor(Color.TRANSPARENT);
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
        errConsent.setVisibility(View.GONE);
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
        if (register && !(cbTerms.isChecked() && cbPrivacy.isChecked() && cbConsent.isChecked())) {
            errConsent.setText("Отметьте все три пункта, чтобы создать аккаунт.");
            errConsent.setVisibility(View.VISIBLE);
            ok = false;
        }
        if (!ok) return;

        long wait = prefs.lockedUntil() - System.currentTimeMillis();
        if (!register && wait > 0) {
            authError.setText("Много попыток. Подождите " + ((wait + 999) / 1000) + " с.");
            authError.setVisibility(View.VISIBLE);
            return;
        }

        setBusy(true);
        Ui.hideKeyboard(this);
        AccountRepository accounts = AccountRepository.get(this);
        // Хеширование долгое, делаем не в главном потоке
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
                    prefs.setLoginFails(0);
                    prefs.setAccountId(result.id);
                    openMain();
                    return;
                }
                if (!register) {
                    // Ошибки считаем в настройках, чтобы перезапуск приложения не снимал блокировку
                    int fails = prefs.loginFails() + 1;
                    if (fails >= MAX_FAILS) {
                        prefs.setLoginFails(0);
                        prefs.setLockedUntil(System.currentTimeMillis() + LOCK_MS);
                    } else {
                        prefs.setLoginFails(fails);
                    }
                }
                authError.setText(message);
                authError.setVisibility(View.VISIBLE);
                startLockTimer();
            });
        });
    }

    private void setBusy(boolean b) {
        busy = b;
        submit.setAlpha(b ? 0.6f : 1f);
        submit.setText(b ? "Проверяем…" : (register ? "Создать аккаунт" : "Войти"));
    }
}
