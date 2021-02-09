package com.able.dao;

import com.able.model.User;
import org.apache.ibatis.annotations.CacheNamespace;
import org.apache.ibatis.annotations.Select;

import java.util.Date;

@CacheNamespace()
public interface UserMapper {

    @Select("select * from user where id = #{id}")
    User selectById(Integer id);

    @Select("select * from user where id = #{id}")
    User selectByIdCache(Integer id);

    User selectByid2(Integer id);

    int updateByid2(User user);

    int insertUser(User user);

    int insertUser1(Date createTime,Date updateTime,Long updateLongTime,String name);

//    List<User> selectIds(@Param("ids") List<Integer> ids);

}
