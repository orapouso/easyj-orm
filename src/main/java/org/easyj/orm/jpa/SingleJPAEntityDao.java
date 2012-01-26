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
import org.easyj.orm.SingleService;
import org.springframework.stereotype.Repository;

@Repository
public class SingleJPAEntityDao extends AbstractSingleDao {

    @PersistenceContext
    private EntityManager em;

    @Override
    protected <E> E merge(E entity) {
        E newT = null;
        try {
            newT = em.merge(entity);
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
                q = em.createNativeQuery(query);
            } else {
                q = em.createNamedQuery(query);
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
    protected <E> E getSingleResultByQuery(String query, Map<String, Object> params, Class<E> klazz, QueryType queryType) {
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
    protected <E> List<E> getResultListByQuery(String query, Map<String, Object> params, Class<E> klazz, QueryType queryType) {
        List result = new ArrayList();
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
            Integer maxResults = (Integer) params.remove(SingleService.PARAM_MAX_RESULTS);
            if(maxResults != null && maxResults > 0) {
                q.setMaxResults(maxResults.intValue());
            }

            Integer startPosition = (Integer) params.remove(SingleService.PARAM_START_POSITION);
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
     * @param {@code EntityManager}
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
        return findListByQuery("FROM " + klazz.getName() + queryParams(params), klazz, params);
    }

}