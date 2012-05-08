/*
 *  Copyright 2009-2012 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.easyj.orm;

import java.util.List;
import java.util.Map;

/**
 * Interface that defines the access to {@code @Repository} layer
 * 
 * @author Rafael Raposo
 * @since 1.1.0
 */
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
