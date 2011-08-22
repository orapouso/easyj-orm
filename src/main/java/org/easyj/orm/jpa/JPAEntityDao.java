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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.PropertyValueException;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.DataException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public class JPAEntityDao {

    @PersistenceContext
    private EntityManager em;

    protected Log logger = LogFactory.getLog(getClass());

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
                logger.debug("Saving entity: " + t.getClass().getSimpleName() + " [" + t.toString() + "]");
                newT = merge(t);
                logger.debug("Entity saved successfully: " + t.getClass().getSimpleName() + " [" + newT + "]");
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
                    logger.error("Error saving entity: some constraint violation occurred: [" + t.toString() + "] - " + msg);
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
            logger.debug("Loading single entity " + klazz.getSimpleName() + " using @NamedQuery=[" + query + "], params=[" + params + "]");
            t = (T) getSingleResultByNamedQuery(query, params);
            logger.debug("Entity loaded successfully: " + klazz.getSimpleName() + " [" + t + "]");
        } catch(IllegalArgumentException e) {
            logger.error("Entity not loaded: Could not find @NamedQuery=[" + query + "] or @NamedQuery is invalid", e);
        } catch(NoResultException e) {
            logger.info("Entity not loaded: @NamedQuery=[" + query + "], params=[" + params + "] returned nothing");
        } catch (NonUniqueResultException e) {
            logger.error("Entity not loaded: No unique result found for @NamedQuery=[" + query + "] with params=[" + params + "].");
        }
        return t;
    }

    @SuppressWarnings("unchecked")
    public <T> T loadSingleByQuery(String query, Class<T> klazz, Map<String, Object> params) {
        T t = null;
        try {
            logger.debug("Loading single entity " + klazz.getSimpleName() + " using JPQuery=[" + query + "], params=[" + params + "]");
            t = (T) getSingleResultByQuery(query, params);
            logger.debug("Entity loaded successfully: " + klazz.getSimpleName() + " [" + t + "]");
        } catch(IllegalArgumentException e) {
            logger.error("Entity not loaded: Invalid JPQuery=[" + query + "]", e);
        } catch(NoResultException e) {
            logger.info("Entity not loaded: JPQuery=[" + query + "], params=[" + params + "] returned nothing");
        } catch (NonUniqueResultException e) {
            logger.error("Entity not loaded: No unique result found for JPQuery=[" + query + "] with params=[" + params + "].");
        }
        return t;
    }

    @SuppressWarnings("unchecked")
    public Object loadSingleByNativeQuery(String query, Map<String, Object> params) {
        Object o = null;
        try {
            logger.debug("Loading single entity using NativeQuery=[" + query + "], params=[" + params + "]");
            o = getSingleResultByNativeQuery(query, params);
            logger.debug("Query loaded successfully: NativeQuery=[" + query + "], object=[" + o + "]");
        } catch (NoResultException e) {
            logger.info("Query not loaded: NativeQuery=[" + query + "] with params=[" + params + "] returned nothing");
        } catch (NonUniqueResultException e) {
            logger.error("Query not loaded: No unique result found for NativeQuery=[" + query + "] with params=[" + params + "].");
        }
        return o;
    }

    @SuppressWarnings("unchecked")
    public <T> T loadSingleByNativeQuery(String query, Class<T> klazz, Map<String, Object> params) {
        T t = null;
        try {
            logger.debug("Loading entity " + klazz.getSimpleName() + " using NativeQuery=[" + query + "], params=[" + params + "]");
            t = (T) getSingleResultByNativeQuery(query, params);
            logger.debug("Entity loaded successfully: " + klazz.getSimpleName() + " [" + t + "]");
        } catch (NoResultException e) {
            logger.info("Entity not loaded: NativeQuery=[" + query + "], params=[" + params + "] returned nothing");
        } catch (NonUniqueResultException e) {
            logger.error("Entity not loaded: No unique result found for NativeQuery=[" + query + "] with params=[" + params + "].");
        }
        return t;
    }

    /**
     * Carrega lista de entidades a partir de uma {@code @NamedQuery}. Também atribui os paramêtros de acordo com um {@code Map}.
     * 
     * @param query {@code @NamedQuery} a ser carregada do banco de dados
     * @param klazz {@code Class} da entidade a ter os registros retornados
     * @param params {@code Map} de parâmetros com nome/valor dos parâmetros na {@code @NamedQuery}. Para queries sem parâmetros, passar um {@code Map} vazio.
     * @return lista com as entidades retornadas pela {@code @NamedQuery}
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> loadListByNamedQuery(String query, Class<T> klazz, Map<String, Object> params) {
        List<T> result = new ArrayList<T>();
        try {
            logger.debug("Loading entity list " + klazz.getSimpleName() + " using @NamedQuery=[" + query + "], params=[" + params + "]");
            result = getResultListByNamedQuery(query, params);
            logger.debug("Entity list loaded successfully: " + klazz.getSimpleName());
        } catch(IllegalArgumentException e) {
            logger.error("Entity list not loaded: Could not find @NamedQuery=[" + query + "] or @NamedQuery is invalid", e);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> loadListByQuery(String query, Class<T> klazz, Map<String, Object> params) {
        List<T> result = new ArrayList<T>();
        try {
            logger.debug("Loading entity list " + klazz.getSimpleName() + " using JPQuery=[" + query + "], params=[" + params + "]");
            result = getResultListByQuery(query, params);
            logger.debug("Entity list loaded successfully: " + klazz.getSimpleName());
        } catch(IllegalArgumentException e) {
            logger.error("Entity list not loaded: Invalid JPQuery=[" + query + "]", e);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> loadListByQuery(String query, Map<String, Object> params) {
        List result = new ArrayList();
        try {
            logger.debug("Loading entity list using NativeQuery=[" + query + "], params=[" + params + "]");
            result = getResultListByQuery(query, params);
            logger.debug("Entity list loaded successfully: NativeQuery=[" + query + "]");
        } catch(IllegalArgumentException e) {
            logger.error("Entity list not loaded: Invalid NativeQuery=[" + query + "]", e);
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
            Integer maxResults = (Integer) params.remove("maxResults");
            if(maxResults != null && maxResults > 0) {
                q.setMaxResults(maxResults.intValue());
            }

            Integer startPosition = (Integer) params.remove("startPosition");
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

}