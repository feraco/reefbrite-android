package com.nordicsemi.nrfUARTv2;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TimePicker;

import java.util.Comparator;

public class PointActivity extends Activity {

    private Button btnBack, btnSave;
    private TimePicker picker;
    private SeekBar blue, white;
    public static Activity pointActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.point_controller);
        pointActivity = this;
        MainActivity.activityRunningState=2;

        btnBack = (Button) findViewById(R.id.go_back_btn_2);
        btnSave = (Button) findViewById(R.id.btn_save);
        picker=(TimePicker)findViewById(R.id.timePicker);
        blue = (SeekBar) findViewById(R.id.blue_bar);
        white = (SeekBar) findViewById(R.id.white_bar);

        if(MainActivity.indexEdit != -1){
            PointModel pModel = MainActivity.pApter.getItem(MainActivity.indexEdit);
            picker.setCurrentHour(pModel.getHour());
            picker.setCurrentMinute(pModel.getMinu());
            blue.setProgress(pModel.getBlueV());
            white.setProgress(pModel.getWhiteV());
        }else{
            blue.setProgress(MainActivity.currBlueValue);
            white.setProgress(MainActivity.currWhiteValue);
        }

        byte[] value = new byte[2];
        value[0] = 3;
        value[1] = (byte)blue.getProgress();
        MainActivity.mService.writeRXCharacteristic(value);
        value[0] = 2;
        value[1] = (byte)white.getProgress();
        MainActivity.mService.writeRXCharacteristic(value);

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.indexEdit = -1;
                MainActivity.activityRunningState=0;
                byte[] value = new byte[]{7};
                MainActivity.mService.writeRXCharacteristic(value);
                finish();
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean isDuplicate = false;

                for(int i=0; i<MainActivity.pApter.getCount(); i++ ){
                    if(MainActivity.pApter.getItem(i).getHour()==picker.getCurrentHour() && MainActivity.pApter.getItem(i).getMinu()==picker.getCurrentMinute()){
                        isDuplicate = true;
                        break;
                    }
                }

                if(MainActivity.indexEdit != -1){
                    PointModel pModel = MainActivity.pApter.getItem(MainActivity.indexEdit);
                    if(!isDuplicate){
                        pModel.setHour(picker.getCurrentHour());
                        pModel.setMinu(picker.getCurrentMinute());
                        pModel.setBlueV(blue.getProgress());
                        pModel.setWhiteV(white.getProgress());
                        sortAdpter(MainActivity.pApter);
                        MainActivity.pApter.notifyDataSetChanged();
                        MainActivity.indexEdit = -1;
                        MainActivity.activityRunningState=0;
                        finish();
                    }else{
                        if(pModel.getHour()==picker.getCurrentHour() && pModel.getMinu()==picker.getCurrentMinute()) {
                            pModel.setBlueV(blue.getProgress());
                            pModel.setWhiteV(white.getProgress());
                            MainActivity.pApter.notifyDataSetChanged();
                            MainActivity.indexEdit = -1;
                            MainActivity.activityRunningState=0;
                            finish();
                        }else{
                            AlertDialog alertDialog = new AlertDialog.Builder(PointActivity.this).create();
                            alertDialog.setTitle("ERROR!");
                            alertDialog.setMessage("The time already set in the schedule.");
                            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Close",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                        }
                                    });
                            alertDialog.show();
                        }
                    }

                }else{
                    if(!isDuplicate){
                        MainActivity.pApter.add(new PointModel(picker.getCurrentHour(), picker.getCurrentMinute(), blue.getProgress(), white.getProgress()));
                        MainActivity.indexEdit = -1;
                        sortAdpter(MainActivity.pApter);
                        MainActivity.pApter.notifyDataSetChanged();
                        MainActivity.activityRunningState=0;
                        finish();
                    }else{
                        AlertDialog alertDialog = new AlertDialog.Builder(PointActivity.this).create();
                        alertDialog.setTitle("ERROR!");
                        alertDialog.setMessage("The time already set in the schedule.");
                        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Close",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                });
                        alertDialog.show();
                    }
                }

                MainActivity.sendEvent();
            }
        });

        blue.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                byte[] value = new byte[2];
                value[0] = 3;
                value[1] = (byte)i;
                MainActivity.mService.writeRXCharacteristic(value);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        white.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                byte[] value = new byte[2];
                value[0] = 2;
                value[1] = (byte)i;
                MainActivity.mService.writeRXCharacteristic(value);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

    }

    public void sortAdpter(PointAdapter pApter){
        pApter.sort(new Comparator<PointModel>() {
            @Override
            public int compare(PointModel p1, PointModel p2) {
                return p1.getTimeInMinu()-p2.getTimeInMinu();   //or whatever your sorting algorithm
            }
        });
    }
}
