package com.nordicsemi.reefBrite;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

public class BrightnessActivity extends Activity {
    private Button btnBack, btnBackButtom;
    private SeekBar blue, white;
    public static Activity brightnessActivity;
    private TextView percentage1, percentage2;
    public static final String TAG = "BrightnessActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.brightness_controller);
        MainActivity.activityRunningState=3;
        brightnessActivity = this;

        btnBack = (Button) findViewById(R.id.go_back_btn_3);
        blue = (SeekBar) findViewById(R.id.blue_bar);
        white = (SeekBar) findViewById(R.id.white_bar);
        btnBackButtom = (Button) findViewById(R.id.btn_back);
        percentage1 = (TextView) findViewById(R.id.percentage1);
        percentage2 = (TextView) findViewById(R.id.percentage2);

        blue.setProgress(MainActivity.currBlueValue);
        percentage1.setText((int)(MainActivity.currBlueValue/2.55)+"%");
        white.setProgress(MainActivity.currWhiteValue);
        percentage2.setText((int)(MainActivity.currWhiteValue/2.55)+"%");

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

        btnBackButtom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.indexEdit = -1;
                MainActivity.activityRunningState=0;
                byte[] value = new byte[]{7};
                MainActivity.mService.writeRXCharacteristic(value);
                finish();
            }
        });


        blue.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                percentage1.setText((int)(i/2.55)+"%");
                byte[] value = new byte[2];
                value[0] = 3;
                value[1] = (byte)i;
                MainActivity.mService.writeRXCharacteristic(value);
                Log.d(TAG, "blue brightness = "+i);
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
                percentage2.setText((int)(i/2.55)+"%");
                byte[] value = new byte[2];
                value[0] = 2;
                value[1] = (byte)i;
                MainActivity.mService.writeRXCharacteristic(value);
                Log.d(TAG, "white brightness = "+i);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

    }
}
