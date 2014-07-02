package com.etsy.conjecture.model;

import com.etsy.conjecture.data.LazyVector;
import com.etsy.conjecture.Utilities;
import static com.google.common.base.Preconditions.checkArgument;
import com.etsy.conjecture.data.Label;
import com.etsy.conjecture.data.LabeledInstance;
import com.etsy.conjecture.data.StringKeyedVector;
import java.util.Collection;

/**
 *  Builds the weight updates as a function
 *  of learning rate and regularization schedule for SGD learning.
 *
 *  Default learning rate and regularization are:
 *  LR: Exponentially decreasing
 *  REG: Lazily applied L1 and L2 regularization
 *  Subclasses overwrite LR and REG functions as necessary
 */
public abstract class SGDOptimizer<L extends Label> implements LazyVector.UpdateFunction {

    private static final long serialVersionUID = 9153480933266800474L;
    double laplace = 0.0;
    double gaussian = 0.0;
    double initialLearningRate = 0.01;
    UpdateableLinearModel model;

    double examplesPerEpoch = 10000;
    boolean useExponentialLearningRate = false;
    double exponentialLearningRateBase = 0.99;

    public SGDOptimizer() {}

    public SGDOptimizer(double g, double l) {
        gaussian = g;
        laplace = l;
    }

    /**
     *  Do minibatch gradient descent
     */
    public StringKeyedVector getUpdates(Collection<LabeledInstance<L>> minibatch) {
        StringKeyedVector updateVec = new StringKeyedVector();
        for (LabeledInstance instance : minibatch) {
            updateVec.add(getUpdate(instance)); // accumulate gradient
            model.truncate(instance);
            model.epoch++;
        }
        updateVec.mul(1/minibatch.size()); // do a single update, scaling weights by the
                                           // average gradient over the minibatch
        return updateVec;
    }

    /**
     *  Get the update to the param vector using a chosen
     *  learning rate / regularization schedule.
     *  Returns a StringKeyedVector of updates for each 
     *  parameter.
     */
    public abstract StringKeyedVector getUpdate(LabeledInstance<L> instance);

    /**
     *  Implements lazy updating of regularization when the regularization
     *  updates aren't sparse (e.g. elastic net l1 and l2, adagrad l1).
     *  
     *  When regularization can be done on just the non-zero elements of
     *  the sample instance (e.g. FTRL proximal, HandsFree), the lazyUpdate
     *  function does nothing (i.e. just returns the unscaled param).
     */
    public double lazyUpdate(String feature, double param, long start, long end) {
        if (Utilities.floatingPointEquals(laplace, 0.0d)
            && Utilities.floatingPointEquals(gaussian, 0.0d)) {
            return param;
        }
        for (long iter = start + 1; iter <= end; iter++) {
            if (Utilities.floatingPointEquals(param, 0.0d)) {
                return 0.0d;
            }
            double eta = getDecreasingLearningRate(iter);
            /**
             * TODO: patch so that param cannot cross 0.0 during gaussian update
             */
            param -= eta * gaussian * param;
            if (param > 0.0) {
                param = Math.max(0.0, param - eta * laplace);
            } else {
                param = Math.min(0.0, param + eta * laplace);
            }
        }
        return param;
    }

    /**
     *  Computes a linearly or exponentially decreasing
     *  learning rate as a function of the current epoch.
     *  Even when we have per feature learning rates, it's 
     *  necessary to keep track of a decreasing learning rate 
     *  for things like truncation.
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

    public SGDOptimizer setGaussianRegularizationWeight(double gaussian) {
        checkArgument(gaussian > 0,
                "gaussian regularization weight must be positive, given: %f",
                gaussian);
        this.gaussian = gaussian;
        return this;
    }

    public SGDOptimizer setLaplaceRegularizationWeight(double laplace) {
        checkArgument(laplace > 0,
                "laplace regularization weight must be positive, given: %f",
                laplace);
        this.laplace = laplace;
        return this;
    }
}
