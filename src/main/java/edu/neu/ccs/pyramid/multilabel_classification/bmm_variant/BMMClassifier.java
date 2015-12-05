package edu.neu.ccs.pyramid.multilabel_classification.bmm_variant;

import edu.neu.ccs.pyramid.classification.lkboost.LKBoost;
import edu.neu.ccs.pyramid.classification.logistic_regression.LogisticRegression;
import edu.neu.ccs.pyramid.dataset.LabelTranslator;
import edu.neu.ccs.pyramid.dataset.MultiLabel;
import edu.neu.ccs.pyramid.dataset.MultiLabelClfDataSet;
import edu.neu.ccs.pyramid.feature.FeatureList;
import edu.neu.ccs.pyramid.multilabel_classification.MultiLabelClassifier;
import edu.neu.ccs.pyramid.classification.Classifier.ProbabilityEstimator;
import org.apache.mahout.math.RandomAccessSparseVector;
import org.apache.mahout.math.Vector;

import java.io.*;


/**
 * Created by Rainicy on 10/23/15.
 */
public class BMMClassifier implements MultiLabelClassifier, Serializable {
    private static final long serialVersionUID = 1L;
    int numLabels;
    int numClusters;
    int numFeatures;
    int numSample = 100;
    boolean allowEmpty = false;

    String predictMode;

    // parameters
    // format: [numClusters][numLabels]
    ProbabilityEstimator[][] binaryClassifiers;
    ProbabilityEstimator multiNomialClassifiers;





    /**
     * Default constructor by given a MultiLabelClfDataSet
     * @param dataSet
     * @param numClusters
     */
    public BMMClassifier(MultiLabelClfDataSet dataSet, int numClusters) {
        this(dataSet.getNumClasses(), numClusters, dataSet.getNumFeatures());
    }

    public BMMClassifier(int numClasses, int numClusters, int numFeatures) {
        this.numLabels = numClasses;
        this.numClusters = numClusters;
        this.numFeatures = numFeatures;
        // initialize distributions
        this.binaryClassifiers = new LogisticRegression[numClusters][numClasses];
        for (int k=0; k<numClusters; k++) {
            for (int l=0; l<numClasses; l++) {
                this.binaryClassifiers[k][l] = new LogisticRegression(2,numFeatures);
            }
        }
        this.multiNomialClassifiers = new LogisticRegression(numClusters, numFeatures,true);
        this.predictMode = "sampling";
    }

    public BMMClassifier() {
    }

    /**
     * factory
     * @param numClasses
     * @param numClusters
     * @param numFeatures
     * @return
     */
    public static BMMClassifier newMixBoost(int numClasses, int numClusters, int numFeatures){
        BMMClassifier bmm = new BMMClassifier();
        bmm.numLabels = numClasses;
        bmm.numClusters = numClusters;
        bmm.numFeatures = numFeatures;
        // initialize distributions
        bmm.binaryClassifiers = new LKBoost[numClusters][numClasses];
        for (int k=0; k<numClusters; k++) {
            for (int l=0; l<numClasses; l++) {
                bmm.binaryClassifiers[k][l] = new LKBoost(2);
            }
        }
        bmm.multiNomialClassifiers = new LKBoost(numClusters);
        bmm.predictMode = "sampling";
        return bmm;
    }

    @Override
    public int getNumClasses() {
        return this.numLabels;
    }

    /**
     * return the log[p(y_n | z_n=k, x_n; w_k)] by all k from 1 to K.
     * @param X
     * @param Y
     * @return
     */
    public double[] clusterConditionalLogProbArr(Vector X, Vector Y) {
        double[] probArr = new double[numClusters];

        for (int k=0; k<numClusters; k++) {
            probArr[k] = clusterConditionalLogProb(X, Y, k);
        }

        return probArr;
    }

