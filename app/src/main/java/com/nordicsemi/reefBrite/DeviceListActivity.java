
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

@SuppressLint("MissingPermission")
public class DeviceListActivity extends Activity {
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;

   // private BluetoothAdapter mBtAdapter;
    private TextView mEmptyList;
    public static final String TAG = "DeviceListActivity";
    
    List<BluetoothDevice> deviceList;
    private DeviceAdapter deviceAdapter;
    private ServiceConnection onService = null;
    Map<String, Integer> devRssiValues;
    private static final long SCAN_PERIOD = 10000; //scanning for 10 seconds
    private static final int PERMISSION_REQUEST_CODE = 101;
    private Handler mHandler;
    private boolean mScanning;

    private String[] getRequiredBlePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+: BLUETOOTH_SCAN/CONNECT plus FINE_LOCATION (we are NOT
            // using the neverForLocation flag, so location permission is required
            // to receive scan results that contain location-correlated data).
            return new String[]{
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION
            };
        }
        return new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        };
    }

    private boolean hasBlePermissions() {
        for (String p : getRequiredBlePermissions()) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.title_bar);
        setContentView(R.layout.device_list);
        android.view.WindowManager.LayoutParams layoutParams = this.getWindow().getAttributes();
        layoutParams.gravity=Gravity.TOP;
        layoutParams.y = 200;
        mHandler = new Handler(Looper.getMainLooper());
        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        populateList();
        mEmptyList = (TextView) findViewById(R.id.empty);
        Button cancelButton = (Button) findViewById(R.id.btn_cancel);
        cancelButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
            	
            	if (mScanning==false) scanLeDevice(true); 
            	else finish();
            }
        });

    }

    private void populateList() {
        /* Initialize device list container */
        Log.d(TAG, "populateList");
        deviceList = new ArrayList<BluetoothDevice>();
        deviceAdapter = new DeviceAdapter(this, deviceList);
        devRssiValues = new HashMap<String, Integer>();

        ListView newDevicesListView = (ListView) findViewById(R.id.new_devices);
        newDevicesListView.setAdapter(deviceAdapter);
        newDevicesListView.setOnItemClickListener(mDeviceClickListener);

           scanLeDevice(true);

    }
    
    private void scanLeDevice(final boolean enable) {
        final Button cancelButton = (Button) findViewById(R.id.btn_cancel);

        if (enable && !hasBlePermissions()) {
            // Ask the user for the permissions we need. The scan will start after
            // onRequestPermissionsResult if they grant them.
            requestPermissions(getRequiredBlePermissions(), PERMISSION_REQUEST_CODE);
            return;
        }

        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "Please turn on Bluetooth and try again.", Toast.LENGTH_LONG).show();
            return;
        }

        // BLE scanning silently returns no results on most Android devices
        // (including Samsung) when location services are disabled, even when the
        // permission has been granted. Tell the user explicitly.
        android.location.LocationManager lm =
                (android.location.LocationManager) getSystemService(LOCATION_SERVICE);
        boolean locationEnabled = false;
        if (lm != null) {
            try {
                locationEnabled = lm.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)
                        || lm.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER);
            } catch (Exception ignored) { /* fall through */ }
        }
        if (enable && !locationEnabled) {
            Toast.makeText(this,
                    "Please turn on Location services so Android can scan for Bluetooth devices.",
                    Toast.LENGTH_LONG).show();
            try {
                startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            } catch (Exception ignored) {}
            return;
        }

        if (mBluetoothLeScanner == null) {
            mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        }
        if (mBluetoothLeScanner == null) return;

        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
					mScanning = false;
                    if (mBluetoothLeScanner != null) {
                        try {
                            mBluetoothLeScanner.stopScan(mScanCallback);
                        } catch (SecurityException ignored) {}
                    }
                    cancelButton.setText(R.string.scan);

                }
            }, SCAN_PERIOD);

            mScanning = true;
            // NOTE: We intentionally do NOT filter by the Nordic UART service UUID
            // here because some ReefBrite firmware revisions advertise that UUID
            // only in the scan response, which would cause the filter to drop every
            // packet and the user would see an empty list. Scanning unfiltered and
            // letting the user pick their light is more reliable.
            ScanSettings settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build();
            List<ScanFilter> filters = new ArrayList<>();
            try {
                mBluetoothLeScanner.startScan(filters, settings, mScanCallback);
            } catch (SecurityException se) {
                Log.e(TAG, "startScan SecurityException", se);
                Toast.makeText(this, "Bluetooth permission denied.", Toast.LENGTH_LONG).show();
                return;
            } catch (IllegalStateException ise) {
                Log.e(TAG, "startScan IllegalStateException", ise);
                Toast.makeText(this, "Bluetooth is not ready. Turn it on and retry.", Toast.LENGTH_LONG).show();
                return;
            }
            cancelButton.setText(R.string.cancel);
        } else {
            mScanning = false;
            if (mBluetoothLeScanner != null) {
                try {
                    mBluetoothLeScanner.stopScan(mScanCallback);
                } catch (SecurityException ignored) {}
            }
            cancelButton.setText(R.string.scan);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = grantResults.length > 0;
            for (int r : grantResults) {
                if (r != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                scanLeDevice(true);
            } else {
                Toast.makeText(this,
                        "Bluetooth and Location permissions are required to find your light.",
                        Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, final ScanResult result) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    addDevice(result.getDevice(), result.getRssi());
                }
            });
        }

        @Override
        public void onBatchScanResults(java.util.List<ScanResult> results) {
            for (ScanResult r : results) {
                addDevice(r.getDevice(), r.getRssi());
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e(TAG, "BLE scan failed, errorCode=" + errorCode);
            final String reason;
            switch (errorCode) {
                case SCAN_FAILED_ALREADY_STARTED: reason = "scan already running"; break;
                case SCAN_FAILED_APPLICATION_REGISTRATION_FAILED: reason = "app registration failed"; break;
                case SCAN_FAILED_FEATURE_UNSUPPORTED: reason = "BLE scan not supported"; break;
                case SCAN_FAILED_INTERNAL_ERROR: reason = "Bluetooth internal error"; break;
                default: reason = "error " + errorCode; break;
            }
            runOnUiThread(new Runnable() {
                @Override public void run() {
                    Toast.makeText(DeviceListActivity.this,
                            "Bluetooth scan failed: " + reason, Toast.LENGTH_LONG).show();
                }
            });
        }
    };
    
    private void addDevice(BluetoothDevice device, int rssi) {
        boolean deviceFound = false;

        for (BluetoothDevice listDev : deviceList) {
            if (listDev.getAddress().equals(device.getAddress())) {
                deviceFound = true;
                break;
            }
        }
        
        
        devRssiValues.put(device.getAddress(), rssi);
        if (!deviceFound) {
        	deviceList.add(device);
            mEmptyList.setVisibility(View.GONE);
                 	
        	

            
            deviceAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
       
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mBluetoothLeScanner != null) {
            mBluetoothLeScanner.stopScan(mScanCallback);
        }
    
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBluetoothLeScanner != null) {
            mBluetoothLeScanner.stopScan(mScanCallback);
        }
        
    }

    private OnItemClickListener mDeviceClickListener = new OnItemClickListener() {
    	
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            BluetoothDevice device = deviceList.get(position);
            if (mBluetoothLeScanner != null) {
                mBluetoothLeScanner.stopScan(mScanCallback);
            }
  
            Bundle b = new Bundle();
            b.putString(BluetoothDevice.EXTRA_DEVICE, deviceList.get(position).getAddress());

            Intent result = new Intent();
            result.putExtras(b);
            setResult(Activity.RESULT_OK, result);
            finish();
        	
        }
    };


    
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
    }
    
    class DeviceAdapter extends BaseAdapter {
        Context context;
        List<BluetoothDevice> devices;
        LayoutInflater inflater;

        public DeviceAdapter(Context context, List<BluetoothDevice> devices) {
            this.context = context;
            inflater = LayoutInflater.from(context);
            this.devices = devices;
        }

        @Override
        public int getCount() {
            return devices.size();
        }

        @Override
        public Object getItem(int position) {
            return devices.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewGroup vg;

            if (convertView != null) {
                vg = (ViewGroup) convertView;
            } else {
                vg = (ViewGroup) inflater.inflate(R.layout.device_element, null);
            }

            BluetoothDevice device = devices.get(position);
            final TextView tvadd = ((TextView) vg.findViewById(R.id.address));
            final TextView tvname = ((TextView) vg.findViewById(R.id.name));
            final TextView tvpaired = (TextView) vg.findViewById(R.id.paired);
            final TextView tvrssi = (TextView) vg.findViewById(R.id.rssi);

            tvrssi.setVisibility(View.VISIBLE);
            byte rssival = (byte) devRssiValues.get(device.getAddress()).intValue();
            if (rssival != 0) {
                tvrssi.setText("Rssi = " + String.valueOf(rssival));
            }
            if(MainActivity.nameMap.containsKey(device.getAddress()+device.getName())){
                tvname.setText(MainActivity.nameMap.get(device.getAddress()+device.getName()));
            }else {
                tvname.setText(device.getName());
                for(String key: MainActivity.nameMap.keySet()){
                    if(key.contains(device.getAddress())){
                        MainActivity.nameMap.remove(key);
                    }
                }
                //save name map
                getSharedPreferences("test", MODE_PRIVATE).edit().putString("hashString", new Gson().toJson(MainActivity.nameMap)).apply();
            }
            tvadd.setText(device.getAddress());
            if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                Log.i(TAG, "device::"+device.getName());
                tvname.setTextColor(Color.BLACK);
                tvadd.setTextColor(Color.DKGRAY);
                tvpaired.setTextColor(Color.DKGRAY);
                tvpaired.setVisibility(View.VISIBLE);
                tvpaired.setText(R.string.paired);
                tvrssi.setVisibility(View.VISIBLE);
                tvrssi.setTextColor(Color.DKGRAY);
                
            } else {
                tvname.setTextColor(Color.BLACK);
                tvadd.setTextColor(Color.DKGRAY);
                tvpaired.setVisibility(View.GONE);
                tvrssi.setVisibility(View.VISIBLE);
                tvrssi.setTextColor(Color.DKGRAY);
            }
            return vg;
        }
    }
    private void showMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
