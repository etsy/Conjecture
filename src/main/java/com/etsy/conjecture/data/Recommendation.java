package com.etsy.conjecture.data;

import java.io.Serializable;

public class Recommendation implements Serializable {

    private static final long serialVersionUID = 1L;

    public final double score;
    public final String id;

    public Recommendation(String id, double score) {
        this.id = id;
        this.score = score;
    }

}