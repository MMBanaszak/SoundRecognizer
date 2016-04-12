package com.vr.record.Recognizer;

import com.savarese.spatial.GenericPoint;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Marek on 2016-04-09.
 */
public class MelBankFilter {

    public static GenericPoint<Double> Apply(double[] array, int resultLength)
    {
        return getMelVector(array,resultLength,16000);
    }

    //http://en.wikipedia.org/wiki/Mel_scale
    private static double getMelFunctionValue(double frequency)
    {
        double melValue = 2595 * Math.log10((frequency / 700) + 1);

        return melValue;
    }

    private static double getFrequencyFromMel(double melValue)
    {
        double frequency = 700 * (Math.pow(Math.E, melValue / 1127) - 1);
        return frequency;
    }

    private static GenericPoint<Double> getMelVector(double[] fftData, int vectorsCount, int maxFrequency)
    {
        GenericPoint<Double> res= new GenericPoint<Double>(vectorsCount-1);

        double maxMelScaleValue = getMelFunctionValue(maxFrequency);

        double melFactor = maxMelScaleValue / (vectorsCount);

        double frequencyFactor = maxFrequency / fftData.length;

        double[] partialVector = getPartialVectors(fftData, vectorsCount, melFactor, frequencyFactor);
        int i=0;
        for(double d : partialVector)
        {
            res.setCoord(i,d);
            i++;
        }

        return res;
    }

    private static double[] getPartialVectors(double[] fftData, int vectorsCount, double melFactor, double frequencyFactor)
    {
        List<double[]> listPartialVectors = new ArrayList<double[]>();

        int[] accumulationPoints = new int[vectorsCount];
        for (int i = 0; i < vectorsCount; i++)
        {
            accumulationPoints[i] = (int)Math.floor(getFrequencyFromMel(melFactor * (1 + i)) / frequencyFactor);
        }

        double[] partialvector = new double[fftData.length];
        double[] tableFactors = getTableFactors(accumulationPoints[1]);
        for (int j = 0; j < accumulationPoints[0]; j++)
        {
            partialvector[j] = fftData[j] * tableFactors[j];
        }
        for (int j = accumulationPoints[0]; j < accumulationPoints[1]; j++)
        {
            partialvector[j] = fftData[j] * tableFactors[j];
        }
        listPartialVectors.add(partialvector);

        /*Calculating inner vectors*/
        int factorCounter = 0;
        for (int i = 2; i < vectorsCount - 1; i++)
        {
            partialvector = new double[fftData.length];
            tableFactors = getTableFactors(accumulationPoints[i + 1] - accumulationPoints[i - 1]);
            factorCounter = 0;
            for (int j = accumulationPoints[i - 1]; j < accumulationPoints[i]; j++)
            {
                partialvector[j] = fftData[j] * tableFactors[factorCounter];
                factorCounter++;
            }
            for (int j = accumulationPoints[i]; j < accumulationPoints[i + 1]; j++)
            {
                partialvector[j] = fftData[j] * tableFactors[factorCounter];
                factorCounter++;
            }
            listPartialVectors.add(partialvector);
        }

        /*Calculating outer vectors*/
        partialvector = new double[fftData.length];
        tableFactors = getTableFactors(fftData.length - accumulationPoints[vectorsCount - 2] + 1);
        factorCounter = 0;
        for (int j = accumulationPoints[vectorsCount - 2]; j <= accumulationPoints[vectorsCount - 1]-1; j++)
        {
            partialvector[j] = fftData[j] * tableFactors[factorCounter];
            factorCounter++;
        }
        for (int j = accumulationPoints[vectorsCount - 1]; j < fftData.length; j++)
        {
            partialvector[j] = fftData[j] * tableFactors[factorCounter];
            factorCounter++;
        }
        listPartialVectors.add(partialvector);

        double[] result = new double[listPartialVectors.size()];
        int vectorCounter = 0;
        for(double[] table : listPartialVectors)
        {
            for (int i = 0; i < table.length; i++)
            {
                result[vectorCounter] += table[i];
            }
            vectorCounter++;
        }
        return result;
    }

    private static double[] getTableFactors(int tableLength)
    {
        double[] table = new double[tableLength];
        double a = tableLength / 2;
        int mid = (int)(Math.ceil(a));
        double factor = 100 / mid;

        for (int i = 0; i <= mid; i++)
        {
            table[i] = positiveFuntion(i + i * factor);
            table[tableLength - i - 1] = table[i];
        }
        return table;
    }

    //x must be between 0-100
    private static double positiveFuntion(double x)
    {

        double y = x * 0.01f;
        return y;
    }
}
