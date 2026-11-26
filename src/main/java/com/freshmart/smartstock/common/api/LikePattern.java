package com.freshmart.smartstock.common.api;

/** Makes user text safe to put inside a SQL LIKE pattern, so % and _ are matched literally. */
public final class LikePattern {

    /** The escape character declared as {@code escape '!'} in the queries (a backslash breaks MySQL literals). */
    public static final char ESCAPE = '!';

    private LikePattern() {
    }

    public static String escape(String text) {
        StringBuilder escaped = new StringBuilder(text.length() + 4);
        for (char c : text.toCharArray()) {
            if (c == ESCAPE || c == '%' || c == '_') {
                escaped.append(ESCAPE);
            }
            escaped.append(c);
        }
        return escaped.toString();
    }
}
