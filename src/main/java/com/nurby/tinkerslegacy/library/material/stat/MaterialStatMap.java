package com.nurby.tinkerslegacy.library.material.stat;

import java.util.Map;

public record MaterialStatMap(Map<String, Number> values) {
    public MaterialStatMap(Map<String, Number> values) {
        this.values = Map.copyOf(values);
    }

    public float getFloat(String key) {
        return values.get(key).floatValue();
    }

    public int getInt(String key) {
        return values.get(key).intValue();
    }

    public double getDouble(String key) {
        return values.get(key).doubleValue();
    }

    public long getLong(String key) {
        return values.get(key).longValue();
    }

    public Number get(String key) {
        return values.get(key);
    }

    public boolean contains(String key) {
        return values.containsKey(key);
    }
}