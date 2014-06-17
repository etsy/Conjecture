package com.etsy.conjecture.evaluation;

import java.io.Serializable;
import java.util.HashMap;

import com.etsy.conjecture.PrimitivePair;
import com.etsy.conjecture.data.RealValuedLabel;

/**
 * a basic container for evaluations TODO: add getters for individual metrics
 */
public class RegressionModelEvaluation implements
        ModelEvaluation<RealValuedLabel>, Serializable {

    private static final long serialVersionUID = 1L;
    private double MSE = 0, MAE = 0, examples = 0;

    public void add(RealValuedLabel real, RealValuedLabel pred) {
        add(real.getValue(), pred.getValue());
    }

    public void merge(ModelEvaluation<RealValuedLabel> other) {
        RegressionModelEvaluation tempOther = (RegressionModelEvaluation) other;
        MSE += tempOther.MSE;
        MAE += tempOther.MAE;
        examples += tempOther.examples;
    }

    public void add(double label, double prediction) {
        double difference = Math.abs(label - prediction);
        MSE += difference * difference;
        MAE += difference;
        examples++;
    }

    public void add(PrimitivePair labelPrediction) {
        add(labelPrediction.getFirst(), labelPrediction.getSecond());
    }

    public double computeMeanSquaredError() {
        return examples > 0 ? MSE / examples : 0;
    }

    public double computeMeanAbsoluteError() {
        return examples > 0 ? MAE / examples : 0;
    }

    public HashMap<String, Double> getStatistics() {
        HashMap<String, Double> m = new HashMap<String, Double>();
        m.put("MSE", computeMeanSquaredError());
        m.put("MAE", computeMeanAbsoluteError());
        return m;
    }

    @Override
    public String toString() {
        StringBuilder buff = new StringBuilder();
        buff.append("MSE: " + computeMeanSquaredError() + "\n");
        buff.append("MAE: " + computeMeanAbsoluteError() + "\n");
        return buff.toString();
    }

    public HashMap<String, Object> getObjects() {
        HashMap<String, Object> m = new HashMap<String, Object>();
        return m;
    }
}
