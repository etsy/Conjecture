package com.etsy.conjecture.data;

import gnu.trove.function.TDoubleFunction;
import gnu.trove.iterator.TObjectDoubleIterator;
import gnu.trove.map.hash.TObjectDoubleHashMap;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoSerializable;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

public class ByteArrayDoubleHashMap implements Serializable, KryoSerializable,
        Iterable<Map.Entry<String, Double>>, Map<String, Double> {

    private static final long serialVersionUID = -7070522686694887436L;

    // - represent the sparse map by a mapping of coordinate name strings
    // (feature names)
    // to doubles.
    protected TObjectDoubleHashMap<byte[]> map;

    protected String keyEncoding;
    protected float loadFactor;
    protected double defaultValue;

    public ByteArrayDoubleHashMap() {
        this(10, 0.8f, 0.0);
    }

    public ByteArrayDoubleHashMap(int initialCapacity, float loadFactor,
            double defaultValue) {
        this(initialCapacity, loadFactor, "ASCII", defaultValue);
    }

    public ByteArrayDoubleHashMap(int initialCapacity, float loadFactor,
            String keyEncoding, double defaultValue) {
        this.map = new TByteArrayDoubleHashMap(initialCapacity, loadFactor,
                defaultValue);
        this.keyEncoding = keyEncoding;
        this.loadFactor = loadFactor;
        this.defaultValue = defaultValue;
    }

    public String byteArrayToString(byte[] b) {
        try {
            return new String(b, keyEncoding);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }

    public byte[] stringToByteArray(String s) {
        try {
            return s.getBytes(keyEncoding);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Customized trove hashmap which does both: customized hash/equality
     * functions, and also storing the values as a primitive array.
     */
    static class TByteArrayDoubleHashMap extends TObjectDoubleHashMap<byte[]> {
        public TByteArrayDoubleHashMap(int initialSize, float loadFactor,
                double defaultValue) {
            super(initialSize, loadFactor, defaultValue);
        }

        protected int hash(Object obj) {
            return Arrays.hashCode((byte[])obj);
        }

        protected boolean equals(Object a, Object b) {
            return b != null && b != REMOVED
                    && Arrays.equals((byte[])a, (byte[])b);
        }

        // - ovrride this to prevent doubling on resize.
        public double put(byte[] key, double value) {
            int index = insertKey(key);
            double previous = 0.0;
            boolean isNewMapping = true;
            if (index < 0) {
                index = -index - 1;
                previous = _values[index];
                isNewMapping = false;
            }
            _values[index] = value;
            if (isNewMapping) {
                postInsertHook2(consumeFreeSlot);
            }

            return previous;
        }

        protected final void postInsertHook2(boolean usedFreeSlot) {
            if (usedFreeSlot) {
                _free--;
            }

            if (++_size > _maxSize || _free == 0) {
                int newCapacity = _size > _maxSize ? gnu.trove.impl.PrimeFinder
                        .nextPrime((int)(capacity() * 1.2) + 10) : capacity();
                if (newCapacity > 1000000) {
                    System.out.println("rehashing to size: " + newCapacity
                            + " from " + capacity());
                }
                rehash(newCapacity);
                computeMaxSize(capacity());
            }
        }
    }

    public int size() {
        return map.size();
    }

    public boolean containsKey(Object key) {
        if (key instanceof byte[]) {
            return map.containsKey(key);
        } else if (key instanceof String) {
            return map.containsKey(stringToByteArray((String)key));
        } else {
            throw new IllegalArgumentException("class "
                    + key.getClass().toString()
                    + " is not valid for ByteArrayDoubleHashMap.containsKey");
        }
    }

    public Set<String> keySet() {
        Set<String> res = new HashSet<String>();
        for (byte[] b : map.keySet()) {
            res.add(byteArrayToString(b));
        }
        return res;
    }

    public Set<Double> values() {
        Set<Double> values = new HashSet<Double>();
        for (Map.Entry<String, Double> e : this) {
            values.add(e.getValue());
        }
        return values;
    }

    public boolean containsValue(Object d) {
        return values().contains((Double)d);
    }

    public Set<Map.Entry<String, Double>> entrySet() {
        Set<Map.Entry<String, Double>> entries = new HashSet<Map.Entry<String, Double>>();
        for (Map.Entry<String, Double> e : this) {
            entries.add(e);
        }
        return entries;
    }

    public boolean isEmpty() {
        return size() > 0;
    }

    public void clear() {
        map.clear();
    }

    public Double remove(Object k) {
        return removePrimitive((String)k);
    }

    public Double get(Object k) {
        return getPrimitive((String)k);
    }

    public Double put(String key, Double value) {
        return putPrimitive(key, value);
    }

    public void putAll(Map<? extends String, ? extends Double> m) {
        for (Map.Entry<? extends String, ? extends Double> e : m.entrySet()) {
            put((String)e.getKey(), (Double)e.getValue());
        }
    }

    public double getPrimitive(byte[] key) {
        return map.get(key);
    }

    public double getPrimitive(String key) {
        return map.get(stringToByteArray(key));
    }

    public double putPrimitive(byte[] key, double value) {
        return map.put(key, value);
    }

    public double putPrimitive(String key, double value) {
        return map.put(stringToByteArray(key), value);
    }

    public double removePrimitive(byte[] key) {
        return map.remove(key);
    }

    public double removePrimitive(String key) {
        return map.remove(stringToByteArray(key));
    }

    public void transformValues(TDoubleFunction func) {
        map.transformValues(func);
    }

    public TObjectDoubleIterator<byte[]> troveIterator() {
        return map.iterator();
    }

    public Iterator<Map.Entry<String, Double>> iterator() {
        return new Iterator<Map.Entry<String, Double>>() {
            private TObjectDoubleIterator<byte[]> iter = troveIterator();

            public boolean hasNext() {
                return iter.hasNext();
            }

            public void remove() {
                iter.remove();
            }

            public Map.Entry<String, Double> next() {
                iter.advance();
                return new AbstractMap.SimpleImmutableEntry<String, Double>(
                        byteArrayToString(iter.key()), iter.value());
            }
        };
    }

    // - java serialization
    private void writeObject(ObjectOutputStream output) throws IOException {
        output.writeObject(keyEncoding);
        output.writeFloat(loadFactor);
        output.writeDouble(defaultValue);
        output.writeInt(map.size());
        for (TObjectDoubleIterator<byte[]> it = map.iterator(); it.hasNext();) {
            it.advance();
            byte[] key = it.key();
            output.writeInt(key.length);
            for (int i = 0; i < key.length; i++) {
                output.writeByte(key[i]);
            }
            output.writeDouble(it.value());
        }
    }

    private void readObject(ObjectInputStream input) throws IOException,
            ClassNotFoundException {
        keyEncoding = (String)input.readObject();
        loadFactor = input.readFloat();
        defaultValue = input.readDouble();
        int size = input.readInt();
        map = new TByteArrayDoubleHashMap(size, loadFactor, defaultValue);
        for (int i = 0; i < size; i++) {
            int length = input.readInt();
            byte[] key = new byte[length];
            for (int j = 0; j < length; j++) {
                key[j] = input.readByte();
            }
            double value = input.readDouble();
            map.put(key, value);
        }
    }

    // - kryo serialization for use in scalding.
    public void write(Kryo kryo, Output output) {
        output.writeString(keyEncoding);
        output.writeFloat(loadFactor);
        output.writeDouble(defaultValue);
        output.writeInt(map.size());
        for (TObjectDoubleIterator<byte[]> it = map.iterator(); it.hasNext();) {
            it.advance();
            byte[] key = it.key();
            output.writeInt(key.length);
            for (int i = 0; i < key.length; i++) {
                output.writeByte(key[i]);
            }
            output.writeDouble(it.value());
        }
    }

    public void read(Kryo kryo, Input input) {
        keyEncoding = input.readString();
        loadFactor = input.readFloat();
        defaultValue = input.readDouble();
        int size = input.readInt();
        map = new TByteArrayDoubleHashMap(size, loadFactor, defaultValue);
        for (int i = 0; i < size; i++) {
            int length = input.readInt();
            byte[] key = new byte[length];
            for (int j = 0; j < length; j++) {
                key[j] = input.readByte();
            }
            double value = input.readDouble();
            map.put(key, value);
        }
    }
}
