package com.sensormonitor.plot;

import android.graphics.Color;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.LegendRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

public class BatteryChargePlot {
    private static final int HORIZONTAL_VIEWPORT_RESOLUTION = 30;
    private static final int HORIZONTAL_GRID_DENSITY = 6;
    private static final int VERTICAL_VIEWPORT_RESOLUTION = 100;
    private static final int VERTICAL_GRID_DENSITY = 3;
    private static final int PRESERVED_POINTS_NUMBER = 1000;

    private int mCounter = 0;
    private GraphView mGraphView;
    private LineGraphSeries<DataPoint> mGraphSeries;

    public BatteryChargePlot(GraphView exObject){
        mGraphView = exObject;

        resetPlot();
    }

    public void resetPlot(){
        mGraphView.getViewport().setMinX(0);
        mGraphView.getViewport().setMaxX(HORIZONTAL_VIEWPORT_RESOLUTION);
        mGraphView.getViewport().setYAxisBoundsManual(true);

        mGraphView.getViewport().setMinY(0);
        mGraphView.getViewport().setMaxY(VERTICAL_VIEWPORT_RESOLUTION);
        mGraphView.getViewport().setXAxisBoundsManual(true);

        mGraphView.getGridLabelRenderer().setNumHorizontalLabels(HORIZONTAL_GRID_DENSITY);
        mGraphView.getGridLabelRenderer().setNumVerticalLabels(VERTICAL_GRID_DENSITY);

        mGraphView.getLegendRenderer().setVisible(true);
        mGraphView.getLegendRenderer().setTextSize(50);
        mGraphView.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.TOP);
        mGraphView.getLegendRenderer().setBackgroundColor(Color.parseColor("#CCFFFFFF"));

        resetData();
    }

    public void resetData(){
        mGraphSeries = new LineGraphSeries<DataPoint>();
        mGraphSeries.setColor(Color.RED);
        mGraphSeries.setTitle("%");

        mGraphView.removeAllSeries();
        mGraphView.addSeries(mGraphSeries);

        mCounter = 0;
    }

    public void drawValue(double value){
        double x = mCounter++;
        double y = value;
        mGraphSeries.appendData(new DataPoint(x, y),
                x > HORIZONTAL_VIEWPORT_RESOLUTION ? true : false,PRESERVED_POINTS_NUMBER);
    }
}
