package com.example.quapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ValidationTest {

    @Test
    public void acceptsPhilippineMobileNumbers() {
        assertTrue(Validation.isValidPhone("09171234567"));
        assertTrue(Validation.isValidPhone("0917 123 4567"));
        assertTrue(Validation.isValidPhone("0917-123-4567"));
    }

    @Test
    public void rejectsMalformedPhones() {
        assertFalse(Validation.isValidPhone(""));
        assertFalse(Validation.isValidPhone(null));
        assertFalse(Validation.isValidPhone("0917123456"));   // 10 digits
        assertFalse(Validation.isValidPhone("19171234567"));  // doesn't start with 09
        assertFalse(Validation.isValidPhone("0917abc4567"));
    }

    @Test
    public void normalizeStripsSpacesAndDashes() {
        assertEquals("09171234567", Validation.normalizePhone(" 0917-123 4567 "));
    }

    @Test
    public void passwordNeedsMinimumLength() {
        assertFalse(Validation.isValidPassword("12345"));
        assertTrue(Validation.isValidPassword("123456"));
        assertFalse(Validation.isValidPassword(null));
    }

    @Test
    public void blankMeansEmptyOrWhitespace() {
        assertTrue(Validation.isBlank(null));
        assertTrue(Validation.isBlank("   "));
        assertFalse(Validation.isBlank(" a "));
    }
}
