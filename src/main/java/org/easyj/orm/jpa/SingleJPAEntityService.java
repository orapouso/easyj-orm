package org.easyj.orm.jpa;

import javax.annotation.Resource;
import org.easyj.orm.AbstractSingleService;
import org.easyj.orm.SingleDao;
import org.springframework.stereotype.Service;

@Service
public class SingleJPAEntityService extends AbstractSingleService {

    @Resource(name="singleJPAEntityDao")
    @Override
    public void setDao(SingleDao dao) {
        this.dao = (SingleJPAEntityDao) dao;
    }

    @Override
    public SingleJPAEntityDao getDao() {
        return (SingleJPAEntityDao) dao;
    }

}
