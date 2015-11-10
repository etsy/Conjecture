package com.etsy.conjecture.model;

import com.etsy.conjecture.data.*;

/**
 *  MIRA takes care of the full update. This is basically just a passthrough to
 *  the MIRA getGradients.
 */
public class MIRAOptimizer<L extends Label> extends SGDOptimizer<L> {

    @Override
    public StringKeyedVector getUpdate(LabeledInstance instance) {
        return model.getGradients(instance);
    }
}
