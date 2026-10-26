package com.example.quapp;

import org.junit.Test;
import static org.junit.Assert.*;

public class Phase4DraftTest {
    @Test public void requiresBothLocalLabelsWithoutInventingEligibility() {
        assertFalse(Phase4Draft.canReviewGroup(null, "Member"));
        assertFalse(Phase4Draft.canReviewGroup("Holder", null));
        assertFalse(Phase4Draft.canReviewGroup(" \n\t", "Member"));
        assertFalse(Phase4Draft.canReviewGroup("Holder", "\n\t"));
    }
    @Test public void acceptsUnicodeAndMultilineLabelsWithoutCountPolicy() {
        assertTrue(Phase4Draft.canReviewGroup("家族 🌿", "A\nB\nC\nD\nE\nF"));
        assertTrue(Phase4Draft.canReviewGroup("A", "B"));
    }
}
