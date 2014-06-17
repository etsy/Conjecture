package com.etsy.conjecture.model;

import java.util.Collection;

import com.etsy.conjecture.data.Label;
import com.etsy.conjecture.data.LabeledInstance;

public interface UpdateableModel<L extends Label, M extends UpdateableModel<L, M>>
        extends Model<L>, Decomposable {
    // - update the model with a single labeled instance.
    public void update(LabeledInstance<L> instance);

    // - update the model with many labeled instances.
    public void update(Collection<LabeledInstance<L>> instances);

    // - merge two models together.
    public void merge(M model, double weight);

    // - multiply the parameter vector by a constant.
    public void reScale(double scale);

    // - set whether to add unseen-features when updating.
    public void setFreezeFeatureSet(boolean freeze);

    // - reset the epoch number after model merging.
    public void setEpoch(long epoch);

    public long getEpoch();
}
