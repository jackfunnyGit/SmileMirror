package com.asus.zenheart.smilemirror;


import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;

import java.util.ArrayList;


public class ModePagerAdapter extends PagerAdapter {

    private static final String LOG_TAG = "ModePagerAdapter";


    private Context mContext;
    private ArrayList<View> mViewList;
    private LayoutInflater mLayoutInflater;
    private int[] mIndexOfLayout;

    //TODO adjust in the future
    private boolean mRecordFlag;
    private HistogramChart mHistogramChart;//used in the future

    public interface ActivityCallback {

        void hideHistogramChart();

        void showHistogramChart();
    }

    private ActivityCallback mCallback;


    public ModePagerAdapter(Context context, int[] index, HistogramChart histogramChart) {
        mContext = context;
        mLayoutInflater = LayoutInflater.from(mContext);
        mIndexOfLayout = index;
        mHistogramChart = histogramChart;
        mViewList = new ArrayList<>(index.length);
        if (context instanceof ActivityCallback) {
            mCallback = (ActivityCallback) context;
        }
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        View view = initView(container, position);
        container.addView(view);
        return view;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView(mViewList.get(position));
    }

    @Override
    public int getCount() {
        return mIndexOfLayout.length;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    private View initView(ViewGroup container, int position) {
        View view = null;
        if (mIndexOfLayout[position] == R.layout.smile_mode) {
            view = mLayoutInflater.inflate(R.layout.smile_mode, container, false);

        } else if (mIndexOfLayout[position] == R.layout.coach_mode) {
            view = mLayoutInflater.inflate(R.layout.coach_mode, container, false);

            final ImageView imageViewRecord = (ImageView) view.findViewById(R.id.video);
            final ImageView imageViewRecordText = (ImageView) view.findViewById(R.id.rec_text);
            final ImageView imageViewPlay = (ImageView) view.findViewById(R.id.image_play);
            final VerticalScrollTextView verticalScrollTextView =
                    (VerticalScrollTextView) view.findViewById(R.id.vertical_scroll_textview);
            final Animation blinkAnimation = AnimationUtil.blinkFactory();

            if (imageViewPlay != null) {
                imageViewPlay.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mRecordFlag) {
                            imageViewRecord.setVisibility(View.VISIBLE);
                            imageViewRecordText.setVisibility(View.INVISIBLE);
                            imageViewRecordText.setAnimation(null);
                            imageViewPlay.setImageResource(R.drawable.play);
                            mRecordFlag = false;
                            if (mCallback != null) {
                                mCallback.showHistogramChart();
                            }
                            verticalScrollTextView.stop();
                        } else {
                            imageViewRecord.setVisibility(View.INVISIBLE);
                            imageViewRecordText.setVisibility(View.VISIBLE);
                            imageViewRecordText.setAnimation(blinkAnimation);
                            imageViewPlay.setImageResource(R.drawable.stop);
                            verticalScrollTextView.start();

                            mRecordFlag = true;
                        }
                    }
                });
            }

        } else {
            Log.e(LOG_TAG, "UNSPECIFIED mode is not expected !!!");
        }

        if (view != null) {
            mViewList.add(view);
        }
        return view;
    }



}

