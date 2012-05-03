package org.easyj.orm;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractSingleService implements SingleService {

    protected SingleDao dao;

    public abstract void setDao(SingleDao dao);
    
    public abstract SingleDao getDao();

    protected Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public <E> E save(E entity) {
        return getDao().save(entity);
    }

    @Override
    public <E> E delete(E entity) {
        return getDao().delete(entity);
    }

    @Override
    public <E, ID> E delete(Class<E> klazz, ID id) {
        return getDao().delete(klazz, id);
    }

    @Override
    public <E, ID> E findOne(Class<E> klazz, final ID param) {
        return getDao().findOne(klazz, param);
    }

    @Override
    public <E> List<E> findAll(Class<E> klazz) {
        return getDao().findAll(klazz);
    }

    @Override
    public <E> List<E> findAll(Class<E> klazz, Map<String, Object> params) {
        return getDao().findAll(klazz, params);
    }

    @Override
    public <E> E findByQuery(String query, Class<E> klazz, Map<String, Object> params) {
        return getDao().findByQuery(query, klazz, params);
    }

    @Override
    public <E> E findByNativeQuery(String query, Class<E> klazz, Map<String, Object> params) {
        return getDao().findByNativeQuery(query, klazz, params);
    }

    @Override
    public <E> List<E> findListByQuery(String query, Class<E> klazz, Map<String, Object> params) {
        return getDao().findListByQuery(query, klazz, params);
    }

    @Override
    public <E> List<E> findListByNativeQuery(String query, Class<E> klazz, Map<String, Object> params) {
        return getDao().findListByNativeQuery(query, klazz, params);
    }

}
