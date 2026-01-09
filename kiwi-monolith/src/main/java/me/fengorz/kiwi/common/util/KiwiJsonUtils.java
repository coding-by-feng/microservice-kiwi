/*
 * Copyright [2019~2025] [codingByFeng]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.fengorz.kiwi.common.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import me.fengorz.kiwi.common.exception.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

/**
 * JSON Utility class using Jackson
 *
 * @author codingByFeng
 */
public final class KiwiJsonUtils {

    private static final Logger logger = LoggerFactory.getLogger(KiwiJsonUtils.class);
    private static final ObjectMapper OBJECT_MAPPER;

    static {
        OBJECT_MAPPER = new ObjectMapper();
        OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        OBJECT_MAPPER.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        OBJECT_MAPPER.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        OBJECT_MAPPER.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
        OBJECT_MAPPER.setTimeZone(TimeZone.getTimeZone("UTC"));
        OBJECT_MAPPER.registerModule(new JavaTimeModule());
    }

    private KiwiJsonUtils() {}

    public static String toJsonStr(Object obj) {
        return toJson(obj);
    }

    /**
     * Converts a Java object to a JSON string.
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
        return fromJson(KiwiJsonUtils.toJson(object), clazz);
    }

    /**
     * Converts a JSON string to a Java object using a TypeReference (for generic types).
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

    /**
     * Get the shared ObjectMapper instance
     */
    public static ObjectMapper getObjectMapper() {
        return OBJECT_MAPPER;
    }
}
