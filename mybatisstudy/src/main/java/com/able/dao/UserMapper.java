package com.able.dao;

import com.able.model.User;

public interface UserMapper {


    User selectById(Integer id);

    User selectByid2(Integer id);

    int updateByid2(User user);

    int insertUser(User user);

//    List<User> selectIds(@Param("ids") List<Integer> ids);

}
