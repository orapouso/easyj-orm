package org.easyj.orm;

import java.util.List;
import java.util.Map;

public interface SingleDao {

    public static enum QueryType {NAMED, JPQL, NATIVE};

    public <E> E save(E entity);
    public <E> E delete(E entity);
    public <E, ID> E delete(Class<E> klazz, ID primaryKey);
    public <E, ID> E findOne(Class<E> klazz, ID id);
    public <E> List<E> findAll(Class<E> klazz);
    public <E> List<E> findAll(Class<E> klazz, Map<String, Object> params);
    
    public <E> E findByQuery(String query, Class<E> klazz, Map<String, Object> params);
    public <E> E findByNativeQuery(String query, Class<E> klazz, Map<String, Object> params);
    
    public <E> List<E> findListByQuery(String query, Class<E> klazz, Map<String, Object> params);
    public <E> List<E> findListByNativeQuery(String query, Class<E> klazz, Map<String, Object> params);
}
