/**
 * Copyright 2009-2017 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ibatis.builder.xml;

import java.io.InputStream;
import java.io.Reader;
import java.util.Properties;
import javax.sql.DataSource;

import org.apache.ibatis.builder.BaseBuilder;
import org.apache.ibatis.builder.BuilderException;
import org.apache.ibatis.datasource.DataSourceFactory;
import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.executor.loader.ProxyFactory;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.io.VFS;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.mapping.DatabaseIdProvider;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.parsing.XPathParser;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.reflection.DefaultReflectorFactory;
import org.apache.ibatis.reflection.MetaClass;
import org.apache.ibatis.reflection.ReflectorFactory;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.reflection.wrapper.ObjectWrapperFactory;
import org.apache.ibatis.session.AutoMappingBehavior;
import org.apache.ibatis.session.AutoMappingUnknownColumnBehavior;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.LocalCacheScope;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;

/**
 * @author Clinton Begin
 * @author Kazuki Shimizu
 */
public class XMLConfigBuilder extends BaseBuilder {

    private boolean parsed;
    private final XPathParser parser;
    private String environment;
    private final ReflectorFactory localReflectorFactory = new DefaultReflectorFactory();

    public XMLConfigBuilder(Reader reader) {
        this(reader, null, null);
    }

    public XMLConfigBuilder(Reader reader, String environment) {
        this(reader, environment, null);
    }

    public XMLConfigBuilder(Reader reader, String environment, Properties props) {
        this(new XPathParser(reader, true, props, new XMLMapperEntityResolver()), environment, props);
    }

    public XMLConfigBuilder(InputStream inputStream) {
        this(inputStream, null, null);
    }

    public XMLConfigBuilder(InputStream inputStream, String environment) {
        this(inputStream, environment, null);
    }

    public XMLConfigBuilder(InputStream inputStream, String environment, Properties props) {
        this(new XPathParser(inputStream, true, props, new XMLMapperEntityResolver()), environment, props);
    }

    private XMLConfigBuilder(XPathParser parser, String environment, Properties props) {
        super(new Configuration());
        ErrorContext.instance().resource("SQL Mapper Configuration");
        this.configuration.setVariables(props);
        this.parsed = false;
        this.environment = environment;
        this.parser = parser;
    }

    public Configuration parse() {
        //只能解析一次
        if (parsed) {
            throw new BuilderException("Each XMLConfigBuilder can only be used once.");
        }
        parsed = true;
        //解析根节点configuration
        parseConfiguration(parser.evalNode("/configuration"));
        return configuration;
    }

    /**
     * 解析configuration 节点
     * @param root
     */
    private void parseConfiguration(XNode root) {
        try {
            //解析properties
            //issue #117 read properties first
            propertiesElement(root.evalNode("properties"));
            //解析settings
            Properties settings = settingsAsProperties(root.evalNode("settings"));
            loadCustomVfs(settings);
            //解析别名
            typeAliasesElement(root.evalNode("typeAliases"));
            /**
             * 解析插件
             */
            pluginElement(root.evalNode("plugins"));
            objectFactoryElement(root.evalNode("objectFactory"));
            objectWrapperFactoryElement(root.evalNode("objectWrapperFactory"));
            reflectorFactoryElement(root.evalNode("reflectorFactory"));
            settingsElement(settings);
            // read it after objectFactory and objectWrapperFactory issue #631
            /**
             * <environments default="development">
             *         <environment id="development">
             *            <transactionManager type="JDBC"/>
             *             <dataSource type="POOLED">
             *                 <property name="driver" value="${mysql.driverClass}"/>
             *                 <property name="url" value="${mysql.jdbcUrl}"/>
             *                 <property name="username" value="${mysql.user}"/>
             *                 <property name="password" value="${mysql.password}"/>
             *             </dataSource>
             *         </environment>
             *     </environments>
             * 解析environments节点 获取transactionManager与dataSource
             */
            environmentsElement(root.evalNode("environments"));
            databaseIdProviderElement(root.evalNode("databaseIdProvider"));
            typeHandlerElement(root.evalNode("typeHandlers"));
            //解析mappers节点
            /**
             *   <mappers>
             *         <!--<package name="classpath:mapper"></package>-->
             *        <mapper resource="mapper/UserMapper.xml"/>
             *     </mappers>
             */
            mapperElement(root.evalNode("mappers"));
        } catch (Exception e) {
            throw new BuilderException("Error parsing SQL Mapper Configuration. Cause: " + e, e);
        }
    }

