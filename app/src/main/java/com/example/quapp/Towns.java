package com.example.quapp;

import java.util.HashMap;
import java.util.Map;

/**
 * Where each town on the Bohol list is (its town hall, roughly), for the venue pin of a new
 * queue. Create Queue has no map to drop a pin on, so the pin is the town's centre: close
 * enough for Directions to find the town and for the proximity check's 500 m to 5 km radius
 * around it (DECISIONS.md "A new queue's pin is its town's centre").
 */
public final class Towns {

    /** Tagbilaran City hall; also used for a town missing from the table. */
    private static final double[] DEFAULT = {9.6496, 123.8547};

    private static final Map<String, double[]> CENTRES = new HashMap<>();

    static {
        CENTRES.put("Tagbilaran City", DEFAULT);
        CENTRES.put("Baclayon", new double[]{9.6227, 123.9114});
        CENTRES.put("Corella", new double[]{9.6866, 123.9217});
        CENTRES.put("Dauis", new double[]{9.6253, 123.8656});
        CENTRES.put("Loon", new double[]{9.7998, 123.7931});
        CENTRES.put("Panglao", new double[]{9.5794, 123.7447});
        CENTRES.put("Maribojoc", new double[]{9.7418, 123.8438});
        CENTRES.put("Cortes", new double[]{9.7189, 123.8792});
        CENTRES.put("Sikatuna", new double[]{9.6906, 123.9728});
        CENTRES.put("Alburquerque", new double[]{9.6086, 123.9575});
    }

    private Towns() {
    }

    /** {latitude, longitude} of the town's centre. */
    public static double[] centre(String municipality) {
        double[] centre = CENTRES.get(municipality);
        return centre == null ? DEFAULT : centre;
    }
}
