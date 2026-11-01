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

/** Курсы валют Банка России, через рубль считается любая пара */
public final class ExchangeRates {
    public static final String SOURCE_URL = "https://www.cbr.ru/scripts/XML_daily.asp";
    private static final int TIMEOUT_MS = 7000;
    private static final int MAX_BYTES = 512 * 1024;

    private static final ExecutorService IO = Executors.newSingleThreadExecutor();

    /** Рублей за единицу валюты */
    private final Map<String, Double> rubPerUnit;
    /** Дата курса */
    public final String date;

    ExchangeRates(Map<String, Double> rubPerUnit, String date) {
        this.rubPerUnit = rubPerUnit;
        this.date = date;
    }

    /** Разбирает XML Банка России */
    public static ExchangeRates parse(InputStream in) throws IOException {
        try {
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            try {
                // DTD не нужен, запрещаем
                f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            } catch (Exception ignored) {
                // На Android параметра может не быть, это нормально
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

    /** «80,5307» -> 80.5307, при ошибке null */
    private static Double number(String s) {
        if (s == null) return null;
        try {
            double d = Double.parseDouble(s.replace(',', '.'));
            return Double.isFinite(d) ? d : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Скачивает курсы, вызывать не из главного потока */
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

    /** Загружает курсы в фоне, ответ приходит в главном потоке */
    public static void fetchAsync(Consumer<ExchangeRates> onOk, Runnable onFail) {
        Handler main = new Handler(Looper.getMainLooper());
        IO.execute(() -> {
            ExchangeRates r = null;
            try {
                r = fetch();
            } catch (IOException ignored) {
                // Нет сети или сервер недоступен
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

    /** Множитель из валюты from в to */
    public double factor(Currency from, Currency to) {
        return rubPerUnit.get(from.name()) / rubPerUnit.get(to.name());
    }

    /** Цена одной единицы of в валюте in */
    public double rate(Currency of, Currency in) {
        return factor(of, in);
    }

    /** Курс для показа: «1 € = 90,10 ₽» */
    public String describe(Currency of, Currency in) {
        double r = rate(of, in);
        String num = String.format(Locale.US, r >= 1 ? "%.2f" : "%.4f", r).replace('.', ',');
        return "1 " + of.symbol + " = " + num + " " + in.symbol;
    }
}
