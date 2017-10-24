package edu.neu.ccs.pyramid.clustering.bm;

import edu.neu.ccs.pyramid.dataset.DataSet;
import edu.neu.ccs.pyramid.dataset.DataSetBuilder;
import edu.neu.ccs.pyramid.dataset.Density;
import edu.neu.ccs.pyramid.dataset.MultiLabel;
import edu.neu.ccs.pyramid.util.ArgMax;
import edu.neu.ccs.pyramid.util.ArgMin;
import edu.neu.ccs.pyramid.util.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.stream.IntStream;


/**
 * select the best BMM from multiple random starts
 * Created by chengli on 9/12/15.
 */
public class BMSelector {
    private static final Logger logger = LogManager.getLogger();

    public static BM select(DataSet dataSet, int numClusters, int numRuns) {
        if (logger.isDebugEnabled()){
            logger.debug("start method select");
        }
        BM best = null;
        double bestObjective = Double.POSITIVE_INFINITY;
        for (int i=0;i<numRuns;i++){
//            System.out.println("fitting BM model "+i);
            BMTrainer trainer = new BMTrainer(dataSet,numClusters, i);
            BM bm = trainer.train();
            double objective = trainer.getObjective();
            if (objective < bestObjective){
                bestObjective = objective;
                best = bm;
            }
        }
        if (logger.isDebugEnabled()){
            logger.debug("finish method select");
        }
        return best;
    }

    public static BMTrainer selectTrainer(DataSet dataSet, int numClusters, int numRuns) {
        // TODO: multi-threads
        BMTrainer[] trainers = new BMTrainer[numRuns];
        double[] trainersObjectives = new double[numRuns];
        IntStream.range(0, numRuns).parallel().forEach(r -> {
            trainers[r] = new BMTrainer(dataSet, numClusters, r);
            trainers[r].train();
            trainersObjectives[r] = trainers[r].getObjective();
        });
        return trainers[ArgMin.argMin(trainersObjectives)];


//        BMTrainer best = null;
//        double bestObjective = Double.POSITIVE_INFINITY;
//        for (int i=0;i<numRuns;i++){
////            System.out.println("fitting BM model "+i);
//            BMTrainer trainer = new BMTrainer(dataSet,numClusters, i);
//            BM bm = trainer.train();
//            double objective = trainer.getObjective();
//            if (objective < bestObjective){
//                bestObjective = objective;
//                best = trainer;
//            }
//        }
//        return best;
    }


    public static double[][] selectGammas(int numClasses, MultiLabel[] multiLabels, int numClusters) {
        DataSet dataSet = DataSetBuilder.getBuilder()
                .numDataPoints(multiLabels.length)
                .numFeatures(numClasses)
                // use sparse format to speed up computation
                .density(Density.SPARSE_RANDOM)
                .build();
        for (int i=0;i<multiLabels.length;i++){
            MultiLabel multiLabel = multiLabels[i];
            for (int label: multiLabel.getMatchedLabels()){
                dataSet.setFeatureValue(i,label,1);
            }
        }
        BMTrainer trainer = BMSelector.selectTrainer(dataSet, numClusters, 10);
//        System.out.println("bm = "+trainer.bm);
//        System.out.println("gamma = "+ Arrays.deepToString(trainer.gammas));
        return trainer.gammas;
    }


    public static Pair<BM,double[][]> selectAll(int numClasses, MultiLabel[] multiLabels, int numClusters) {
        DataSet dataSet = DataSetBuilder.getBuilder()
                .numDataPoints(multiLabels.length)
                .numFeatures(numClasses)
                // use sparse format to speed up computation
                .density(Density.SPARSE_RANDOM)
                .build();
        for (int i=0;i<multiLabels.length;i++){
            MultiLabel multiLabel = multiLabels[i];
            for (int label: multiLabel.getMatchedLabels()){
                dataSet.setFeatureValue(i,label,1);
            }
        }
        BMTrainer trainer = BMSelector.selectTrainer(dataSet, numClusters, 10);
//        System.out.println("bm = "+trainer.bm);
//        System.out.println("gamma = "+ Arrays.deepToString(trainer.gammas));
        Pair<BM,double[][]> pair = new Pair<>();
        pair.setFirst(trainer.getBm());
        pair.setSecond(trainer.gammas);
        return pair;
    }

}