    private Properties settingsAsProperties(XNode context) {
        if (context == null) {
            return new Properties();
        }
        Properties props = context.getChildrenAsProperties();
        // Check that all settings are known to the configuration class
        MetaClass metaConfig = MetaClass.forClass(Configuration.class, localReflectorFactory);
        for (Object key : props.keySet()) {
            if (!metaConfig.hasSetter(String.valueOf(key))) {
                throw new BuilderException("The setting " + key + " is not known.  Make sure you spelled it correctly (case sensitive).");
            }
        }
        return props;
    }

    private void loadCustomVfs(Properties props) throws ClassNotFoundException {
        String value = props.getProperty("vfsImpl");
        if (value != null) {
            String[] clazzes = value.split(",");
            for (String clazz : clazzes) {
                if (!clazz.isEmpty()) {
                    @SuppressWarnings("unchecked")
                    Class<? extends VFS> vfsImpl = (Class<? extends VFS>) Resources.classForName(clazz);
                    configuration.setVfsImpl(vfsImpl);
                }
            }
        }
    }

    /**
     *  <typeAliases>
     *         //<package name="com.able.model"/>
     *         <typeAlias type="com.able.model.User" alias="user"/>
     *     </typeAliases>
     */
    private void typeAliasesElement(XNode parent) {
        if (parent != null) {
            //循环解析子节点
            for (XNode child : parent.getChildren()) {
                if ("package".equals(child.getName())) {
                    //解析package节点
                    String typeAliasPackage = child.getStringAttribute("name");
                    configuration.getTypeAliasRegistry().registerAliases(typeAliasPackage);
                } else {
                    //解析typeAlias节点
                    //解析别名与class类型
                    String alias = child.getStringAttribute("alias");
                    String type = child.getStringAttribute("type");
                    try {
                        Class<?> clazz = Resources.classForName(type);
                        if (alias == null) {
                            //xml中没有指定别名 则解析class上的Alias注解
                            typeAliasRegistry.registerAlias(clazz);
                        } else {
                            //xml中指定别名 则直接使用
                            typeAliasRegistry.registerAlias(alias, clazz);
                        }
                    } catch (ClassNotFoundException e) {
                        throw new BuilderException("Error registering typeAlias for '" + alias + "'. Cause: " + e, e);
                    }
                }
            }
        }
    }

    /**
     *plugins>
     * <plugin interceptor="com.tuling.plugins.ExamplePlugin" ></plugin>
     *</plugins>
     */
    private void pluginElement(XNode parent) throws Exception {
        if (parent != null) {
            //遍历子节点
            for (XNode child : parent.getChildren()) {
                //获取类名称
                String interceptor = child.getStringAttribute("interceptor");
                Properties properties = child.getChildrenAsProperties();
                //实例化
                Interceptor interceptorInstance = (Interceptor) resolveClass(interceptor).newInstance();
                //设置属性
                interceptorInstance.setProperties(properties);
                //添加到拦截器链中
                configuration.addInterceptor(interceptorInstance);
            }
        }
    }

    private void objectFactoryElement(XNode context) throws Exception {
        if (context != null) {
            String type = context.getStringAttribute("type");
            Properties properties = context.getChildrenAsProperties();
            ObjectFactory factory = (ObjectFactory) resolveClass(type).newInstance();
            factory.setProperties(properties);
            configuration.setObjectFactory(factory);
        }
    }

    private void objectWrapperFactoryElement(XNode context) throws Exception {
        if (context != null) {
            String type = context.getStringAttribute("type");
            ObjectWrapperFactory factory = (ObjectWrapperFactory) resolveClass(type).newInstance();
            configuration.setObjectWrapperFactory(factory);
        }
    }

