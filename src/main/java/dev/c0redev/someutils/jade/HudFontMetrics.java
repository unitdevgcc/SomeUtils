package dev.c0redev.someutils.jade;


final class HudFontMetrics {

    private HudFontMetrics() {
    }

    static int estimateWidth(String text) {
        int width = 0;
        for (int i = 0; i < text.length(); i++) {
            width += advance(text.charAt(i));
        }
        return width;
    }

    private static int advance(char c) {
        return switch (c) {
        case ' ' -> 4;
        case 'i', 'l', '.', ',', ':', ';', '!', '\'', '|', 'I' -> 3;
        case 't', 'f', 'j', '(', ')', '[', ']' -> 4;
        case 'r' -> 5;
        case 'm', 'w', 'M', 'W', '@', '#', '%' -> 7;
        case 'Й', 'г', 'к' -> 5;
        case 'Д', 'Ц', 'Ъ', 'щ', 'ъ', 'ы' -> 7;
        case 'Ж', 'Ф', 'Ш', 'Ы', 'Ю', 'ю' -> 8;
        case 'Щ', 'й', 'Ё', 'ё' -> 9;
        default -> 6;
        };
    }
}
