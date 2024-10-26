package io.andy.shorten_url.util;

import io.andy.shorten_url.exception.server.ObjectUtilException;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.Objects;

@Slf4j
public class ObjectUtil {
    public static <T> T fieldFilter(T target, String[] excludeFields) {
        if (Objects.isNull(target) || Objects.isNull(excludeFields) || excludeFields.length == 0) {
            log.error("target class or fields cannot be null");
            throw new IllegalArgumentException("target class or fields cannot be null");
        }
        try {
            Class<?> targetClass = target.getClass();
            for (String field : excludeFields) {
                Field fieldOfClass = targetClass.getDeclaredField(field);
                fieldOfClass.setAccessible(true);
                fieldOfClass.set(target, null);
            }
            return (T) targetClass.cast(target);
        } catch (Exception e) {
            log.error("failed to filter field of {}.class. caused by {}", target, e.getMessage());
            throw new ObjectUtilException("FAILED TO FIELD FILTER");
        }
    }
}
