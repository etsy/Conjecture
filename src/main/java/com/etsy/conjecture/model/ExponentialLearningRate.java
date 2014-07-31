package com.etsy.conjecture.model;
import static com.google.common.base.Preconditions.checkArgument;

import java.io.Serializable;

public class ExponentialLearningRate extends LearningRateComputation {

    double examplesPerEpoch = 10000;
    boolean useExponentialLearningRate = false;
    double exponentialLearningRateBase = 0.99;
    double initialLearningRate = 0.1;

    public double computeFeatureLearningRate(String feature, long t, double gradient) {
        return computeLearningRate(t);
    }

    public double computeLearningRate(long t){
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

    public ExponentialLearningRate setExamplesPerEpoch(double examples) {
        checkArgument(examples > 0,
                "examples per epoch must be positive, given %f", examples);
        this.examplesPerEpoch = examples;
        return this;
    }

    public ExponentialLearningRate setUseExponentialLearningRate(boolean useExponentialLearningRate) {
        this.useExponentialLearningRate = useExponentialLearningRate;
        return this;
    }

    public ExponentialLearningRate setExponentialLearningRateBase(double base) {
        checkArgument(base > 0,
                "exponential learning rate base must be positive, given: %f",
                base);
        checkArgument(
                base <= 1.0,
                "exponential learning rate base must be at most 1.0, given: %f",
                base);
        this.exponentialLearningRateBase = base;
        return this;
    }

    public ExponentialLearningRate setInitialLearningRate(double rate) {
        checkArgument(rate > 0, "Initial learning rate must be greater than 0. Given: %s", rate);
        this.initialLearningRate = rate;
        return this;
    }

    protected String getLearningRateSchedule() {
        return "exponential";
    }

}