package com.asus.zenheart.smilemirror;

import android.util.Log;

public class SmileDegreeCounter {

    private static final String LOG_TAG = "SmileDegreeCounter";
    private static final int DEFAULT_WINDOW_SIZE = 1;
    /**
     * Smile degree is classified into four levels
     */
    private static final int SMILE_DEGREE_LEVELS = 4;

    private int mMovingWindowSize = DEFAULT_WINDOW_SIZE;
    private int mWeightAverageSize = DEFAULT_WINDOW_SIZE * (DEFAULT_WINDOW_SIZE + 1) / 2;

    private float[] mBuffer;
    private int mIndex = 0;
    private float mTotal = 0;
    private float mWeightTotal = 0;

    //this is for the first time to count with weight
    private boolean mIsFirstCountWeight;

    private float mSimpleMovingAverage;
    private float mWeightedMovingAverage;

    private int[] mSmileCounts = new int[SMILE_DEGREE_LEVELS];
    private boolean mIsRecording;

    private static final float SMILE_LEVEL_L4 = 0.7f;
    private static final float SMILE_LEVEL_L3 = 0.5f;
    private static final float SMILE_LEVEL_L2 = 0.3f;
    private static final float SMILE_LEVEL_L1 = 0;

    public SmileDegreeCounter() {
        setMovingWindowSize(DEFAULT_WINDOW_SIZE);
    }

    public SmileDegreeCounter(int movingWindowSize) {
        setMovingWindowSize(movingWindowSize);
    }

    private void clearRecordingCount() {
        for (int i = 0; i < mSmileCounts.length; i++) {
            mSmileCounts[i] = 0;
        }

    }

    /**
     * Sets the windowSize and reset all the buffer data
     *
     * @param n The windowSize
     */
    private void setMovingWindowSize(int n) {
        if (n < 1) {
            return;
        }
        if (mMovingWindowSize == n) {
            return;
        }
        mMovingWindowSize = n;
        mWeightAverageSize = n * (n + 1) / 2;
        mBuffer = new float[mMovingWindowSize];
        mIsFirstCountWeight = true;
        mTotal = 0;
        mWeightTotal = 0;
        mIndex = 0;
    }

    private void recordingCount(float happiness) {

        if (happiness < SMILE_LEVEL_L2) {
            mSmileCounts[0]++;
        } else if (SMILE_LEVEL_L2 <= happiness && happiness < SMILE_LEVEL_L3) {
            mSmileCounts[1]++;
        } else if (SMILE_LEVEL_L3 <= happiness && happiness < SMILE_LEVEL_L4) {
            mSmileCounts[2]++;
        } else {
            mSmileCounts[3]++;
        }
    }

    private void startRecordingCount() {
        clearRecordingCount();
        mIsRecording = true;
    }

    private void stopRecordingCount() {
        mIsRecording = false;
    }

    /**
     * this method counts {@param happiness} into two average ways,for example,there are N elements
     * SimpleMovingAverage (SMA) : accumulate N elements values and divided by N ,
     * ex :(N1+N2+....Nn)/N, which is stored at {@link #mSimpleMovingAverage}
     * WeightedMovingAverage (WMA) : accumulate elements multiplied by N before accumulation,
     * ex:(1*N1+2*N2+....N*Nn)/(1+2+...N), which is store at {@link #mWeightedMovingAverage}
     *
     * @param happiness the score of smile degree,valued from 0 to 1,and -1 if recognized as not
     *                  smile
     * @return the results of SMA,mSimpleMovingAverage
     */
    private float countByMovingAverage(float happiness) {
        if (mBuffer == null) {
            mBuffer = new float[mMovingWindowSize];
        }

        //TO calculate the mWeightTotal
        if (mIsFirstCountWeight) {
            mWeightTotal = mWeightTotal + happiness * (mIndex + 1);
        } else {
            mWeightTotal = mWeightTotal - mTotal;
            mWeightTotal = mWeightTotal + happiness * mMovingWindowSize;
        }
        //TO calculate the mTotal with the new happiness
        //minus first value
        mTotal = mTotal - mBuffer[mIndex];
        //replace mBuffer[mIndex] with new happiness value and add to total
        mBuffer[mIndex] = happiness;
        //add new value to mTotal
        mTotal = mTotal + mBuffer[mIndex];

        mIndex++;
        if (mIndex > mMovingWindowSize - 1) {
            mIndex = mIndex % mMovingWindowSize;
            mIsFirstCountWeight = false;
        }

        mSimpleMovingAverage = mTotal / mMovingWindowSize;
        mWeightedMovingAverage = mWeightTotal / mWeightAverageSize;

        return mSimpleMovingAverage;
    }

    public float getWeightedMovingAverage() {
        return mWeightedMovingAverage;
    }

    public float getSimpleMovingAverage() {
        return mSimpleMovingAverage;
    }


    public int[] getSmileCounts() {
        return mSmileCounts;
    }

    public float[] getSmileCountsPercent() {
        float[] smilePercent = new float[mSmileCounts.length];
        final int totalCounts = sum(mSmileCounts);
        if (totalCounts == 0) {
            return smilePercent;
        }
        for (int i = 0; i < smilePercent.length; i++) {
            smilePercent[i] = (float) mSmileCounts[i] * 100 / totalCounts;
            Log.d(LOG_TAG, "smilePercent is = " + smilePercent[i]);
        }
        return smilePercent;
    }

    /**
     * Sets the smileDegree to count the average values
     *
     * @param smileDegree The smileDegree about face's smiling
     */
    public SmileDegreeCounter setSmileDegree(float smileDegree) {
        final float smileAverage = countByMovingAverage(smileDegree);
        if (mIsRecording) {
            recordingCount(smileAverage);
        }
        return this;
    }

    /**
     * Sets the Counter recording flag
     *
     * @param isRecording The recoding flag representing if recording now
     */
    public SmileDegreeCounter setIsRecording(boolean isRecording) {
        if (mIsRecording && !isRecording) {
            //counter is recording now,and user click stop recording
            stopRecordingCount();
        } else if (!mIsRecording && isRecording) {
            //counter is not recording , and user click start recording
            startRecordingCount();
        }
        return this;
    }

    private static int sum(int data[]) {
        int sum = 0;
        for (int value : data) {
            sum = sum + value;
        }
        return sum;
    }
}
