package com.asus.zenheart.smilemirror;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;

//TODO this is the output view for the statistics in the future
public class HistogramChart extends View {
    private static final String LOG_TAG = "HistogramChart";
    private static final float TEXT_SIZE_DP = 16f;
    private static final float STROKE_WIDTH = 1f;
    /**
     * Used to draw histogram bar
     */
    private static final int PAINT_COLOR[] = new int[]{
            R.color.smile_level_one_face_color,
            R.color.smile_level_two_face_color,
            R.color.smile_level_three_face_color,
            R.color.smile_level_four_face_color
    };
    private static final int IMAGES_ID[] = new int[]{
            R.drawable.chart_l1,
            R.drawable.chart_l2,
            R.drawable.chart_l3,
            R.drawable.chart_l4,
    };

    private float mProportion[] = {0, 0, 0, 0};

    private Context mContext;
    private Resources mResources;

    private float mPaddingWidth;
    private float mBarWidth;
    private float mBarHeight;
    private float mBarImageHeight;
    private float mTextHeight;
    private Paint mPaint;
    private Paint mTextPaint;

    public HistogramChart(Context context) {
        super(context);
        initHistogramChart(context);

    }

    public HistogramChart(Context context, AttributeSet attrs) {
        super(context, attrs);
        initHistogramChart(context);
    }

    public HistogramChart(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initHistogramChart(context);
    }

    private void initHistogramChart(Context context) {
        mContext = context;
        mResources = context.getResources();
        mPaddingWidth = mResources.getDimension(R.dimen.vertical_text_view_width_padding);
        mBarWidth = mResources.getDimension(R.dimen.histogram_chart_bar_width);
        mBarHeight = mResources.getDimension(R.dimen.histogram_chart_bar_height);
        mBarImageHeight = mResources.getDimension(R.dimen.histogram_chart_bar_image_height);
        mPaint = new Paint();
        mTextPaint = new Paint();
        final float textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DP,
                mResources.getDisplayMetrics());
        mTextPaint.setTextSize(textSize);
        mTextPaint.setStrokeWidth(STROKE_WIDTH);
        mTextPaint.setColor(Color.WHITE);
        mTextHeight = getFontHeight(mTextPaint);
    }

    public void onDraw(Canvas canvas) {
        Log.i(LOG_TAG, "onDraw ....");
        final float height = getHeight();
        final float width = getWidth() - mPaddingWidth * 2;
        final float gap = countSpaceGap(width);
        float x = mPaddingWidth;
        for (int i = 0; i < PAINT_COLOR.length; i++) {
            final int drawColor = ContextCompat.getColor(mContext, PAINT_COLOR[i]);
            float y = height - mProportion[i] * mBarHeight / 100;
            //draw histogram bar
            mPaint.setColor(drawColor);
            canvas.drawRect(x, y, x + mBarWidth, height, mPaint);
            //TODO: fix in the future with the inBitmap
            //draw bar image
            Bitmap bitmap = BitmapFactory.decodeResource(mResources, IMAGES_ID[i]);
            canvas.drawBitmap(bitmap, x, y - mBarImageHeight, null);
            //draw proportion value
            canvas.drawText(String.format(" %d%%", (int)(mProportion[i]+0.5)), x , y - mBarImageHeight +
                    mTextHeight, mTextPaint);
            x = x + mBarWidth + gap;
        }
        //draw a line at the top of the view
        canvas.drawLine(mPaddingWidth, 1, mPaddingWidth + width, 1, mTextPaint);
    }

    /**
     * Count the space gap between each bar
     *
     * @param width The actual view width excluding the padding width
     * @return The space gap between each bar
     */
    private float countSpaceGap(float width) {
        final float spaceLeft = width - mBarWidth * mProportion.length;
        return (spaceLeft < 0) ? 0 : spaceLeft / (mProportion.length - 1);
    }



    /**
     * Set the statistics data
     * @param data The statistics values in percent,valued from 0~100
     */
    public void setData(float data[]) {
        //redraw
        mProportion = data;
        invalidate();
    }

    /**
     * to measure the height of a word with this method
     *
     * @param paint the textSize
     * @return an integer corresponding to row space
     */
    private static float getFontHeight(Paint paint) {
        Paint.FontMetrics fm = paint.getFontMetrics();
        return (float) Math.ceil(fm.bottom - fm.top);
    }
}

