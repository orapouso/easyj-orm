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

package org.easyj.orm.jpa;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import org.easyj.orm.AbstractSingleDao;
import org.easyj.orm.SingleDao;
import org.springframework.stereotype.Repository;

/**
 * Single JPA {@code Dao} that implements JPA specific methods
 * 
 * @author Rafael Raposo
 * @since 1.1.0
 */
@Repository
public class SingleJPAEntityDao extends AbstractSingleDao {

    @PersistenceContext
    private EntityManager em;

    @Override
    protected <E> E merge(E entity) {
        E newT = null;
        try {
            newT = getEm().merge(entity);
        } finally {
            closeEm();
        }
        return newT;
    }

    @Override
    protected int executeUpdate(String query, Map<String, Object> params, QueryType queryType) {
        Query q;
        int result = -1;
        try {
            if(query.toLowerCase().startsWith("update ") || query.toLowerCase().startsWith("insert into") || query.toLowerCase().startsWith("delete from ")) {
                q = getEm().createNativeQuery(query);
            } else {
                q = getEm().createNamedQuery(query);
            }
            if(setParameters(q, params)) {
                result = q.executeUpdate();
            }
        } finally {
            closeEm();
        }
        return result;
    }

    @Override
    protected <E> E getSingleResultByQuery(String query, Class<E> klazz, Map<String, Object> params, QueryType queryType) {
        E entity = null;
        TypedQuery<E> q = null;
        try {
            if(QueryType.JPQL.equals(queryType)) {
                q = getEm().createQuery(query, klazz);
            } else if(QueryType.NAMED.equals(queryType)) {
                q = getEm().createNamedQuery(query, klazz);
            }
            if(setParameters(q, params)) {
                entity = q.getSingleResult();
            }
        } finally {
            closeEm();
        }
        return entity;
    }

    @Override
    protected <E> List<E> getResultListByQuery(String query, Class<E> klazz, Map<String, Object> params, QueryType queryType) {
        List<E> result = new ArrayList<E>();
        TypedQuery<E> q = null;
        try {
            if(QueryType.JPQL.equals(queryType)) {
                q = getEm().createQuery(query, klazz);
            } else if(QueryType.NAMED.equals(queryType)) {
                q = getEm().createNamedQuery(query, klazz);
            }
            if(setParameters(q, params)) {
                result = q.getResultList();
            }
        } finally {
            closeEm();
        }
        return result;
    }

    /**
     * Binds parameter map to the query.
     * 
     * There are two special parameters that is of use:
     * {@link SingleService.PARAM_MAX_RESULTS} used to limit maximum results returned
     * {@link SingleService.PARAM_START_POSITION} used to tell the starting position the result should start
     *
     * @param q query to bind parameters
     * @param params parameter map to bind into the query
     * @return true if all parameters where bound successfully, otherwise false
     */
    private boolean setParameters(Query q, Map<String, Object> params) {
        if(q != null && params != null) {
            Integer maxResults = (Integer) params.remove(SingleDao.PARAM_MAX_RESULTS);
            if(maxResults != null && maxResults > 0) {
                q.setMaxResults(maxResults.intValue());
            }

            Integer startPosition = (Integer) params.remove(SingleDao.PARAM_START_POSITION);
            if(startPosition != null && startPosition > -1) {
                q.setFirstResult(startPosition.intValue());
            }

            for(Entry<String, Object> o : params.entrySet()) {
                try {
                    q.setParameter(o.getKey().trim(), o.getValue());
                } catch(IllegalArgumentException ex) {
                    logger.debug("Illegal Query Parameter", ex);
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Sets the {@code EntityManager}
     * @param em {@code EntityManager}
     */
    public void setEm(EntityManager em) {
        this.em = em;
    }

    /**
     * Returns the {@code EntityManager}
     * @return {@code EntityManager}
     */
    public EntityManager getEm() {
        return em;
    }

    public void closeEm() {
        if(getEm().isOpen()) {
            getEm().close();
        }
    }

     /**
     * Finds a single entity in the database
     * 
     * @param klazz class of entity to be found
     * @param primaryKey primary key value of the entity to be found
     * @return entity found or null if none is found
     */
    @Override
    public <E, ID> E findOne(Class<E> klazz, ID primaryKey) {
        return getEm().find(klazz, primaryKey);
    }

    @Override
    protected <E> void remove(E entity) {
        getEm().remove(entity);
    }

    @Override
    public <E> List<E> findAll(Class<E> klazz) {
        return findAll(klazz, null);
    }

    @Override
    public <E> List<E> findAll(Class<E> klazz, Map<String, Object> params) {
        return findListByQuery("FROM " + klazz.getName() + " c " + queryParams(params), klazz, params);
    }

}