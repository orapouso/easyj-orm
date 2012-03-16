package org.easyj.orm.jpa;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.persistence.EntityExistsException;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import org.easyj.orm.EntityService;
import org.easyj.orm.GenericService;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

public class GenericJPAEntityService<E extends Serializable, ID> implements GenericService< E, ID > {

    protected GenericJPAEntityDao<E, ID> dao;

    protected Class<E> entityClass;

    protected Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public String save(E entity) {
        String ret = EntityService.STATUS_SUCCESS;
        E newEntity;

        try {
            newEntity = dao.save(entity);

            Method getId = entity.getClass().getMethod("getId", new Class[0]);

            entity.getClass().getMethod("setId", getId.getReturnType()).invoke(entity, getId.invoke(newEntity, new Object[0]));
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
    public void delete(E entity) {
        dao.delete(entity);
    }

    @Override
    public E delete(ID primaryKey) {
        return dao.delete(primaryKey);
    }

    @Override
    public E findOne(ID primaryKey) {
        return dao.findOne(primaryKey);
    }

    @SuppressWarnings("unchecked")
    @Override
    public E findByUK(E t) {
        return findByUK(fillUKParams(t));
    }

    @Override
    public E findByUK(Map<String, Object> params) {
        String namedQuery = getEntityClass().getSimpleName() + ".findByUK";
        String query = getNamedQuery(namedQuery);
        if(query != null) {
            return loadByNamedQuery(getEntityClass().getSimpleName() + ".findByUK", params);
        }
        query = "FROM " + getEntityClass().getName() + " c WHERE ";
        for(String key : params.keySet()) {
            query += " c." + key + " = :" + key + " ";
        }
        return loadByQuery(query, params);
    }

    @Override
    public List<E> findAll() {
        return dao.findAll();
    }
    
    @Override
    public List<E> findAll(Map<String, Object> params) {
        return dao.findAll(params);
    }

    protected E loadByNamedQuery(String namedQuery, Map<String, Object> params) {
        return getDao().loadSingleByNamedQuery(namedQuery, params);
    }

    protected E loadByQuery(String query, Map<String, Object> params) {
        return getDao().loadSingleByQuery(query, params);
    }

    protected List<E> loadListByNamedQuery(String namedQuery, Map<String, Object> params) {
        return getDao().loadListByNamedQuery(namedQuery, params);
    }

    protected List<E> loadListByQuery(String query, Map<String, Object> params) {
        return getDao().loadListByQuery(query, params);
    }

    public GenericJPAEntityDao<E, ID> getDao() {
        return this.dao;
    }

    protected Map<String, Object> fillUKParams(E entity) {
        String query = getNamedQuery(entity.getClass().getSimpleName() + ".findByUK");
        if(query == null) {
            return null;
        }

        Pattern pattern = Pattern.compile("\\S+\\s*=\\s*:\\S+");
        Matcher matcher = pattern.matcher(query);
        Matcher param;
        pattern = Pattern.compile(":\\S+");

        String[] props;
        String group;
        Object paramValue;
        Map<String, Object> ukParams = new HashMap<String, Object>();
        while(matcher.find()) {
            group = matcher.group();
            props = group.split("=")[0].trim().split("\\.");
            paramValue = entity;
            for(String prop : props) {
                try {
                    paramValue = paramValue.getClass().getMethod("get" + StringUtils.capitalize(prop)).invoke(paramValue);
                } catch(Exception silent) {}
            }
            param = pattern.matcher(group);
            if(param.find()) {
                ukParams.put(param.group().replace(":", "").trim(), paramValue);
            }
        }
            
        return ukParams;
    }

    protected String getNamedQuery(String queryName){
        NamedQueries queries = getEntityClass().getAnnotation(NamedQueries.class);

        if(queries != null) {
            queryName = queryName.toLowerCase();
            for(NamedQuery named : queries.value()) {
                if(queryName.equals(named.name().toLowerCase())) {
                    return named.query();
                }
            }
        }

        return null;
    }

    protected Class<E> getEntityClass() {
        
        if(entityClass == null) {
            ParameterizedType pt = (ParameterizedType) getClass().getGenericSuperclass();
            entityClass = (Class<E>) pt.getActualTypeArguments()[0];
        }
        
        return entityClass;
    }

}
