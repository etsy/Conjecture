package com.etsy.conjecture.data;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import com.google.gson.Gson;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class LazyVectorTest {

    final double eps = 0.000001;

    // Update function to use for testing.
    // Decay the parameters over time.
    final static LazyVector.UpdateFunction uf = new LazyVector.UpdateFunction() {

        private static final long serialVersionUID = 1019666879466468375L;
        public double lazyUpdate(String k, double p, long a, long b) {
            return p * Math.pow(0.9, b - a);
        }
    };

    // Build an SKV in a way which exercises a bunch of different code.
    public LazyVector buildLV() {
        LazyVector lv = new LazyVector(uf);
        lv.setCoordinate("foo", 1.0);
        lv.addToCoordinate("bar", -2.0);
        lv.addToCoordinate("baz", 0.0);
        lv.setCoordinate("dave", 5.0);
        lv.deleteCoordinate("dave");
        return lv;
    }

    /**
     * Basic testing of coordinate getting and setting.
     */
    @Test
    public void testCoordinates() {
        LazyVector lv = buildLV();
        assertEquals(2, lv.size());
        assertEquals(1.0, lv.getCoordinate("foo"), eps);
        assertEquals(-2.0, lv.getCoordinate("bar"), eps);
        assertEquals(0.0, lv.getCoordinate("baz"), eps);
        assertEquals(0.0, lv.getCoordinate("dave"), eps);
        assertEquals(0.0, lv.getCoordinate("test"), eps);
    }

    /**
     * Basic testing of lazy updating.
     */
    @Test
    public void testCoordinatesLazy() {
        LazyVector lv = buildLV();
        lv.incrementIteration();
        assertEquals(2, lv.size());
        assertEquals(0.9, lv.getCoordinate("foo"), eps);
        assertEquals(-1.8, lv.getCoordinate("bar"), eps);
        assertEquals(0.0, lv.getCoordinate("baz"), eps);
        assertEquals(0.0, lv.getCoordinate("dave"), eps);
        assertEquals(0.0, lv.getCoordinate("test"), eps);
        lv.setCoordinate("bar", 2.0);
        lv.incrementIteration();
        assertEquals(2, lv.size());
        assertEquals(0.81, lv.getCoordinate("foo"), eps);
        assertEquals(1.8, lv.getCoordinate("bar"), eps);
    }

    /**
     * Test addScaled.
     */
    @Test
    public void testAddScaledToSKV() {
        LazyVector lv = buildLV();
        StringKeyedVector accum = new StringKeyedVector();
        accum.addScaled(lv, 2.0);
        assertEquals(2, accum.size());
        assertEquals(2.0, accum.getCoordinate("foo"), eps);
        assertEquals(-4.0, accum.getCoordinate("bar"), eps);
        lv.incrementIteration();
        accum.addScaled(lv, -2.0);
        assertEquals(2, accum.size());
        assertEquals(0.2, accum.getCoordinate("foo"), eps);
        assertEquals(-0.4, accum.getCoordinate("bar"), eps);
    }

    /**
     * Test addScaled.
     */
    @Test
    public void testAddScaledToLV() {
        LazyVector lv = buildLV();
        LazyVector accum = new LazyVector(uf);
        accum.setCoordinate("foo", 10.0);
        accum.incrementIteration();
        accum.incrementIteration();
        accum.incrementIteration(); // foo is now 7.29
        accum.addScaled(lv, 2.0);
        assertEquals(2, accum.size());
        assertEquals(9.29, accum.getCoordinate("foo"), eps);
        assertEquals(-4.0, accum.getCoordinate("bar"), eps);
        lv.incrementIteration(); // foo is now 0.9
        accum.incrementIteration(); // foo is now 8.361
        accum.addScaled(lv, -2.0);
        assertEquals(1, accum.size());
        assertEquals(6.561, accum.getCoordinate("foo"), eps);
    }

    /**
     * Test addScaled.
     */
    @Test
    public void testAddScaledToSelf() {
        LazyVector lv = buildLV();
        lv.incrementIteration();
        lv.incrementIteration();
        lv.addScaled(lv, 1.0);
        assertEquals(2, lv.size());
        assertEquals(1.0 * 0.81 * 2, lv.getCoordinate("foo"), eps);
        assertEquals(-2.0 * 0.81 * 2, lv.getCoordinate("bar"), eps);
    }

    /**
     * Test addScaled.
     */
    @Test
    public void testAddScaledSKVToLV() {
        LazyVector accum = new LazyVector(uf);
        StringKeyedVector skv = new StringKeyedVector();
        skv.setCoordinate("foo", 1.0);
        skv.setCoordinate("bar", 5.0);
        accum.addScaled(skv, 2.0);
        assertEquals(2, accum.size());
        assertEquals(2.0, accum.getCoordinate("foo"), eps);
        assertEquals(10.0, accum.getCoordinate("bar"), eps);
        accum.incrementIteration();
        accum.incrementIteration(); // foo: 1.62, bar: 8.10
        accum.addScaled(skv, -1.0);
        assertEquals(2, accum.size());
        assertEquals(0.62, accum.getCoordinate("foo"), eps);
        assertEquals(3.10, accum.getCoordinate("bar"), eps);
    }

    /**
     * Test the dot product.
     */
    @Test
    public void testDotProduct() {
        LazyVector skv = buildLV();
        StringKeyedVector skv2 = new StringKeyedVector(skv);
        assertEquals(5.0, skv.dot(skv), eps);
        skv.incrementIteration();
        assertEquals(5.0 * 0.81, skv.dot(skv), eps);
        skv2.addToCoordinate("baz", -10.0);
        assertEquals(5.0 * 0.9, skv.dot(skv2), eps);
    }

    /**
     * Test freezing the keys.
     */
    @Test
    public void testFreezing() {
        LazyVector skv = buildLV();
        skv.setFreezeKeySet(true);
        skv.addToCoordinate("fake", 1.0);
        assertEquals(2, skv.size());
        skv.setCoordinate("fake2", 2.0);
        assertEquals(2, skv.size());
        skv.setFreezeKeySet(false);
        skv.setCoordinate("fake2", 2.0);
        assertEquals(3, skv.size());
    }

    /**
     * Test java serialization.
     */
    @Test
    public void testJavaSerialization() throws Exception {
        LazyVector skv = buildLV();
        skv.incrementIteration();
        // Serialize to a byte array in ram.
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(skv);
        oos.flush();
        // Deserialize.
        ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bis);
        LazyVector des = (LazyVector)ois.readObject();
        assertFalse(des.getFreezeKeySet());
        assertEquals(2, des.size());
        assertEquals(0.9, des.getCoordinate("foo"), eps);
        assertEquals(-1.8, des.getCoordinate("bar"), eps);
        des.incrementIteration();
        assertEquals(2, des.size());
        assertEquals(0.81, des.getCoordinate("foo"), eps);
        assertEquals(-1.62, des.getCoordinate("bar"), eps);
    }

    /**
     * Test kryo serialization.
     */
    @Test
    public void testKryoSerialization() throws Exception {
        LazyVector skv = buildLV();
        skv.incrementIteration();
        // Serialize to a byte array in ram.
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Output ko = new Output(bos);
        Kryo kry = new Kryo();
        kry.writeObject(ko, skv);
        ko.flush();
        // Deserialize.
        ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
        Input ki = new Input(bis);
        LazyVector des = (LazyVector)kry.readObject(ki, LazyVector.class);
        assertFalse(des.getFreezeKeySet());
        assertEquals(2, des.size());
        assertEquals(0.9, des.getCoordinate("foo"), eps);
        assertEquals(-1.8, des.getCoordinate("bar"), eps);
        des.incrementIteration();
        assertEquals(2, des.size());
        assertEquals(0.81, des.getCoordinate("foo"), eps);
        assertEquals(-1.62, des.getCoordinate("bar"), eps);
    }

    /**
     * Make sure Gson serializes this thing properly.
     */
    @Test
    public void testGson() {
        Gson gson = new Gson();
        String json = gson.toJson(buildLV());
        String vector1 = "\"vector\":{\"foo\":1.0,\"bar\":-2.0}";
        String vector2 = "\"vector\":{\"bar\":-2.0,\"foo\":1.0}";
        String fks = "\"freezeKeySet\":false";
        assertTrue(json.contains(vector1) || json.contains(vector2));
        assertTrue(json.contains(fks));
        assertFalse(json.contains("iterations"));
    }

}
