package com.ajayaventure.filter;

import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsrfFilterTest {

    private static final Pattern TOKEN = Pattern.compile("^[A-Za-z0-9_-]{43}$");

    @Test
    void cookieNameAndHeaderConventions() {
        assertEquals("X-XSRF-Token", CsrfFilter.HEADER_NAME);
        assertEquals("AV-XSRF", CsrfFilter.COOKIE_NAME);
    }

    @Test
    void tokenShapeIsUrlSafeBase64WithoutPadding() {
        // Token generation uses Base64 URL alphabet without padding (43 chars for 32 bytes).
        String sample = "m2BXnB4ICzysV8_K9cGkf7QPCrYgmAbFKqWr9r9lGVs";
        assertTrue(TOKEN.matcher(sample).matches());
    }
}