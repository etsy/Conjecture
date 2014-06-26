package com.etsy.conjecture.data;

import java.util.Map;
import com.google.common.collect.Maps;

/**
 * representing a probability of membership in each class
 */
public class MulticlassPrediction extends MulticlassLabel {

    private static final long serialVersionUID = -1L;

    /**
     * class membership probabilities
     */
    private Map<String, Double> classProbs;

    public MulticlassPrediction(Map<String, Double> classProbs) {
        this.classProbs = Maps.newHashMap(classProbs);
        boolean first = true;
        double maxProb = 0;
        String maxCategory = null;
        for (String key : classProbs.keySet()) {
            if (first || classProbs.get(key) > maxProb) {
                maxProb = classProbs.get(key);
                maxCategory = key;
                first = false;
            }
        }
        setLabel(maxCategory);
    }

    public Double getProb(String category) {
        return classProbs.get(category);
    }

    public Double getProbOrElse(String category, Double def) {
        if (classProbs.containsKey(category)) {
            return classProbs.get(category);
        } else {
            return def;
        }
    }

    public Map<String, Double> getMap() {
        return classProbs;
    }

}
