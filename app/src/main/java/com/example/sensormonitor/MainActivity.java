package com.example.sensormonitor;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGatt;

import android.graphics.Color;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.LegendRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;


import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import android.os.IBinder;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.Context;
import android.content.ComponentName;
import android.content.BroadcastReceiver;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;

import android.util.Log;

public class MainActivity extends AppCompatActivity {

    private static final String DEVICE_NAME = "ATMEL-BAS";
    private static final int REQUEST_ENABLE_BT = 0;

    private static final String TAG = "MainActivity";

    private int mCounter = 0;

    private TextView mStatusBlueTv;
    private Button mOnBtn, mConBtn;

    private Handler mHandler;

    private GraphView mTempGv;
    private LineGraphSeries<DataPoint> mTempSeries;

    private BluetoothLeScanner bluetoothLeScanner;
    private BluetoothDevice mSelectedDevice;
    private BluetoothAdapter mBlueAdapter;

    private BluetoothGattCharacteristic mNotifyCharacteristic;
    private BluetoothLEService mBluetoothLEService;
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLEService = ((BluetoothLEService.LocalBinder) service).getService();
            if (!mBluetoothLEService.initialize()) {
                finish();
            }

            Handler connectHandler = new Handler();
            connectHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mBluetoothLEService.connect(mSelectedDevice.getAddress());
                }
            }, 1000);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLEService = null;
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLEService.ACTION_GATT_CONNECTED.equals(action)) {
                mStatusBlueTv.setText("Device connected");
                initPlot();
            } else if (BluetoothLEService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mStatusBlueTv.setText("Device disconnected");
                initPlot();
            } else if (BluetoothLEService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                displayGattServices(mBluetoothLEService.getSupportedGattServices());

                //Handler connectHandler = new Handler();
                //connectHandler.postDelayed(new Runnable() {
                    //@Override
                    //public void run() {
                        if (mNotifyCharacteristic != null) {
                            final int charaProp = mNotifyCharacteristic.getProperties();
                            if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                                mBluetoothLEService.readCharacteristic(mNotifyCharacteristic);
                            }
                            if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                                mBluetoothLEService.setCharacteristicNotification(mNotifyCharacteristic, true);
                            }
                        }
                    //}
                //}, 1000);
            } else if (BluetoothLEService.ACTION_DATA_AVAILABLE.equals(action)) {
                float temperature = intent.getFloatExtra(BluetoothLEService.EXTRA_DATA, 0);
                drawPoint(Double.valueOf(mCounter++), temperature);
            }
        }
    };

    BluetoothGatt gatt;

    private BluetoothGattCallback gattCallback = new BluetoothGattCallback() {

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mStatusBlueTv = findViewById(R.id.statusBluetoothTv);
        mOnBtn        = findViewById(R.id.onBtn);
        mConBtn       = findViewById(R.id.conBtn);
        mTempGv       = findViewById(R.id.tempGv);

        mBlueAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBlueAdapter == null){
            mStatusBlueTv.setText("Bluetooth is not available");
            mOnBtn.setEnabled(false);
            mConBtn.setEnabled(false);
        }
        else if (mBlueAdapter.isEnabled()) {
            mStatusBlueTv.setText("Bluetooth is on");
        }
        else {
            mConBtn.setEnabled(false);
            mStatusBlueTv.setText("Bluetooth is off");
        }

        mOnBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                if (!mBlueAdapter.isEnabled()){
                    mStatusBlueTv.setText("Enabling bluetooth...");
                    Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(intent, REQUEST_ENABLE_BT);
                }
                else{
                    mBlueAdapter.disable();
                    mStatusBlueTv.setText("Bluetooth is off");
                    mConBtn.setEnabled(false);
                    mSelectedDevice = null;
                }
            }
        });

        mConBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                if (mBluetoothLEService != null){
                    disconnectService();
                    mStatusBlueTv.setText("Device disconnected");
                }
                else {
                    mStatusBlueTv.setText("Scanning for a device...");
                    startScanning(true);
                }
            }
        });

        initPlot();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
        } else {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION},
                    101);
        }

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "Your devices that don't support BLE", Toast.LENGTH_LONG).show();
            finish();
        }

        registerReceiver(mGattUpdateReceiver, GattUpdateIntentFilter());
        if (mBluetoothLEService != null && mSelectedDevice != null) {
            final boolean result = mBluetoothLEService.connect(mSelectedDevice.getAddress());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disconnectService();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        switch(requestCode){
            case REQUEST_ENABLE_BT:
                if (resultCode == RESULT_OK){
                    mStatusBlueTv.setText("Bluetooth is on");
                    mConBtn.setEnabled(true);
                } else{
                    showToast("Couldn't on bluetooth");
                    mStatusBlueTv.setText("Bluetooth is off");
                }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            BluetoothDevice foundDevice = result.getDevice();
            if (foundDevice.getName().equalsIgnoreCase(DEVICE_NAME)){
                mSelectedDevice = foundDevice;
                mStatusBlueTv.setText("Device found");
                startScanning(false);
                connectService();
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            for(Iterator<ScanResult> r = results.iterator(); r.hasNext();){
                BluetoothDevice foundDevice = r.next().getDevice();
                if (foundDevice.getName().equalsIgnoreCase(DEVICE_NAME)){
                    mSelectedDevice = foundDevice;
                    mStatusBlueTv.setText("Device found");
                    startScanning(false);
                    connectService();
                    break;
                }
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            mStatusBlueTv.setText("Scanning failed");
            startScanning(false);
        }
    };

    private static IntentFilter GattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLEService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLEService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLEService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLEService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    private void connectService(){
        if (mSelectedDevice != null) {
            mStatusBlueTv.setText("Connecting service...");
            Handler connectHandler = new Handler();
            connectHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Intent gattServiceIntent = new Intent(MainActivity.this, BluetoothLEService.class);
                    bindService(gattServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
                }
            }, 1000);
        }
    }

    private void disconnectService(){
        if (mSelectedDevice != null){
            mBluetoothLEService.disconnect();
            unbindService(mServiceConnection);
            mBluetoothLEService = null;
        }
    }

    private void startScanning(final boolean enable) {
        bluetoothLeScanner = mBlueAdapter.getBluetoothLeScanner();

        if (mHandler == null){
            mHandler = new Handler();
        }

        if (enable) {
            //filter for battery service.
            List<ScanFilter> scanFilters = new ArrayList<>();
            //default setting.
            final ScanSettings settings = new ScanSettings.Builder().build();
            ScanFilter scanFilter = new ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString(SampleGattAttributes.UUID_HEALTH_THERMOMETER_SERVICE)).build();
            scanFilters.add(scanFilter);
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    bluetoothLeScanner.stopScan(scanCallback);
                    mStatusBlueTv.setText("Can't find device");
                }
            }, 10000);
            bluetoothLeScanner.startScan(scanFilters, settings, scanCallback);
        } else {
            mHandler.removeCallbacksAndMessages(null);
            bluetoothLeScanner.stopScan(scanCallback);
        }
    }

    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null)
            return;
        String uuid = null;
        String serviceString = "unknown service";
        String charaString = "unknown characteristic";

        for (BluetoothGattService gattService : gattServices) {

            uuid = gattService.getUuid().toString();

            serviceString = SampleGattAttributes.lookup(uuid);

            if (serviceString != null) {
                List<BluetoothGattCharacteristic> gattCharacteristics =
                        gattService.getCharacteristics();

                for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                    for (BluetoothGattDescriptor descriptor: gattCharacteristic.getDescriptors()){
                        Log.e(TAG, "BluetoothGattDescriptor: "+descriptor.getUuid().toString());
                    }

                    uuid = gattCharacteristic.getUuid().toString();
                    if (uuid.equals(SampleGattAttributes.UUID_HEALTH_THERMOMETER_MEASUREMENT_UUID)) {
                        mNotifyCharacteristic = gattCharacteristic;
                    }
                    return;
                }
            }
        }
    }

    private void showToast(String msg){
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void initPlot(){
        mTempSeries = new LineGraphSeries<DataPoint>();
        mTempSeries.setColor(Color.RED);
        mTempSeries.setTitle("t°C");

        mTempGv.getViewport().setMinX(0);
        mTempGv.getViewport().setMaxX(30);

        mTempGv.getViewport().setMinY(0);
        mTempGv.getViewport().setMaxY(50);

        mTempGv.getViewport().setYAxisBoundsManual(true);
        mTempGv.getViewport().setXAxisBoundsManual(true);

        mTempGv.getGridLabelRenderer().setNumHorizontalLabels(6);
        mTempGv.getGridLabelRenderer().setNumVerticalLabels(10);

        mTempGv.getLegendRenderer().setVisible(true);
        mTempGv.getLegendRenderer().setTextSize(50);
        mTempGv.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.TOP);
        mTempGv.getLegendRenderer().setBackgroundColor(Color.parseColor("#CCFFFFFF"));

        mTempGv.removeAllSeries();
        mTempGv.addSeries(mTempSeries);
    }

    private void drawPoint(double x, double y){
        mTempSeries.appendData(new DataPoint(x, y), x > 30 ? true : false,1000);
    }
}
