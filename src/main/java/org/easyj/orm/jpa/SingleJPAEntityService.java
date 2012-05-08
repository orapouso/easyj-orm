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

import javax.annotation.Resource;
import org.easyj.orm.AbstractSingleService;
import org.easyj.orm.SingleDao;
import org.springframework.stereotype.Service;

/**
 * JPA {@code @Service} that provides JPA Dao as the {@code @Repository} technology
 * 
 * @author Rafael Raposo
 * @since 1.1.0
 */
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
