package io.andy.shorten_url.util;

import io.andy.shorten_url.exception.server.ObjectUtilException;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Slf4j
public class ObjectUtil {
    public static <T> T fieldFilter(T target, String... fields) {
        try {
            List<String> excludeFields = Arrays.asList(fields);
            T object = (T) target.getClass().getDeclaredConstructor().newInstance();
            for (Field field : target.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                if (!excludeFields.contains(field.getName())) {
                    field.set(object, field.get(target));
                }
            }
            return object;
        } catch (Exception e) {
            log.error("failed to filter field of {}.class. caused by {}", target, e.getMessage());
            throw new ObjectUtilException("FAILED TO FIELD FILTER");
        }
    }

    public static <T> T fieldFilter(Optional<T> target, String... fields) {
        T object = target.orElse(null);
        return fieldFilter(object, fields);
    }
}
