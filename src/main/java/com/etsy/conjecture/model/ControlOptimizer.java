package com.etsy.conjecture.model;

import com.etsy.conjecture.data.*;

import java.util.*;

/**
 *  Current search ads control. Remove after current exp.
 */
public class ControlOptimizer<L extends Label> extends SGDOptimizer<L> {

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
        return getFeatureLearningRate(feature);
    }

    public double getFeatureLearningRate(String feature) {
        return initialLearningRate/Math.sqrt(summedGradients.getCoordinate(feature));
    }

    @Override
    public void teardown() {
        summedGradients = new StringKeyedVector();
    }
}
