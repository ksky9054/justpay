package ru.eridian.justpay.data;

import android.content.Context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import ru.eridian.justpay.R;

/** База брендов: по названию подписки находит значок сервиса */
public final class BrandCatalog {

    /** Один бренд: имя значка, название и фирменный цвет */
    public static final class Brand {
        public final String slug;
        public final String title;
        public final int color;
        /** Номер ресурса значка, -1 пока не искали */
        public int resId = -1;

        Brand(String slug, String title, int color) {
            this.slug = slug;
            this.title = title;
            this.color = color;
        }
    }

    // Название из нескольких слов: слова и бренд, к которому они ведут
    private static final class Key {
        final String[] words;
        final int length;
        final Brand brand;

        Key(String[] words, int length, Brand brand) {
            this.words = words;
            this.length = length;
            this.brand = brand;
        }
    }

    // Раскладки клавиатуры: буква на английской и на той же клавише в русской
    private static final String EN_KEYS = "qwertyuiop[]asdfghjkl;'zxcvbnm,.";
    private static final String RU_KEYS = "йцукенгшщзхъфывапролджэячсмитьбю";

    // Транслит по буквам алфавита от «а» до «я»
    private static final String[] TRANSLIT = {
            "a", "b", "v", "g", "d", "e", "zh", "z", "i", "i", "k", "l", "m", "n", "o", "p",
            "r", "s", "t", "u", "f", "h", "ts", "ch", "sh", "sh", "", "y", "", "e", "yu", "ya"};

    private static BrandCatalog instance;

    private final Map<String, Brand> exact = new HashMap<>();
    private final Map<String, List<Key>> byFirstWord = new HashMap<>();
    private final Map<String, Brand> bySkeleton = new HashMap<>();
    private final Set<String> ambiguous = new HashSet<>();

    /** База из файла res/raw/brands.tsv, читается один раз */
    public static synchronized BrandCatalog get(Context context) {
        if (instance == null) {
            List<String> lines = new ArrayList<>();
            try (InputStream in = context.getResources().openRawResource(R.raw.brands);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) lines.add(line);
            } catch (IOException e) {
                // без базы значки просто не показываются
            }
            instance = new BrandCatalog(lines);
        }
        return instance;
    }

    /** Строки вида: значок, название, цвет, другие названия через | */
    public BrandCatalog(List<String> lines) {
        List<Brand> brands = new ArrayList<>();
        List<String[]> rows = new ArrayList<>();
        for (String line : lines) {
            String[] p = line.split("\t", -1);
            if (p.length < 4) continue;
            Brand brand = new Brand(p[0], p[1], 0xFF000000 | Integer.parseInt(p[2], 16));
            brands.add(brand);
            rows.add(p);
        }
        // Сначала главные названия, потом остальные, чтобы при совпадении выигрывало главное
        for (int i = 0; i < brands.size(); i++) addName(brands.get(i).title, brands.get(i));
        for (int i = 0; i < brands.size(); i++) {
            String aliases = rows.get(i)[3];
            if (aliases.isEmpty()) continue;
            for (String alias : aliases.split("\\|")) addName(alias, brands.get(i));
        }
    }

    private void addName(String name, Brand brand) {
        String clean = clean(name);
        String glued = clean.replace(" ", "");
        if (glued.length() < 2) return;
        if (!exact.containsKey(glued)) exact.put(glued, brand);

        // Искать название внутри длинной строки можно только у длинных названий
        // (исключение «VK»), иначе слово «box» находилось бы в любом «Lunch box»
        if (glued.length() >= 4 || glued.equals("vk") || glued.equals("вк")) {
            String[] words = clean.split(" ");
            byFirstWord.computeIfAbsent(words[0], k -> new ArrayList<>()).add(new Key(words, glued.length(), brand));
        }

        String skeleton = skeleton(glued);
        if (skeleton.length() >= 4) {
            Brand old = bySkeleton.get(skeleton);
            if (old == null) bySkeleton.put(skeleton, brand);
            else if (old != brand) ambiguous.add(skeleton);
        }
    }

    /** Бренд по названию подписки или null, если подходящего нет */
    public Brand find(String name) {
        if (name == null) return null;
        Brand found = lookup(clean(name));
        if (found != null) return found;
        // Название могли набрать не в той раскладке: «кштщзщшиц» вместо «kinopoisk»
        String swapped = clean(swapLayout(name));
        return swapped.isEmpty() ? null : lookup(swapped);
    }

    private Brand lookup(String text) {
        if (text.isEmpty()) return null;

        Brand brand = exact.get(text.replace(" ", ""));
        if (brand != null) return brand;

        // Название внутри строки: «Netflix Premium», берём самое длинное совпадение
        String[] words = text.split(" ");
        Key best = null;
        for (int i = 0; i < words.length; i++) {
            List<Key> keys = byFirstWord.get(words[i]);
            if (keys == null) continue;
            for (Key key : keys) {
                if (matchesAt(words, i, key.words) && (best == null || key.length > best.length)) best = key;
            }
        }
        if (best != null) return best.brand;

        // Русское написание латинского названия: «нетфликс» и «netflix» похожи по согласным
        String skeleton = skeleton(text.replace(" ", ""));
        if (skeleton.length() >= 4 && !ambiguous.contains(skeleton)) return bySkeleton.get(skeleton);
        return null;
    }

    private static boolean matchesAt(String[] words, int start, String[] key) {
        if (start + key.length > words.length) return false;
        for (int i = 0; i < key.length; i++) {
            if (!words[start + i].equals(key[i])) return false;
        }
        return true;
    }

    /** Строчные буквы и цифры, остальное превращается в пробел */
    static String clean(String name) {
        String s = name.toLowerCase(Locale.ROOT).replace('ё', 'е');
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            out.append(Character.isLetterOrDigit(c) ? c : ' ');
        }
        return out.toString().trim().replaceAll(" +", " ");
    }

    /** Меняет буквы русской раскладки на английские и наоборот */
    static String swapLayout(String name) {
        String s = name.toLowerCase(Locale.ROOT);
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            int en = EN_KEYS.indexOf(c);
            int ru = RU_KEYS.indexOf(c);
            if (en >= 0) out.append(RU_KEYS.charAt(en));
            else if (ru >= 0) out.append(EN_KEYS.charAt(ru));
            else out.append(c);
        }
        return out.toString();
    }

    /** Только согласные латиницей: «нетфликс» и «netflix» дают одинаковое «ntflks» */
    static String skeleton(String text) {
        StringBuilder latin = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= 'а' && c <= 'я') latin.append(TRANSLIT[c - 'а']);
            else latin.append(c);
        }
        String s = latin.toString().replace("ph", "f").replace("x", "ks")
                .replace('c', 'k').replace('q', 'k').replace('w', 'v');
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ("aeiouyh".indexOf(c) >= 0) continue;
            if (out.length() > 0 && out.charAt(out.length() - 1) == c) continue;
            out.append(c);
        }
        return out.toString();
    }
}
