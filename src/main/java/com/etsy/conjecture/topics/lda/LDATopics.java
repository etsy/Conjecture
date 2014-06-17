package com.etsy.conjecture.topics.lda;

import java.io.Serializable;

public interface LDATopics extends Serializable {

    public void setDict(LDADict dict) throws Exception;

    public LDADict getDict();

    public double wordProb(int word, int topic);

    public int numTopics();

    public int dictSize();

}
