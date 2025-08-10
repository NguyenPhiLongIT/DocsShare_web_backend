package com.docsshare_web_backend.commons.filters;

import jakarta.persistence.Column;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class CommonFilter {

    /**
     * Creates a Specification for filtering entities based on a filter request.
     * Dynamically determines searchable fields and processes filter fields.
     *
     * @param <T>         Entity type (e.g., Blog, Report)
     * @param <R>         Filter request type (e.g., BlogFilterRequest, ReportFilterRequest)
     * @param request     The filter request object
     * @param entityClass The entity class
     * @return Specification for JPA query
     */
    public static <T, R> Specification<T> filter(R request, Class<T> entityClass) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            List<String> searchableFields = getSearchableFields(request.getClass(), entityClass);

            // Process each field in the filter request
            for (Field field : request.getClass().getDeclaredFields()) {
                Predicate predicate = processField(field, request, root, criteriaBuilder, entityClass, searchableFields);
                if (predicate != null) {
                    predicates.add(predicate);
                }
            }

            // Combine predicates with OR
            if (predicates.isEmpty()) {
                return criteriaBuilder.conjunction(); // Return true if no predicates
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Processes a single filter request field and returns the corresponding Predicate.
     *
     * @param field            The filter request field
     * @param request          The filter request object
     * @param root             JPA query root
     * @param criteriaBuilder  JPA CriteriaBuilder
     * @param entityClass      The entity class
     * @param searchableFields List of searchable fields for direct string matches
     * @return Predicate or null if the field is not applicable
     */
    private static <T, R> Predicate processField(Field field, R request, Root<T> root,
                                                 CriteriaBuilder criteriaBuilder, Class<T> entityClass,
                                                 List<String> searchableFields) {
        String fieldName = "";
        try {
            field.setAccessible(true);
            Object value = field.get(request);
            if (value == null) {
                return null; // Skip null values
            }

            fieldName = field.getName();
            Class<?> fieldType = field.getType();

            if (fieldName.equals("q") && value instanceof String && !((String) value).trim().isEmpty()) {
                return handleQueryString((String) value, root, criteriaBuilder, entityClass);
            }
            if (isDateType(fieldType) && (fieldName.endsWith("_from") || fieldName.endsWith("_to"))) {
                return handleDateRange(fieldName, fieldType, value, root, criteriaBuilder);
            }
            if (isNumericType(fieldType) && (fieldName.endsWith("_lte") || fieldName.endsWith("_gte"))) {
                return handleNumericRange(fieldName, value, root, criteriaBuilder);
            }
//            if (fieldType == String.class && (fieldName.equals("type") || fieldName.equals("status") || fieldName.equals("userType") || fieldName.endsWith("Type") || fieldName.endsWith("_type"))) {
//                return handleEnumField(fieldName, (String) value, root, criteriaBuilder);
//            }
            if (fieldType.isEnum() || fieldName.equalsIgnoreCase("type") || fieldName.equalsIgnoreCase("status") || fieldName.toLowerCase().endsWith("type") || fieldName.toLowerCase().endsWith("status")) {
                return handleEnumField(fieldName, value.toString(), root, criteriaBuilder);
            }
            if (fieldType == String.class && value instanceof String && !((String) value).trim().isEmpty() &&
                    searchableFields.contains(fieldName.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase())) {
                return handleStringField(fieldName, value.toString(), root, criteriaBuilder);
            }

            return null; // Field not processed
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Error accessing field: " + fieldName, e);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid value for field: " + fieldName, e);
        }
    }

    /**
     * Handles the query string 'q' by searching all String fields in the entity.
     *
     * @param queryValue      The query string
     * @param root            JPA query root
     * @param criteriaBuilder JPA CriteriaBuilder
     * @param entityClass     The entity class
     * @return Predicate for the query string search
     */
    private static <T> Predicate handleQueryString(String queryValue, Root<T> root,
                                                   CriteriaBuilder criteriaBuilder, Class<T> entityClass) {
        String searchPattern = "%" + queryValue.trim().toLowerCase() + "%";
        List<Predicate> searchPredicates = new ArrayList<>();

        // ✅ Nếu là ForumPost, xử lý đặc biệt: title, content, filePath, tags
        if (entityClass.getSimpleName().equals("ForumPost")) {
            Predicate title = criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), searchPattern);
            Predicate content = criteriaBuilder.like(criteriaBuilder.lower(root.get("content")), searchPattern);
            Predicate filePath = criteriaBuilder.like(criteriaBuilder.lower(root.get("filePath")), searchPattern);
            Predicate tag = criteriaBuilder.like(criteriaBuilder.lower(root.joinSet("tags", jakarta.persistence.criteria.JoinType.LEFT)), searchPattern);

            return criteriaBuilder.or(title, content, filePath, tag);
        }

        // ✅ Default: tìm trong các trường String
        for (Field field : entityClass.getDeclaredFields()) {
            if (field.getType() == String.class) {
                String fieldName = field.getName();
                Column column = field.getAnnotation(Column.class);
                if (column != null && !column.name().isEmpty()) {
                    fieldName = column.name();
                }
                try {
                    root.get(fieldName);
                    searchPredicates.add(criteriaBuilder.like(
                            criteriaBuilder.lower(root.get(fieldName)),
                            searchPattern
                    ));
                } catch (IllegalArgumentException e) {
                    // Skip
                }
            }
        }

        return criteriaBuilder.or(searchPredicates.toArray(new Predicate[0]));
    }

