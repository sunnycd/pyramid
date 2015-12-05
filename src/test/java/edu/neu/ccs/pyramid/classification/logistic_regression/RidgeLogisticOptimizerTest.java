package edu.neu.ccs.pyramid.classification.logistic_regression;

import edu.neu.ccs.pyramid.configuration.Config;
import edu.neu.ccs.pyramid.dataset.ClfDataSet;
import edu.neu.ccs.pyramid.dataset.DataSetType;
import edu.neu.ccs.pyramid.dataset.TRECFormat;
import edu.neu.ccs.pyramid.eval.Accuracy;
import edu.neu.ccs.pyramid.optimization.*;

import java.io.File;

import static org.junit.Assert.*;

public class RidgeLogisticOptimizerTest {
    private static final Config config = new Config("config/local.config");
    private static final String DATASETS = config.getString("input.datasets");
    private static final String TMP = config.getString("output.tmp");
    public static void main(String[] args) throws Exception{
        test1();
//        test2();
    }

    private static void test1() throws Exception{
//        ClfDataSet dataSet = TRECFormat.loadClfDataSet(new File(DATASETS, "/imdb/3/train.trec"),
//                DataSetType.CLF_SPARSE, true);
//        ClfDataSet testSet = TRECFormat.loadClfDataSet(new File(DATASETS, "/imdb/3/test.trec"),
//                DataSetType.CLF_SPARSE, true);
//        ClfDataSet dataSet = TRECFormat.loadClfDataSet(new File(DATASETS, "20newsgroup/1/train.trec"),
//                DataSetType.CLF_SPARSE, true);
//        ClfDataSet testSet = TRECFormat.loadClfDataSet(new File(DATASETS, "20newsgroup/1/test.trec"),
//                DataSetType.CLF_SPARSE, true);
        ClfDataSet dataSet = TRECFormat.loadClfDataSet(new File(DATASETS, "/spam/trec_data/train.trec"),
                DataSetType.CLF_SPARSE, true);
        ClfDataSet testSet = TRECFormat.loadClfDataSet(new File(DATASETS, "/spam/trec_data/test.trec"),
                DataSetType.CLF_SPARSE, true);
        double variance =1000;
        LogisticRegression logisticRegression = new LogisticRegression(dataSet.getNumClasses(),dataSet.getNumFeatures());
        RidgeLogisticOptimizer optimizer = new RidgeLogisticOptimizer(logisticRegression,dataSet,variance);
        optimizer.setParallelism(true);
        optimizer.getOptimizer().getTerminator().setMaxIteration(10000).setMode(Terminator.Mode.STANDARD);
        System.out.println("after initialization");
        System.out.println("train acc = " + Accuracy.accuracy(logisticRegression, dataSet));
        System.out.println("test acc = "+Accuracy.accuracy(logisticRegression,testSet));
        optimizer.optimize();
        System.out.println("after training");
        System.out.println("train acc = " + Accuracy.accuracy(logisticRegression, dataSet));
        System.out.println("test acc = "+Accuracy.accuracy(logisticRegression,testSet));
        System.out.println(optimizer.getOptimizer().getTerminator().getHistory());
    }

    private static void test2() throws Exception{
        ClfDataSet dataSet = TRECFormat.loadClfDataSet(new File(DATASETS, "/spam/trec_data/train.trec"),
                DataSetType.CLF_SPARSE, true);
        ClfDataSet testSet = TRECFormat.loadClfDataSet(new File(DATASETS, "/spam/trec_data/test.trec"),
                DataSetType.CLF_SPARSE, true);
        LogisticRegression logisticRegression = new LogisticRegression(dataSet.getNumClasses(),dataSet.getNumFeatures());

        // generate equal weights
        double[] gammas = new double[dataSet.getNumDataPoints()];
        for (int n=0; n<dataSet.getNumDataPoints(); n++) {
            gammas[n] =1.0;
        }

        // generate the targets distributions.
        int[] labels = dataSet.getLabels();
        double[][] targets = new double[dataSet.getNumDataPoints()][2];
        for (int n=0; n<dataSet.getNumDataPoints(); n++) {
            int label = labels[n];
            if (label == 0.0) {
                targets[n][0] = 1;
            } else {
                targets[n][1] = 1;
            }
        }

        RidgeLogisticOptimizer optimizer = new RidgeLogisticOptimizer(logisticRegression,dataSet,gammas,targets,500);
        optimizer.getOptimizer().getTerminator().setMaxIteration(10000).setMode(Terminator.Mode.STANDARD);
        System.out.println("after initialization");
        System.out.println("train acc = " + Accuracy.accuracy(logisticRegression, dataSet));
        System.out.println("test acc = "+Accuracy.accuracy(logisticRegression,testSet));
        optimizer.optimize();
        System.out.println("after training");
        System.out.println("train acc = " + Accuracy.accuracy(logisticRegression, dataSet));
        System.out.println("test acc = "+Accuracy.accuracy(logisticRegression,testSet));
        System.out.println(optimizer.getOptimizer().getTerminator().getHistory());
    }
}