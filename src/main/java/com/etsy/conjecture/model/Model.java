package com.etsy.conjecture.model;

import java.io.Serializable;
import com.etsy.conjecture.data.Label;
import com.etsy.conjecture.data.StringKeyedVector;

public interface Model<L extends Label> extends Serializable {

    public L predict(StringKeyedVector instance);

}
