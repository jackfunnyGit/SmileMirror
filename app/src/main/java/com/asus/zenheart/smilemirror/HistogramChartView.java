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
import android.view.View;

import com.asus.zenheart.smilemirror.Util.LogUtils;

import java.util.Locale;

public class HistogramChartView extends View {
    private static final String LOG_TAG = "HistogramChart";
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

    private float mPaddingTop;
    private float mPaddingWidth;
    private float mBarImageHeight;
    private float mBarImageWidth;

    private float mTextHeight;
    private Paint mPaint;
    private Paint mTextPaint;

    private BitmapFactory.Options mOptions;

    public HistogramChartView(Context context) {
        super(context);
        initHistogramChart(context);
    }

    public HistogramChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initHistogramChart(context);
    }

    public HistogramChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initHistogramChart(context);
    }

    private void initHistogramChart(Context context) {
        mContext = context;
        mResources = context.getResources();
        mPaddingWidth = mResources.getDimension(R.dimen.vertical_text_view_width_padding);
        mPaddingTop = mResources.getDimension(R.dimen.sm_histogram_chart_top_padding);
        mPaint = new Paint();
        mTextPaint = new Paint();
        mOptions = new BitmapFactory.Options();
        mOptions.inScaled = false;
        mTextPaint.setTextSize(
                mResources.getDimensionPixelSize(R.dimen.sm_histogram_chart_text_size));
        mTextPaint.setStrokeWidth(STROKE_WIDTH);
        mTextPaint.setColor(Color.WHITE);
        mTextHeight = getFontHeight(mTextPaint);
        initBarImageParm();

    }

    public void onDraw(Canvas canvas) {
        final float viewHeight = getHeight();
        final float viewWidth = getWidth() - mPaddingWidth * 2;
        final float gap = countSpaceGap(viewWidth);
        final float maxBarHeight = countMaxBarHeight(viewHeight, mBarImageHeight);

        float x = mPaddingWidth;
        for (int i = 0; i < PAINT_COLOR.length; i++) {
            final int drawColor = ContextCompat.getColor(mContext, PAINT_COLOR[i]);
            float y = viewHeight - mProportion[i] * maxBarHeight / 100;
            //draw histogram bar
            mPaint.setColor(drawColor);
            canvas.drawRect(x, y, x + mBarImageWidth, viewHeight, mPaint);
            //TODO: fix in the future with the inBitmap
            //draw bar image
            Bitmap bitmap = BitmapFactory.decodeResource(mResources, IMAGES_ID[i], mOptions);
            final int barImageHeight = bitmap.getHeight();
            canvas.drawBitmap(bitmap, x, y - barImageHeight, null);
            //draw proportion text
            canvas.drawText(String.format(Locale.US, " %d%%", (int) (mProportion[i] + 0.5)), x,
                    y - barImageHeight + mTextHeight, mTextPaint);
            x = x + mBarImageWidth + gap;
        }
        //draw a line at the top of the view
        canvas.drawLine(mPaddingWidth, 1, mPaddingWidth + viewWidth, 1, mTextPaint);
    }

    /**
     * Count the space gap between each bar
     *
     * @param width The actual view width excluding the padding width
     * @return The space gap between each bar
     */
    private float countSpaceGap(float width) {
        final float spaceLeft = width - mBarImageWidth * mProportion.length;
        return (spaceLeft < 0) ? 0 : spaceLeft / (mProportion.length - 1);
    }

    private float countMaxBarHeight(float viewHeight, float imageHeight) {
        float maxBarHeight = viewHeight - imageHeight - mPaddingTop;
        return (maxBarHeight < imageHeight) ? imageHeight : maxBarHeight;
    }

    private void initBarImageParm() {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(mResources, IMAGES_ID[0], options);
        mBarImageWidth = options.outWidth;
        mBarImageHeight = options.outHeight;
        LogUtils.d(LOG_TAG,
                "BarImageWidth = " + mBarImageWidth + "BarImageHeight = " + mBarImageHeight);
    }

    /**
     * Set the statistics data
     *
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

