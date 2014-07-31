package com.etsy.conjecture.model;

import java.io.Serializable;

public abstract class LearningRateComputation implements Serializable {

    private static final long serialVersionUID = 6542742510494701712L;
    
    protected final String learningRateSchedule;

    protected LearningRateComputation() {
        learningRateSchedule = getLearningRateSchedule();
    }

    protected abstract String getLearningRateSchedule();

    public abstract double computeFeatureLearningRate(String feature, long epoch, double gradient);

}