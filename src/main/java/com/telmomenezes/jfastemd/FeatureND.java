package com.telmomenezes.jfastemd;

import org.nd4j.linalg.api.ndarray.INDArray;

/**
 * @author Telmo Menezes (telmo@telmomenezes.com)
 *
 */
public class FeatureND implements Feature {
    private INDArray values;

    public FeatureND(INDArray values) {
        this.values = values;
    }
    
    public double groundDist(Feature f) {
        FeatureND f2d = (FeatureND)f;

        /*double sum = 0;
        for (int i = 0; i < values.length; i++) {
            double delta = this.values[i] - f2d.values[i];
            sum += delta * delta;
        }*/

        double mm = values.squaredDistance(f2d.values);
        return Math.sqrt(mm);
    }
}