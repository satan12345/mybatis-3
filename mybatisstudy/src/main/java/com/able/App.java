package com.able;


import com.able.model.User;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

/***
 * @Author 徐庶   QQ:1092002729
 * @Slogan 致敬大师，致敬未来的你
 */
public class App {
    public static void main(String[] args) {
        String resource = "mybatis-config.xml";

        try {
            //将XML配置文件构建为Configuration配置类

             InputStream resourceAsStream = Resources.getResourceAsStream(resource);
            // 通过加载配置文件流构建一个SqlSessionFactory  DefaultSqlSessionFactory
            SqlSessionFactory sqlMapper = new SqlSessionFactoryBuilder().build(resourceAsStream);
            // 数据源 执行器  DefaultSqlSession
            SqlSession session = sqlMapper.openSession();
            try {
                // 执行查询 底层执行jdbc
                User user = (User) session.selectOne("com.able.dao.UserMapper.selectById", 1);
                System.out.println("user = " + user);

//                //获得代理对象
//                UserMapper mapper = session.getMapper(UserMapper.class);
//                System.out.println(mapper.getClass());
//                user = mapper.selectByid(1);
////                session.commit();
//                System.out.println(user.getName());
//
//                UserMapper userMapper= (UserMapper) Proxy.newProxyInstance(User.class.getClassLoader(),
//				new Class[]{UserMapper.class}, (proxy, method, args1) -> {
//				    System.out.println("方法"+method.getName()+"被调用了,参数为"+args1);
//					return null;
//				});
//                userMapper.selectByid(123);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                session.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
