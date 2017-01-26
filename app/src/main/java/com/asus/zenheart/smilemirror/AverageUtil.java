package com.asus.zenheart.smilemirror;

import android.util.Log;

public class AverageUtil {

    private static final String LOG_TAG = "AverageUtil";
    private static final int DEFAULT_WINDOW_SIZE = 1;

    private static int sMovingWindowSize = DEFAULT_WINDOW_SIZE;
    private static int sWeightAverageSize = DEFAULT_WINDOW_SIZE * (DEFAULT_WINDOW_SIZE + 1) / 2;

    private static float[] sBuffer;
    private static int sIndex = 0;
    private static float sTotal = 0;
    private static float sWeightTotal = 0;

    //this is for the first time to count with weight
    private static boolean sWeightCountFlag = false;

    private static float sSimpleMovingAverage;
    private static float sWeightedMovingAverage;

    /*unused*/
    // jack +++ for coach mode,used in the future
    public static float[] sSmileCounts = new float[4];
    public static Long sTimeStampStart;
    public static Long sTimeStampEnd;
    public static boolean sCountFlag;
    public static final boolean COUNT_START = true;
    public static final boolean COUNT_END = false;
    private static final float SMILE_DEGREE_L1 = 20;
    private static final float SMILE_DEGREE_L2 = 50;
    private static final float SMILE_DEGREE_L3 = 80;
    // jack ---

    /**
     * this method counts {@param happiness} into two average ways,for example,there are N elements
     * SimpleMovingAverage (SMA) : accumulate N elements values and divided by N ,
     * ex :(N1+N2+....Nn)/N, which is stored at {@link #sSimpleMovingAverage}
     * WeightedMovingAverage (WMA) : accumulate elements multiplied by N before accumulation,
     * ex:(1*N1+2*N2+....N*Nn)/(1+2+...N), which is store at {@link #sWeightedMovingAverage}
     *
     * @param happiness the score of smile degree,valued from 0 to 1,and -1 if recognized as not
     *                  smile
     * @return the results of SMA,sSimpleMovingAverage
     */
    public static float movingAverage(float happiness) {
        if (sBuffer == null) {
            sBuffer = new float[sMovingWindowSize];
        }

        //TO calculate the sWeightTotal
        if (sWeightCountFlag) {
            sWeightTotal = sWeightTotal - sTotal;
            sWeightTotal = sWeightTotal + happiness * sMovingWindowSize;
        } else {
            sWeightTotal = sWeightTotal + happiness * (sIndex + 1);
        }
        //TO calculate the sTotal with the new happiness
        //minus first value
        sTotal = sTotal - sBuffer[sIndex];
        //replace sBuffer[sIndex] with new happiness value and add to total
        sBuffer[sIndex] = happiness;
        //add new value to sTotal
        sTotal = sTotal + sBuffer[sIndex];

        sIndex++;
        if (sIndex > sMovingWindowSize - 1) {
            sIndex = sIndex % sMovingWindowSize;
            sWeightCountFlag = true;
        }

        sSimpleMovingAverage = sTotal / sMovingWindowSize;
        sWeightedMovingAverage = sWeightTotal / sWeightAverageSize;

        return sSimpleMovingAverage;
    }

    public static void setMovingWindowSize(int n) {
        if (n < 1) {
            return;
        }
        if (sMovingWindowSize == n) {
            return;
        }
        sMovingWindowSize = n;
        sWeightAverageSize = n * (n + 1) / 2;
        sBuffer = new float[sMovingWindowSize];
        sWeightCountFlag = false;
        sTotal = 0;
        sWeightTotal = 0;
        sIndex = 0;
    }


    public static float getWeightedMovingAverage() {
        return sWeightedMovingAverage;
    }

    public static float getSimpleMovingAverage() {
        return sSimpleMovingAverage;
    }

    //it will be used in the future
    /*unused*/
    public static void countSmileDegree(float happiness) {
        Log.i(LOG_TAG, "sCountFlag = " + sCountFlag + " happiness = " + happiness);
        if (!sCountFlag) {
            return;
        }
        if (happiness < SMILE_DEGREE_L1) {
            sSmileCounts[0]++;
        } else if (SMILE_DEGREE_L1 <= happiness && happiness < SMILE_DEGREE_L2) {
            sSmileCounts[1]++;
        } else if (SMILE_DEGREE_L2 <= happiness && happiness < SMILE_DEGREE_L3) {
            sSmileCounts[2]++;
        } else {
            sSmileCounts[3]++;
        }
    }
    //it will be used in the future
    /*unused*/
    public static void startCount() {
        countClear();
        sTimeStampStart = System.currentTimeMillis();
        sCountFlag = COUNT_START;
    }
    //it will be used in the future
    /*unused*/
    public static float[] stopCount() {
        sTimeStampEnd = System.currentTimeMillis();
        sCountFlag = COUNT_END;
        return sSmileCounts;
    }
    //it will be used in the future
    /*unused*/
    public static long getRecordMin() {
        return (sTimeStampEnd - sTimeStampEnd) / 6000;
    }

    private static void countClear() {
        for (int i = 0; i < sSmileCounts.length; i++) {
            sSmileCounts[i] = 0;
        }
    }
}
