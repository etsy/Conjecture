package com.etsy.conjecture.evaluation;

import com.etsy.conjecture.data.Label;

import java.util.Map;

public interface ModelEvaluation<L extends Label> {
    public void add(L real, L predicted);

    public Map<String, Double> getStatistics();

    public Map<String, Object> getObjects();

    public void merge(ModelEvaluation<L> other);
}
