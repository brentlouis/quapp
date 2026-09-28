package com.example.quapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RollingAverageTest {

    @Test
    public void emptyAverageIsZero() {
        RollingAverage average = new RollingAverage(3);
        assertTrue(average.isEmpty());
        assertEquals(0, average.average(), 0.0001);
    }

    @Test
    public void averagesSamplesWhileUnderWindow() {
        RollingAverage average = new RollingAverage(3);
        average.add(2);
        average.add(4);
        assertEquals(2, average.size());
        assertEquals(3, average.average(), 0.0001);
    }

    @Test
    public void oldestSampleDropsOutWhenWindowIsFull() {
        RollingAverage average = new RollingAverage(3);
        average.add(100); // should fall out
        average.add(1);
        average.add(2);
        average.add(3);
        assertEquals(3, average.size());
        assertEquals(2, average.average(), 0.0001);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsWindowSmallerThanOne() {
        new RollingAverage(0);
    }
}
