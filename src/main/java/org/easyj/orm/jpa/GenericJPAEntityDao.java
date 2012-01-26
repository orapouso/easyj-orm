package org.easyj.orm.jpa;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.persistence.EntityExistsException;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import org.easyj.orm.GenericDao;
import org.easyj.orm.EntityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public class GenericJPAEntityDao <E extends Serializable, ID> implements GenericDao <E , ID> {

    @PersistenceContext
    protected EntityManager em;
    
    protected Class<E> entityClass;

    protected Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Persiste a entidade no banco de dados de acordo com o mapeamento do JPA
     *
     * @param entity Entity to be saved
     * @return saved entity
     * @throws EntityExistsException quando a entidade já existe no banco de dados
     * @throws PersistenceException para algum erro de inconsisência de dados ou erro genérico de persistência
     * @throws Exception qualquer outro erro inesperado
     */
    @Override
    public E save (E entity)
            throws EntityExistsException, PersistenceException, Exception {
        E newT = null;
        if(entity != null) {
            try {
                logger.debug("Saving entity: {} [{}]", entity.getClass().getSimpleName(), entity.toString());
                newT = merge(entity);
                logger.debug("Entity saved successfully: {} [{}]", entity.getClass().getSimpleName(), newT);
            } catch(EntityExistsException e) {
                logger.error("Error saving entity: Entity already exists");
                throw e;
            } catch(PersistenceException e) {
                logger.error("Error saving entity: Persistence error", e);
                throw e;
            } catch(Exception e) {
                logger.error("Error saving entity: UNEXPECTED ERROR", e);
                throw e;
            }
        }
        return newT;
    }

    public int saveByNamedQuery(String query, Map<String, Object> params) {
        return executeUpdate(query, params);
    }

    public int saveByNativeQuery(String query, Map<String, Object> params) {
        return executeUpdate(query, params);
    }

    /**
     * Deletes entity from database
     * 
     * @param t Entidade a ser removida
     */
    @Override
    public void delete(E t) {
        getEm().remove(t);
    }
    
    /**
     * Deletes entity from database
     * 
     * @param primaryKey Entity's primary key
     * @return removed entity
     */
    @Override
    public E delete(ID primaryKey) {
        E entity = findOne(primaryKey);
        if(entity != null) {
            delete(entity);
        }
        return entity;
    }

    /**
     * Finds a single entity by its primary key
     * 
     * @param primaryKey Entity's primary key
     * @return entity found or null if not found
     */
    @Override
    public E findOne(ID primaryKey) {
        return getEm().find(getEntityClass(), primaryKey);
    }
    
    @Override
    public List<E> findAll() {
        return getResultListByQuery("from " + getEntityClass().getName(), null);
    }

    @Override
    public List<E> findAll(Map<String, Object> params) {
        String q = "from " + getEntityClass().getName() + queryParams(params);
        
        return getResultListByQuery(q, params);
    }
    
    public E loadSingleByNamedQuery(String query, Map<String, Object> params) {
        E entity = null;
        try {
            logger.debug("Loading single entity {} using @NamedQuery=[{}], params=[{}]", new Object[] {getEntityClass().getSimpleName(), query, params});
            entity = (E) getSingleResultByNamedQuery(query, params);
            logger.debug("Entity loaded successfully: {} [{}]", getEntityClass().getSimpleName(), entity);
        } catch(IllegalArgumentException e) {
            logger.error("Entity not loaded: Could not find @NamedQuery=[{}] or @NamedQuery is invalid", query, e);
        } catch(NoResultException e) {
            logger.debug("Entity not loaded: @NamedQuery=[{}], params=[{}] returned nothing", query, params);
        } catch (NonUniqueResultException e) {
            logger.error("Entity not loaded: No unique result found for @NamedQuery=[{}] with params=[{}].", query, params);
        }
        return entity;
    }

    public E loadSingleByQuery(String query, Map<String, Object> params) {
        E entity = null;
        try {
            logger.debug("Loading single entity {} using JPQuery=[{}], params=[{}]", new Object[] {getEntityClass().getSimpleName(), query, params});
            entity = (E) getSingleResultByQuery(query, params);
            logger.debug("Entity loaded successfully: {} [{}]", getEntityClass().getSimpleName(), entity);
        } catch(IllegalArgumentException e) {
            logger.error("Entity not loaded: Invalid JPQuery=[{}]", query, e);
        } catch(NoResultException e) {
            logger.debug("Entity not loaded: JPQuery=[{}], params=[{}] returned nothing", query, params);
        } catch (NonUniqueResultException e) {
            logger.error("Entity not loaded: No unique result found for JPQuery=[{}] with params=[{}].", query, params);
        }
        return entity;
    }

    /**
     * Loads entities from a {@code @NamedQuery}. Accepts param {@code Map} to fill the query.
     * 
     * @param query {@code @NamedQuery} to be executed
     * @param klazz {@code Class} type from expected entities
     * @param params Parameters {@code Map} with key matching parameters in {@code @NamedQuery}. For queries without parameters, pass an empty {@code Map}.
     * @return entity list returned from the database
     */
    public List<E> loadListByNamedQuery(String query, Map<String, Object> params) {
        List<E> result = new ArrayList<E>();
        try {
            logger.debug("Loading entity list {} using @NamedQuery=[{}], params=[{}]", new Object[] {getEntityClass().getSimpleName(), query, params});
            result = getResultListByNamedQuery(query, params);
            logger.debug("Entity list loaded successfully: [{}]", getEntityClass().getSimpleName());
        } catch(IllegalArgumentException e) {
            logger.error("Entity list not loaded: Could not find @NamedQuery=[{}] or @NamedQuery is invalid", query, e);
        }
        return result;
    }

    public List<E> loadListByQuery(String query, Map<String, Object> params) {
        List<E> result = new ArrayList<E>();
        try {
            logger.debug("Loading entity list {} using JPQuery=[{}], params=[{}]", new Object[] {getEntityClass().getSimpleName(), query, params});
            result = getResultListByQuery(query, params);
            logger.debug("Entity list loaded successfully: [{}]", getEntityClass().getSimpleName());
        } catch(IllegalArgumentException e) {
            logger.error("Entity list not loaded: Invalid JPQuery=[{}]", query, e);
        }
        return result;
    }

    protected E merge(E entity) {
        E newEntity = null;
        try {
            newEntity = em.merge(entity);
        } finally {
            closeEm();
        }
        return newEntity;
    }

    protected int executeUpdate(String query, Map<String, Object> params) {
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

    protected E getSingleResultByNamedQuery(String query, Map<String, Object> params) {
        return getSingleResult(query, params, "named");
    }

    protected E getSingleResultByQuery(String query, Map<String, Object> params) {
        return getSingleResult(query, params, "");
    }

    protected E getSingleResult(String query, Map<String, Object> params, String queryType) {
        E entity = null;
        TypedQuery<E> q;
        try {
            if("named".equals(queryType)) {
                q = em.createNamedQuery(query, getEntityClass());
            } else {
                q = em.createQuery(query, getEntityClass());
            }
            if(setParameters(q, params)) {
                entity = (E) q.getSingleResult();
            }
        } finally {
            closeEm();
        }
        return (E) entity;
    }

    protected List<E> getResultListByNamedQuery(String query, Map<String, Object> params) {
        return getResultList(query, params, "named");
    }

    protected List<E> getResultListByQuery(String query, Map<String, Object> params) {
        return getResultList(query, params, "");
    }

    protected List<E> getResultList(String query, Map<String, Object> params, String queryType) {
        List<E> result = new ArrayList();
        TypedQuery<E> q;
        try {
            if("named".equals(queryType)) {
                q = em.createNamedQuery(query, getEntityClass());
            } else {
                q = em.createQuery(query, getEntityClass());
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
     * Adiciona mapa de parâmetros a query
     *
     * @param q Query que requer parâmetros
     * @param params Mapa de parâmetros
     * @return true se todos os parâmetros foram adicionados, false se houve erro em algum parâmetro
     */
    protected boolean setParameters(Query q, Map<String, Object> params) {
        if(q != null && params != null) {
            Integer maxResults = (Integer) params.remove(EntityService.PARAM_MAX_RESULTS);
            if(maxResults != null && maxResults > 0) {
                q.setMaxResults(maxResults.intValue());
            }

            Integer startPosition = (Integer) params.remove(EntityService.PARAM_START_POSITION);
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
     * Returns a query string from the parameters given
     * 
     * @param params
     * @return 
     */
    protected String queryParams(Map<String, Object> params) {
        String q = "";
        
        for(String key : params.keySet()) {
            q += " c." + key + " = :" + key + " AND ";
        }

        return q.substring(0, q.length() - 4);
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

    protected Class<E> getEntityClass() {
        if(entityClass == null) {
            ParameterizedType pt = (ParameterizedType) getClass().getGenericSuperclass();
            entityClass = (Class<E>) pt.getActualTypeArguments()[0];
        }
        
        return entityClass;
    }

}