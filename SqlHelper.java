package com.hnjme.help;


import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.ParameterMode;
import org.apache.ibatis.reflection.DefaultReflectorFactory;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.ReflectorFactory;
import org.apache.ibatis.reflection.factory.DefaultObjectFactory;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.reflection.wrapper.DefaultObjectWrapperFactory;
import org.apache.ibatis.reflection.wrapper.ObjectWrapperFactory;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.apache.log4j.Logger;

import com.hnjme.core.util.web.AppUtil;

/**
 * Mybatis - 获取Mybatis查询sql工具
 * 引用自:http://blog.csdn.net/isea533/article/details/40044417
 * @author liuzh/abel533/isea533 wangyong 2015-8-8修改
 * 
 */
public class SqlHelper
{
    /**
     * 异常日志记录
     */
    private static Logger logger = Logger.getLogger(SqlHelper.class);

    /**
     * 取对应方法的SQL
     * @param mapperInterface mapper类 如:ProjectMaterialPlanMapper.class 
     * 请不要用projectMaterialPlanMapper.getClass()
     * @param methodName 方法名
     * @param params 参数
     * @return
     */
    public static String getMapperSql(Class mapperInterface, String methodName, Object params)
    {
        Configuration configuration = getConfiguration();
        String fullMapperMethodName = mapperInterface.getCanonicalName() + "." + methodName;
        if (params == null)
        {
            return getNamespaceSql(configuration, fullMapperMethodName, null);
        }
        return getNamespaceSql(configuration, fullMapperMethodName, params);
    }

    /**
     * 取对应方法的SQL
     * @param namespace 这个是类名加方法的全称 如:com.jme.business.projectMaterialPlan.mapper.ProjectMaterialPlanMapper.updateNotNullProperties
     * @param params 参数
     * @return SQL
     */
    public static String getMapperSql(String namespace, Object params)
    {
        Configuration configuration = getConfiguration();
        if (params == null)
        {
            return getNamespaceSql(configuration, namespace, null);
        }
        return getNamespaceSql(configuration, namespace, params);
    }

    /*
     * 通过接口获取sql
     * 
     * @param mapper
     * 
     * @param methodName
     * 
     * @param args
     * 
     * @return
     * 
     * 
     * public static String getMapperSql(Object mapper, String methodName,
     * Object params) { MetaObject metaObject = forObject(mapper); SqlSession
     * session = (SqlSession) metaObject.getValue("h.sqlSession"); Class
     * mapperInterface = (Class) metaObject.getValue("h.mapperInterface");
     * String fullMethodName = mapperInterface.getCanonicalName() + "." +
     * methodName; if (params == null) { return getNamespaceSql(session,
     * fullMethodName, null); } else { return getMapperSql(session,
     * mapperInterface, methodName, params); } } public static String
     * getMapperSql(Configuration configuration,Object mapper, String
     * methodName, Object params) { MetaObject metaObject =
     * forObject(mapper,configuration); SqlSession session = (SqlSession)
     * metaObject.getValue("h.sqlSession"); Class mapperInterface = (Class)
     * metaObject.getValue("h.mapperInterface"); String fullMethodName =
     * mapperInterface.getCanonicalName() + "." + methodName; if (params ==
     * null) { return getNamespaceSql(session, fullMethodName, null); } else {
     * return getMapperSql(session, mapperInterface, methodName, params); } }
     */
    /**
     * 通过接口获取sql
     * 
     * @param mapper
     * @param methodName
     * @param args
     * @return
     */
    public static String getMapperSql(Object mapper, Class mapperInterface, String methodName, Object params)
    {
        MetaObject metaObject = forObject(mapper);
        SqlSession session = (SqlSession) metaObject.getValue("h.sqlSession");
        String fullMethodName = mapperInterface.getCanonicalName() + "." + methodName;
        if (params == null)
        {
            return getNamespaceSql(session, fullMethodName, null);
        }
        else
        {
            return getMapperSql(session, mapperInterface, methodName, params);
        }
    }

