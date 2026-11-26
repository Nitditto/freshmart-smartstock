package com.freshmart.smartstock.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.freshmart.smartstock.common.api.LikePattern;
import org.junit.jupiter.api.Test;

class LikePatternTest {

    @Test
    void wildcardsAndTheEscapeCharacterAreEscaped() {
        assertEquals("RCQ!_001", LikePattern.escape("RCQ_001"));
        assertEquals("100!%", LikePattern.escape("100%"));
        assertEquals("a!!b", LikePattern.escape("a!b"));
        assertEquals("!%!_!!", LikePattern.escape("%_!"));
    }

    @Test
    void plainAndVietnameseTextIsUnchanged() {
        assertEquals("Cà chua bi", LikePattern.escape("Cà chua bi"));
        assertEquals("", LikePattern.escape(""));
    }
}
