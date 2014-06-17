package com.etsy.conjecture.topics.lda;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public class LDADict implements Serializable {

    private static final long serialVersionUID = 2363682000942209420L;
    private ArrayList<String> words;
    private HashMap<String, Integer> dict;

    public LDADict(Set<String> unique_words) {
        words = new ArrayList<String>(unique_words.size());
        dict = new HashMap<String, Integer>();
        for (String s : unique_words) {
            words.add(s);
            dict.put(s, dict.size());
        }
    }

    public String word(int index) {
        return words.get(index);
    }

    public int index(String word) {
        return dict.get(word);
    }

    public int size() {
        return words.size();
    }

    public boolean contains(String word) {
        return dict.containsKey(word);
    }

    public String toString() {
        return "LDADict(size: " + size() + ")";
    }
}
