package com.etsy.conjecture.model;

import com.etsy.conjecture.Utilities;
import com.etsy.conjecture.data.ClusterLabel;
import com.etsy.conjecture.data.StringKeyedVector;
import com.etsy.conjecture.data.ClusterPrediction;
import com.etsy.conjecture.Utilities;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import com.google.common.collect.Maps;

/**
 *  Implements sparse, streaming kmeans as described here:
 *  http://www.eecs.tufts.edu/~dsculley/papers/fastkmeans.pdf
 */ 
public class KMeans extends ClusteringModel<ClusterLabel> {
  
  private static final long serialVersionUID = 1L;
  private Map<String, Double> clusterCounts = new HashMap<String, Double>();

  public KMeans(String[] categories) {
    for(String s : categories) {
        param.put(s, new StringKeyedVector());
        clusterCounts.put(s, 0.0);
    }
  }

  private Map<String, StringKeyedVector> predefinedCenters;

  public KMeans(Map<String, StringKeyedVector> centers) {
    this.predefinedCenters = Maps.newHashMap(centers);
    for(String key : predefinedCenters.keySet()) {
      param.put(key, predefinedCenters.get(key));
      clusterCounts.put(key, 0.0);
    }
  }

  public ClusterPrediction predict(StringKeyedVector instance) {
    Map<String, Double> scores = new HashMap<String, Double>();
    for(Map.Entry<String, StringKeyedVector> e : param.entrySet()) {
      scores.put(e.getKey(), e.getValue().dot(instance));
    }
    return new ClusterPrediction(scores);
  }

  public void update(StringKeyedVector instance) {
    // Get closest center to instance
    String closest_center = predict(instance).getLabel();
    // Update the per center count
    Double current_count = clusterCounts.get(closest_center);
    clusterCounts.put(closest_center, current_count+1.0);
    // Get per center learning rate
    Double learning_rate = 1.0/clusterCounts.get(closest_center);
    // take gradient step
    StringKeyedVector center = param.get(closest_center);
    center.mul(1-learning_rate);
    instance.mul(learning_rate);
    center.add(instance);
    l1Projection(center);
    param.put(closest_center, center);
  }

  public Double getCurrent(StringKeyedVector center, Double theta) {
    Double current = 0.0;
    for (double v : center.values()) {
      current += Math.max(0, Math.abs(v)-theta);
    }
    return current;
  }

  /*
   *  Use bisection to find an approximate value of theta
   */
  public Double findTheta(StringKeyedVector center, Double norm) {
    Double upper = center.max();
    Double lower = 0.0;
    Double current = norm;
    Double theta = 0.0;
    while (current > projectionBallRadius * (1 + projectionErrorTolerance)) {
      theta = (upper + lower)/2.0;
      current = getCurrent(center, theta);
      if (current <= projectionBallRadius) {
        upper = theta;
      } else {
        lower = theta;
      }
    }
    return theta;
  }

  public void doProjection(StringKeyedVector center, Double theta) {
    Iterator it = center.iterator();
        while (it.hasNext()) {
            Map.Entry<String,Double> pairs = (Map.Entry)it.next();
            String key = pairs.getKey();
            double value = pairs.getValue();
            double projectedValue = Math.signum(value) * Math.max(0.0, Math.abs(value) - theta);
            center.setCoordinate(key, projectedValue);
        }
  }

  /**
   * An e-Accurate projection to the L1 ball, described here:
   * http://www.eecs.tufts.edu/~dsculley/papers/fastkmeans.pdf
   */
  public void l1Projection(StringKeyedVector center) {
    Double norm = center.LPNorm(1.0);
    if (norm <= projectionBallRadius + projectionErrorTolerance) {
      return;
    } else {
      Double theta = findTheta(center, norm);
      doProjection(center, theta);
    }
  }
}