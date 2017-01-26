package com.asus.zenheart.smilemirror;


import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

//TODO this is the output view for the statistics in the future
public class HistogramChart extends View {

    public HistogramChart(Context context) {
        super(context);
        initHistogramChart();

    }

    public HistogramChart(Context context, AttributeSet attrs) {
        super(context, attrs);
        initHistogramChart();
    }

    public HistogramChart(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initHistogramChart();
    }

    private void initHistogramChart() {

    }

    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }

    public void setData(float data[]) {
        //redraw
        invalidate();
    }

    private float sum(float data[]) {
        float sum = 0;
        for (float value : data) {
            sum = sum + value;
        }
        return sum;
    }
}

