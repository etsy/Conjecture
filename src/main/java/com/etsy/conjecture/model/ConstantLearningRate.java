package com.etsy.conjecture.model;
import static com.google.common.base.Preconditions.checkArgument;

import java.io.Serializable;

public class ConstantLearningRate extends SingleLearningRate {

    double initialLearningRate = 0.01;

    public double computeLearningRate(long t){
        return initialLearningRate;       
    }
}