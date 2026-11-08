package ru.eridian.justpay.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.util.Arrays;

/** Проверка подбора значка по названию подписки */
public class BrandCatalogTest {
    // Маленькая база: значок, название, цвет, другие названия
    private final BrandCatalog catalog = new BrandCatalog(Arrays.asList(
            "netflix\tNetflix\tE50914\t",
            "kinopoisk\tKinopoisk\tFF5500\tКиноПоиск|Кинопоиск HD",
            "spotify\tSpotify\t1ED760\tСпотифай",
            "applemusic\tApple Music\tFA243C\t",
            "apple\tApple\t000000\tЭппл",
            "vk\tVK\t0077FF\tВК",
            "box\tBox\t0061D5\t"));

    private String slug(String name) {
        BrandCatalog.Brand brand = catalog.find(name);
        return brand == null ? null : brand.slug;
    }

    @Test
    public void englishName_matches() {
        assertEquals("netflix", slug("Netflix"));
    }

    @Test
    public void russianName_matches() {
        assertEquals("kinopoisk", slug("Кинопоиск"));
    }

    @Test
    public void extraWords_areIgnored() {
        assertEquals("netflix", slug("Netflix Premium 4K"));
    }

    @Test
    public void longestNameWins() {
        assertEquals("applemusic", slug("Apple Music семейная"));
        assertEquals("apple", slug("Apple iCloud"));
    }

    @Test
    public void wrongKeyboardLayout_isFixed() {
        // «кинопоиск» набрали в английской раскладке
        assertEquals("kinopoisk", slug("rbyjgjbcr"));
        // «spotify» набрали в русской раскладке
        assertEquals("spotify", slug("ызщеашн"));
    }

    @Test
    public void russianSpellingOfEnglishName_matchesBySound() {
        assertEquals("netflix", slug("нетфликс"));
    }

    @Test
    public void shortBrandWordDoesNotMatchInsideOtherName() {
        assertNull(slug("Lunch box"));
        assertNotNull(slug("Box"));
    }

    @Test
    public void shortBrandVk_matchesInsideName() {
        assertEquals("vk", slug("VK Музыка"));
    }

    @Test
    public void unknownName_givesNothing() {
        assertNull(slug("Моя секция плавания"));
        assertNull(slug(""));
        assertNull(slug(null));
    }
}
