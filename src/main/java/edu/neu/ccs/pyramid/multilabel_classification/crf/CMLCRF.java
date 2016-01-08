package edu.neu.ccs.pyramid.multilabel_classification.crf;

import edu.neu.ccs.pyramid.dataset.LabelTranslator;
import edu.neu.ccs.pyramid.dataset.MultiLabel;
import edu.neu.ccs.pyramid.dataset.MultiLabelClfDataSet;
import edu.neu.ccs.pyramid.feature.FeatureList;
import edu.neu.ccs.pyramid.multilabel_classification.MultiLabelClassifier;
import edu.neu.ccs.pyramid.util.MathUtil;
import org.apache.mahout.math.Vector;

import java.io.Serializable;
import java.util.List;

import static edu.neu.ccs.pyramid.dataset.DataSetUtil.gatherMultiLabels;

/**
 * Created by Rainicy on 12/12/15.
 */
public class CMLCRF implements MultiLabelClassifier, Serializable {
    private static final long serialVersionUID = 2L;
    /**
     * Y_1, Y_2,...,Y_L
     */
    private int numClasses;
    /**
     * X feature length
     */
    private int numFeatures;

    private Weights weights;

    private List<MultiLabel> supportedCombinations;

    private int numSupported;

    public CMLCRF(MultiLabelClfDataSet dataSet) {
        this(dataSet.getNumClasses(), dataSet.getNumFeatures());
        this.setSupportedCombinations(gatherMultiLabels(dataSet));
        System.out.println("supported vector: " + supportedCombinations);
        this.numSupported = supportedCombinations.size();
    }

    public CMLCRF(int numClasses, int numFeatures) {
        this.numClasses = numClasses;
        this.numFeatures = numFeatures;
        this.weights = new Weights(numClasses, numFeatures);
    }


    public void setSupportedCombinations(List<MultiLabel> multiLabels) {
        this.supportedCombinations = multiLabels;
        this.numSupported = multiLabels.size();
    }


    /**
     * get the scores for all possible label combination
     * y and a given feature x.
     * @param vector
     * @return
     */
    public double[] predictCombinationScores(Vector vector){
        double[] scores = new double[this.numSupported];
        for (int k=0;k<scores.length;k++){
            scores[k] = predictCombinationScore(vector, k);
        }
        return scores;
    }


    /**
     * get the score by a given feature x and given label combination.
     * @param vector
     * @param label
     * @return
     */
    public double predictCombinationScore(Vector vector, MultiLabel label){
        double score = 0.0;
        for (int l=0; l<numClasses; l++) {
            if (label.matchClass(l)) {
                score += this.weights.getWeightsWithoutBiasForClass(l).dot(vector);
                score += this.weights.getBiasForClass(l);
            }
        }
        int start = this.weights.getNumWeightsForFeatures();
        for (int l1=0; l1<numClasses; l1++) {
            for (int l2=l1+1; l2<numClasses; l2++) {
                if (!label.matchClass(l1) && !label.matchClass(l2)) {
                    score += this.weights.getWeightForIndex(start);
                } else if (label.matchClass(l1) && !label.matchClass(l2)) {
                    score += this.weights.getWeightForIndex(start + 1);
                } else if (!label.matchClass(l1) && label.matchClass(l2)) {
                    score += this.weights.getWeightForIndex(start + 2);
                } else {
                    score += this.weights.getWeightForIndex(start + 3);
                }
                start += 4;
            }
        }
        return score;
    }

    /**
     *
     * get the score of a given feature x and given label
     * combination y_k.
     * @param vector
     * @param k
     * @return
     */
    public double predictCombinationScore(Vector vector, int k){
        return predictCombinationScore(vector, supportedCombinations.get(k));
    }

    public double[] predictCombinationProbs(Vector vector){
        double[] scoreVector = this.predictCombinationScores(vector);
        double[] probVector = new double[this.numSupported];
        double logDenominator = MathUtil.logSumExp(scoreVector);
        for (int k=0;k<this.numSupported;k++){
            double logNumerator = scoreVector[k];
            double pro = Math.exp(logNumerator-logDenominator);
            probVector[k]=pro;
        }
        return probVector;
    }

    public double[] predictLogCombinationProbs(Vector vector){
        double[] scoreVector = this.predictCombinationScores(vector);
        double[] logProbVector = new double[this.numSupported];
        double logDenominator = MathUtil.logSumExp(scoreVector);
        for (int k=0;k<this.numSupported;k++) {
            double logNumerator = scoreVector[k];
            logProbVector[k]=logNumerator-logDenominator;
        }
        return logProbVector;
    }

    @Override
    public int getNumClasses() {
        return numClasses;
    }

    public int getNumSupported() {
        return numSupported;
    }

    public int getNumFeatures() {
        return numFeatures;
    }

    public Weights getWeights() {
        return weights;
    }

    public List<MultiLabel> getSupportedCombinations() {
        return supportedCombinations;
    }


    @Override
    public MultiLabel predict(Vector vector) {
        double[] scores = predictCombinationScores(vector);
        double maxScore = Double.NEGATIVE_INFINITY;
        int predictedCombination = 0;
        for (int k=0;k<scores.length;k++){
            double scoreCombinationK = scores[k];
            if (scoreCombinationK > maxScore){
                maxScore = scoreCombinationK;
                predictedCombination = k;
            }
        }
//        System.out.println(this.supportedCombinations.get(predictedClass));
        return this.supportedCombinations.get(predictedCombination);
    }

    @Override
    public FeatureList getFeatureList() {
        return null;
    }

    @Override
    public LabelTranslator getLabelTranslator() {
        return null;
    }


    // TODO
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("CMLCRF{");
        sb.append('}');
        return sb.toString();
    }
}