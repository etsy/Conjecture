package com.etsy.conjecture.evaluation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import com.etsy.conjecture.data.Label;

public class EvaluationAggregator<L extends Label> implements Serializable {

    private static final long serialVersionUID = 5825037849957449364L;
    protected Map<String, DescriptiveStatistics> stats = new TreeMap<String, DescriptiveStatistics>();
    protected Map<String, List<Object>> obj = new HashMap<String, List<Object>>();

    public void add(ModelEvaluation<L> eval) {
        Map<String, Double> fold = eval.getStatistics();
        if (!stats.isEmpty()) {
            if (!fold.keySet().equals(stats.keySet())) {
                throw new java.lang.RuntimeException(
                        "Tried to add incompatible folds, with fields:"
                                + fold.keySet().toString() + " and "
                                + stats.keySet().toString());
            }
            for (Map.Entry<String, Double> e : fold.entrySet()) {
                stats.get(e.getKey()).addValue(e.getValue());
            }
            for (Map.Entry<String, Object> e : eval.getObjects().entrySet()) {
                obj.get(e.getKey()).add(e.getValue());
            }
        } else {
            for (Map.Entry<String, Double> e : fold.entrySet()) {
                DescriptiveStatistics ds = new DescriptiveStatistics();
                ds.addValue(e.getValue());
                stats.put(e.getKey(), ds);
            }
            for (Map.Entry<String, Object> e : eval.getObjects().entrySet()) {
                obj.put(e.getKey(), new ArrayList<Object>(5));
                obj.get(e.getKey()).add(e.getValue());
            }
        }
    }

    public double getValue(String key) {
       return stats.get(key).getMean();
    }

    @Override
    public String toString() {
        StringBuilder buff = new StringBuilder("Stat:\tMean\tStdDev\tMedian\n");
        for (Map.Entry<String, DescriptiveStatistics> e : stats.entrySet()) {
            buff.append(e.getKey() + ":\t" + format(e.getValue()) + "\n");
        }
        for (Map.Entry<String, List<Object>> e : obj.entrySet()) {
            buff.append(e.getKey()).append(":\n");
            for (Object o : e.getValue()) {
                buff.append("----\n").append(o.toString()).append("\n");
            }
        }
        return buff.toString();
    }

    private String format(DescriptiveStatistics stats) {
        return String.format("%.4f\t%.4f\t%.4f", stats.getMean(),
                stats.getStandardDeviation(), stats.getPercentile(50));
    }
}
