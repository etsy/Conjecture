package com.etsy.conjecture.model;

import java.io.Serializable;

class LearningRateComputation implements Serializable {

    private static final long serialVersionUID = 6542742510494701712L;

    double examplesPerEpoch = 10000;
    boolean useExponentialLearningRate = false;
    double exponentialLearningRateBase = 0.99;
    double initialLearningRate = 0.1;

    public double computeLearningRate(long t) {
        double epoch_fudged = Math.max(1.0, (t + 1) / examplesPerEpoch);
        if (useExponentialLearningRate) {
            return Math.max(
                0d,
                initialLearningRate
                * Math.pow(exponentialLearningRateBase,
                           epoch_fudged));
        } else {
            return Math.max(0d, initialLearningRate / epoch_fudged);
        }
    }

    public LearningRateComputation setExamplesPerEpoch(double examplesPerEpoch) {
        this.examplesPerEpoch = examplesPerEpoch;
        return this;
    }

    public LearningRateComputation setUseExponentialLearningRate(boolean useExponentialLearningRate) {
        this.useExponentialLearningRate = useExponentialLearningRate;
        return this;
    }

    public LearningRateComputation setExponentialLearningRateBase(double exponentialLearningRateBase) {
        this.exponentialLearningRateBase = exponentialLearningRateBase;
        return this;
    }

    public LearningRateComputation setInitialLearningRate(double initialLearningRate) {
        this.initialLearningRate = initialLearningRate;
        return this;
    }
}
