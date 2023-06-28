
/*
 * Copyright (c) 2015, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.nordicsemi.reefBrite;


import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;


import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class MainActivity extends Activity implements RadioGroup.OnCheckedChangeListener {
    private static final int REQUEST_SELECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    public static final String TAG = "nRFUART";
    private static final int UART_PROFILE_CONNECTED = 20;
    private static final int UART_PROFILE_DISCONNECTED = 21;
    public static int indexEdit = -1;

    private int mState = UART_PROFILE_DISCONNECTED;
    public static UartService mService = null;
    private BluetoothDevice mDevice = null;
    private BluetoothAdapter mBtAdapter = null;
    private ListView messageListView;
    private static ArrayList<PointModel> listAdapter = new ArrayList<PointModel>();
    private Button btnConnectDisconnect, btnBrightness, btnAdd, btnChart, btnName;
    public static PointAdapter pApter;
    public static int activityRunningState = 0; // 0 is MainActivity 1 is ChartActivity 2 is PointActivity 3 is BrightnessActivity
    public static int currBlueValue, currWhiteValue;
    private String whichActivity = "";
    private volatile boolean isFirst = true;
    public static HashMap<String, String> nameMap = new HashMap<String, String>();
    private Context context;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        context = this;

        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }

        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBtAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        messageListView = (ListView) findViewById(R.id.pointMessage);

        pApter= new PointAdapter(listAdapter,getApplicationContext());
        messageListView.setAdapter(pApter);
        messageListView.setDivider(null);
        btnConnectDisconnect = (Button) findViewById(R.id.btn_select);
        btnName = (Button) findViewById(R.id.deviceName);
        btnBrightness = (Button) findViewById(R.id.brightnessButton);
        btnAdd = (Button) findViewById(R.id.addBut);
        btnChart = (Button) findViewById(R.id.timeChartBut);
        service_init();

        btnName.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                showAlertDialogWithTextField();
            }
        });

        // Handle Disconnect & Connect button
        btnConnectDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mBtAdapter.isEnabled()) {
                    Log.i(TAG, "onClick - BT not enabled yet");
                    Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
                } else {
                    if (btnConnectDisconnect.getText().equals("Connect")) {

                        //Connect button pressed, open DeviceListActivity class, with popup windows that scan for devices

                        Intent newIntent = new Intent(MainActivity.this, DeviceListActivity.class);
                        startActivityForResult(newIntent, REQUEST_SELECT_DEVICE);
                        isFirst = true;
                    } else {
                        //Disconnect button pressed
                        if (mDevice != null) {
                            byte[] value = new byte[]{5};
                            mService.writeRXCharacteristic(value);
                            mService.disconnect();
                        }
                    }
                }
            }
        });
        // Handle Send button
        btnBrightness.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                byte[] value = new byte[]{8};
                MainActivity.mService.writeRXCharacteristic(value);
                whichActivity = BrightnessActivity.class.getName();
            }
        });

        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(pApter.getCount()<10) {
                    byte[] value = new byte[]{8};
                    MainActivity.mService.writeRXCharacteristic(value);
                    whichActivity = PointActivity.class.getName();
                }else{
                    androidx.appcompat.app.AlertDialog alertDialog = new androidx.appcompat.app.AlertDialog.Builder(MainActivity.this).create();
                    alertDialog.setTitle("NOTIFICATION!");
                    alertDialog.setMessage("Reach maximum points.");
                    alertDialog.setButton(androidx.appcompat.app.AlertDialog.BUTTON_NEUTRAL, "Close",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                    alertDialog.show();
                }
            }
        });

        btnChart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ChartActivity.class);
                startActivity(intent);
            }
        });

        //load name map
        try {
            nameMap = new Gson().fromJson(getSharedPreferences("test", MODE_PRIVATE).getString("hashString", "oopsDintWork"), new TypeToken<HashMap<String, String>>() {
            }.getType());
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void showAlertDialogWithTextField() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Enter a new name (max 7 characters):");

        final EditText input = new EditText(context);
        builder.setView(input);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String enteredText = input.getText().toString();
                // Handle the entered text
                if(enteredText.length()>7){
                    Toast.makeText(context, "The name exceed 7 characters. Please retry.", Toast.LENGTH_SHORT).show();
                }else if(enteredText.length()==0) {
                    Toast.makeText(context, "The name cannot be empty. Please retry.", Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(context, "Change to the new name: " + enteredText + ". The controller is restarting.", Toast.LENGTH_SHORT).show();
                    if (mDevice != null) {
                        //update name map
                        nameMap.put(mDevice.getAddress()+mDevice.getName(), enteredText);
                        //save name map
                        getSharedPreferences("test", MODE_PRIVATE).edit().putString("hashString", new Gson().toJson(nameMap)).apply();
                        //update name button
                        btnName.setText(enteredText);
                        //send name
                        byte[] textBytes = enteredText.getBytes();
                        byte[] nameArray = new byte[1 + textBytes.length];
                        nameArray[0] = 9;
                        System.arraycopy(textBytes, 0, nameArray, 1, textBytes.length);
                        mService.writeRXCharacteristic(nameArray);
                    }
                }
                // Dismiss the keyboard
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void goToBrightnessActivity(){
        Intent intent = new Intent(MainActivity.this, BrightnessActivity.class);
        startActivity(intent);
    }

    private void goToPointActivity(){
        Intent intent = new Intent(MainActivity.this, PointActivity.class);
        startActivity(intent);
    }

    //UART service connected/disconnected
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder rawBinder) {
            mService = ((UartService.LocalBinder) rawBinder).getService();
            Log.d(TAG, "onServiceConnected mService= " + mService);
            if (!mService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }

        }

        public void onServiceDisconnected(ComponentName classname) {
            ////     mService.disconnect(mDevice);
            mService = null;
        }
    };

    private Handler mHandler = new Handler() {
        @Override

        //Handler events that received from UART service 
        public void handleMessage(Message msg) {

        }
    };

    private final BroadcastReceiver UARTStatusChangeReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            final Intent mIntent = intent;
            //*********************//
            if (action.equals(UartService.ACTION_GATT_CONNECTED)) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        Log.d(TAG, "UART_CONNECT_MSG");
                        btnConnectDisconnect.setText("Disconnect");
                        btnBrightness.setEnabled(true);
                        btnAdd.setEnabled(true);
                        btnChart.setEnabled(true);
                        btnBrightness.setTextColor(Color.parseColor("#007AFF"));
                        btnAdd.setTextColor(Color.parseColor("#007AFF"));
                        btnChart.setTextColor(Color.parseColor("#007AFF"));
                        if(nameMap.containsKey(mDevice.getAddress()+mDevice.getName())){
                            ((Button) findViewById(R.id.deviceName)).setText(nameMap.get(mDevice.getAddress()+mDevice.getName()));
                        }else{
                            ((Button) findViewById(R.id.deviceName)).setText(mDevice.getName());
                        }

                        messageListView.smoothScrollToPosition(listAdapter.size() - 1);
                        mState = UART_PROFILE_CONNECTED;

                    }
                });
            }

            //*********************//
            if (action.equals(UartService.ACTION_GATT_DISCONNECTED)) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        Log.d(TAG, "UART_DISCONNECT_MSG");
                        btnConnectDisconnect.setText("Connect");
                        btnBrightness.setEnabled(false);
                        btnAdd.setEnabled(false);
                        btnChart.setEnabled(false);
                        btnBrightness.setTextColor(Color.parseColor("#B8B8B8"));
                        btnAdd.setTextColor(Color.parseColor("#B8B8B8"));
                        btnChart.setTextColor(Color.parseColor("#B8B8B8"));
                        ((TextView) findViewById(R.id.deviceName)).setText("");
                        pApter.clear();
                        mState = UART_PROFILE_DISCONNECTED;
                        mService.close();
                        if(activityRunningState==1){
                            MainActivity.activityRunningState=0;
                            ChartActivity.chartActivity.finish();
                        }else if(activityRunningState==2){
                            MainActivity.activityRunningState=0;
                            PointActivity.pointActivity.finish();
                        }else if(activityRunningState==3){
                            MainActivity.activityRunningState=0;
                            BrightnessActivity.brightnessActivity.finish();
                        }
                        //setUiState();

                    }
                });
            }


            //*********************//
            if (action.equals(UartService.ACTION_GATT_SERVICES_DISCOVERED)) {
                mService.enableTXNotification();
            }
            //*********************//
            if (action.equals(UartService.ACTION_DATA_AVAILABLE)) {// Receive byte array

                final byte[] value = intent.getByteArrayExtra(UartService.EXTRA_DATA);
                runOnUiThread(new Runnable() {
                    public void run() {
                        if(value[0]==0){
                            Date date = new Date();   // given date
                            Calendar calendar = GregorianCalendar.getInstance(); // creates a new calendar instance
                            calendar.setTime(date);   // assigns calendar to given date
                            int currHour = calendar.get(Calendar.HOUR_OF_DAY); // gets hour in 24h format
                            int currMinu = calendar.get(Calendar.MINUTE);
                            byte[] clockArray = new byte[]{0,(byte) currHour,(byte) currMinu};
                            mService.writeRXCharacteristic(clockArray);
                            int clockHour = value[1];
                            int clockMinu = value[2];
                            int timeDiff = (currHour*60+currMinu) - (clockHour*60+clockMinu);
                            Log.d(TAG, "Current Time: " + currHour + ":" + currMinu);
                            Log.d(TAG, "Clock Time: " + clockHour + ":" + clockMinu);

                            //if (timeDiff>10 || timeDiff<-10){
                                String currTime = currHour+":";
                                String clockTime = clockHour+":";
                                if(currMinu<10){
                                    currTime += ("0"+currMinu);
                                }else{
                                    currTime += currMinu;
                                }
                                if(currHour<12){
                                    currTime += "AM";
                                }else{
                                    currTime += "PM";
                                }

                                if(clockMinu<10){
                                    clockTime += ("0"+clockMinu);
                                }else{
                                    clockTime += clockMinu;
                                }
                                if(clockHour<12){
                                    clockTime += "AM";
                                }else{
                                    clockTime += "PM";
                                }

                                androidx.appcompat.app.AlertDialog alertDialog = new androidx.appcompat.app.AlertDialog.Builder(MainActivity.this).create();
                                alertDialog.setTitle("NOTIFICATION!");
                                alertDialog.setMessage("Clock time "+ clockTime + " already changed to correct time " + currTime);
                                alertDialog.setButton(androidx.appcompat.app.AlertDialog.BUTTON_NEUTRAL, "Close",
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                dialog.dismiss();
                                            }
                                        });
                                alertDialog.show();
                           // }

                        }else if(value[0]==1){
                            if(value[1]!=0){
                                int count = 0;
                                PointModel pModel = new PointModel();
                                for(int i=3; i<value.length; i++){
                                    if(count==3){
                                        pModel.setWhiteV(value[i]& 0xFF);
                                        pApter.add(pModel);
                                        count = 0;
                                        if(isFirst){//set the first time when received from controller
                                            isFirst = false;
                                            PointActivity.firstTime = pModel.getTimeInMinu();
                                        }
                                        pModel = new PointModel();
                                    }else{
                                        if(count==0){
                                            pModel.setHour(value[i]);
                                            count++;
                                        }else if(count==1){
                                            pModel.setMinu(value[i]);
                                            count++;
                                        }else if(count==2){
                                            pModel.setBlueV(value[i]& 0xFF);
                                            count++;
                                        }
                                    }
                                }
                            }else{//preset points
                                pApter.addAll(new PointModel(8,0,50,50));
                                PointActivity.firstTime = 8*60;//set the first time when there is no point
                                sendEvent();
                            }

                        }else if(value[0]==8){
                            currBlueValue = value[1]& 0xFF;
                            currWhiteValue = value[2]& 0xFF;
                            if(whichActivity.equals(BrightnessActivity.class.getName()))
                                goToBrightnessActivity();
                            if(whichActivity.equals(PointActivity.class.getName()))
                                goToPointActivity();
                        }
                    }
                });
            }
            //*********************//
            if (action.equals(UartService.DEVICE_DOES_NOT_SUPPORT_UART)) {
                showMessage("Device doesn't support UART. Disconnecting");
                mService.disconnect();
            }
        }
    };

    public static void sendEvent(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                int n = 1;
                int packageNum = 0;
                int arrayIndex = 0;
                int totalPackageNum = listAdapter.size()/4;
                int lastPackageSize = (listAdapter.size()%4)*4 + 3;
                byte[] value = new byte[19];
                value[arrayIndex++] = 1;
                value[arrayIndex++] = (byte) listAdapter.size();
                value[arrayIndex++] = (byte) packageNum;
                for(PointModel pModel: listAdapter){
                    if(n==5){
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        mService.writeRXCharacteristic(value);
                        if(packageNum==totalPackageNum){
                            value = new byte[lastPackageSize];
                        }else{
                            value = new byte[19];
                        }
                        n = 1;
                        packageNum ++;
                        arrayIndex = 0;
                        value[arrayIndex++] = 1;
                        value[arrayIndex++] = (byte) listAdapter.size();
                        value[arrayIndex++] = (byte) packageNum;
                    }
                    value[arrayIndex++] = (byte) pModel.getHour();
                    value[arrayIndex++] = (byte) pModel.getMinu();
                    value[arrayIndex++] = (byte) pModel.getBlueV();
                    value[arrayIndex++] = (byte) pModel.getWhiteV();
                    n++;
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mService.writeRXCharacteristic(value);
            }
        }).start();

    }

    private void service_init() {
        Intent bindIntent = new Intent(this, UartService.class);
        bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE);

        LocalBroadcastManager.getInstance(this).registerReceiver(UARTStatusChangeReceiver, makeGattUpdateIntentFilter());
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(UartService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(UartService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(UartService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(UartService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(UartService.DEVICE_DOES_NOT_SUPPORT_UART);
        return intentFilter;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy()");

        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(UARTStatusChangeReceiver);
        } catch (Exception ignore) {
            Log.e(TAG, ignore.toString());
        }
        unbindService(mServiceConnection);
        mService.stopSelf();
        mService = null;
        pApter.clear();

    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        super.onStop();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        if (!mBtAdapter.isEnabled()) {
            Log.i(TAG, "onResume - BT not enabled yet");
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {

            case REQUEST_SELECT_DEVICE:
                //When the DeviceListActivity return, with the selected device address
                if (resultCode == Activity.RESULT_OK && data != null) {
                    String deviceAddress = data.getStringExtra(BluetoothDevice.EXTRA_DEVICE);
                    mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress);

                    Log.d(TAG, "... onActivityResultdevice.address==" + mDevice + "mserviceValue" + mService);
                   // ((TextView) findViewById(R.id.deviceName)).setText(mDevice.getName() + " - connecting");
                    mService.connect(deviceAddress);


                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    Toast.makeText(this, "Bluetooth has turned on ", Toast.LENGTH_SHORT).show();

                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(this, "Problem in BT Turning ON ", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
                Log.e(TAG, "wrong request code");
                break;
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {

    }


    private void showMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onBackPressed() {
        if (mState == UART_PROFILE_CONNECTED) {
            Intent startMain = new Intent(Intent.ACTION_MAIN);
            startMain.addCategory(Intent.CATEGORY_HOME);
            startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startMain);
            showMessage("nRFUART's running in background.\n             Disconnect to exit");
        } else {
            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(R.string.popup_title)
                    .setMessage(R.string.popup_message)
                    .setPositiveButton(R.string.popup_yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .setNegativeButton(R.string.popup_no, null)
                    .show();
        }
    }
}
