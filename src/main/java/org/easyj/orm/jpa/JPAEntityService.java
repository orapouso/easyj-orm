package org.easyj.orm.jpa;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Resource;
import javax.persistence.EntityExistsException;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import org.easyj.orm.EntityService;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class JPAEntityService implements EntityService {

    @Resource(name="JPAEntityDao")
    private JPAEntityDao dao;

    protected Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public <T> String save(T t) {
        String ret = EntityService.STATUS_SUCCESS;
        T newT;

        try {
            newT = dao.save(t);

            Method getId = t.getClass().getMethod("getId", new Class[0]);

            t.getClass().getMethod("setId", getId.getReturnType()).invoke(t, getId.invoke(newT, new Object[0]));
        } catch(EntityExistsException ex) {
            ret = EntityService.STATUS_ERROR_EXISTS;
        } catch(ConstraintViolationException ex) {
            ret = EntityService.STATUS_ERROR_CONSTRAINT_VIOLATION;
        } catch (NoSuchMethodException ex) {
            //ignore
        } catch(Exception ex) {
            ret = EntityService.STATUS_ERROR;
        }

        return ret;
    }

    @Override
    public <T> int remove(Class<T> klazz, Map<String, Object> params) {
        return dao.remove(klazz, params);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T load(Class<T> klazz, final Object param) {
        if(param instanceof Map) {
            return loadByNamedQuery(klazz.getSimpleName() + ".findById", klazz, (Map<String, Object>) param);
        } else {
            return getDao().load(klazz, param);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T loadUK(T t) {
        return loadUK((Class<T>) t.getClass(), fillUKParams(t));
    }

    @Override
    public <T> T loadUK(Class<T> klazz, Map<String, Object> params) {
        String namedQuery = klazz.getSimpleName() + ".findByUK";
        String query = getNamedQuery(namedQuery, klazz);
        if(query != null) {
            return loadByNamedQuery(klazz.getSimpleName() + ".findByUK", klazz, params);
        }
        query = "SELECT k FROM " + klazz.getSimpleName() + " c WHERE ";
        for(String key : params.keySet()) {
            query += " c." + key + " = :" + key + " ";
        }
        return loadByQuery(query, klazz, params);
    }

    @Override
    public <T> List<T> loadAll(Class<T> klazz) {
        String namedQuery = klazz.getSimpleName() + ".findAll";
        String query = getNamedQuery(namedQuery, klazz);
        if(query != null) {
            return loadListByNamedQuery(namedQuery, klazz, null);
        }
        query = "SELECT k FROM " + klazz.getSimpleName() + " c ";
        return loadListByQuery(query, klazz, null);
    }

    @Override
    public <T> List<T> loadList(Class<T> klazz, Map<String, Object> params) {
        String namedQuery = klazz.getSimpleName() + ".findList";
        String query = getNamedQuery(namedQuery, klazz);
        if(query != null) {
            return loadListByNamedQuery(namedQuery, klazz, params);
        }
        query = "SELECT k FROM " + klazz.getSimpleName() + " c WHERE ";
        for(String key : params.keySet()) {
            query += " c." + key + " = :" + key + " ";
        }
        return loadListByQuery(query, klazz, params);
    }

    public <T> List<T> loadList(Class<T> klazz, String namedQuery, Map<String, Object> params) {
        String query = getNamedQuery(namedQuery, klazz);
        if(query != null) {
            return loadListByNamedQuery(namedQuery, klazz, params);
        }
        query = namedQuery;
        return loadListByQuery(query, klazz, params);
    }

    protected <T> T loadByNamedQuery(String namedQuery, Class<T> klazz, Map<String, Object> params) {
        return getDao().loadSingleByNamedQuery(namedQuery, klazz, params);
    }

    protected <T> T loadByQuery(String query, Class<T> klazz, Map<String, Object> params) {
        return getDao().loadSingleByQuery(query, klazz, params);
    }

    protected <T> List<T> loadListByNamedQuery(String namedQuery, Class<T> klazz, Map<String, Object> params) {
        return getDao().loadListByNamedQuery(namedQuery, klazz, params);
    }

    protected <T> List<T> loadListByQuery(String query, Class<T> klazz, Map<String, Object> params) {
        return getDao().loadListByQuery(query, klazz, params);
    }

    public JPAEntityDao getDao() {
        return this.dao;
    }

    protected <T> Map<String, Object> fillUKParams(T entity) {
        String query = null;
        String lowerQuery = null;
        for(NamedQuery n : entity.getClass().getAnnotation(NamedQueries.class).value()) {
            if(n.name().toLowerCase().indexOf(".findbyuk") > -1) {
                query = n.query();
                lowerQuery = query.toLowerCase();
                break;
            }
        }


        String[] props;
        Object oParam;
        Map<String, Object> ukParams = new HashMap<String, Object>();

        if(query != null) {
            int ini = lowerQuery.indexOf(" where ") + " where ".length();
            int end = lowerQuery.indexOf(" order by ");
            query = query.substring(ini, end > -1 ? end : query.length()).trim();
            String[] params = query.split(" and ");
            for(String param : params) {
                try {
                    props = param.split("=")[0].trim().split("\\.");
                    oParam = entity;
                    for(String prop : props) {
                        try {
                            oParam = oParam.getClass().getMethod("get" + StringUtils.capitalize(prop)).invoke(oParam);
                        } catch(Exception silent) {}
                    }
                    ukParams.put(param.split("=")[1].replace(":", ""), oParam);
                } catch (Exception ex) {}
            }
        }

        return ukParams;
    }

    protected <T> String getNamedQuery(String query, Class<T> klazz){
        NamedQueries queries = klazz.getAnnotation(NamedQueries.class);

        for(NamedQuery named : queries.value()) {
            if(query.equals(named.name())) {
                return named.query();
            }
        }

        return null;
    }

}
