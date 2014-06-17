package com.etsy.conjecture.model;

import java.util.Iterator;
import java.util.Map;

/**
 * Type of model to be used with the LargeModelTrainer.
 */
public interface Decomposable {

    /**
     * Present the model internals to be summed across submodels.
     */
    public Iterator<Map.Entry<String, Double>> decompose();

    /**
     * After rebuilding a blank model, fill in the parameters.
     */
    public void setParameter(String name, double value);

}
