package com.sensormonitor.bluetooth;

import java.util.HashMap;

public class ProvidedGattServices {
    public static final String UUID_HEALTH_THERMOMETER_SERVICE = "00001809-0000-1000-8000-00805f9b34fb";
    public static final String UUID_BATTERY_SERVICE = "0000180f-0000-1000-8000-00805f9b34fb";

    private static HashMap<String, String> services = new HashMap();

    static {
        services.put(UUID_HEALTH_THERMOMETER_SERVICE, "Health Thermometer");
        services.put(UUID_BATTERY_SERVICE, "Battery Service");
    }

    public static String lookup(String uuid) {
        return services.get(uuid);
    }
}