    private void reflectorFactoryElement(XNode context) throws Exception {
        if (context != null) {
            String type = context.getStringAttribute("type");
            ReflectorFactory factory = (ReflectorFactory) resolveClass(type).newInstance();
            configuration.setReflectorFactory(factory);
        }
    }

    /**
     * 解析properties节点
     * @param context
     * @throws Exception
     */
    private void propertiesElement(XNode context) throws Exception {
        if (context != null) {
            Properties defaults = context.getChildrenAsProperties();
            String resource = context.getStringAttribute("resource");
            String url = context.getStringAttribute("url");
            if (resource != null && url != null) {
                throw new BuilderException("The properties element cannot specify both a URL and a resource based property file reference.  Please specify one or the other.");
            }
            if (resource != null) {
                defaults.putAll(Resources.getResourceAsProperties(resource));
            } else if (url != null) {
                defaults.putAll(Resources.getUrlAsProperties(url));
            }
            Properties vars = configuration.getVariables();
            if (vars != null) {
                defaults.putAll(vars);
            }
            parser.setVariables(defaults);
            configuration.setVariables(defaults);
        }
    }

    private void settingsElement(Properties props) throws Exception {
        configuration.setAutoMappingBehavior(AutoMappingBehavior.valueOf(props.getProperty("autoMappingBehavior", "PARTIAL")));
        configuration.setAutoMappingUnknownColumnBehavior(AutoMappingUnknownColumnBehavior.valueOf(props.getProperty("autoMappingUnknownColumnBehavior", "NONE")));
        configuration.setCacheEnabled(booleanValueOf(props.getProperty("cacheEnabled"), true));
        configuration.setProxyFactory((ProxyFactory) createInstance(props.getProperty("proxyFactory")));
        configuration.setLazyLoadingEnabled(booleanValueOf(props.getProperty("lazyLoadingEnabled"), false));
        configuration.setAggressiveLazyLoading(booleanValueOf(props.getProperty("aggressiveLazyLoading"), false));
        configuration.setMultipleResultSetsEnabled(booleanValueOf(props.getProperty("multipleResultSetsEnabled"), true));
        configuration.setUseColumnLabel(booleanValueOf(props.getProperty("useColumnLabel"), true));
        configuration.setUseGeneratedKeys(booleanValueOf(props.getProperty("useGeneratedKeys"), false));
        configuration.setDefaultExecutorType(ExecutorType.valueOf(props.getProperty("defaultExecutorType", "SIMPLE")));
        configuration.setDefaultStatementTimeout(integerValueOf(props.getProperty("defaultStatementTimeout"), null));
        configuration.setDefaultFetchSize(integerValueOf(props.getProperty("defaultFetchSize"), null));
        configuration.setMapUnderscoreToCamelCase(booleanValueOf(props.getProperty("mapUnderscoreToCamelCase"), false));
        configuration.setSafeRowBoundsEnabled(booleanValueOf(props.getProperty("safeRowBoundsEnabled"), false));
        configuration.setLocalCacheScope(LocalCacheScope.valueOf(props.getProperty("localCacheScope", "SESSION")));
        configuration.setJdbcTypeForNull(JdbcType.valueOf(props.getProperty("jdbcTypeForNull", "OTHER")));
        configuration.setLazyLoadTriggerMethods(stringSetValueOf(props.getProperty("lazyLoadTriggerMethods"), "equals,clone,hashCode,toString"));
        configuration.setSafeResultHandlerEnabled(booleanValueOf(props.getProperty("safeResultHandlerEnabled"), true));
        configuration.setDefaultScriptingLanguage(resolveClass(props.getProperty("defaultScriptingLanguage")));
        @SuppressWarnings("unchecked")
        Class<? extends TypeHandler> typeHandler = (Class<? extends TypeHandler>) resolveClass(props.getProperty("defaultEnumTypeHandler"));
        configuration.setDefaultEnumTypeHandler(typeHandler);
        configuration.setCallSettersOnNulls(booleanValueOf(props.getProperty("callSettersOnNulls"), false));
        configuration.setUseActualParamName(booleanValueOf(props.getProperty("useActualParamName"), true));
        configuration.setReturnInstanceForEmptyRow(booleanValueOf(props.getProperty("returnInstanceForEmptyRow"), false));
        configuration.setLogPrefix(props.getProperty("logPrefix"));
        @SuppressWarnings("unchecked")
        Class<? extends Log> logImpl = (Class<? extends Log>) resolveClass(props.getProperty("logImpl"));
        configuration.setLogImpl(logImpl);
        configuration.setConfigurationFactory(resolveClass(props.getProperty("configurationFactory")));
    }

