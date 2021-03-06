package edu.neu.ccs.pyramid.calibration;

import edu.neu.ccs.pyramid.calibration.VectorCalibrator;
import edu.neu.ccs.pyramid.dataset.RegDataSet;
import edu.neu.ccs.pyramid.regression.IsotonicRegression;
import edu.neu.ccs.pyramid.util.ArgMin;
import edu.neu.ccs.pyramid.util.Pair;
import org.apache.mahout.math.Vector;


import java.io.Serializable;
import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class VectorCardIsoSetCalibrator implements Serializable, VectorCalibrator {
    private static final long serialVersionUID = 1L;
    Map<Integer,IsotonicRegression> calibrations;
    private int scoreIndex;
    private int cardIndex;

    public VectorCardIsoSetCalibrator(RegDataSet regDataSet, int scoreIndex, int cardIndex, boolean interpolate) {
        this.scoreIndex = scoreIndex;
        this.cardIndex = cardIndex;
        this.calibrations = new HashMap<>();
        Set<Integer> cardinalities = new HashSet<>();
        for (int i=0;i<regDataSet.getNumDataPoints();i++) {
            cardinalities.add((int)regDataSet.getRow(i).get(cardIndex));
        }

        for (int cardinality : cardinalities) {
            Stream<Pair<Double, Double>> stream = IntStream.range(0, regDataSet.getNumDataPoints()).parallel()
                    .boxed().filter(i->((int)regDataSet.getRow(i).get(cardIndex))==cardinality)
                    //todo deal with regression labels
                    .map(i->new Pair<>(regDataSet.getRow(i).get(scoreIndex),regDataSet.getLabels()[i]));
            calibrations.put(cardinality, new IsotonicRegression(stream, interpolate));
        }
    }


    public IsotonicRegression getIsotonicReg(int card) {
        return calibrations.get(card);
    }

    public double calibrate(Vector vector){
        double uncalibrated = vector.get(scoreIndex);
        int cardinality = (int)vector.get(cardIndex);
        //deal with unseen cardinality
        if (!calibrations.containsKey(cardinality)){
            return 0;
        }
        return calibrations.get(cardinality).predict(uncalibrated);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("VectorCardIsoSetCalibrator{");
        for (int k:calibrations.keySet()){
            sb.append("card "+k).append(":\n");
            sb.append(calibrations.get(k)).append("\n");
        }
        sb.append('}');
        return sb.toString();
    }
}
