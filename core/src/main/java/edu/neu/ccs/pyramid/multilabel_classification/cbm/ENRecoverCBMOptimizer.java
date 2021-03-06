package edu.neu.ccs.pyramid.multilabel_classification.cbm;

import edu.neu.ccs.pyramid.classification.PriorProbClassifier;
import edu.neu.ccs.pyramid.classification.logistic_regression.ElasticNetLogisticTrainer;
import edu.neu.ccs.pyramid.classification.logistic_regression.LogisticLoss;
import edu.neu.ccs.pyramid.classification.logistic_regression.LogisticRegression;
import edu.neu.ccs.pyramid.classification.logistic_regression.RidgeLogisticOptimizer;
import edu.neu.ccs.pyramid.dataset.DataSetUtil;
import edu.neu.ccs.pyramid.dataset.MultiLabelClfDataSet;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by chengli on 4/24/17.
 */
public class ENRecoverCBMOptimizer extends AbstractRecoverCBMOptimizer{
    private static final Logger logger = LogManager.getLogger();
    private double regularizationMultiClass = 1.0;
    private double regularizationBinary = 1.0;
    private double l1RatioBinary = 0.0;
    private double l1RatioMultiClass = 0.0;
    private boolean lineSearch = true;
    private boolean activeSet = false;

    public ENRecoverCBMOptimizer(CBM cbm, MultiLabelClfDataSet dataSet) {
        super(cbm, dataSet);
    }

    public void setRegularizationMultiClass(double regularizationMultiClass) {
        this.regularizationMultiClass = regularizationMultiClass;
    }

    public void setRegularizationBinary(double regularizationBinary) {
        this.regularizationBinary = regularizationBinary;
    }

    public void setL1RatioBinary(double l1RatioBinary) {
        this.l1RatioBinary = l1RatioBinary;
    }

    public void setL1RatioMultiClass(double l1RatioMultiClass) {
        this.l1RatioMultiClass = l1RatioMultiClass;
    }

    public void setLineSearch(boolean lineSearch) {
        this.lineSearch = lineSearch;
    }

    public void setActiveSet(boolean activeSet) {
        this.activeSet = activeSet;
    }

    @Override
    protected void updateBinaryClassifier(int component, int label, MultiLabelClfDataSet activeDataset, double[] activeGammas) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        if (cbm.binaryClassifiers[component][label] == null || cbm.binaryClassifiers[component][label] instanceof PriorProbClassifier){
            cbm.binaryClassifiers[component][label] = new LogisticRegression(2, activeDataset.getNumFeatures());
        }


        int[] binaryLabels = DataSetUtil.toBinaryLabels(activeDataset.getMultiLabels(), label);
        double[][] targetsDistribution = DataSetUtil.labelsToDistributions(binaryLabels, 2);
        // no parallelism
        ElasticNetLogisticTrainer elasticNetLogisticTrainer = new ElasticNetLogisticTrainer.Builder((LogisticRegression)
                cbm.binaryClassifiers[component][label],  activeDataset, 2, targetsDistribution, activeGammas)
                .setRegularization(regularizationBinary)
                .setL1Ratio(l1RatioBinary)
                .setLineSearch(lineSearch).build();
        elasticNetLogisticTrainer.setActiveSet(activeSet);
        elasticNetLogisticTrainer.getTerminator().setMaxIteration(this.binaryUpdatesPerIter);
        elasticNetLogisticTrainer.optimize();
        if (logger.isDebugEnabled()){
            logger.debug("time spent on updating component "+component+" label "+label+" = "+stopWatch);
        }
    }

    @Override
    protected void updateMultiClassClassifier() {
        if (logger.isDebugEnabled()){
            logger.debug("start updateMultiClassClassifier");
        }
        // parallel
        ElasticNetLogisticTrainer elasticNetLogisticTrainer = new ElasticNetLogisticTrainer.Builder((LogisticRegression)
                cbm.multiClassClassifier, dataSet, cbm.multiClassClassifier.getNumClasses(), gammas)
                .setRegularization(regularizationMultiClass)
                .setL1Ratio(l1RatioMultiClass)
                .setLineSearch(lineSearch).build();
        elasticNetLogisticTrainer.setActiveSet(activeSet);
        elasticNetLogisticTrainer.getTerminator().setMaxIteration(this.multiclassUpdatesPerIter);
        elasticNetLogisticTrainer.optimize();
        if (logger.isDebugEnabled()){
            logger.debug("finish updateMultiClassClassifier");
        }
    }

    // todo deal with prior classifier
    //todo incorrect
    @Override
    protected double binaryObj(int component, int classIndex) {
        int[] binaryLabels = DataSetUtil.toBinaryLabels(dataSet.getMultiLabels(), classIndex);
        double[][] targetsDistribution = DataSetUtil.labelsToDistributions(binaryLabels, 2);
        double[] weights = new double[dataSet.getNumDataPoints()];
        for (int i=0; i< dataSet.getNumDataPoints(); i++) {
            weights[i] = gammas[i][component];
        }
        LogisticLoss logisticLoss = new LogisticLoss((LogisticRegression) cbm.binaryClassifiers[component][classIndex],
                dataSet, weights, targetsDistribution, regularizationBinary, l1RatioBinary, false);
        return logisticLoss.getValueEL();
    }

    @Override
    protected double multiClassClassifierObj() {
        LogisticLoss logisticLoss =  new LogisticLoss((LogisticRegression) cbm.multiClassClassifier,
                dataSet, gammas, regularizationMultiClass, l1RatioMultiClass, true);
        return logisticLoss.getValueEL();
    }
}
