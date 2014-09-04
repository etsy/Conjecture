package com.etsy.conjecture.model;

import com.etsy.conjecture.data.LazyVector;
import com.etsy.conjecture.data.Label;
import com.etsy.conjecture.data.LabeledInstance;
import java.util.Collection;
import com.etsy.conjecture.Utilities;
import com.etsy.conjecture.data.StringKeyedVector;

public class ElasticNetOptimizer extends SGDOptimizer implements LazyVector.UpdateFunction {

    @Override
    public StringKeyedVector getUpdate(LabeledInstance instance) {
        StringKeyedVector gradients = model.getGradients(instance);
        double learningRate = getDecreasingLearningRate(model.epoch);
        gradients.mul(-learningRate);
        return gradients;
    }

}
