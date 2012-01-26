package org.easyj.orm;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

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
     * Executes an update using a database driven native query defined inside an entity
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
     * @param klazz Entity {@code Class}
     * @param primaryKey Entity's primary key value
     * @return removed entity
     */
    @Override
    public <E> E delete(E entity) {
        remove(entity);
        
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
    public <E, ID> E delete(Class<E> klazz, ID primarykey) {
        E entity = findOne(klazz, primarykey);
        
        delete(entity);
        
        return entity;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <E> E findByQuery(String query, Class<E> klazz, Map<String, Object> params) {
        if(query.toLowerCase().indexOf("from ") > -1) {
            return getSingleResultByQuery(query, params, klazz, QueryType.JPQL);
        }
        return getSingleResultByQuery(query, params, klazz, QueryType.NAMED);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <E> E findByNativeQuery(String query, Class<E> klazz, Map<String, Object> params) {
        return getSingleResultByQuery(query, params, klazz, QueryType.NATIVE);
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
            return getResultListByQuery(query, params, klazz, QueryType.JPQL);
        }
        return getResultListByQuery(query, params, klazz, QueryType.NAMED);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <E> List<E> findListByNativeQuery(String query, Class<E> klazz, Map<String, Object> params) {
        return (List<E>) getResultListByQuery(query, params, klazz, QueryType.NATIVE);
    }

    protected abstract <E> E merge(E entity);
    
    protected abstract <E> void remove(E entity);
    
    protected abstract int executeUpdate(String query, Map<String, Object> params, QueryType queryType);

    protected abstract <E> E getSingleResultByQuery(String query, Map<String, Object> params, Class<E> klazz, QueryType queryType);

    protected abstract <E> List<E> getResultListByQuery(String query, Map<String, Object> params, Class<E> klazz, QueryType queryType);

    /**
     * Returns a query string from the parameters given
     * 
     * @param params
     * @return 
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