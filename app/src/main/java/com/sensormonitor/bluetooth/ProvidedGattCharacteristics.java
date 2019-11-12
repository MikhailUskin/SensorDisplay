package com.sensormonitor.bluetooth;

import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.util.Log;

public class ProvidedGattCharacteristics {
    private static final String TAG = "ProvidedGattChar";

    public static final String UUID_BATTERY_LEVEL_CHARACTERISTIC = "00002a19-0000-1000-8000-00805f9b34fb";
    public static final String UUID_BATTERY_POWER_STATE_CHARACTERISTIC = "00002a1a-0000-1000-8000-00805f9b34fb";
    public static final String UUID_BATTERY_LEVEL_STATE_CHARACTERISTIC = "00002a1b-0000-1000-8000-00805f9b34fb";
    public static final String UUID_TEMPERATURE_MEASUREMENT_CHARACTERISTIC = "00002a1c-0000-1000-8000-00805f9b34fb";
    public static final String UUID_TEMPERATURE_TYPE_CHARACTERISTIC = "00002a1d-0000-1000-8000-00805f9b34fb";
    public static final String UUID_INTERMEDIATE_TEMPERATURE_CHARACTERISTIC = "00002a1e-0000-1000-8000-00805f9b34fb";
    public static final String UUID_TEMPERATURE_CELSIUS_CHARACTERISTIC = "00002a1f-0000-1000-8000-00805f9b34fb";
    public static final String UUID_TEMPERATURE_FAHRENHEIT_CHARACTERISTIC = "00002a20-0000-1000-8000-00805f9b34fb";
    public static final String UUID_MEASUREMENT_INTERVAL_CHARACTERISTIC = "00002a21-0000-1000-8000-00805f9b34fb";

    private static HashMap<String, String> characteristics = new HashMap();

    static {
        characteristics.put(UUID_BATTERY_LEVEL_CHARACTERISTIC, "Battery Level");
        characteristics.put(UUID_BATTERY_POWER_STATE_CHARACTERISTIC, "Battery Power State");
        characteristics.put(UUID_BATTERY_LEVEL_STATE_CHARACTERISTIC, "Battery Level State");
        characteristics.put(UUID_TEMPERATURE_MEASUREMENT_CHARACTERISTIC, "TemperaturePlot Measurement");
        characteristics.put(UUID_TEMPERATURE_TYPE_CHARACTERISTIC, "TemperaturePlot Type");
        characteristics.put(UUID_INTERMEDIATE_TEMPERATURE_CHARACTERISTIC, "Intermediate TemperaturePlot");
        characteristics.put(UUID_TEMPERATURE_CELSIUS_CHARACTERISTIC, "TemperaturePlot Celsius");
        characteristics.put(UUID_TEMPERATURE_FAHRENHEIT_CHARACTERISTIC, "TemperaturePlot Fahrenheit");
        characteristics.put(UUID_MEASUREMENT_INTERVAL_CHARACTERISTIC, "Measurement Interval");
    }

    public static String lookup(String uuid) {
        return characteristics.get(uuid);
    }

    public static List<BluetoothGattCharacteristic> get(List<BluetoothGattService> gattServices) {
        if (gattServices == null) {
            return null;
        }

        List<BluetoothGattCharacteristic> expectedCharacterisitcs = new ArrayList<BluetoothGattCharacteristic>();
        for (BluetoothGattService gattService : gattServices) {
            String serviceString = ProvidedGattServices.lookup(gattService.getUuid().toString());
            if (serviceString != null) {
                for (BluetoothGattCharacteristic gattCharacteristic : gattService.getCharacteristics()) {
                    for (BluetoothGattDescriptor descriptor : gattCharacteristic.getDescriptors()) {
                        Log.e(TAG, "BluetoothGattDescriptor: " + descriptor.getUuid().toString());
                    }

                    String uuidString = gattCharacteristic.getUuid().toString();
                    String foundCharacterisitc = lookup(uuidString);
                    if (foundCharacterisitc != null) {
                        expectedCharacterisitcs.add(gattCharacteristic);
                    }
                }
            }
        }

        return expectedCharacterisitcs;
    }
}