    /**
     * 通过Mapper方法名获取sql
     * 
     * @param session
     * @param fullMapperMethodName
     * @param args
     * @return
     */
    public static String getMapperSql(SqlSession session, String fullMapperMethodName, Object params)
    {
        if (params == null)
        {
            return getNamespaceSql(session, fullMapperMethodName, null);
        }
        String methodName = fullMapperMethodName.substring(fullMapperMethodName.lastIndexOf('.') + 1);
        Class mapperInterface = null;
        try
        {
            mapperInterface = Class.forName(fullMapperMethodName.substring(0, fullMapperMethodName.lastIndexOf('.')));
            return getMapperSql(session, mapperInterface, methodName, params);
        }
        catch (ClassNotFoundException e)
        {
            throw new IllegalArgumentException("参数" + fullMapperMethodName + "无效！");
        }
    }

    /**
     * 通过Mapper接口和方法名
     * 
     * @param session
     * @param mapperInterface
     * @param methodName
     * @param args
     * @return
     */
    public static String getMapperSql(SqlSession session, Class mapperInterface, String methodName, Object params)
    {
        String fullMapperMethodName = mapperInterface.getCanonicalName() + "." + methodName;
        if (params == null)
        {
            return getNamespaceSql(session, fullMapperMethodName, null);
        }
        return getNamespaceSql(session, fullMapperMethodName, params);
    }

    /**
     * 通过Mapper接口和方法名 取SQL
     * @param configuration
     * @param mapperInterface
     * @param methodName
     * @param params
     * @return
     */
    public static String getMapperSql(Configuration configuration, Class mapperInterface, String methodName,
            Object params)
    {
        String fullMapperMethodName = mapperInterface.getCanonicalName() + "." + methodName;
        if (params == null)
        {
            return getNamespaceSql(configuration, fullMapperMethodName, null);
        }
        return getNamespaceSql(configuration, fullMapperMethodName, params);
    }

    /**
     * 通过命名空间方式获取sql
     * 
     * @param session
     * @param namespace
     * @return
     */
    public static String getNamespaceSql(SqlSession session, String namespace)
    {
        return getNamespaceSql(session, namespace, null);
    }

    /**
     * 通过命名空间方式获取sql
     * 
     * @param session
     * @param namespace
     * @param params
     * @return
     */
    public static String getNamespaceSql(SqlSession session, String namespace, Object params)
    {
        return getNamespaceSql(session.getConfiguration(), namespace, params);
    }

    /**
     * 通过命名空间方式获取sql
     * @param configuration
     * @param namespace
     * @param params
     * @return
     */
    public static String getNamespaceSql(Configuration configuration, String namespace, Object params)
    {
        params = wrapCollection(params);
        MappedStatement mappedStatement = configuration.getMappedStatement(namespace);
        TypeHandlerRegistry typeHandlerRegistry = mappedStatement.getConfiguration().getTypeHandlerRegistry();
        BoundSql boundSql = mappedStatement.getBoundSql(params);
        List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
        String sql = boundSql.getSql();
        if (parameterMappings != null)
        {
            for (int i = 0; i < parameterMappings.size(); i++)
            {
                ParameterMapping parameterMapping = parameterMappings.get(i);
                if (parameterMapping.getMode() != ParameterMode.OUT)
                {
                    Object value;
                    String propertyName = parameterMapping.getProperty();
                    if (boundSql.hasAdditionalParameter(propertyName))
                    {
                        value = boundSql.getAdditionalParameter(propertyName);
                    }
                    else if (params == null)
                    {
                        value = null;
                    }
                    else if (typeHandlerRegistry.hasTypeHandler(params.getClass()))
                    {
                        value = params;
                    }
                    else
                    {
                        MetaObject metaObject = configuration.newMetaObject(params);
                        value = metaObject.getValue(propertyName);
                    }
                    JdbcType jdbcType = parameterMapping.getJdbcType();
                    if (value == null && jdbcType == null)
                        jdbcType = org.apache.ibatis.type.JdbcType.NULL;// configuration.getJdbcTypeForNull();
                    sql = replaceParameter(sql, value, jdbcType, parameterMapping.getJavaType());
                }
            }
        }
        logger.debug("namespace:" + namespace + "\n sql:" + sql);
        return sql;
    }