//    private static <T> Predicate handleQueryString(String queryValue, Root<T> root,
//                                                   CriteriaBuilder criteriaBuilder, Class<T> entityClass) {
//        String searchPattern = "%" + queryValue.trim().toLowerCase() + "%";
//        List<Predicate> searchPredicates = new ArrayList<>();
//
//        // Get all String fields from the entity
//        for (Field field : entityClass.getDeclaredFields()) {
//            if (field.getType() == String.class) {
//                String fieldName = field.getName();
//                // Check for @Column annotation to get database column name
//                Column column = field.getAnnotation(Column.class);
//                if (column != null && !column.name().isEmpty()) {
//                    fieldName = column.name();
//                }
//                try {
//                    // Ensure the field is accessible in the query
//                    root.get(fieldName);
//                    searchPredicates.add(criteriaBuilder.like(
//                            criteriaBuilder.lower(root.get(fieldName)),
//                            searchPattern
//                    ));
//                } catch (IllegalArgumentException e) {
//                    // Skip fields that are not valid in the JPA query (e.g., non-persistent fields)
//                }
//            }
//        }
//
//        return criteriaBuilder.or(searchPredicates.toArray(new Predicate[0]));
//    }

    /**
     * Handles date range fields (e.g., createdAt_from, dueDate_to).
     *
     * @param fieldName       The filter field name
     * @param fieldType       The field type (LocalDate or LocalDateTime)
     * @param value           The field value
     * @param root            JPA query root
     * @param criteriaBuilder JPA CriteriaBuilder
     * @return Predicate for the date range
     */
    private static <T> Predicate handleDateRange(String fieldName, Class<?> fieldType, Object value,
                                                 Root<T> root, CriteriaBuilder criteriaBuilder) {
        String baseFieldName = fieldName.substring(0, fieldName.lastIndexOf('_'));

        // Validate entity field type
        try {
            Class<?> entityFieldType = root.get(baseFieldName).getJavaType();
            if (!isDateType(entityFieldType)) {
                throw new IllegalArgumentException("Entity field " + baseFieldName + " is not a date type");
            }
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid entity field: " + baseFieldName, e);
        }

        LocalDateTime dateTimeValue;
        if (fieldType == LocalDate.class) {
            LocalDate dateValue = (LocalDate) value;
            dateTimeValue = fieldName.endsWith("_from")
                    ? dateValue.atStartOfDay()
                    : dateValue.atTime(23, 59, 59);
        } else {
            dateTimeValue = (LocalDateTime) value;
        }

        if (fieldName.endsWith("_from")) {
            return criteriaBuilder.greaterThanOrEqualTo(root.get(baseFieldName), dateTimeValue);
        } else {
            return criteriaBuilder.lessThanOrEqualTo(root.get(baseFieldName), dateTimeValue);
        }
    }

    /**
     * Handles numeric range fields (e.g., userId_lte, userId_gte).
     *
     * @param fieldName       The filter field name
     * @param value           The field value
     * @param root            JPA query root
     * @param criteriaBuilder JPA CriteriaBuilder
     * @return Predicate for the numeric range
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> Predicate handleNumericRange(String fieldName, Object value, Root<T> root,
                                                    CriteriaBuilder criteriaBuilder) {
        String baseFieldName = fieldName.substring(0, fieldName.lastIndexOf('_'));
        String entityFieldName = baseFieldName.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();

        if (baseFieldName.equals("userId")) {
            // Special case for resident.id or police.id
            Predicate residentPredicate = fieldName.endsWith("_gte")
                    ? criteriaBuilder.greaterThanOrEqualTo(root.get("resident").get("id"), (Comparable) value)
                    : criteriaBuilder.lessThanOrEqualTo(root.get("resident").get("id"), (Comparable) value);
            Predicate policePredicate = fieldName.endsWith("_gte")
                    ? criteriaBuilder.greaterThanOrEqualTo(root.get("police").get("id"), (Comparable) value)
                    : criteriaBuilder.lessThanOrEqualTo(root.get("police").get("id"), (Comparable) value);
            return criteriaBuilder.or(residentPredicate, policePredicate);
        }

        // Validate entity field type
        try {
            Class<?> entityFieldType = root.get(entityFieldName).getJavaType();
            if (!isNumericType(entityFieldType)) {
                throw new IllegalArgumentException("Entity field " + entityFieldName + " is not a numeric type");
            }
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid entity field: " + entityFieldName, e);
        }

        if (fieldName.endsWith("_gte")) {
            return criteriaBuilder.greaterThanOrEqualTo(root.get(entityFieldName), (Comparable) value);
        } else {
            return criteriaBuilder.lessThanOrEqualTo(root.get(entityFieldName), (Comparable) value);
        }
    }

    /**
     * Handles enum fields (e.g., type, status).
     *
     * @param fieldName       The filter field name
     * @param value           The enum string value
     * @param root            JPA query root
     * @param criteriaBuilder JPA CriteriaBuilder
     * @return Predicate for the enum field
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> Predicate handleEnumField(String fieldName, String value, Root<T> root,
                                                 CriteriaBuilder criteriaBuilder) {

        try {
            Class<Enum> enumClass = (Class<Enum>) root.get(fieldName).getJavaType();
            String trimmedValue = value.trim().toUpperCase();

            for (Enum constant : enumClass.getEnumConstants()) {
                if (constant.name().equalsIgnoreCase(trimmedValue)) {
                    return criteriaBuilder.equal(root.get(fieldName), constant);
                }
            }

            throw new IllegalArgumentException("Invalid enum value: " + value);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid value for field: " + fieldName + ", expected one of: " +
                    Arrays.toString(root.get(fieldName).getJavaType().getEnumConstants()));
        }
    }

    /**
     * Handles direct string field matches.
     *
     * @param fieldName       The filter field name
     * @param value           The string value
     * @param root            JPA query root
     * @param criteriaBuilder JPA CriteriaBuilder
     * @return Predicate for the string field
     */
    private static <T> Predicate handleStringField(String fieldName, String value, Root<T> root,
                                                   CriteriaBuilder criteriaBuilder) {

        if (value.trim().isEmpty()) {
            return null;
        }
        String entityFieldName = fieldName.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
        return criteriaBuilder.equal(root.get(entityFieldName), value);
    }

    /**
     * Determines searchable fields for direct string matches by matching filter request fields to entity fields.
     *
     * @param filterClass The filter request class
     * @param entityClass The entity class
     * @return List of searchable entity field names
     */
    private static <T, R> List<String> getSearchableFields(Class<R> filterClass, Class<T> entityClass) {
        // Get entity fields
        Set<String> entityFields = Arrays.stream(entityClass.getDeclaredFields())
                .map(field -> {
                    try {
                        Annotation annotation = field.getAnnotation((Class<? extends Annotation>) Column.class);
                        if (annotation != null) {
                            Method nameMethod = Column.class.getMethod("name");
                            String columnName = (String) nameMethod.invoke(annotation);
                            if (columnName != null && !columnName.isEmpty()) {
                                return columnName;
                            }
                        }
                        return field.getName();
                    } catch (Exception e) {
                        return field.getName(); // Fallback to field name if annotation processing fails
                    }
                })
                .collect(Collectors.toSet());

        // Filter request fields that are Strings and match entity fields
        List<String> searchableFields = new ArrayList<>();
        for (Field field : filterClass.getDeclaredFields()) {
            String fieldName = field.getName();
            if (field.getType() == String.class &&
                    !fieldName.equals("q") &&
                    !fieldName.endsWith("_from") &&
                    !fieldName.endsWith("_to") &&
                    !fieldName.endsWith("_lte") &&
                    !fieldName.endsWith("_gte")) {
                String entityFieldName = fieldName.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
                if (entityFields.contains(entityFieldName)) {
                    searchableFields.add(entityFieldName);
                }
            }
        }
        return searchableFields;
    }

    /**
     * Checks if a type is a date type.
     *
     * @param type The field type
     * @return True if the type is LocalDate or LocalDateTime
     */
    private static boolean isDateType(Class<?> type) {
        return type == LocalDate.class || type == LocalDateTime.class;
    }

    /**
     * Checks if a type is numeric.
     *
     * @param type The field type
     * @return True if the type is numeric (Integer, Long, Double, Float)
     */
    private static boolean isNumericType(Class<?> type) {
        return type == Integer.class || type == Long.class || type == Double.class || type == Float.class;
    }
}
