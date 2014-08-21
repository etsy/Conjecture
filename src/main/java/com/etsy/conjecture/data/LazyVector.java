package com.etsy.conjecture.data;

import gnu.trove.function.TDoubleFunction;
import gnu.trove.iterator.TObjectDoubleIterator;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoSerializable;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.etsy.conjecture.Utilities;

public class LazyVector extends StringKeyedVector implements Serializable,
        KryoSerializable {

    private static final long serialVersionUID = -7070522686694887436L;

    protected transient ByteArrayDoubleHashMap iterations;

    protected long iteration = 0;

    protected UpdateFunction updater;

    /**
     * The function used to update the parameters during the lazy update
     */
    public static interface UpdateFunction extends Serializable {
        public double lazyUpdate(String key, double param, long startIteration,
                long endIteration);
    }

    public LazyVector() {
        this(new UpdateFunction() {
            private static final long serialVersionUID = 1740773207106961880L;

            public double lazyUpdate(String key, double p, long a, long b) {
                return p;
            }
        });
    }

    public LazyVector(UpdateFunction uf) {
        this(10, uf);
    }

    public LazyVector(int initialCapacity, UpdateFunction uf) {
        super(initialCapacity);
        iterations = new ByteArrayDoubleHashMap(initialCapacity, LOAD_FACTOR,
                FEATURE_ENCODING, 0.0);
        updater = uf;
    }

    public LazyVector(StringKeyedVector skv, UpdateFunction uf) {
        if (skv instanceof LazyVector) {
            ((LazyVector)skv).delazify();
        }
        this.vector = skv.vector;
        iterations = new ByteArrayDoubleHashMap(skv.size(), LOAD_FACTOR,
                FEATURE_ENCODING, 0.0);
        updater = uf;
    }

    public LazyVector(ByteArrayDoubleHashMap map, UpdateFunction uf) {
        super(map);
        iterations = new ByteArrayDoubleHashMap(10, LOAD_FACTOR,
                FEATURE_ENCODING, 0.0);
        updater = uf;
    }

    public LazyVector(Map<String, Double> jmap, UpdateFunction uf) {
        super(jmap);
        iterations = new ByteArrayDoubleHashMap(10, LOAD_FACTOR,
                FEATURE_ENCODING, 0.0);
        updater = uf;
    }

    public void incrementIteration() {
        iteration++;
    }

    public void delazify() {
        for (TObjectDoubleIterator<byte[]> it = vector.troveIterator(); it
                .hasNext();) {
            it.advance();
            long startIter = (long)iterations.getPrimitive(it.key()); // defaults
                                                                      // to 0.0
            if (startIter < iteration) {
                it.setValue(updater.lazyUpdate(it.key().toString(), it.value(), startIter, iteration));
                iterations.putPrimitive(it.key(), (double)iteration);
            }
        }
        removeZeroCoordinates();
    }

    public double delazifyCoordinate(String key) {
        return delazifyCoordinate(vector.stringToByteArray(key));
    }

    public double delazifyCoordinate(byte[] key) {
        if (vector.containsKey(key)) {
            long oldIteration = (long)iterations.getPrimitive(key);
            double initial = vector.getPrimitive(key);
            if (oldIteration < iteration) {
                double updated = updater.lazyUpdate(key.toString(), initial, oldIteration,
                        iteration);
                if (Utilities.floatingPointEquals(updated, 0.0d)) {
                    vector.removePrimitive(key);
                    iterations.removePrimitive(key);
                } else {
                    iterations.putPrimitive(key, (double)iteration);
                    vector.putPrimitive(key, updated);
                }
                return updated;
            } else {
                return initial;
            }
        }
        return 0.0;
    }

    public void skipToIteration(long iter) {
        delazify();
        iteration = iter;
        for (TObjectDoubleIterator<byte[]> it = iterations.troveIterator(); it
                .hasNext();) {
            it.advance();
            it.setValue((double)iter);
        }
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
            iterations.putPrimitive(key, (double)iteration);
        }
        return 0d;
    }

    /**
     * remove a coordinate from the vector (same as setting it to 0).
     */
    public double deleteCoordinate(String key) {
        if (vector.containsKey(key) && !freezeKeySet) {
            iterations.removePrimitive(key);
            return vector.removePrimitive(key);
        } else {
            return 0d;
        }
    }

    public Map<String, Double> getMap() {
        return vector;
    }

    protected double addToCoordinateInternal(byte[] bkey, double value) {
        delazifyCoordinate(bkey);
        if (vector.containsKey(bkey)) {
            double updated = vector.getPrimitive(bkey) + value;
            if (Utilities.floatingPointEquals(updated, 0.0d)) {
                iterations.removePrimitive(bkey);
                return vector.removePrimitive(bkey);
            } else {
                iterations.putPrimitive(bkey, (double)iteration);
                return vector.putPrimitive(bkey, updated);
            }
        } else if (!freezeKeySet && !Utilities.floatingPointEquals(value, 0.0d)) {
            vector.putPrimitive(bkey, value);
            iterations.putPrimitive(bkey, (double)iteration);
        }
        return 0d;
    }

    /**
     * return the value of a coordinate.
     */
    public double getCoordinate(String key) {
        delazifyCoordinate(key);
        return vector.getPrimitive(key);
    }

    /**
     * the dimension of the vector.
     */
    public int size() {
        delazify();
        return vector.size();
    }

    /**
     * whether this vector has a non-zero value for a coordinate.
     */
    public boolean containsKey(String key) {
        delazify();
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
        delazify();
        return vector.keySet();
    }

    /**
     * the set of values in the map.
     */
    public Set<Double> values() {
        delazify();
        return vector.values();
    }

    /**
     * Apply an arbitrary scalar function to the values.
     */
    public void transformValues(TDoubleFunction func) {
        delazify();
        vector.transformValues(func);
    }

    /**
     * Remove zeros that may have appeared as a result of a transform
     */
    public void removeZeroCoordinates() {
        for (TObjectDoubleIterator<byte[]> it = vector.troveIterator(); it
                .hasNext();) {
            it.advance();
            if (Utilities.floatingPointEquals(it.value(), 0d)) {
                iterations.removePrimitive(it.key());
                it.remove();
            }
        }
    }

    /**
     * compute the inner product between this and vec.
     */
    public double dot(StringKeyedVector skv) {
        if (skv instanceof LazyVector) {
            return dotWithLazy((LazyVector)skv);
        } else {
            return dotWithSKV(skv);
        }
    }

    protected double dotWithSKV(StringKeyedVector vec) {
        // dont figure out which ones bigger etc, since delazifying this to get
        // the size is too slow.
        double res = 0.0;
        for (TObjectDoubleIterator<byte[]> it = vec.vector.troveIterator(); it
                .hasNext();) {
            it.advance();
            res += it.value() * delazifyCoordinate(it.key());
        }
        return res;
    }

    protected double dotWithLazy(LazyVector vec) {
        ByteArrayDoubleHashMap vec_small = this.size() > vec.size() ? vec.vector
                : this.vector;
        ByteArrayDoubleHashMap vec_big = this.size() > vec.size() ? this.vector
                : vec.vector;
        ArrayList<byte[]> commonCoordinates = new ArrayList<byte[]>(); // prevent
                                                                       // modification
                                                                       // during
                                                                       // iteration.
        double res = 0.0;
        for (TObjectDoubleIterator<byte[]> it = vec_small.troveIterator(); it
                .hasNext();) {
            it.advance();
            if (vec_big.containsKey(it.key())) {
                commonCoordinates.add(it.key());
            }
        }
        for (byte[] key : commonCoordinates) {
            delazifyCoordinate(key);
            vec.delazifyCoordinate(key);
            res += vec_small.getPrimitive(key) * vec_big.getPrimitive(key);
        }
        return res;
    }

    /**
     * compute the LP norm for given p < infinity.
     */
    public double LPNorm(double p) {
        delazify();
        return super.LPNorm(p);
    }

    /**
     * immutable access the underlying hash map.
     */
    public Iterator<Map.Entry<String, Double>> iterator() {
        delazify();
        return vector.iterator();
    }

    public String toString() {
        delazify();
        return super.toString();
    }

    private Object writeReplace() throws java.io.ObjectStreamException {
        delazify();
        return this;
    }

    // - java serialization
    private void writeObject(ObjectOutputStream output) throws IOException {
        output.writeLong(iteration);
        output.writeObject(vector);
        output.writeObject(updater);
        output.writeBoolean(freezeKeySet);
    }

    private void readObject(ObjectInputStream input) throws IOException,
            ClassNotFoundException {
        iteration = input.readLong();
        vector = (ByteArrayDoubleHashMap)input.readObject();
        updater = (UpdateFunction)input.readObject();
        freezeKeySet = input.readBoolean();
        // set up iteration info,
        iterations = new ByteArrayDoubleHashMap(10, LOAD_FACTOR,
                (double)iteration);
    }

    // - kryo serialization for use in scalding.
    public void write(Kryo kryo, Output output) {
        delazify();
        output.writeLong(iteration);
        kryo.writeObject(output, vector);
        kryo.writeClassAndObject(output, updater);
        output.writeBoolean(freezeKeySet);
    }

    public void read(Kryo kryo, Input input) {
        iteration = input.readLong();
        vector = kryo.readObject(input, ByteArrayDoubleHashMap.class);
        updater = (UpdateFunction)kryo.readClassAndObject(input);
        freezeKeySet = input.readBoolean();
        // set up iteration info,
        iterations = new ByteArrayDoubleHashMap(10, LOAD_FACTOR,
                (double)iteration);
    }
}
