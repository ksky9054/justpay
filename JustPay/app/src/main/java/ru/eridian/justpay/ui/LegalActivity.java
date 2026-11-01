package ru.eridian.justpay.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.graphics.Typeface;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import ru.eridian.justpay.R;

/** Текст документа: соглашение, политика или согласие */
public class LegalActivity extends BaseActivity {
    public static final String EXTRA_DOC = "doc";
    public static final int TERMS = 0;
    public static final int PRIVACY = 1;
    public static final int CONSENT = 2;

    private static final String[] TITLES = {
            "Пользовательское соглашение",
            "Политика обработки персональных данных",
            "Согласие на обработку персональных данных"};
    private static final int[] FILES = {R.raw.doc_terms, R.raw.doc_privacy, R.raw.doc_consent};

    /** Открывает документ с анимацией справа */
    public static void open(BaseActivity from, int doc) {
        from.startActivity(new Intent(from, LegalActivity.class).putExtra(EXTRA_DOC, doc));
        from.overridePendingTransition(R.anim.slide_in_right, R.anim.hold);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_legal);
        Ui.fitSystemBars(findViewById(R.id.root));
        interceptBack();

        int doc = getIntent().getIntExtra(EXTRA_DOC, TERMS);
        if (doc < 0 || doc >= TITLES.length) doc = TERMS;

        ((TextView) findViewById(R.id.legal_title)).setText(TITLES[doc]);
        ((TextView) findViewById(R.id.legal_text)).setText(readDocument(FILES[doc]));
        findViewById(R.id.btn_back).setOnClickListener(v -> finishToRight());
    }

    @Override
    protected boolean onBack() {
        finishToRight();
        return true;
    }

    // Строки, которые начинаются с «## », становятся жирными заголовками разделов
    private CharSequence readDocument(int resId) {
        SpannableStringBuilder text = new SpannableStringBuilder();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(
                getResources().openRawResource(resId), StandardCharsets.UTF_8))) {
            String line;
            while ((line = in.readLine()) != null) {
                int start = text.length();
                if (line.startsWith("## ")) {
                    text.append(line.substring(3)).append('\n');
                    text.setSpan(new StyleSpan(Typeface.BOLD), start, text.length() - 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    text.setSpan(new RelativeSizeSpan(1.15f), start, text.length() - 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                } else {
                    text.append(line).append('\n');
                }
            }
        } catch (IOException e) {
            text.append("Не удалось открыть документ.");
        }
        return text;
    }
}
