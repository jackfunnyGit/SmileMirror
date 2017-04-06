package com.asus.zenheart.smilemirror;

import android.content.Context;
import android.content.res.Resources;
import android.content.Intent;
import android.support.v4.view.PagerAdapter;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.asus.zenheart.smilemirror.Util.AnimationUtil;
import com.asus.zenheart.smilemirror.editor.SpeechEditorActivity;
import java.util.ArrayList;

public class ModePagerAdapter extends PagerAdapter {

    private static final String LOG_TAG = "ModePagerAdapter";

    private Context mContext;
    private ArrayList<View> mViewList;
    private LayoutInflater mLayoutInflater;
    private ViewGroup mContainer;
    private int[] mIndexOfLayout;

    private boolean mIsRecording;
    private boolean mIsCountingDown;

    public interface ActivityCallback {

        void hideChartPage();

        void showChartPage();

        void startRecord();

        void stopRecord();

    }

    private ActivityCallback mCallback;

    public ModePagerAdapter(Context context, int[] index, ViewGroup container) {

        mContext = context;
        mContainer = container;
        mLayoutInflater = LayoutInflater.from(mContext);
        mIndexOfLayout = index;
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
            final ImageView imageViewRecord = (ImageView) view.findViewById(R.id.video_image_view);
            final ImageView imageViewRecordText = (ImageView) view.findViewById(R.id.rec_text);
            final ImageView imageViewPlay = (ImageView) view.findViewById(R.id.image_play);
            // ShihJie: Intent to Editor
            final ImageView imageViewList = (ImageView) view.findViewById(R.id.image_list);
            final VerticalScrollTextView verticalScrollTextView =
                    (VerticalScrollTextView) view.findViewById(R.id.vertical_scroll_textview);
            final Animation blinkAnimation = AnimationUtil.blinkFactory();

            if (imageViewList != null) {
                imageViewList.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(mContext, SpeechEditorActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                                Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        mContext.startActivity(intent);
                    }
                });

            }

            if (imageViewPlay != null) {
                imageViewPlay.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mIsRecording) {
                            imageViewRecord.setVisibility(View.VISIBLE);
                            imageViewRecordText.setVisibility(View.INVISIBLE);
                            imageViewRecordText.setAnimation(null);
                            imageViewPlay.setImageResource(R.drawable.play);
                            if (mCallback != null) {
                                mCallback.stopRecord();
                                mCallback.showChartPage();
                            }
                            verticalScrollTextView.stop();
                            mIsRecording = false;
                        } else if (!mIsCountingDown) {
                            final DownCountView downCountView = new DownCountView(mContext) {
                                @Override
                                public void onFinished(View view) {
                                    imageViewRecord.setVisibility(View.INVISIBLE);
                                    imageViewRecordText.setVisibility(View.VISIBLE);
                                    imageViewRecordText.setAnimation(blinkAnimation);
                                    imageViewPlay.setImageResource(R.drawable.stop);
                                    verticalScrollTextView.start(
                                            VerticalScrollTextView.FIRST_DELAY_TIME_MILLS);
                                    if (mCallback != null) {
                                        mCallback.startRecord();
                                    }
                                    mContainer.removeView(view);
                                    mIsRecording = true;
                                    mIsCountingDown = false;
                                }
                            };
                            final Resources resources = mContext.getResources();
                            int height = (int) resources
                                    .getDimension(R.dimen.count_down_page_height_size);
                            int width = (int) resources
                                    .getDimension(R.dimen.count_down_page_width_size);
                            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(width,
                                    height);
                            params.gravity = Gravity.CENTER;
                            downCountView.setLayoutParams(params);
                            mContainer.addView(downCountView);
                            mIsCountingDown = true;
                            downCountView.startCount();
                        }
                    }
                });
            }
            final ImageView dragView = (ImageView) view.findViewById(R.id.image_drag);
            dragView.setOnTouchListener(new View.OnTouchListener() {
                int downY;
                int moveY;
                int scrollY;
                int layoutHeight;
                ViewGroup.LayoutParams param;

                @Override
                public boolean onTouch(View v, MotionEvent event) {

                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            downY = (int) event.getRawY();
                            param = verticalScrollTextView.getLayoutParams();
                            //save the origin layout height when when user touches down
                            layoutHeight = param.height;
                            return true;
                        case MotionEvent.ACTION_MOVE:
                            moveY = (int) event.getRawY();
                            scrollY = moveY - downY;
                            int height = layoutHeight + scrollY;
                            if (height < 0) {
                                height = 0;
                            }
                            //TODO:add height max to prevent pull view out of screen
                            param.height = height;
                            verticalScrollTextView.setLayoutParams(param);
                            return true;
                        case MotionEvent.ACTION_UP:
                            return true;
                    }
                    return true;
                }
            });

        } else {
            Log.e(LOG_TAG, "UNSPECIFIED mode is not expected !!!");
        }

        if (view != null) {
            mViewList.add(view);
        }
        return view;
    }

}

