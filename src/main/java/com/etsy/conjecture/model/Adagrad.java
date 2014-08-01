package com.etsy.conjecture.model;
import static com.google.common.base.Preconditions.checkArgument;
import com.etsy.conjecture.data.StringKeyedVector;

import java.io.Serializable;

/**
 *  AdaGrad provides a per-feature learning rate at each time step t.
 *  Described here: http://www.ark.cs.cmu.edu/cdyer/adagrad.pdf
 */
public class Adagrad extends LearningRateComputation {

    private StringKeyedVector gradients = new StringKeyedVector();
    private double n = 1d;

    public double computeFeatureLearningRate(String feature, long t, double gradient) {
        if (gradients.containsKey(feature)) {
            gradients.addToCoordinate(feature, gradient*gradient);
            return n/Math.sqrt(gradients.getCoordinate(feature));
        } else {
            gradients.addToCoordinate(feature, 1d+(gradient * gradient));
            return n/Math.sqrt(gradients.getCoordinate(feature));
        }
    }

    public Adagrad setInitialLearningRate(double rate) {
        checkArgument(rate > 0, "Initial learning rate must be greater than 0. Given: %s", rate);
        this.n = rate;
        return this;
    }
}