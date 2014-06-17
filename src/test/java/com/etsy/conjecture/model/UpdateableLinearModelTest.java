package com.etsy.conjecture.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.etsy.conjecture.data.BinaryLabeledInstance;

public class UpdateableLinearModelTest {

    final double eps = 0.000001;

    BinaryLabeledInstance getPositiveInstance() {
        BinaryLabeledInstance bli = new BinaryLabeledInstance(1.0);
        bli.setCoordinate("foo", 1.0);
        bli.setCoordinate("bar", 2.0);
        return bli;
    }

    BinaryLabeledInstance getNegativeInstance() {
        BinaryLabeledInstance bli = new BinaryLabeledInstance(0.0);
        bli.setCoordinate("foo", 1.0);
        bli.setCoordinate("baz", -1.0);
        return bli;
    }

    @Test
    public void testLogisticRegressionBasic() {
        LogisticRegression slr = new LogisticRegression();
        // perform one update and check parameter values.
        double eta = slr.computeLearningRate();
        slr.update(getPositiveInstance());
        assertEquals(eta * 0.5, slr.getParam().getCoordinate("foo"), eps);
        assertEquals(eta * 1.0, slr.getParam().getCoordinate("bar"), eps);
        assertTrue(slr.predict(getPositiveInstance().getVector()).getValue() > 0.5);
        // perform a second update.
        slr.update(getNegativeInstance());
        assertTrue(slr.predict(getPositiveInstance().getVector()).getValue() > 0.5);
        assertTrue(slr.predict(getNegativeInstance().getVector()).getValue() < 0.5);
    }

    @Test
    public void testLogisticRegressionLaplaceRegularization() {
        LogisticRegression slr = new LogisticRegression();
        slr.setLaplaceRegularizationWeight(0.1);
        // perform one update and check parameter values.
        double eta = slr.computeLearningRate();
        slr.update(getPositiveInstance());
        assertEquals(eta * 0.5, slr.getParam().getCoordinate("foo"), eps);
        assertEquals(eta * 1.0, slr.getParam().getCoordinate("bar"), eps);
        double eta2 = slr.computeLearningRate();
        slr.update(getNegativeInstance());
        assertEquals(eta * 1.0 - eta2 * 0.1, slr.getParam()
                .getCoordinate("bar"), eps);
        // update with a different example enough times to make bar -> 0.
        for (int i = 0; i < 10; i++) {
            slr.update(getNegativeInstance());
        }
        assertEquals(2, slr.getParam().size());
        assertEquals(0.0, slr.getParam().getCoordinate("bar"), eps);
    }

    @Test
    public void testLogisticRegressionGaussianRegularization() {
        LogisticRegression slr = new LogisticRegression();
        slr.setGaussianRegularizationWeight(0.2);
        // perform one update and check parameter values.
        double eta = slr.computeLearningRate();
        slr.update(getPositiveInstance());
        assertEquals(eta * 0.5, slr.getParam().getCoordinate("foo"), eps);
        assertEquals(eta * 1.0, slr.getParam().getCoordinate("bar"), eps);
        double eta2 = slr.computeLearningRate();
        slr.update(getNegativeInstance());
        assertEquals(eta * 1.0 * (1.0 - eta2 * 0.2), slr.getParam()
                .getCoordinate("bar"), eps);
    }

    @Test
    public void testPerceptronBasic() {
        Perceptron p = new Perceptron();
        // perform one update and check parameter values.
        double eta = p.computeLearningRate();
        p.update(getPositiveInstance());
        assertEquals(eta * 1.0, p.getParam().getCoordinate("foo"), eps);
        assertEquals(eta * 2.0, p.getParam().getCoordinate("bar"), eps);
        assertTrue(p.predict(getPositiveInstance().getVector()).getValue() > 0.5);
        // perform a second update.
        p.update(getNegativeInstance());
        assertTrue(p.predict(getPositiveInstance().getVector()).getValue() > 0.5);
        assertTrue(p.predict(getNegativeInstance().getVector()).getValue() < 0.5);
    }

}
