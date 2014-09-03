package com.etsy.conjecture.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import com.etsy.conjecture.data.StringKeyedVector;

import org.junit.Test;

import com.etsy.conjecture.data.BinaryLabeledInstance;

public class UpdateableLinearModelTest {

    final double eps = 0.000001;
    final SGDOptimizer optimizer = new ElasticNetOptimizer();

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
        LogisticRegression slr = new LogisticRegression(optimizer);
        // perform one update and check parameter values.
        double eta = slr.optimizer.getDecreasingLearningRate(slr.epoch);
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
        SGDOptimizer laplaceOptimizer = optimizer.setLaplaceRegularizationWeight(0.1);
        LogisticRegression slr = new LogisticRegression(laplaceOptimizer);
        // perform one update and check parameter values.
        double eta = slr.optimizer.getDecreasingLearningRate(slr.epoch);
        slr.update(getPositiveInstance());
        assertEquals(eta * 0.5, slr.getParam().getCoordinate("foo"), eps);
        assertEquals(eta * 1.0, slr.getParam().getCoordinate("bar"), eps);
        double eta2 = slr.optimizer.getDecreasingLearningRate(slr.epoch);
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
        SGDOptimizer gaussianOptimizer = optimizer.setGaussianRegularizationWeight(0.2);
        LogisticRegression slr = new LogisticRegression(gaussianOptimizer);
        // perform one update and check parameter values.
        double eta = slr.optimizer.getDecreasingLearningRate(slr.epoch);
        slr.update(getPositiveInstance());
        assertEquals(eta * 0.5, slr.getParam().getCoordinate("foo"), eps);
        assertEquals(eta * 1.0, slr.getParam().getCoordinate("bar"), eps);
        double eta2 = slr.optimizer.getDecreasingLearningRate(slr.epoch);
        slr.update(getNegativeInstance());
        assertEquals(eta * 1.0 * (1.0 - eta2 * 0.2), slr.getParam()
                .getCoordinate("bar"), eps);
    }

    @Test
    public void testPerceptronBasic() {
        Hinge p = new Hinge(optimizer).setThreshold(0.0);
        // perform one update and check parameter values.
        double eta = p.optimizer.getDecreasingLearningRate(p.epoch);
        p.update(getPositiveInstance());
        assertEquals(eta * 1.0, p.getParam().getCoordinate("foo"), eps);
        assertEquals(eta * 2.0, p.getParam().getCoordinate("bar"), eps);
        assertTrue(p.predict(getPositiveInstance().getVector()).getValue() > 0.5);
        // perform a second update.
        p.update(getNegativeInstance());
        assertTrue(p.predict(getPositiveInstance().getVector()).getValue() > 0.5);
        assertTrue(p.predict(getNegativeInstance().getVector()).getValue() < 0.5);
    }

    public void testInstanceNotModified(UpdateableLinearModel model) {
        BinaryLabeledInstance instance = getPositiveInstance();
        StringKeyedVector instanceCopy = instance.getVector().copy();
        model.update(instance);
        assertEquals(instance.getVector().getCoordinate("foo"), instanceCopy.getCoordinate("foo"), 0.0);
        assertEquals(instance.getVector().getCoordinate("bar"), instanceCopy.getCoordinate("bar"), 0.0);
    }

    @Test
    public void testInstanceNotModifiedByOptimizer() {
        ElasticNetOptimizer eOptimizer = new ElasticNetOptimizer();
        LogisticRegression eModel = new LogisticRegression(eOptimizer);
        testInstanceNotModified(eModel);

        FTRLOptimizer ftrlOptimizer = new FTRLOptimizer();
        LogisticRegression fModel = new LogisticRegression(ftrlOptimizer);
        testInstanceNotModified(fModel);

        AdagradOptimizer adagradOptimizer = new AdagradOptimizer();
        LogisticRegression aModel = new LogisticRegression(adagradOptimizer);
        testInstanceNotModified(aModel);

        MIRA mModel = new MIRA();
        testInstanceNotModified(mModel);
    }

    @Test
    public void testInstanceNotModifiedByModel() {
        LogisticRegression lrModel = new LogisticRegression(optimizer);
        testInstanceNotModified(lrModel);

        LeastSquaresRegressionModel lsModel = new LeastSquaresRegressionModel(optimizer);
        testInstanceNotModified(lsModel);

        Hinge hModel = new Hinge(optimizer);
        testInstanceNotModified(hModel);
    }

}
