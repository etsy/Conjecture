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

public class StringKeyedVectorTest {

    final double eps = 0.000001;

    // Build an SKV in a way which exercises a bunch of different code.
    public StringKeyedVector buildSKV() {
        StringKeyedVector skv = new StringKeyedVector();
        skv.setCoordinate("foo", 1.0);
        skv.addToCoordinate("bar", -2.0);
        skv.addToCoordinate("baz", 0.0);
        skv.setCoordinate("dave", 5.0);
        skv.deleteCoordinate("dave");
        return skv;
    }

    /**
     * Basic testing of coordinate getting and setting.
     */
    @Test
    public void testCoordinates() {
        StringKeyedVector skv = buildSKV();
        assertEquals(2, skv.size());
        assertEquals(1.0, skv.getCoordinate("foo"), eps);
        assertEquals(-2.0, skv.getCoordinate("bar"), eps);
        assertEquals(0.0, skv.getCoordinate("baz"), eps);
        assertEquals(0.0, skv.getCoordinate("dave"), eps);
        assertEquals(0.0, skv.getCoordinate("test"), eps);
    }

    /**
     * Test addScaled.
     */
    @Test
    public void testAddScaled() {
        StringKeyedVector skv = buildSKV();
        StringKeyedVector accum = new StringKeyedVector();
        skv.addScaled(accum, 1.0);
        accum.addScaled(skv, 2.0);
        assertEquals(2, accum.size());
        assertEquals(2.0, accum.getCoordinate("foo"), eps);
        assertEquals(-4.0, accum.getCoordinate("bar"), eps);
        accum.addScaled(skv, -2.0);
        assertEquals(0, accum.size());
    }

    /**
     * Test the dot product.
     */
    @Test
    public void testDotProduct() {
        StringKeyedVector skv = buildSKV();
        assertEquals(5.0, skv.dot(skv), eps);
        StringKeyedVector skv2 = new StringKeyedVector(skv);
        skv2.addToCoordinate("baz", -10.0);
        assertEquals(5.0, skv.dot(skv2), eps);
        assertEquals(105.0, skv2.dot(skv2), eps);
    }

    /**
     * Test freezing the keys.
     */
    @Test
    public void testFreezing() {
        StringKeyedVector skv = buildSKV();
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
        StringKeyedVector skv = buildSKV();
        // Serialize to a byte array in ram.
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(skv);
        oos.flush();
        // Deserialize.
        ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bis);
        StringKeyedVector des = (StringKeyedVector)ois.readObject();
        assertFalse(des.getFreezeKeySet());
        assertEquals(2, des.size());
        assertEquals(1.0, des.getCoordinate("foo"), eps);
        assertEquals(-2.0, des.getCoordinate("bar"), eps);
        assertEquals(0.0, des.getCoordinate("baz"), eps);
        assertEquals(0.0, des.getCoordinate("dave"), eps);
        assertEquals(0.0, des.getCoordinate("test"), eps);
    }

    /**
     * Test kryo serialization.
     */
    @Test
    public void testKryoSerialization() throws Exception {
        StringKeyedVector skv = buildSKV();
        // Serialize to a byte array in ram.
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Output ko = new Output(bos);
        Kryo kry = new Kryo();
        kry.writeObject(ko, skv);
        ko.flush();
        // Deserialize.
        ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
        Input ki = new Input(bis);
        StringKeyedVector des = (StringKeyedVector)kry.readObject(ki,
                StringKeyedVector.class);
        assertFalse(des.getFreezeKeySet());
        assertEquals(2, des.size());
        assertEquals(1.0, des.getCoordinate("foo"), eps);
        assertEquals(-2.0, des.getCoordinate("bar"), eps);
        assertEquals(0.0, des.getCoordinate("baz"), eps);
        assertEquals(0.0, des.getCoordinate("dave"), eps);
        assertEquals(0.0, des.getCoordinate("test"), eps);
    }

    /**
     * Make sure Gson serializes this thing properly.
     */
    @Test
    public void testGson() {
        Gson gson = new Gson();
        String json = gson.toJson(buildSKV());
        String vector1 = "\"vector\":{\"foo\":1.0,\"bar\":-2.0}";
        String vector2 = "\"vector\":{\"bar\":-2.0,\"foo\":1.0}";
        String fks = "\"freezeKeySet\":false";
        assertTrue(json.contains(vector1) || json.contains(vector2));
        assertTrue(json.contains(fks));
    }

}