    /**
     *  <environments default="development">
     *         <environment id="development">
     *            <transactionManager type="JDBC"/>
     *             <dataSource type="POOLED">
     *                 <property name="driver" value="${mysql.driverClass}"/>
     *                 <property name="url" value="${mysql.jdbcUrl}"/>
     *                 <property name="username" value="${mysql.user}"/>
     *                 <property name="password" value="${mysql.password}"/>
     *             </dataSource>
     *         </environment>
     *     </environments>
     * 解析environments节点属性
     * @param context
     * @throws Exception
     */
    private void environmentsElement(XNode context) throws Exception {
        if (context != null) {

            if (environment == null) {
                //获取默认的环境属性
                environment = context.getStringAttribute("default");
            }
            for (XNode child : context.getChildren()) {
                String id = child.getStringAttribute("id");
                //判断设置的默认属性与指定的环境属性是否相同
                if (isSpecifiedEnvironment(id)) {
                    //解析transactionManager节点属性 获取事务工厂对象
                    TransactionFactory txFactory = transactionManagerElement(child.evalNode("transactionManager"));
                    //解析datasource属性 获取数据源工厂
                    DataSourceFactory dsFactory = dataSourceElement(child.evalNode("dataSource"));
                    //获取数据源对象
                    DataSource dataSource = dsFactory.getDataSource();
                    /**
                     * 利用构建者模式创建environmentBuilder对象
                     * 然后environmentBuilder.build() 创建Environment对象
                     * 赋值到configuration的environment属性中去
                     */
                    Environment.Builder environmentBuilder = new Environment.Builder(id)
                            .transactionFactory(txFactory)
                            .dataSource(dataSource);
                    configuration.setEnvironment(environmentBuilder.build());
                }
            }
        }
    }

    private void databaseIdProviderElement(XNode context) throws Exception {
        DatabaseIdProvider databaseIdProvider = null;
        if (context != null) {
            String type = context.getStringAttribute("type");
            // awful patch to keep backward compatibility
            if ("VENDOR".equals(type)) {
                type = "DB_VENDOR";
            }
            Properties properties = context.getChildrenAsProperties();
            databaseIdProvider = (DatabaseIdProvider) resolveClass(type).newInstance();
            databaseIdProvider.setProperties(properties);
        }
        Environment environment = configuration.getEnvironment();
        if (environment != null && databaseIdProvider != null) {
            String databaseId = databaseIdProvider.getDatabaseId(environment.getDataSource());
            configuration.setDatabaseId(databaseId);
        }
    }

    /**
     *  <transactionManager type="JDBC"/>
     *  解析transactionManager节点
     * @param context
     * @return
     * @throws Exception
     */
    private TransactionFactory transactionManagerElement(XNode context) throws Exception {
        if (context != null) {
            //解析type属性
            String type = context.getStringAttribute("type");
            //获取所有的子属性
            Properties props = context.getChildrenAsProperties();
            /**
             * 从typeAliasRegistry中获取对应的class类型 然后实例化 并设置相应的属性
             */
            TransactionFactory factory = (TransactionFactory) resolveClass(type).newInstance();
            factory.setProperties(props);
            return factory;
        }
        throw new BuilderException("Environment declaration requires a TransactionFactory.");
    }

