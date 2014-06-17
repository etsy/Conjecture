package com.etsy.conjecture.topics.lda;

import java.io.Serializable;
import java.util.Random;

public class LDARandomTopics implements LDATopics, Serializable {

    private static final long serialVersionUID = -3258304331549481829L;
    int num_topics;
    int dict_size;
    LDADict dict;
    Random rnd = new Random();

    public LDARandomTopics(LDADict dict, int num_topics) {
        this.num_topics = num_topics;
        this.dict_size = dict.size();
        this.dict = dict;
    }

    public double wordProb(int word, int topic) {
        // not gonna normalize or anything, central limit theorem bro.
        rnd.setSeed(topic * dict_size + word);
        double mean = 1.0 / dict_size;
        // So if theres 100 words, return something between 0.005 and 0.015
        double rand = Math.max(0.0,
                mean + (rnd.nextBoolean() ? 1 : -1) * rnd.nextDouble()
                        * (mean / 2));
        return rand;
    }

    public int numTopics() {
        return num_topics;
    }

    public int dictSize() {
        return dict_size;
    }

    public LDADict getDict() {
        return dict;
    }

    public void setDict(LDADict d) {
        dict = d;
        dict_size = d.size();
    }

}
