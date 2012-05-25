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
import javax.persistence.NoResultException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

/**
 * Abstract {@code Dao} that defines access methods to {@code @Services}
 * <br><br>
 * Concrete classes need to implement technology (JPA, JDBC) specific methods
 * 
 * @author Rafael Raposo
 * @since 1.1.0
 */
@Transactional
public abstract class AbstractSingleDao implements SingleDao {
    
    protected Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Persists entity into database
     *
     * @param entity Entity to be persisted
     * @return persisted entity
     */
    @Override
    public <E> E save (E entity) {
        E newT = null;
        if(entity != null) {
            newT = merge(entity);
        }
        return newT;
    }

    /**
     * Executes an update using a pre-defined {@code @NamedQuery} or a custom JPQL query
     * 
     * 
     * @param query query to be compiled and executed
     * @param params parameters to fill into the query. Can be null if there are no parameters
     * @return the number of rows affected by the update
     */
    public int saveByQuery(String query, Map<String, Object> params) {
        String lcq = query.toLowerCase();
        if(lcq.startsWith("insert into ") || lcq.startsWith("update ") || lcq.startsWith("delete from ")) {
            return executeUpdate(query, params, QueryType.JPQL);
        }
        return executeUpdate(query, params, QueryType.NAMED);
    }

    /**
     * Executes an update using a database driven native query
     * 
     * @param query query to be compiled and executed
     * @param params parameters to fill into the query. Can be null if there are no parameters
     * @return the number of rows affected by the update
     */
    public int saveByNativeQuery(String query, Map<String, Object> params) {
        return executeUpdate(query, params, QueryType.NATIVE);
    }

    /**
     * Removes entity from database
     * 
     * @param entity Entity to be removed
     * @return removed entity
     */
    @Override
    public <E> E delete(E entity) {
        if(entity != null) {
            remove(entity);
        }
        
        return entity;
    }

    /**
     * Deletes entity from database
     * 
     * @param klazz Entity {@code Class}
     * @param primaryKey Entity's primary key value
     * @return removed entity
     */
    @Override
    public <E, ID> E delete(Class<E> klazz, ID primaryKey) {
        E entity = findOne(klazz, primaryKey);
        
        return delete(entity);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <E> E findByQuery(String query, Class<E> klazz, Map<String, Object> params) {
        if(query.toLowerCase().indexOf("from ") > -1) {
            return findSingleResultByQuery(query, params, klazz, QueryType.JPQL);
        }
        return findSingleResultByQuery(query, params, klazz, QueryType.NAMED);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <E> E findByNativeQuery(String query, Class<E> klazz, Map<String, Object> params) {
        return findSingleResultByQuery(query, params, klazz, QueryType.NATIVE);
    }
    
    public <E> E findSingleResultByQuery(String query, Map<String, Object> params, Class<E> klazz, QueryType type) {
        try {
            return getSingleResultByQuery(query, klazz, params, type);
        } catch(NoResultException ex) {}
        return null;
    }

    /**
     * Loads entities from a {@code @NamedQuery}. Accepts param {@code Map} to fill the query.
     * 
     * @param query {@code @NamedQuery} to be executed
     * @param klazz {@code Class} type from expected entities
     * @param params Parameters {@code Map} with key matching parameters in {@code @NamedQuery}. For queries without parameters, pass an empty {@code Map}.
     * @return entity list returned from the database
     */
    @SuppressWarnings("unchecked")
    @Override
    public <E> List<E> findListByQuery(String query, Class<E> klazz, Map<String, Object> params) {
        if(query.toLowerCase().indexOf("from ") > -1) {
            return getResultListByQuery(query, klazz, params, QueryType.JPQL);
        }
        return getResultListByQuery(query, klazz, params, QueryType.NAMED);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <E> List<E> findListByNativeQuery(String query, Class<E> klazz, Map<String, Object> params) {
        return (List<E>) getResultListByQuery(query, klazz, params, QueryType.NATIVE);
    }

    protected abstract <E> E merge(E entity);
    
    protected abstract <E> void remove(E entity);
    
    protected abstract int executeUpdate(String query, Map<String, Object> params, QueryType queryType);

    protected abstract <E> E getSingleResultByQuery(String query, Class<E> klazz, Map<String, Object> params, QueryType queryType);

    protected abstract <E> List<E> getResultListByQuery(String query, Class<E> klazz, Map<String, Object> params, QueryType queryType);

    /**
     * Returns a query string from the parameters given
     * 
     * @param params
     * @return query string
     */
    protected String queryParams(Map<String, Object> params) {
        if(params == null) return "";
        String q = "";
        
        for(String key : params.keySet()) {
            q += " c." + key + " = :" + key + " AND ";
        }

        return q.substring(0, q.length() - 4);
    }

}