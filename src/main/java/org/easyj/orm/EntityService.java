package org.easyj.orm;

import java.util.List;
import java.util.Map;

public interface EntityService {

    public static final String STATUS_SUCCESS                    = "success";
    public static final String STATUS_ERROR                      = "error";

    public static final String ENTITY_NULL                       = "error.entity.null";
    public static final String ENTITY_NOT_FOUND                  = "error.entity.not.found";
    public static final String STATUS_ERROR_EXISTS               = "error.entity.exists";
    public static final String STATUS_ERROR_CONSTRAINT_VIOLATION = "error.constraint.violation";
    public static final String NO_PARAMS_SET                     = "error.no.params";
    public static final String NULL_PARAM                        = "error.null.param";
    public static final String INVALID_PARAM                     = "error.invalid.param";

    public <T> String save(T t);
    public <T> int remove(Class<T> klazz, Map<String, Object> params);
    public <T> T load(Class<T> klazz, final Object id);
    public <T> T loadUK(T t);
    public <T> T loadUK(Class<T> klazz, Map<String, Object> params);
    public <T> List<T> loadAll(Class<T> klazz);
    public <T> List<T> loadList(Class<T> klazz, Map<String, Object> params);
}
