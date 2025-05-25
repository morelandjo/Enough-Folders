package com.enoughfolders.integrations.util;

import net.minecraft.client.gui.screens.Screen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Utility class for reflection operations used in integration modules.
 */
public final class ReflectionUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReflectionUtils.class);

    private ReflectionUtils() {
        // Private constructor to prevent instantiation
    }

    /**
     * Finds a field in a class that matches a predicate.
     * 
     * @param clazz the class to search
     * @param predicate the predicate to match against fields
     * @param <T> the type of the field
     * @return an Optional containing the field if found, or empty if not found
     */
    public static <T> Optional<Field> findField(Class<?> clazz, Predicate<Field> predicate) {
        try {
            for (Field field : clazz.getDeclaredFields()) {
                if (predicate.test(field)) {
                    field.setAccessible(true);
                    return Optional.of(field);
                }
            }
            
            // Check superclass if not found
            Class<?> superClass = clazz.getSuperclass();
            if (superClass != null && superClass != Object.class) {
                return findField(superClass, predicate);
            }
        } catch (Exception e) {
            LOGGER.error("Error finding field in " + clazz.getName(), e);
        }
        
        return Optional.empty();
    }

    /**
     * Gets the value of a field from an object.
     * 
     * @param object the object containing the field
     * @param fieldName the name of the field to get
     * @param <T> the type of the field value
     * @return an Optional containing the field value if found, or empty if not found or an error occurred
     */
    @SuppressWarnings("unchecked")
    public static <T> Optional<T> getFieldValue(Object object, String fieldName) {
        if (object == null) {
            return Optional.empty();
        }
        
        try {
            Field field = findField(object.getClass(), f -> f.getName().equals(fieldName)).orElse(null);
            if (field != null) {
                return Optional.ofNullable((T) field.get(object));
            }
        } catch (Exception e) {
            LOGGER.error("Error getting field value " + fieldName + " from " + object.getClass().getName(), e);
        }
        
        return Optional.empty();
    }

    /**
     * Finds a field in a screen based on its type.
     * 
     * @param screen the screen to search
     * @param fieldType the type of field to find
     * @param <T> the type of the field
     * @return an Optional containing the field value if found, or empty if not found
     */
    @SuppressWarnings("unchecked")
    public static <T> Optional<T> findFieldInScreen(Screen screen, Class<T> fieldType) {
        try {
            for (Field field : screen.getClass().getDeclaredFields()) {
                if (fieldType.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    return Optional.ofNullable((T) field.get(screen));
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error finding field of type " + fieldType.getName() + " in screen " + screen.getClass().getName(), e);
        }
        
        return Optional.empty();
    }

    /**
     * Invokes a method on an object.
     * 
     * @param object the object to invoke the method on
     * @param methodName the name of the method to invoke
     * @param parameterTypes the parameter types of the method
     * @param args the arguments to pass to the method
     * @param <T> the return type of the method
     * @return an Optional containing the method result if successful, or empty if not found or an error occurred
     */
    @SuppressWarnings("unchecked")
    public static <T> Optional<T> invokeMethod(Object object, String methodName, Class<?>[] parameterTypes, Object[] args) {
        if (object == null) {
            return Optional.empty();
        }
        
        try {
            Method method = object.getClass().getDeclaredMethod(methodName, parameterTypes);
            method.setAccessible(true);
            return Optional.ofNullable((T) method.invoke(object, args));
        } catch (Exception e) {
            LOGGER.error("Error invoking method " + methodName + " on " + object.getClass().getName(), e);
        }
        
        return Optional.empty();
    }
}
