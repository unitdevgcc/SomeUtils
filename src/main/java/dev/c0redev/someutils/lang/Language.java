package dev.c0redev.someutils.lang;

import java.util.Locale;

public enum Language {
    AUTO, RU, EN;

    public static Language from(String value) {
        try {
            return value == null ? AUTO : valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return AUTO;
        }
    }
}
