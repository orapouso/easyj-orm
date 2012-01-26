package org.easyj.orm;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityExistsException;
import javax.persistence.PersistenceException;

public interface GenericDao<E extends Serializable, ID> {

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

    public E save(E entity) throws EntityExistsException, PersistenceException, Exception;
    public void delete(E entity);
    public E delete(ID primaryKey);
    public E findOne(ID id);
    public List<E> findAll();
    public List<E> findAll(Map<String, Object> params);
}
