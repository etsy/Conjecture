package com.etsy.conjecture.data;

import gnu.trove.function.TDoubleFunction;
import gnu.trove.iterator.TObjectDoubleIterator;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.etsy.conjecture.Utilities;
import com.google.gson.Gson;

public class StringKeyedVector implements Serializable,
        Iterable<Map.Entry<String, Double>> {

    private static final long serialVersionUID = -7070522686694887436L;

    // - represent the sparse vector by a mapping of coordinate name strings
    // (feature names)
    // to doubles.
    protected ByteArrayDoubleHashMap vector;

    // - whether to permit the addition of more features to this vector.
    protected boolean freezeKeySet = false;

    // - the load factor for the underlying hashmap.
    public static final float LOAD_FACTOR = 0.9f;

    public static final String FEATURE_ENCODING = "ASCII";

    public StringKeyedVector() {
        this(10);
    }

    public StringKeyedVector(int initialCapacity) {
        vector = new ByteArrayDoubleHashMap(initialCapacity, LOAD_FACTOR,
                FEATURE_ENCODING, 0.0);
    }

    public StringKeyedVector(StringKeyedVector skv) {
        this(skv.size());
        add(skv);
    }

    public StringKeyedVector(Map<String, Double> jmap) {
        vector = new ByteArrayDoubleHashMap(jmap.size(), LOAD_FACTOR,
                FEATURE_ENCODING, 0.0);
        vector.putAll(jmap);
    }

    /**
     * returns whether the key set is frozen (true means that further dimensions
     * cannot be added to this vector).
     */
    public boolean getFreezeKeySet() {
        return freezeKeySet;
    }

    /**
     * sets whether the key set is frozen (true means that further dimensions
     * cannot be added to this vector).
     */
    public void setFreezeKeySet(boolean freeze) {
        freezeKeySet = freeze;
    }

    /**
     * disregards prior value at a particular key, replacing with the specified
     * value.
     */
    public double setCoordinate(String key, double value) {
        if (Utilities.floatingPointEquals(value, 0d)) {
            return deleteCoordinate(key);
        } else if (!freezeKeySet) {
            vector.putPrimitive(key, value);
        }
        return 0d;
    }

    /**
     * remove a coordinate from the vector (same as setting it to 0).
     */
    public double deleteCoordinate(String key) {
        if (vector.containsKey(key) && !freezeKeySet) {
            return vector.removePrimitive(key);
        } else {
            return 0d;
        }
    }

    public Map<String, Double> getMap() {
        return vector;
    }

    /**
     * add to a specified coordinate (treating it as 0 if it was not present).
     */
    public double addToCoordinate(String key, double value) {
        byte[] bkey = vector.stringToByteArray(key);
        return addToCoordinateInternal(bkey, value);
    }

    protected double addToCoordinateInternal(byte[] bkey, double value) {
        if (vector.containsKey(bkey)) {
            double updated = vector.getPrimitive(bkey) + value;
            if (Utilities.floatingPointEquals(updated, 0.0d)) {
                return vector.removePrimitive(bkey);
            } else {
                return vector.putPrimitive(bkey, updated);
            }
        } else if (!freezeKeySet && !Utilities.floatingPointEquals(value, 0.0d)) {
            vector.putPrimitive(bkey, value);
        }
        return 0d;
    }

    /**
     * return the value of a coordinate.
     */
    public double getCoordinate(String key) {
        return vector.getPrimitive(key);
    }

    /**
     * add a multiple of vec to this.
     */
    public void addScaled(StringKeyedVector vec, double scale) {
        if (vec instanceof LazyVector) {
            ((LazyVector)vec).delazify();
        }
        for (TObjectDoubleIterator<byte[]> it = vec.vector.troveIterator(); it
                .hasNext();) {
            it.advance();
            addToCoordinateInternal(it.key(), scale * it.value());
        }
    }

    public StringKeyedVector multiplyPointwise(StringKeyedVector vec) {
        StringKeyedVector res = new StringKeyedVector();
        if (vec instanceof LazyVector) {
            ((LazyVector)vec).delazify();
        }
        for (TObjectDoubleIterator<byte[]> it = vec.vector.troveIterator(); it
                .hasNext();) {
            it.advance();
            res.vector.putPrimitive(it.key(), vector.getPrimitive(it.key())
                    * it.value());
        }
        return res;
    }

    public StringKeyedVector projectOntoNonZeroCoordinates(StringKeyedVector vec) {
        StringKeyedVector res = new StringKeyedVector();
        if (vec instanceof LazyVector) {
            ((LazyVector)vec).delazify();
        }
        for (TObjectDoubleIterator<byte[]> it = vec.vector.troveIterator(); it
                .hasNext();) {
            it.advance();
            res.addToCoordinateInternal(it.key(), vector.getPrimitive(it.key()));
        }
        return res;
    }

    /**
     * the dimension of the vector.
     */
    public int size() {
        return vector.size();
    }

    /**
     * whether this vector has a non-zero value for a coordinate.
     */
    public boolean containsKey(String key) {
        return vector.containsKey(key);
    }

    /**
     * whether this vector has a non-zero value for a coordinate.
     */
    public boolean contains(String key) {
        return containsKey(key);
    }

    /**
     * the set of non-zero coordinate names.
     */
    public Set<String> keySet() {
        return vector.keySet();
    }

    /**
     * the set of values in the map.
     */
    public Set<Double> values() {
        return vector.values();
    }

    /**
     * add vec to this
     */
    public void add(StringKeyedVector vec) {
        addScaled(vec, 1.0);
    }

    /**
     * subtract vec from this.
     */
    public void sub(StringKeyedVector vec) {
        addScaled(vec, -1.0);
    }

    /**
     * multiply this vector by a scalar.
     */
    public void mul(final double a) {
        transformValues(new TDoubleFunction() {
            public double execute(double b) {
                return a * b;
            }
        });
    }

    /**
     * Apply an arbitrary scalar function to the values.
     */
    public void transformValues(TDoubleFunction func) {
        vector.transformValues(func);
    }

    /**
     * Remove zeros that may have appeared as a result of a transform
     */
    public void removeZeroCoordinates() {
        @SuppressWarnings("unused")
        int i = 0;
        for (TObjectDoubleIterator<byte[]> it = vector.troveIterator(); it
                .hasNext();) {
            it.advance();
            if (Utilities.floatingPointEquals(it.value(), 0d)) {
                i++;
                it.remove();
            }
        }
    }

    /**
     * compute the inner product between this and vec.
     */
    public double dot(StringKeyedVector vec) {
        if (vec instanceof LazyVector) {
            return vec.dot(this);
        }
        ByteArrayDoubleHashMap vec_small = this.size() > vec.size() ? vec.vector
                : this.vector;
        ByteArrayDoubleHashMap vec_big = this.size() > vec.size() ? this.vector
                : vec.vector;
        double res = 0.0;
        for (TObjectDoubleIterator<byte[]> it = vec_small.troveIterator(); it
                .hasNext();) {
            it.advance();
            if (vec_big.containsKey(it.key())) {
                res += it.value() * vec_big.getPrimitive(it.key());
            }
        }
        return res;
    }

    /**
     * compute the LP norm for given p < infinity.
     */
    public double LPNorm(double p) {
        double tot = 0d;
        for (double v : vector.values()) {
            tot += Math.pow(Math.abs(v), p);
        }
        return Math.pow(tot, 1d / p);
    }

    /**
     * Find the max value.
     */
    public double max() {
        double max = 0.0;
        for (double v : vector.values()) {
            if (v > max) {
                max = v;
            }
        }
        return max;
    }

    /**
     * immutable access the underlying hash map.
     */
    public Iterator<Map.Entry<String, Double>> iterator() {
        return vector.iterator();
    }

    public String toString() {
        Gson gson = new Gson();
        return gson.toJson(vector);
    }

    /**
     * performs a deep copy of a stringkeyedvector
     *
     */
    public StringKeyedVector copy() {
        StringKeyedVector out = new StringKeyedVector(this.size());
        Iterator<Map.Entry<String, Double>> it = this.iterator();

        while (it.hasNext()) {
            Map.Entry<String, Double> entry = it.next();
            String key = entry.getKey();
            Double value = entry.getValue();

            out.setCoordinate(key, value);
        }

        return out;
    }
}
