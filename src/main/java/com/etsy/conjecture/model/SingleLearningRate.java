package com.etsy.conjecture.model;
import static com.google.common.base.Preconditions.checkArgument;
import java.io.Serializable;

public abstract class SingleLearningRate extends LearningRateComputation {

	protected double initialLearningRate = 0.1;

    public abstract double computeLearningRate(long epoch);

    /**
     *  Returns the feature and gradient independent learning rate
     */
    public double computeFeatureLearningRate(String feature, long t, double gradient) {
        return computeLearningRate(t);
    }

    public SingleLearningRate setInitialLearningRate(double rate) {
        checkArgument(rate > 0, "Initial learning rate must be greater than 0. Given: %s", rate);
        this.initialLearningRate = rate;
        return this;
    }

}