    /**
     * 根据类型替换参数 仅作为数字和字符串两种类型进行处理，需要特殊处理的可以继续完善这里
     * 
     * @param sql
     * @param value
     * @param jdbcType
     * @param javaType
     * @return
     */
    private static String replaceParameter(String sql, Object value, JdbcType jdbcType, Class javaType)
    {
        String strValue = String.valueOf(value);
        if (jdbcType != null)
        {
            switch (jdbcType)
            {
                // 数字
                case BIT:
                case TINYINT:
                case SMALLINT:
                case INTEGER:
                case BIGINT:
                case FLOAT:
                case REAL:
                case DOUBLE:
                case NUMERIC:
                case DECIMAL:
                    break;
                // 日期
                case DATE:
                    if (strValue != null && strValue.length() > 10) {
                        strValue = "timestamp'" + strValue + "'";
                    } else {
                        strValue = "date'" + strValue + "'";
                    }
                    break;
                case TIMESTAMP:
                    strValue = "timestamp'" + strValue + "'";
                    break;
                // 其他，包含字符串和其他特殊类型
                case TIME:
                default:
                    strValue = "'" + strValue + "'";

            }
        }
        else if (Number.class.isAssignableFrom(javaType))
        {
            // 不加单引号
        }
        else
        {
            strValue = "'" + strValue + "'";
        }
        return sql.replaceFirst("\\?", strValue);
    }

    /**
     * 获取指定的方法
     * 
     * @param clazz
     * @param methodName
     * @return
     */
    private static Method getDeclaredMethods(Class clazz, String methodName)
    {
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods)
        {
            if (method.getName().equals(methodName))
            {
                return method;
            }
        }
        throw new IllegalArgumentException("方法" + methodName + "不存在！");
    }

    /**
     * 获取参数注解名
     * 
     * @param method
     * @param i
     * @param paramName
     * @return
     */
    private static String getParamNameFromAnnotation(Method method, int i, String paramName)
    {
        final Object[] paramAnnos = method.getParameterAnnotations()[i];
        for (Object paramAnno : paramAnnos)
        {
            if (paramAnno instanceof Param)
            {
                paramName = ((Param) paramAnno).value();
            }
        }
        return paramName;
    }

    /**
     * 简单包装参数
     * 
     * @param object
     * @return
     */
    private static Object wrapCollection(final Object object)
    {
        if (object instanceof List)
        {
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("list", object);
            return map;
        }
        else if (object != null && object.getClass().isArray())
        {
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("array", object);
            return map;
        }
        return object;
    }

    private static final ObjectFactory DEFAULT_OBJECT_FACTORY = new DefaultObjectFactory();

    private static final ObjectWrapperFactory DEFAULT_OBJECT_WRAPPER_FACTORY = new DefaultObjectWrapperFactory();

    private static final ReflectorFactory  DEFAULT_REFLECTOR_FACTORY = new DefaultReflectorFactory();
    /**
     * 反射对象，增加对低版本Mybatis的支持
     * 
     * @param object 反射对象
     * @return
     */
    private static MetaObject forObject(Object object)
    {
        return MetaObject.forObject(object, DEFAULT_OBJECT_FACTORY, DEFAULT_OBJECT_WRAPPER_FACTORY, DEFAULT_REFLECTOR_FACTORY);//forObject(object, DEFAULT_OBJECT_FACTORY, DEFAULT_OBJECT_WRAPPER_FACTORY);
    }

    /**
     * 取Mybatis Configuration
     * @return 
     */
    private static Configuration getConfiguration()
    {
    	Object sqlSessionFactory = AppUtil.getBean("sqlSessionFactory");
        Configuration configuration = ((org.apache.ibatis.session.defaults.DefaultSqlSessionFactory)sqlSessionFactory).getConfiguration();

        return configuration;
    }
}