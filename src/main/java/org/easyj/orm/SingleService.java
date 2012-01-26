package org.easyj.orm;

import java.util.List;
import java.util.Map;

public interface SingleService {

    public static final String STATUS_SUCCESS                    = "success";
    public static final String STATUS_ERROR                      = "error";

    public static final String ENTITY_NULL                       = "error.entity.null";
    public static final String ENTITY_NOT_FOUND                  = "error.entity.not.found";
    public static final String STATUS_ERROR_EXISTS               = "error.entity.exists";
    public static final String STATUS_ERROR_CONSTRAINT_VIOLATION = "error.constraint.violation";
    public static final String NO_PARAMS_SET                     = "error.no.params";
    public static final String NULL_PARAM                        = "error.null.param";
    public static final String INVALID_PARAM                     = "error.invalid.param";
    
    public static final String PARAM_MAX_RESULTS = "maxResults";
    public static final String PARAM_START_POSITION = "startPosition";

    public <E> E save(E entity);
    public <E> E delete(E entity);
    public <E, ID> E delete(Class<E> klazz, ID primaryKey);
    public <E, ID> E findOne(Class<E> klazz, ID id);
    public <E> List<E> findAll(Class<E> klazz);
    public <E> List<E> findAll(Class<E> klazz, Map<String, Object> params);
    
    public <E> E findByQuery(String query, Class<E> klazz, Map<String, Object> params);
    public <E> E findByNativeQuery(String query, Class<E> klazz, Map<String, Object> params);
    
    public <E> List<E> findListByQuery(String query, Class<E> klazz, Map<String, Object> params);
    public <E> List<E> findListByNativeQuery(String query, Class<E> klazz, Map<String, Object> params);
}