    /**解析datasource节点 创建数据源工厂
     * <dataSource type="POOLED">
     *     <property name="driver" value="${mysql.driverClass}"/>
     *     <property name="url" value="${mysql.jdbcUrl}"/>
     *     <property name="username" value="${mysql.user}"/>
     *     <property name="password" value="${mysql.password}"/>
     * </dataSource>
     * @param context
     * @return
     * @throws Exception
     */
    private DataSourceFactory dataSourceElement(XNode context) throws Exception {
        if (context != null) {
            /**
             * 获取数据源类型
             */
            String type = context.getStringAttribute("type");
            /**
             *将驱动 url username password 属性封装成properties
             */
            Properties props = context.getChildrenAsProperties();
            //根据数据源属性进行反射实例化
            DataSourceFactory factory = (DataSourceFactory) resolveClass(type).newInstance();
            //设置数据源属性
            factory.setProperties(props);
            //返回数据源工厂对象
            return factory;
        }
        throw new BuilderException("Environment declaration requires a DataSourceFactory.");
    }

    private void typeHandlerElement(XNode parent) throws Exception {
        if (parent != null) {
            for (XNode child : parent.getChildren()) {
                if ("package".equals(child.getName())) {
                    String typeHandlerPackage = child.getStringAttribute("name");
                    typeHandlerRegistry.register(typeHandlerPackage);
                } else {
                    String javaTypeName = child.getStringAttribute("javaType");
                    String jdbcTypeName = child.getStringAttribute("jdbcType");
                    String handlerTypeName = child.getStringAttribute("handler");
                    Class<?> javaTypeClass = resolveClass(javaTypeName);
                    JdbcType jdbcType = resolveJdbcType(jdbcTypeName);
                    Class<?> typeHandlerClass = resolveClass(handlerTypeName);
                    if (javaTypeClass != null) {
                        if (jdbcType == null) {
                            typeHandlerRegistry.register(javaTypeClass, typeHandlerClass);
                        } else {
                            typeHandlerRegistry.register(javaTypeClass, jdbcType, typeHandlerClass);
                        }
                    } else {
                        typeHandlerRegistry.register(typeHandlerClass);
                    }
                }
            }
        }
    }

    /**
     * 解析mappers节点
     * <mappers>
     *        <package name="classpath:mapper"></package>
     *        <mapper resource="mapper/UserMapper.xml"/>
     *     </mappers>
     * @param parent
     * <mappers>
     * <mapper resource="mapper/UserMapper.xml"/>
     * </mappers>
     * @throws Exception
     */
    private void mapperElement(XNode parent) throws Exception {
        if (parent != null) {
            //遍历子节点
            for (XNode child : parent.getChildren()) {
                if ("package".equals(child.getName())) {
                    //解析package子节点
                    String mapperPackage = child.getStringAttribute("name");
                    configuration.addMappers(mapperPackage);
                } else {
                    //解析mapper子节点
                    String resource = child.getStringAttribute("resource");
                    String url = child.getStringAttribute("url");
                    String mapperClass = child.getStringAttribute("class");
                    if (resource != null && url == null && mapperClass == null) {
                        //解析resource 从classpath下解析配置文件
                        ErrorContext.instance().resource(resource);
                        InputStream inputStream = Resources.getResourceAsStream(resource);
                        //创建xmlmapperbuidler用于解析mapper
                        XMLMapperBuilder mapperParser = new XMLMapperBuilder(inputStream,
                                configuration, resource, configuration.getSqlFragments());
                        //解析mapper
                        mapperParser.parse();
                    } else if (resource == null && url != null && mapperClass == null) {
                        //解析url
                        ErrorContext.instance().resource(url);
                        InputStream inputStream = Resources.getUrlAsStream(url);
                        XMLMapperBuilder mapperParser = new XMLMapperBuilder(inputStream, configuration, url, configuration.getSqlFragments());
                        mapperParser.parse();
                    } else if (resource == null && url == null && mapperClass != null) {
                        //解析class
                        Class<?> mapperInterface = Resources.classForName(mapperClass);
                        //增加mapper到mapperRegister中
                        configuration.addMapper(mapperInterface);
                    } else {
                        throw new BuilderException("A mapper element may only specify a url, resource or class, but not more than one.");
                    }
                }
            }
        }
    }

    private boolean isSpecifiedEnvironment(String id) {
        if (environment == null) {
            throw new BuilderException("No environment specified.");
        } else if (id == null) {
            throw new BuilderException("Environment requires an id attribute.");
        } else if (environment.equals(id)) {
            return true;
        }
        return false;
    }

}
