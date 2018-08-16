package com.nordicsemi.nrfUARTv2;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;

import java.util.ArrayList;

public class ChartActivity extends Activity {

    private Button btnBack, btnSimu;
    private LineChart chart;
    private boolean timeIsRunning;
    private int time;
    public static Activity chartActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chart_controller);
        chartActivity = this;
        MainActivity.activityRunningState=1;

        btnBack = (Button) findViewById(R.id.go_back_btn_1);
        btnSimu = (Button) findViewById(R.id.btn_simulation);

        if(MainActivity.pApter.getCount() > 0){
            createGraph();
        }

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.activityRunningState=0;
                finish();
            }
        });

        btnSimu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                time = 0;
                timeIsRunning = true;
                final AlertDialog alertDialog = new AlertDialog.Builder(ChartActivity.this).create();
                alertDialog.setCanceledOnTouchOutside(false);
                alertDialog.setTitle("SIMULATION!");
                alertDialog.setMessage("0:00");
                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                timeIsRunning = false;
                                byte[] value = new byte[]{7};
                                MainActivity.mService.writeRXCharacteristic(value);
                                dialog.dismiss();
                            }
                        });
                alertDialog.show();
                TextView messageText = (TextView)alertDialog.findViewById(android.R.id.message);
                messageText.setGravity(Gravity.CENTER);
                messageText.setTextSize(40);

                Runnable myRunnable = new Runnable(){

                    public void run(){
                        while(timeIsRunning){
                            final int hour = time/60;
                            final int minu = time%60;
                            if(hour != 24){
                                runOnUiThread(new Runnable() {
                                    public void run() {
                                        if(minu<10){
                                            alertDialog.setMessage(hour+":0"+minu);
                                        }else{
                                            alertDialog.setMessage(hour+":"+minu);
                                        }
                                    }
                                });
                                time++;
                                byte[] value = new byte[]{4,(byte)hour, (byte)minu};
                                MainActivity.mService.writeRXCharacteristic(value);
                            }
                            try {
                                Thread.sleep(40);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                };

                Thread thread = new Thread(myRunnable);
                thread.start();
            }
        });
    }

    public void createGraph(){
        chart = (LineChart) findViewById(R.id.line_chart);

        ArrayList<Entry> blueEntries = new ArrayList<>();
        ArrayList<Entry> whiteEntries = new ArrayList<>();

        float startPointYB, startPointYW;
        PointModel pStart = MainActivity.pApter.getItem(0);
        PointModel pEnd = MainActivity.pApter.getItem(MainActivity.pApter.getCount()-1);

        if(Float.parseFloat(pStart.getHour()+"."+pStart.getMinu()) !=0){
            if(MainActivity.pApter.getCount()==1){
                startPointYB = (float)(pStart.getBlueV()/2.55);
                startPointYW = (float)(pStart.getWhiteV()/2.55);
            }else{
                float x1 = pStart.getHour()*60 + pStart.getMinu();
                float x2 = pEnd.getHour()*60 + pEnd.getMinu();
                float yb1 = (float)(pStart.getBlueV()/2.55);
                float yb2 = (float)(pEnd.getBlueV()/2.55);
                float yw1 = (float)(pStart.getWhiteV()/2.55);
                float yw2 = (float)(pEnd.getWhiteV()/2.55);
                startPointYB = yb1 - x1 * ((yb1 - yb2)/(24*60 - x2 + x1));
                startPointYW = yw1 - x1 * ((yw1 - yw2)/(24*60 - x2 + x1));

            }

            blueEntries.add(new Entry(0, startPointYB));
            whiteEntries.add(new Entry(0, startPointYW));
        }else{
            startPointYB = (float)(pStart.getBlueV()/2.55);
            startPointYW = (float)(pStart.getWhiteV()/2.55);
        }


        for(int i=0; i<MainActivity.pApter.getCount(); i++){
            PointModel pModel = MainActivity.pApter.getItem(i);
            float x = Float.parseFloat(pModel.getHour()+"."+pModel.getMinu());
            float yb = (float)(pModel.getBlueV()/2.55);
            float yw = (float)(pModel.getWhiteV()/2.55);
            blueEntries.add(new Entry(x, yb));
            whiteEntries.add(new Entry(x, yw));
        }

        blueEntries.add(new Entry(24, startPointYB));
        whiteEntries.add(new Entry(24, startPointYW));

        LineDataSet blueDataSet = new LineDataSet(blueEntries, "Channel1");
        blueDataSet.setColor(ContextCompat.getColor(this, R.color.blue));
        blueDataSet.setValueTextColor(ContextCompat.getColor(this, R.color.black));

        LineDataSet whiteDataSet = new LineDataSet(whiteEntries, "Channel2");
        whiteDataSet.setColor(ContextCompat.getColor(this, R.color.white));
        whiteDataSet.setValueTextColor(ContextCompat.getColor(this, R.color.black));

        //****
        // Controlling X axis
        XAxis xAxis = chart.getXAxis();
        // Set the xAxis position to bottom. Default is top
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        //Customizing x axis value
        final String[] time = new String[25];

        for(int i=0; i<time.length; i++){
            time[i] = "" + i;
        }
        IAxisValueFormatter formatter = new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                return time[(int) value];
            }
        };


        xAxis.setGranularity(1f); // minimum axis-step (interval) is 1
        xAxis.setValueFormatter(formatter);
        //***
        // Controlling right side of y axis
        YAxis yAxisRight = chart.getAxisRight();
        yAxisRight.setGranularity(1f);
        yAxisRight.setStartAtZero(true);
        yAxisRight.setAxisMaxValue(100);
        YAxis yAxisLeft = chart.getAxisLeft();
        yAxisLeft.setGranularity(1f);
        yAxisLeft.setStartAtZero(true);
        yAxisLeft.setAxisMaxValue(100);

        // Setting Data
        LineData data = new LineData();
        data.addDataSet(blueDataSet);
        data.addDataSet(whiteDataSet);
        chart.setData(data);
        chart.setVisibleXRange(0,24);
        //refresh
        chart.invalidate();
    }
}
