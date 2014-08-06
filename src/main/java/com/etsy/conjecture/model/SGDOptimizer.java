package com.etsy.conjecture.model;

import com.etsy.conjecture.data.LazyVector;
import com.etsy.conjecture.Utilities;
import static com.google.common.base.Preconditions.checkArgument;

/**
 *  Builds the weight update as a function
 *  of learning rate and regularization for SGD learning.
 */
public abstract class SGDOptimizer implements LazyVector.UpdateFunction {

    private static final long serialVersionUID = 9153480933266800474L;
    double laplace = 0.0;
    double gaussian = 0.0;
    double initialLearningRate = 0.01;

    double examplesPerEpoch = 10000;
    boolean useExponentialLearningRate = false;
    double exponentialLearningRateBase = 0.99;

    public SGDOptimizer() {}

    public SGDOptimizer(double g, double l) {
        gaussian = g;
        laplace = l;
    }

    /**
     *  Get this feature's weight update according to the current
     *  epoch gradient.
     */
    public abstract double getUpdate(String feature, long epoch, double gradient);

    /**
     *  Implements lazy updating of regularization when the regularization
     *  updates aren't sparse (e.g. elastic net, adagrad l1)
     */
    public abstract double lazyUpdate(String feature, double param, long start, long end);

    /**
     *  Computes a linearly or exponentially decreasing
     *  learning rate as a function of the current epoch.
     *  Even when we have things like per feature learning
     *  rates, it's necessary to keep track of a decreasing
     *  learning rate for things like truncation.
     */
    public double getDecreasingLearningRate(long t){
        double epoch_fudged = Math.max(1.0, (t + 1) / examplesPerEpoch);
        if (useExponentialLearningRate) {
            return Math.max(
                0d,
                this.initialLearningRate
                * Math.pow(this.exponentialLearningRateBase,
                           epoch_fudged));
        } else {
            return Math.max(0d, this.initialLearningRate / epoch_fudged);
        }
    }

    public SGDOptimizer setInitialLearningRate(double rate) {
        checkArgument(rate > 0, "Initial learning rate must be greater than 0. Given: %s", rate);
        this.initialLearningRate = rate;
        return this;
    }

    public SGDOptimizer setExamplesPerEpoch(double examples) {
        checkArgument(examples > 0,
                "examples per epoch must be positive, given %f", examples);
        this.examplesPerEpoch = examples;
        return this;
    }

    public SGDOptimizer setUseExponentialLearningRate(boolean useExponentialLearningRate) {
        this.useExponentialLearningRate = useExponentialLearningRate;
        return this;
    }

    public SGDOptimizer setExponentialLearningRateBase(double base) {
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
}
