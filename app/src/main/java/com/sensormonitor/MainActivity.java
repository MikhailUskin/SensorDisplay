package com.sensormonitor;

import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;

import android.support.v7.app.AppCompatActivity;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import android.view.View;

import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;

import com.sensormonitor.bluetooth.DeviceService;
import com.sensormonitor.bluetooth.ProvidedGattCharacteristics;
import com.sensormonitor.bluetooth.ProvidedGattServices;
import com.sensormonitor.plot.TemperaturePlot;
import com.sensormonitor.plot.BatteryChargePlot;

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

import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGatt;

import android.util.Log;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    // Expected name
    private static final String DEVICE_NAME = "ATMEL-BAS";

    private static final int REQUEST_ENABLE_BT = 0;

    private Handler mHandler;

    private TextView mStatusBlueTv;
    private Button mOnBtn;
    private Button mConBtn;
    private TemperaturePlot mTempPlot;
    private BatteryChargePlot mChargePlot;

    private BluetoothLeScanner bluetoothLeScanner;
    private BluetoothDevice mSelectedDevice;
    private BluetoothAdapter mBlueAdapter;
    private List<BluetoothGattCharacteristic> mNotifiedCharacteristics;
    private DeviceService mBluetoothService;

    // Handles connection with bluetooth service
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothService = ((DeviceService.LocalBinder) service).getService();
            if (!mBluetoothService.initialize()) {
                finish();
            }

            Handler connectHandler = new Handler();
            connectHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mBluetoothService.connect(mSelectedDevice.getAddress());
                }
            }, 1000);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothService = null;
        }
    };

    // Handles various events fired by the Service.
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (DeviceService.ACTION_GATT_CONNECTED.equals(action)) {
                mStatusBlueTv.setText("Device connected");
                mChargePlot.resetPlot();
                mTempPlot.resetPlot();
            } else if (DeviceService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mStatusBlueTv.setText("Device disconnected");
                mChargePlot.resetPlot();
                mTempPlot.resetPlot();
            } else if (mBluetoothService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                mNotifiedCharacteristics = ProvidedGattCharacteristics.get(mBluetoothService.getSupportedGattServices());
                //Handler connectHandler = new Handler();
                //connectHandler.postDelayed(new Runnable() {
                //@Override
                //public void run() {
                if (mNotifiedCharacteristics != null) {
                    for (BluetoothGattCharacteristic characteristic : mNotifiedCharacteristics) {
                        final int properties = characteristic.getProperties();
                        if ((properties | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                            mBluetoothService.readCharacteristic(characteristic);
                        }
                        if ((properties | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                            mBluetoothService.setCharacteristicNotification(characteristic, true);
                        }
                    }
                }
                //}
                //}, 1000);
            } else if (DeviceService.ACTION_DATA_AVAILABLE.equals(action)) {
                drawPoint(intent);
            }
        }
    };

    private final void drawPoint(Intent dataIntent){
        if (dataIntent.hasExtra(DeviceService.DATA_TYPE_BATTERY_CHARGE)){
            float charge = dataIntent.getFloatExtra(DeviceService.DATA_TYPE_BATTERY_CHARGE, 0);
            mChargePlot.drawValue(charge);
        }
        if (dataIntent.hasExtra(DeviceService.DATA_TYPE_TEMPERATURE)){
            float temperature = dataIntent.getFloatExtra(DeviceService.DATA_TYPE_TEMPERATURE, 0);
            mTempPlot.drawValue(temperature);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mStatusBlueTv = findViewById(R.id.statusBluetoothTv);
        mOnBtn        = findViewById(R.id.onBtn);
        mConBtn       = findViewById(R.id.conBtn);

        mTempPlot      = new TemperaturePlot((GraphView) findViewById(R.id.tempGv));
        mChargePlot    = new BatteryChargePlot((GraphView) findViewById(R.id.chargeGv));

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
                if (mBluetoothService != null){
                    disconnectService();
                    mStatusBlueTv.setText("Device disconnected");
                }
                else {
                    mStatusBlueTv.setText("Scanning for a device...");
                    startScanning();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
        } else {
            ActivityCompat.requestPermissions(
                    this, new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION}, 101);
        }

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "Your devices doesn't support BLE", Toast.LENGTH_LONG).show();
            finish();
        }

        registerReceiver(mReceiver, GattUpdateIntentFilter());
        if (mBluetoothService != null && mSelectedDevice != null) {
            final boolean result = mBluetoothService.connect(mSelectedDevice.getAddress());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
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
                    showToast("Can't enable bluetooth");
                    mStatusBlueTv.setText("Bluetooth is off");
                }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    public void stopScanning(){
        bluetoothLeScanner = mBlueAdapter.getBluetoothLeScanner();

        if (mHandler == null){
            mHandler = new Handler();
        }

        mHandler.removeCallbacksAndMessages(null);
        bluetoothLeScanner.stopScan(scanCallback);
    }

    public void startScanning(){
        bluetoothLeScanner = mBlueAdapter.getBluetoothLeScanner();

        if (mHandler == null){
            mHandler = new Handler();
        }

        List<ScanFilter> scanFilters = new ArrayList<>();
        scanFilters.add(new ScanFilter.Builder().setServiceUuid(
                ParcelUuid.fromString(ProvidedGattServices.UUID_HEALTH_THERMOMETER_SERVICE)).build());
        scanFilters.add(new ScanFilter.Builder().setServiceUuid(
                ParcelUuid.fromString(ProvidedGattServices.UUID_BATTERY_SERVICE)).build());

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                bluetoothLeScanner.stopScan(scanCallback);
                mStatusBlueTv.setText("Can't find device");
            }
        }, 10000);

        final ScanSettings settings = new ScanSettings.Builder().build();
        bluetoothLeScanner.startScan(scanFilters, settings, scanCallback);
    }

    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            BluetoothDevice foundDevice = result.getDevice();
            if (foundDevice.getName().equalsIgnoreCase(DEVICE_NAME)){
                mSelectedDevice = foundDevice;
                mStatusBlueTv.setText("Device found");
                stopScanning();
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
                    startScanning();
                    connectService();
                    break;
                }
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            mStatusBlueTv.setText("Scanning failed");
            stopScanning();
        }
    };

    private static IntentFilter GattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(DeviceService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(DeviceService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(DeviceService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(DeviceService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    private void connectService(){
        if (mSelectedDevice != null) {
            mStatusBlueTv.setText("Connecting service...");
            Handler connectHandler = new Handler();
            connectHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Intent gattServiceIntent = new Intent(MainActivity.this, DeviceService.class);
                    bindService(gattServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
                }
            }, 1000);
        }
    }

    private void disconnectService(){
        if (mSelectedDevice != null){
            mBluetoothService.disconnect();
            unbindService(mServiceConnection);
            mBluetoothService = null;
        }
    }

    private void showToast(String msg){
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
