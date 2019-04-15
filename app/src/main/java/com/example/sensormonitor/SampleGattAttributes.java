package com.example.sensormonitor;

import java.util.HashMap;

public class SampleGattAttributes {
    public static final String UUID_HEALTH_THERMOMETER_SERVICE = "00001809-0000-1000-8000-00805f9b34fb";
    public static final String UUID_HEALTH_THERMOMETER_MEASUREMENT_UUID = "00002a1c-0000-1000-8000-00805f9b34fb";
    public static final String UUID_TEMPERATURE_TYPE_UUID = "00002a1d-0000-1000-8000-00805f9b34fb";

    private static HashMap<String, String> attributes = new HashMap();

    static {
        attributes.put(UUID_HEALTH_THERMOMETER_SERVICE, "Health Thermometer Service");
        attributes.put(UUID_HEALTH_THERMOMETER_MEASUREMENT_UUID, "Health Thermometer Measurement");
        attributes.put(UUID_TEMPERATURE_TYPE_UUID, "Temperature Type");
    }

    public static String lookup(String uuid) {
        String name = attributes.get(uuid);
        return name;
    }
}
