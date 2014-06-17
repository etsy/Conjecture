package com.etsy.conjecture.evaluation;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TestReceiverOperatingCharacteristic {

    static double[] labels = { 1, 0, 1, 0, 0, 0, 1, 1, 0, 1, 0, 0, 0, 1, 1, 0,
            0, 0 };
    static double[] predictions = { 0.80962, 0.48458, 0.65812, 0.16117,
            0.47375, 0.26587, 0.71517, 0.63866, 0.36296, 0.89639, 0.35936,
            0.22413, 0.36402, 0.41459, 0.83148, 0.23271, 0.23271, 0.23271 };

    // from scikit learn
    static double AUC = 0.97402597402597402;

    @Test
    public void testAUC() {
        ReceiverOperatingCharacteristic roc = new ReceiverOperatingCharacteristic();
        for (int i = 0; i < labels.length; i++) {
            roc.add(labels[i], predictions[i]);
        }

        assertEquals(AUC, roc.binaryAUC(), 0.0000001);
    }

}