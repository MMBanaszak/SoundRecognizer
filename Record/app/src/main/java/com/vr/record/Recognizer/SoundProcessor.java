package com.vr.record.Recognizer;
import com.savarese.spatial.GenericPoint;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Marek on 2016-04-09.
 */
public class SoundProcessor {
    private static FastFourierTransformer fft = new FastFourierTransformer(DftNormalization.STANDARD);

    //Call with Util.convertSamplesByteToFloat(byte[] buffer, floatData);
    public static List<GenericPoint<Double>> CalculateMelVectors(double[] inputData)
    {
        // divide input buffer into smaller 256-size buffers and do Hamming window on them
        List<double[]> dividedData = DivideArrayIntoSamples(inputData, 256);
        List<double[]> processedSamples = DoHammingWindow(dividedData);

        List<GenericPoint<Double>> finalMelVectors = new ArrayList<GenericPoint<Double>>();
        for (double[] sample : processedSamples) {
            Complex[] complexResult = fft.transform(sample, TransformType.FORWARD);
            double[] doubleResult= Utils.convertComplexToDouble(complexResult);
            double[] halfOfDoubleResult = Utils.partArray(doubleResult, (int) Math.floor(doubleResult.length / 2));

            GenericPoint<Double> melVector = MelBankFilter.Apply(halfOfDoubleResult, 12);
            finalMelVectors.add(melVector);
        }
        return finalMelVectors;
    }

    private static List<double[]> DivideArrayIntoSamples(double[] inputArray, int sampleLength)
    {
        List<double[]> listOfSamples = new ArrayList<double[]>();
        int maxCapacity = inputArray.length / sampleLength;
        for (int i = 0; i < maxCapacity; i++)
        {
            double[] part = new double[sampleLength];
            int j = 0;
            while (j < sampleLength)
            {
                part[j] = inputArray[j + i * sampleLength];
                j++;
            }
            listOfSamples.add(part);
        }
        return listOfSamples;
    }

    private static List<double[]> DoHammingWindow(List<double[]> dividedData)
    {
        List<double[]> result = new ArrayList<double[]>();
        for(double[] table : dividedData)
        {
            result.add(DoHammingWindow(table));
        }
        return result;
    }

    private static double[] DoHammingWindow(double[] table)
    {
        for (int i = 0; i < table.length; i++)
        {
            double param = 0.5 - 0.5 * Math.cos((2 * Math.PI * i) / table.length);
            table[i] = table[i] * (float)param;
        }
        return table;
    }
}
