package com.example.quapp;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class QueueLinkTest {

    @Test
    public void slugIsLowercaseWithDashes() {
        assertEquals("free-medical-mission", QueueLink.slug("Free Medical Mission"));
    }

    @Test
    public void slugDropsAccentsAndPunctuation() {
        assertEquals("pina-fair-2026", QueueLink.slug("Piña Fair, 2026!"));
        assertEquals("barangay-clearance", QueueLink.slug("  Barangay -- Clearance  "));
    }
}
