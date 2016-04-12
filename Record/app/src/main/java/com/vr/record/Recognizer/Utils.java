package com.vr.record.Recognizer;

import org.apache.commons.math3.complex.Complex;
/**
 * Created by Marek on 2016-04-09.
 */
public class Utils {

    public static double[] convertRawByteArrayToDoubleArray(byte[] byteArray)
    {
        double[] outBuffer = new double[byteArray.length / 2];
        for (int index = 0; index < byteArray.length; index += 2)
        {
            short sample = (short)((byteArray[index + 1] << 8) |
                    byteArray[index + 0]);

            double sample32 = sample / 32768f;
            outBuffer[(int)index / 2] = sample32;
        }
        return outBuffer;
    }

    public static double[] convertRawShortArrayToDoubleArray(short[] byteArray)
    {
        double[] outBuffer = new double[byteArray.length / 2];
        for (int index = 0; index < byteArray.length; index += 2)
        {
            short sample = byteArray[index];

            double sample32 = sample / 32768f;
            outBuffer[(int)index / 2] = sample32;
        }
        return outBuffer;
    }

    public static double[] convertComplexToDouble(Complex[] data)
    {
        double[] result = new double[data.length];
        int i = 0;
        for (Complex complex : data)
        {
            double value = Math.sqrt(complex.getReal() * complex.getReal() + complex.getImaginary() * complex.getImaginary());
            result[i] = value;
            i++;
        }
        return result;
    }

    public static double[] partArray(double[] array, int size) {
        double[] part = new double[size];
        System.arraycopy(array, 0, part, 0, size);
        return part;
    }

    public static byte[] partArray(byte[] array, int size) {
        byte[] part = new byte[size];
        System.arraycopy(array, 0, part, 0, size);
        return part;
    }
}
