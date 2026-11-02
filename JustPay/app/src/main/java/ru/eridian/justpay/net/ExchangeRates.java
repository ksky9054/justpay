package ru.eridian.justpay.net;

import android.os.Handler;
import android.os.Looper;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import ru.eridian.justpay.data.Currency;

/**
 * Курсы валют Банка России (https://www.cbr.ru/scripts/XML_daily.asp). Один ответ содержит курс
 * каждой валюты к рублю, поэтому через рубль можно пересчитать любую пару валют приложения.
 * Запрос выполняется только когда пользователь меняет валюту и в аккаунте есть подписки;
 * серверу не передаётся ничего, кроме самого запроса.
 */
public final class ExchangeRates {
    public static final String SOURCE_URL = "https://www.cbr.ru/scripts/XML_daily.asp";
    private static final int TIMEOUT_MS = 7000;
    private static final int MAX_BYTES = 512 * 1024;

    private static final ExecutorService IO = Executors.newSingleThreadExecutor();

    /** Сколько рублей стоит одна единица валюты. */
    private final Map<String, Double> rubPerUnit;
    /** Дата курса в формате ЦБ, например «28.10.2026». */
    public final String date;

    ExchangeRates(Map<String, Double> rubPerUnit, String date) {
        this.rubPerUnit = rubPerUnit;
        this.date = date;
    }

    /** Разбирает XML ЦБ РФ. Кодировка (windows-1251) берётся из заголовка документа. */
    public static ExchangeRates parse(InputStream in) throws IOException {
        try {
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            try {
                // Внешние сущности не нужны: запрещаем DTD, где платформа это позволяет
                f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            } catch (Exception ignored) {
                // На Android этот параметр может не поддерживаться, внешние сущности там и так не загружаются
            }
            DocumentBuilder b = f.newDocumentBuilder();
            Document doc = b.parse(in);
            Element root = doc.getDocumentElement();
            String date = root.getAttribute("Date");

            Map<String, Double> map = new HashMap<>();
            map.put(Currency.RUB.name(), 1.0);
            NodeList list = doc.getElementsByTagName("Valute");
            for (int i = 0; i < list.getLength(); i++) {
                Element v = (Element) list.item(i);
                String code = text(v, "CharCode");
                if (code == null) continue;
                Double value = number(text(v, "Value"));
                Double nominal = number(text(v, "Nominal"));
                if (value == null || nominal == null || value <= 0 || nominal <= 0) continue;
                map.put(code, value / nominal);
            }
            return new ExchangeRates(map, date);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Не удалось разобрать ответ с курсами", e);
        }
    }

    private static String text(Element parent, String tag) {
        NodeList n = parent.getElementsByTagName(tag);
        return n.getLength() == 0 ? null : n.item(0).getTextContent().trim();
    }

    /** «80,5307» -> 80.5307; null, если число некорректно. */
    private static Double number(String s) {
        if (s == null) return null;
        try {
            double d = Double.parseDouble(s.replace(',', '.'));
            return Double.isFinite(d) ? d : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Скачивает и разбирает курсы. Вызывать не из главного потока. */
    public static ExchangeRates fetch() throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(SOURCE_URL).openConnection();
        c.setConnectTimeout(TIMEOUT_MS);
        c.setReadTimeout(TIMEOUT_MS);
        c.setInstanceFollowRedirects(false);
        c.setRequestProperty("User-Agent", "JustPay/1.0");
        try {
            if (c.getResponseCode() != 200) throw new IOException("Сервер ответил кодом " + c.getResponseCode());
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            try (InputStream in = c.getInputStream()) {
                int n;
                while ((n = in.read(chunk)) > 0) {
                    buf.write(chunk, 0, n);
                    if (buf.size() > MAX_BYTES) throw new IOException("Слишком большой ответ");
                }
            }
            return parse(new ByteArrayInputStream(buf.toByteArray()));
        } finally {
            c.disconnect();
        }
    }

    /** Загружает курсы в фоне; результат приходит в главном потоке. */
    public static void fetchAsync(Consumer<ExchangeRates> onOk, Runnable onFail) {
        Handler main = new Handler(Looper.getMainLooper());
        IO.execute(() -> {
            ExchangeRates r = null;
            try {
                r = fetch();
            } catch (IOException ignored) {
                // Нет сети, таймаут или сервер недоступен: для пользователя это одно и то же
            }
            final ExchangeRates result = r;
            main.post(() -> {
                if (result == null) onFail.run();
                else onOk.accept(result);
            });
        });
    }

    public boolean supports(Currency a, Currency b) {
        return rubPerUnit.containsKey(a.name()) && rubPerUnit.containsKey(b.name());
    }

    /** Во сколько раз умножить сумму в валюте from, чтобы получить сумму в валюте to. */
    public double factor(Currency from, Currency to) {
        return rubPerUnit.get(from.name()) / rubPerUnit.get(to.name());
    }

    /** Сколько стоит одна единица валюты of, выраженная в валюте in. */
    public double rate(Currency of, Currency in) {
        return factor(of, in);
    }

    /** Курс для показа пользователю: «1 € = 90,10 ₽» (для мелких курсов больше знаков). */
    public String describe(Currency of, Currency in) {
        double r = rate(of, in);
        String num = String.format(Locale.US, r >= 1 ? "%.2f" : "%.4f", r).replace('.', ',');
        return "1 " + of.symbol + " = " + num + " " + in.symbol;
    }
}
