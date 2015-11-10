package com.etsy.conjecture.model;

import com.etsy.conjecture.data.*;

public class ElasticNetOptimizer<L extends Label> extends SGDOptimizer<L> implements LazyVector.UpdateFunction {

    @Override
    public StringKeyedVector getUpdate(LabeledInstance instance) {
        StringKeyedVector gradients = model.getGradients(instance);
        double learningRate = getDecreasingLearningRate(model.epoch);
        gradients.mul(-learningRate);
        return gradients;
    }

}
