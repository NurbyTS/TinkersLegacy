package com.nurby.tinkerslegacy.library.material.stat;

public record StatField(
        String name,
        Class<?> type
) {
    public boolean isValid(Object value) {
        if (type == int.class) return value instanceof Integer;
        if (type == float.class) return value instanceof Float;
        if (type == double.class) return value instanceof Double;
        if (type == long.class) return value instanceof Long;
        if (type == short.class) return value instanceof Short;
        if (type == byte.class) return value instanceof Byte;
        return type.isInstance(value);
    }
}