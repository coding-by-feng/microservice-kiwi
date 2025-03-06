package me.fengorz.kiwi.common.sdk.util.enumeration;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class EnumToMapConverter {

    /**
     * Converts an Enum class with code and name properties into a Map.
     *
     * @param enumClass The Enum class to convert.
     * @param fieldName The name of the field containing the code (e.g., "code").
     * @param <E> The Enum type.
     * @param <K> The type of the code field (e.g., String, Integer).
     * @return A Map where the key is the code field value and the value is the Enum instance.
     * @throws IllegalArgumentException If the enumClass is not an Enum, or if the code field is not found or inaccessible.
     */
    public static <E extends Enum<E>, K> Map<K, E> enumToMap(Class<E> enumClass, String fieldName) {
        // Validate that the provided class is an Enum
        if (!enumClass.isEnum()) {
            throw new IllegalArgumentException("Class must be an Enum: " + enumClass.getName());
        }

        Map<K, E> enumMap = new HashMap<>();

        try {
            // Get the code field
            Field enumField = enumClass.getDeclaredField(fieldName);
            if (!enumField.isAccessible()) {
                enumField.setAccessible(true);  // Make the field accessible if private
            }

            // Get all Enum constants
            E[] enumConstants = enumClass.getEnumConstants();
            // Use Java 8 streams to create the Map
            Arrays.stream(enumConstants).forEach(enumConstant -> {
                try {
                    @SuppressWarnings("unchecked")
                    K field = (K) enumField.get(enumConstant);
                    enumMap.put(field, enumConstant);
                } catch (IllegalAccessException e) {
                    throw new IllegalArgumentException("Failed to access code field '" + fieldName + "' in " + enumConstant, e);
                }
            });

        } catch (NoSuchFieldException e) {
            throw new IllegalArgumentException("Field '" + fieldName + "' not found in " + enumClass.getName(), e);
        }

        return enumMap;
    }

}