    /**
     * return one value for log [p(y_n | z_n=k, x_n; w_k)] by given k;
     * @param X
     * @param Y
     * @param k
     * @return
     */
    private double clusterConditionalLogProb(Vector X, Vector Y, int k) {
        double logProbResult = 0.0;
        for (int l=0; l< binaryClassifiers[k].length; l++) {
            double[] logProbs = binaryClassifiers[k][l].predictLogClassProbs(X);
            if (Y.get(l) == 1.0) {
                logProbResult += logProbs[1];
            } else {
                logProbResult += logProbs[0];
            }
        }
        return logProbResult;
    }

//    public MultiLabel[] predict(MultiLabelClfDataSet dataSet){
//
//        List<MultiLabel> results = new LinkedList<>();
//
//        for (int n=0; n<dataSet.getNumDataPoints(); n++) {
//            results.add(predict(dataSet.getRow(n)));
//        }
//        return results.toArray(new MultiLabel[results.size()]);
//    }



    public MultiLabel predict(Vector vector) {

        // new a BMMPredictor
        BMMPredictor bmmPredictor = new BMMPredictor(vector, multiNomialClassifiers, binaryClassifiers, numClusters, numLabels);
        bmmPredictor.setNumSamples(numSample);
        bmmPredictor.setAllowEmpty(allowEmpty);
        // samples methods
        if (predictMode.equals("sampling")) {
            return bmmPredictor.predictBySampling();

        } else if (predictMode.equals("dynamic")) {
            return bmmPredictor.predictByDynamic();
        } else if (predictMode.equals("greedy")) {
            return bmmPredictor.predictByGreedy();
        } else {
            throw new RuntimeException("Unknown predictMode: " + predictMode);
        }

    }


    public String toString() {
        Vector vector = new RandomAccessSparseVector(numFeatures);
        double[] mixtureCoefficients = multiNomialClassifiers.predictClassProbs(vector);
        final StringBuilder sb = new StringBuilder("BMM{\n");
        sb.append("numLabels=").append(numLabels).append("\n");
        sb.append("numClusters=").append(numClusters).append("\n");
        for (int k=0;k<numClusters;k++){
            sb.append("cluster ").append(k).append(":\n");
            sb.append("proportion = ").append(mixtureCoefficients[k]).append("\n");
        }
        sb.append('}');
        return sb.toString();
    }

    public void setPredictMode(String mode) {
        this.predictMode = mode;
    }
    public void setAllowEmpty(boolean allowEmpty) {
        this.allowEmpty = allowEmpty;
    }


    @Override
    public FeatureList getFeatureList() {
        return null;
    }

    @Override
    public LabelTranslator getLabelTranslator() {
        return null;
    }

    public void setNumSample(int numSample) {
        this.numSample = numSample;
    }

    public static BMMClassifier deserialize(File file) throws Exception {
        try (
                FileInputStream fileInputStream = new FileInputStream(file);
                BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
                ObjectInputStream objectInputStream = new ObjectInputStream(bufferedInputStream);
        ){
            BMMClassifier bmmClassifier = (BMMClassifier) objectInputStream.readObject();
            return bmmClassifier;
        }
    }

    public static BMMClassifier deserialize(String file) throws Exception {
        File file1 = new File(file);
        return deserialize(file1);
    }

    @Override
    public void serialize(File file) throws Exception {
        File parent = file.getParentFile();
        if (!parent.exists()) {
            parent.mkdir();
        }
        try (
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(bufferedOutputStream);
        ){
            objectOutputStream.writeObject(this);
        }
    }

    @Override
    public void serialize(String file) throws Exception {
        File file1 = new File(file);
        serialize(file1);
    }

    public ProbabilityEstimator[][] getBinaryClassifiers() {
        return binaryClassifiers;
    }

    public ProbabilityEstimator getMultiNomialClassifiers() {
        return multiNomialClassifiers;
    }

    public int getNumClusters() {
        return numClusters;
    }
}