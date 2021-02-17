/**
 *    Copyright 2009-2021 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.able.dao;

import com.able.model.User;
import org.apache.ibatis.annotations.CacheNamespace;
import org.apache.ibatis.annotations.Select;

import java.util.Date;

@CacheNamespace()
public interface UserMapper {

    @Select("select * from user where id = #{id}")
    User selectById(Integer id);

    User selectById3(Integer id);

    @Select("select * from user where id = #{id}")
    User selectByIdCache(Integer id);

    User selectByid2(Integer id);

    int updateByid2(User user);

    int insertUser(User user);

    int insertUser1(Date createTime,Date updateTime,Long updateLongTime,String name);

//    List<User> selectIds(@Param("ids") List<Integer> ids);

}
