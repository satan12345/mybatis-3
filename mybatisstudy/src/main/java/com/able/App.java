package com.able;


import com.able.dao.UserMapper;
import com.able.model.User;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;


public class App {
    /**
     * >org.apache.ibatis.builder.xml.XMLConfigBuilder 解析mybatis的配置文件 mybatis-config.xml
     *  >org.apache.ibatis.builder.xml.XMLMapperBuilder 解析mapper配置文件
     *   >org.apache.ibatis.builder.xml.XMLStatementBuilder 解析mapper中的sql语句
     *    >org.apache.ibatis.builder.SqlSourceBuilder
     *     >org.apache.ibatis.scripting.xmltags.XMLScriptBuilder
     *  >org.apache.ibatis.builder.annotation.MapperAnnotationBuilder
     */

    SqlSessionFactory sqlSessionFactory = null;
    SqlSession session = null;

    @Before
    public void init() throws IOException {
        String resource = "mybatis-config.xml";
        //将XML配置文件构建为Configuration配置类
        InputStream resourceAsStream = Resources.getResourceAsStream(resource);
        // 通过加载配置文件流构建一个SqlSessionFactory  DefaultSqlSessionFactory
        sqlSessionFactory = new SqlSessionFactoryBuilder().build(resourceAsStream);
        // 数据源 执行器  DefaultSqlSession
        session = sqlSessionFactory.openSession(true);

    }

    @After
    public void destory() {
        session.close();
    }

    @Test
    public void test2() {
        SqlSession sqlSession1 = sqlSessionFactory.openSession(true);

        final User user1 = sqlSession1.getMapper(UserMapper.class).selectByIdCache(1);
        sqlSession1.close();
        SqlSession sqlSession2 = sqlSessionFactory.openSession(true);
        final User user2 = sqlSession2.getMapper(UserMapper.class).selectByIdCache(1);
        System.err.println(user1 == user2);

    }

    @Test
    public void test() {
        try {
            UserMapper mapper = session.getMapper(UserMapper.class);
            User user = mapper.selectById(1);
            System.out.println("user1 = " + user);

//            // 执行查询 底层执行jdbc
//            User user = session.selectOne("com.able.dao.UserMapper.selectById", 1);
//            System.out.println("user = " + user);
//            System.err.println(user1 == user);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            session.close();
        }
    }

    @Test
    public void testCreate() {
        UserMapper userMapper = session.getMapper(UserMapper.class);
        User user = new User();
        user.setCreateTime(new Date());
        user.setName("宇智波鼬");
        user.setUpdateTime(new Date());
        user.setUpdateLongTime(System.currentTimeMillis());
        final int i = userMapper.insertUser(user);
        System.out.println("i = " + i);
//        final int i = userMapper.insertUser1(new Date(), new Date(), System.currentTimeMillis(), "测试一下");
//        System.err.println(i);
        session.commit();
    }

}
