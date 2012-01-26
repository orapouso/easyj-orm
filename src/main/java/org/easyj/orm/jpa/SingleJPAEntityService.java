package org.easyj.orm.jpa;

import javax.annotation.Resource;
import org.easyj.orm.AbstractSingleService;
import org.springframework.stereotype.Service;

@Service
public class SingleJPAEntityService extends AbstractSingleService {

    @Override
    @Resource(name="SingleJPAEntityDao")
    protected SingleJPAEntityDao getDao() {
        return (SingleJPAEntityDao) dao;
    }

}
