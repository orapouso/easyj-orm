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

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

/**
 * Complete JPA {@code @Service} that exposes all needed methods for persistence
 * 
 * @author Rafael Raposo
 * @since 1.0.0
 * @deprecated Use SingleJPAEntityService instead
 */
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
        String query = getNamedQuery(entity.getClass().getSimpleName() + ".findByUK", entity.getClass());
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

    protected <T> String getNamedQuery(String queryName, Class<T> klazz){
        NamedQueries queries = klazz.getAnnotation(NamedQueries.class);

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

}
