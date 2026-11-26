package com.freshmart.smartstock.llm;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/** Accent- and case-insensitive comparison of Vietnamese names ("Cà Chua" == "ca chua"). */
public final class VietnameseText {

    private static final Pattern COMBINING_MARKS = Pattern.compile("\\p{M}+");
    private static final Pattern SPACES = Pattern.compile("\\s+");

    private VietnameseText() {
    }

    /** Lower-case, accents removed (đ -> d), whitespace collapsed; null becomes "". */
    public static String fold(String text) {
        if (text == null) {
            return "";
        }
        // "đ" is a base letter, not a letter + combining mark, so NFD alone does not turn it into "d".
        String decomposed = Normalizer.normalize(text.replace('đ', 'd').replace('Đ', 'D'), Normalizer.Form.NFD);
        String plain = COMBINING_MARKS.matcher(decomposed).replaceAll("");
        return SPACES.matcher(plain.toLowerCase(Locale.ROOT)).replaceAll(" ").trim();
    }
}
