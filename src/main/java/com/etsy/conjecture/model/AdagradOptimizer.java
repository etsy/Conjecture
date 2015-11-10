package com.etsy.conjecture.model;

import com.etsy.conjecture.*;
import com.etsy.conjecture.data.*;

import java.util.*;

/**
 *  AdaGrad provides adaptive per-feature learning rates at each time step t.
 *  Described here: http://www.ark.cs.cmu.edu/cdyer/adagrad.pdf
 */
public class AdagradOptimizer<L extends Label> extends SGDOptimizer<L> {

    private StringKeyedVector unnormalizedGradients = new StringKeyedVector();
    private StringKeyedVector summedGradients = new StringKeyedVector();

    @Override
    public StringKeyedVector getUpdate(LabeledInstance instance) {
        StringKeyedVector gradients = model.getGradients(instance);
        StringKeyedVector updateVec = new StringKeyedVector();
        Iterator<Map.Entry<String, Double>> it = gradients.iterator();
        while (it.hasNext()) {
            Map.Entry<String,Double> pairs = (Map.Entry)it.next();
            String feature = pairs.getKey();
            double gradient = pairs.getValue();
            double featureLearningRate = updateAndGetFeatureLearningRate(feature, gradient);
            updateVec.setCoordinate(feature, gradient * -featureLearningRate);
       }
       return updateVec;
    }

    /**
     *  Update adaptive feature specific learning rates
     */
    public double updateAndGetFeatureLearningRate(String feature, double gradient) {
        double gradUpdate = 0.0;
        if (summedGradients.containsKey(feature)) {
            gradUpdate = gradient * gradient;
        } else {
            /**
             *  Unmentioned in the literature, but initializing
             *  the squared gradient at 1.0 rather than 0.0
             *  helps avoid oscillation.
             */
            gradUpdate = 1d+(gradient * gradient);
        }
        summedGradients.addToCoordinate(feature, gradUpdate);
        unnormalizedGradients.addToCoordinate(feature, gradient);
        return getFeatureLearningRate(feature);
    }

    public double getFeatureLearningRate(String feature) {
        return initialLearningRate/Math.sqrt(summedGradients.getCoordinate(feature));
    }

    /**
     *  Overrides the lazy l1 and l2 regularization in the base class
     *  to do adagrad with l1 regularization.
     *
     *  Lazily calculates and applies the update that minimizes the l1
     *  regularized objective. See "Adding l1 regularization" in
     *  http://www.ark.cs.cmu.edu/cdyer/adagrad.pdf
     */
    @Override
    public double lazyUpdate(String feature, double param, long start, long end) {
        if (Utilities.floatingPointEquals(laplace, 0.0d)) {
            return param;
        }
        for (long iter = start + 1; iter <= end; iter++) {
            if (Utilities.floatingPointEquals(param, 0.0d)) {
                return 0.0d;
            }
            if (laplace > 0.0) {
                return adagradL1(feature, param, iter);
            }
        }
        return param;
    }

    public double adagradL1(String feature, double param, long iter) {
        double eta = (initialLearningRate*iter)/Math.sqrt(summedGradients.getCoordinate(feature));
        double u = unnormalizedGradients.getCoordinate(feature);
        double normalizedGradient = u/iter;
        if (Math.abs(normalizedGradient) <= laplace) {
            param = 0.0;
        } else {
            param = -(Math.signum(u) * eta * (normalizedGradient - laplace));
        }
        return param;
    }

    @Override
    public void teardown() {
        summedGradients = new StringKeyedVector();
        unnormalizedGradients = new StringKeyedVector();
    }

}
