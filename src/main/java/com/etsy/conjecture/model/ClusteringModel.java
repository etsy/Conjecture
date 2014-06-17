package com.etsy.conjecture.model;

import com.etsy.conjecture.data.LabeledInstance;
import com.etsy.conjecture.data.ClusterLabel;
import com.etsy.conjecture.data.MulticlassPrediction;
import com.etsy.conjecture.data.StringKeyedVector;
import com.etsy.conjecture.data.LazyVector;
import com.etsy.conjecture.Utilities;
import com.etsy.conjecture.data.Label;
import com.etsy.conjecture.data.LabeledInstance;


import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import static com.google.common.base.Preconditions.checkArgument;

public abstract class ClusteringModel<ClusterLabel extends Label> implements UpdateableModel<ClusterLabel, ClusteringModel<ClusterLabel>>, Serializable {

  static final long serialVersionUID = 666L;
  protected double projectionErrorTolerance = 0.01;
  protected double projectionBallRadius = 1.0;
  protected int numClusters = 100;

  protected Map<String, StringKeyedVector> param = new HashMap<String, StringKeyedVector>();

  public void update(LabeledInstance<ClusterLabel> instance) {
    update(instance.getVector());
  }

  public void update(Collection<LabeledInstance<ClusterLabel>> instances) {
    for(LabeledInstance<ClusterLabel> instance : instances) {
      update(instance.getVector());
    }
  }

  public abstract void update(StringKeyedVector instance);

  public abstract ClusterLabel predict(StringKeyedVector instance);

  protected ClusteringModel() {
    Map<String, StringKeyedVector> init_param = new HashMap<String, StringKeyedVector>();
    for (int i = 0; i < numClusters; i++) {
      init_param.put(Integer.toString(i), new StringKeyedVector());
    }
    this.param = init_param;
  }

  protected ClusteringModel(HashMap<String, StringKeyedVector> param) {
    Map<String, StringKeyedVector> init_param = new HashMap<String, StringKeyedVector>();
    Iterator it = param.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry<String,StringKeyedVector> pairs = (Map.Entry)it.next();
      init_param.put(pairs.getKey(), pairs.getValue());
      it.remove();
    }
    this.param = init_param;
  }


  public void setFreezeFeatureSet(boolean freeze) {
  for(Map.Entry<String, StringKeyedVector> e : param.entrySet()) {
      e.getValue().setFreezeKeySet(freeze);
    }
  }

  public void reScale(double scale) {
    for(String cat : param.keySet()) {
      param.get(cat).mul(scale);
    }
  }

  public void merge(ClusteringModel<ClusterLabel> model, double scale) {
    for(String cat : param.keySet()) {
      param.get(cat).addScaled(model.param.get(cat), scale);
    }
  }

  public ClusteringModel<ClusterLabel> setNumClusters(int k) {
    checkArgument(k >= 0, "number of clusters must be non-negative, given: %s", k);
    this.numClusters = k;
    return this;
  }

  public ClusteringModel<ClusterLabel> setL1ProjectionErrorTolerance(double e) {
    checkArgument(e >= 0, "error tolerance must be non-negative, given: %s", e);
    this.projectionErrorTolerance = e;
    return this;
  }

  public ClusteringModel<ClusterLabel> setL1ProjectionBallRadius(double r) {
    checkArgument(r >= 0, "radius must be non-negative, given: %s", r);
    this.projectionBallRadius = r;
    return this;
  }

  public Iterator<Map.Entry<String, Double>> decompose() {
    throw new UnsupportedOperationException("not done yet");
  }

  public void setParameter(String name, double value){
    throw new UnsupportedOperationException("not done yet");
  }

  public long getEpoch() {
    return 0;
  }

  public void setEpoch(long epoch) {
    // this class doesnt care about epoch.
  }

}
