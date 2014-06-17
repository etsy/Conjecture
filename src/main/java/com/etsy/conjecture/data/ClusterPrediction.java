package com.etsy.conjecture.data;

import java.util.Map;
import com.google.common.collect.Maps;

/**
 * Representing a probability of membership in each cluster
 */
public class ClusterPrediction extends ClusterLabel{

    private static final long serialVersionUID = -1L;

    /**
     * Cluster membership probabilities
     */
    private Map<String, Double> clusterProbs;

    public ClusterPrediction(Map<String, Double> clusterProbs) {
        this.clusterProbs = Maps.newHashMap(clusterProbs);
        boolean first = true;
        double maxProb = 0;
        String maxCategory = null;
        for (String key : clusterProbs.keySet()) {
            if(first || clusterProbs.get(key) > maxProb) {
              maxProb = clusterProbs.get(key);
              maxCategory = key;
              first = false;
            }
        }
        setLabel(maxCategory);
    }

    public Map<String,Double> getMap() {
        return clusterProbs;
    }

}
