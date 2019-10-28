package com.example.sensormonitor;

import android.graphics.Color;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.LegendRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

public class TemperaturePlot {

    private static final int HORIZONTAL_VIEWPORT_RESOLUTION = 45;
    private static final int HORIZONTAL_GRID_DENSITY = 3;

    private static final int VERTICAL_VIEWPORT_RESOLUTION = 50;
    private static final int VERTICAL_GRID_DENSITY = 50;

    private static final int PRESERVED_POINTS_NUMBER = 1000;

    private GraphView mTempGv;
    private LineGraphSeries<DataPoint> mTempSeries;

    TemperaturePlot(GraphView exObject){
        mTempGv = exObject;

        mTempGv.getViewport().setMinX(0);
        mTempGv.getViewport().setMaxX(HORIZONTAL_VIEWPORT_RESOLUTION);
        mTempGv.getViewport().setYAxisBoundsManual(true);

        mTempGv.getViewport().setMinY(0);
        mTempGv.getViewport().setMaxY(VERTICAL_VIEWPORT_RESOLUTION);
        mTempGv.getViewport().setXAxisBoundsManual(true);

        mTempGv.getGridLabelRenderer().setNumHorizontalLabels(HORIZONTAL_GRID_DENSITY);
        mTempGv.getGridLabelRenderer().setNumVerticalLabels(VERTICAL_GRID_DENSITY);

        mTempGv.getLegendRenderer().setVisible(true);
        mTempGv.getLegendRenderer().setTextSize(50);
        mTempGv.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.TOP);
        mTempGv.getLegendRenderer().setBackgroundColor(Color.parseColor("#CCFFFFFF"));

        resetData();
    }

    public void resetData(){
        mTempSeries = new LineGraphSeries<>();
        mTempSeries.setColor(Color.RED);
        mTempSeries.setTitle("t°C");

        mTempGv.removeAllSeries();
        mTempGv.addSeries(mTempSeries);
    }

    public void drawPoint(double x, double y){
        mTempSeries.appendData(new DataPoint(x, y),
                x > HORIZONTAL_VIEWPORT_RESOLUTION ? true : false,PRESERVED_POINTS_NUMBER);
    }
}
