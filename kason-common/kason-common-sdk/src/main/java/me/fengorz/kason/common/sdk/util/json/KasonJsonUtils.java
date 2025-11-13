package me.fengorz.kason.common.sdk.util.json;

import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.experimental.UtilityClass;
import me.fengorz.kason.common.sdk.exception.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

/**
 * Utility class for JSON-Object conversion using Jackson JSON.
 *
 * @Author Kason Zhan
 * @Date 06/03/2025
 */
@UtilityClass
public class KasonJsonUtils {

    private static final Logger logger = LoggerFactory.getLogger(KasonJsonUtils.class);
    private static final ObjectMapper OBJECT_MAPPER;

    static {
        OBJECT_MAPPER = new ObjectMapper();
        // Custom configurations for the ObjectMapper
        OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);  // Ignore null fields in serialization
        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);  // Ignore unknown properties
        OBJECT_MAPPER.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);  // Allow serialization of empty beans

        OBJECT_MAPPER.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);  // Write dates as ISO strings
        OBJECT_MAPPER.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));  // Custom date format
        OBJECT_MAPPER.setTimeZone(TimeZone.getTimeZone("UTC"));  // Use UTC timezone
        // Java 8 date/time support (LocalDateTime, LocalDate, etc.)
        OBJECT_MAPPER.registerModule(new JavaTimeModule());
    }

    public static String toJsonStr(Object obj) {
        return JSONUtil.toJsonStr(obj);
    }

    /**
     * Converts a Java object to a JSON string.
     *
     * @param object The Java object to convert.
     * @return The JSON string representation of the object.
     * @throws ServiceException If serialization fails.
     */
    public static String toJson(Object object) throws ServiceException {
        if (object == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize object to JSON: {}", object, e);
            throw new ServiceException("Failed to convert object to JSON: " + e.getMessage(), e);
        }
    }

    /**
     * Converts a Java object to a pretty-printed JSON string.
     *
     * @param object The Java object to convert.
     * @return The pretty-printed JSON string representation of the object.
     * @throws ServiceException If serialization fails.
     */
    public static String toJsonPretty(Object object) throws ServiceException {
        if (object == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(object);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize object to pretty JSON: {}", object, e);
            throw new ServiceException("Failed to convert object to pretty JSON: " + e.getMessage(), e);
        }
    }

    /**
     * Converts a JSON string to a Java object.
     *
     * @param json  The JSON string to convert.
     * @param clazz The target class of the Java object.
     * @param <T>   The type of the target object.
     * @return The deserialized Java object.
     * @throws ServiceException If deserialization fails.
     */
    public static <T> T fromJson(String json, Class<T> clazz) throws ServiceException {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            logger.error("Failed to deserialize JSON to object of type {}: {}", clazz.getName(), json, e);
            throw new ServiceException("Failed to convert JSON to object: " + e.getMessage(), e);
        }
    }

    public static <T> T fromObjectToJson(Object object, Class<T> clazz) throws ServiceException {
        return fromJson(KasonJsonUtils.toJson(object), clazz);
    }

    /**
     * Converts a JSON string to a Java object using a TypeReference (for generic types).
     *
     * @param json          The JSON string to convert.
     * @param typeReference The TypeReference of the target object.
     * @param <T>           The type of the target object.
     * @return The deserialized Java object.
     * @throws ServiceException If deserialization fails.
     */
    public static <T> T fromJson(String json, TypeReference<T> typeReference) throws ServiceException {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, typeReference);
        } catch (JsonProcessingException e) {
            logger.error("Failed to deserialize JSON to object with TypeReference {}: {}", typeReference.getType(), json, e);
            throw new ServiceException("Failed to convert JSON to object: " + e.getMessage(), e);
        }
    }
}