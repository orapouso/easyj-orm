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
import javax.persistence.EntityExistsException;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import javax.persistence.RollbackException;
import org.easyj.orm.EntityService;
import org.hibernate.PropertyValueException;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.DataException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Complete JPA Dao that does almost everything related to persistence
 * 
 * @author Rafael Raposo
 * @since 1.0.0
 * @deprecated Use SingleJPAEntityDao instead
 */
@Repository
@Transactional
public class JPAEntityDao {

    @PersistenceContext
    private EntityManager em;

    protected Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Persiste a entidade no banco de dados de acordo com o mapeamento do JPA
     *
     * @param t Entidade a ser persistida
     * @return T entidade persistida no banco:<br>
     * @throws EntityExistsException quando a entidade já existe no banco de dados
     * @throws PersistenceException para algum erro de inconsisência de dados ou erro genérico de persistência
     * @throws Exception qualquer outro erro inesperado
     */
    public <T> T save (T t)
            throws EntityExistsException, PersistenceException, Exception {
        T newT = null;
        if(t != null) {
            try {
                logger.debug("Saving entity: {} [{}]", t.getClass().getSimpleName(), t.toString());
                newT = merge(t);
                logger.debug("Entity saved successfully: {} [{}]", t.getClass().getSimpleName(), newT);
            } catch(EntityExistsException e) {
                logger.error("Error saving entity: Entity already exists");
                throw e;
            } catch(PersistenceException e) {
                if(e instanceof RollbackException && e.getCause().getClass() == PersistenceException.class) {
                    e = (PersistenceException) e.getCause();
                }
                String msg;
                if(e.getCause() instanceof ConstraintViolationException) {
                    msg = e.getCause().getCause().getMessage();
                    logger.error("Error saving entity: some constraint violation occurred: [{}] - {}", t.toString(), msg);
                    if(msg.toLowerCase().indexOf("duplicate") > -1) {
                        throw new EntityExistsException(msg);
                    } else {
                        throw (ConstraintViolationException) e.getCause();
                    }
                } else if(e.getCause() instanceof DataException){
                    logger.error("Error saving entity: inconsistent data", e);
                } else if(e.getCause() instanceof PropertyValueException){
                    logger.error("Error saving entity: missing mandatory (NOT NULL) values", e);
                } else {
                    logger.error("Error saving entity: generic persistence error", e);
                }
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
     * Remove a entidade do banco de dados
     * 
     * @param <T> Entidade a ser removida
     * @return entidade removida
     */
    @SuppressWarnings("unchecked")
    public <T> int remove(Class<T> klazz, Map<String, Object> params) {
        int ret = 0;
        if(params.size() > 0) {
            ret = saveByNamedQuery(klazz.getSimpleName() + ".delete", params);
        }
        return ret;
    }

    @SuppressWarnings("unchecked")
    public <T> T load(Class<T> klazz, Object param) {
        return getEm().find(klazz, param);
    }

    @SuppressWarnings("unchecked")
    public <T> T loadSingleByNamedQuery(String query, Class<T> klazz, Map<String, Object> params) {
        T t = null;
        try {
            logger.debug("Loading single entity {} using @NamedQuery=[{}], params=[{}]", new Object[] {klazz.getSimpleName(), query, params});
            t = (T) getSingleResultByNamedQuery(query, params);
            logger.debug("Entity loaded successfully: {} [{}]", klazz.getSimpleName(), t);
        } catch(IllegalArgumentException e) {
            logger.error("Entity not loaded: Could not find @NamedQuery=[{}] or @NamedQuery is invalid", query, e);
        } catch(NoResultException e) {
            logger.debug("Entity not loaded: @NamedQuery=[{}], params=[{}] returned nothing", query, params);
        } catch (NonUniqueResultException e) {
            logger.error("Entity not loaded: No unique result found for @NamedQuery=[{}] with params=[{}].", query, params);
        }
        return t;
    }

    @SuppressWarnings("unchecked")
    public <T> T loadSingleByQuery(String query, Class<T> klazz, Map<String, Object> params) {
        T t = null;
        try {
            logger.debug("Loading single entity {} using JPQuery=[{}], params=[{}]", new Object[] {klazz.getSimpleName(), query, params});
            t = (T) getSingleResultByQuery(query, params);
            logger.debug("Entity loaded successfully: {} [{}]", klazz.getSimpleName(), t);
        } catch(IllegalArgumentException e) {
            logger.error("Entity not loaded: Invalid JPQuery=[{}]", query, e);
        } catch(NoResultException e) {
            logger.debug("Entity not loaded: JPQuery=[{}], params=[{}] returned nothing", query, params);
        } catch (NonUniqueResultException e) {
            logger.error("Entity not loaded: No unique result found for JPQuery=[{}] with params=[{}].", query, params);
        }
        return t;
    }

    @SuppressWarnings("unchecked")
    public Object loadSingleByNativeQuery(String query, Map<String, Object> params) {
        Object o = null;
        try {
            logger.debug("Loading single entity using NativeQuery=[{}], params=[{}]", query, params);
            o = getSingleResultByNativeQuery(query, params);
            logger.debug("Query loaded successfully: NativeQuery=[{}], object=[{}]", query, o);
        } catch (NoResultException e) {
            logger.debug("Query not loaded: NativeQuery=[{}], params=[{}] returned nothing", query, params);
        } catch (NonUniqueResultException e) {
            logger.error("Query not loaded: No unique result found for NativeQuery=[{}] with params=[{}].", query, params);
        }
        return o;
    }

    @SuppressWarnings("unchecked")
    public <T> T loadSingleByNativeQuery(String query, Class<T> klazz, Map<String, Object> params) {
        T t = null;
        try {
            logger.debug("Loading single entity {} using NativeQuery=[{}], params=[{}]", new Object[] {klazz.getSimpleName(), query, params});
            t = (T) getSingleResultByNativeQuery(query, params);
            logger.debug("Entity loaded successfully: {} [{}]", klazz.getSimpleName(), t);
        } catch (NoResultException e) {
            logger.debug("Entity not loaded: NativeQuery=[{}], params=[{}] returned nothing", query, params);
        } catch (NonUniqueResultException e) {
            logger.error("Entity not loaded: No unique result found for NativeQuery=[{}] with params=[{}].", query, params);
        }
        return t;
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
    public <T> List<T> loadListByNamedQuery(String query, Class<T> klazz, Map<String, Object> params) {
        List<T> result = new ArrayList<T>();
        try {
            logger.debug("Loading entity list {} using @NamedQuery=[{}], params=[{}]", new Object[] {klazz.getSimpleName(), query, params});
            result = getResultListByNamedQuery(query, params);
            logger.debug("Entity list loaded successfully: [{}]", klazz.getSimpleName());
        } catch(IllegalArgumentException e) {
            logger.error("Entity list not loaded: Could not find @NamedQuery=[{}] or @NamedQuery is invalid", query, e);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> loadListByQuery(String query, Class<T> klazz, Map<String, Object> params) {
        List<T> result = new ArrayList<T>();
        try {
            logger.debug("Loading entity list {} using JPQuery=[{}], params=[{}]", new Object[] {klazz.getSimpleName(), query, params});
            result = getResultListByQuery(query, params);
            logger.debug("Entity list loaded successfully: [{}]", klazz.getSimpleName());
        } catch(IllegalArgumentException e) {
            logger.error("Entity list not loaded: Invalid JPQuery=[{}]", query, e);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> loadListByQuery(String query, Map<String, Object> params) {
        List result = new ArrayList();
        try {
            logger.debug("Loading entity list using NativeQuery=[{}], params=[{}]", query, params);
            result = getResultListByQuery(query, params);
            logger.debug("Entity list loaded successfully: NativeQuery=[{}]", query);
        } catch(IllegalArgumentException e) {
            logger.error("Entity list not loaded: Invalid NativeQuery=[{}]", query, e);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> loadListByNativeQuery(String query, Map<String, Object> params) {
        return (List<Object[]>) getResultListByNativeQuery(query, params);
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> loadListByNativeQuery(String query, Class<T> klazz, Map<String, Object> params) {
        return (List<T>) getResultListByNativeQuery(query, params);
    }

    private <T> T merge(T t) {
        T newT = null;
        try {
            newT = em.merge(t);
        } finally {
            closeEm();
        }
        return newT;
    }

    private int executeUpdate(String query, Map<String, Object> params) {
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

    private Object getSingleResultByNamedQuery(String query, Map<String, Object> params) {
        return getSingleResult(query, params, "named");
    }

    private Object getSingleResultByNativeQuery(String query, Map<String, Object> params) {
        return getSingleResult(query, params, "native");
    }

    private Object getSingleResultByQuery(String query, Map<String, Object> params) {
        return getSingleResult(query, params, "");
    }

    private Object getSingleResult(String query, Map<String, Object> params, String queryType) {
        Object o = null;
        Query q;
        try {
            if("native".equals(queryType)) {
                q = em.createNativeQuery(query);
            } else if("named".equals(queryType)) {
                q = em.createNamedQuery(query);
            } else {
                q = em.createQuery(query);
            }
            if(setParameters(q, params)) {
                o = q.getSingleResult();
            }
        } finally {
            closeEm();
        }
        return o;
    }

    private List getResultListByNamedQuery(String query, Map<String, Object> params) {
        return getResultList(query, params, "named");
    }

    private List getResultListByNativeQuery(String query, Map<String, Object> params) {
        return getResultList(query, params, "native");
    }

    private List getResultListByQuery(String query, Map<String, Object> params) {
        return getResultList(query, params, "");
    }

    private List getResultList(String query, Map<String, Object> params, String queryType) {
        List result = new ArrayList();
        Query q;
        try {
            if("native".equals(queryType)) {
                q = em.createNativeQuery(query);
            } else if("named".equals(queryType)) {
                q = em.createNamedQuery(query);
            } else {
                q = em.createQuery(query);
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
    private boolean setParameters(Query q, Map<String, Object> params) {
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

}