package com.etsy.conjecture.model;

import com.etsy.conjecture.data.LazyVector;
import com.etsy.conjecture.data.StringKeyedVector;
import static com.google.common.base.Preconditions.checkArgument;
import com.etsy.conjecture.Utilities;
import com.etsy.conjecture.data.Label;
import com.etsy.conjecture.data.LabeledInstance;
import com.etsy.conjecture.data.RealValuedLabel;

/**
 *  MIRA takes care of the full update. This is basically just a passthrough to
 *  the MIRA getGradients.
 */
public class MIRAOptimizer extends SGDOptimizer {

    @Override
    public StringKeyedVector getUpdate(LabeledInstance instance) {
        return model.getGradients(instance);
    }
}
