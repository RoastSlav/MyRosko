package SqlSession;

import Cache.*;
import ClassMappers.ClassMapperFactory;
import ConfigurationModels.Configuration;
import ConfigurationModels.Mapper;
import Exceptions.MyBatisException;
import SqlMappingModels.*;

import java.io.IOException;
import java.lang.reflect.Field;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static StatementUtility.MapperUtility.getMapping;
import static StatementUtility.MapperUtility.getResultMap;
import static StatementUtility.ObjectBuilder.constructObject;
import static StatementUtility.StatementBuilder.prepareStatement;
import static Utility.StringUtility.normalize;

public class SqlSession implements AutoCloseable {
    private final Connection conn;
    private final Configuration config;
    private final HashMap<String, Cache> caches = new HashMap<String, Cache>();

    protected SqlSession(Connection conn, Configuration config) {
        this.conn = conn;
        this.config = config;
    }

    public <T> T getMapper(Class<T> type) {
        boolean registeredMapper = false;
        Mapper mapper = null;
        for (Mapper m : config.mappers) {
            if (m.namespace.equals(type.getName())) {
                registeredMapper = true;
                mapper = m;
                break;
            }
        }

        if (!registeredMapper)
            throw new MyBatisException(type.getName() + " is not a registered mapper");

        ClassMapperFactory factory = new ClassMapperFactory(conn, config);
        return factory.createMapper(type, mapper);
    }

    public int delete(String statement) {
        return this.delete(statement, null);
    }

    public int delete(String statement, Object params) {
        Mapper mapper = new Mapper();
        SqlDelete delete = (SqlDelete) getMapping(statement, config, mapper);
        Cache cache = getCache(mapper);
        if (delete.flushCache && cache != null)
            cache.clearCache();
        String parameterType = delete.parameterType;

        if (params != null && !params.getClass().getSimpleName().equals(convertClassName(parameterType)))
            throw new MyBatisException("Params type is not matching the statement parameter type!");

        try {
            PreparedStatement preparedStatement = prepareStatement(delete.sql, params, delete.paramNames, conn);
            return preparedStatement.executeUpdate();
        } catch (Exception e) {
            throw new MyBatisException(e);
        }
    }

    public int insert(String statement) {
        return this.insert(statement, null);
    }

    public int insert(String statement, Object params) {
        Mapper mapper = new Mapper();
        SqlInsert insert = (SqlInsert) getMapping(statement, config, mapper);
        Cache cache = getCache(mapper);
        if (insert.flushCache && cache != null)
            cache.clearCache();
        String parameterType = insert.parameterType;

        if (params != null && !params.getClass().getSimpleName().equals(convertClassName(parameterType)))
            throw new MyBatisException("Params type is not matching the statement parameter type!");

        try {
            PreparedStatement preparedStatement = prepareStatement(insert.sql, params, insert.paramNames, conn);
            return preparedStatement.executeUpdate();
        } catch (Exception e) {
            throw new MyBatisException(e);
        }
    }

    public int update(String statement) {
        return this.update(statement, null);
    }

    public int update(String statement, Object params) {
        Mapper mapper = new Mapper();
        SqlUpdate update = (SqlUpdate) getMapping(statement, config, mapper);
        Cache cache = getCache(mapper);
        if (update.flushCache && cache != null)
            cache.clearCache();

        String parameterType = update.parameterType;

        if (params != null && !params.getClass().getSimpleName().equals(convertClassName(parameterType)))
            throw new MyBatisException("Params type is not matching the statement parameter type!");

        try {
            PreparedStatement preparedStatement = prepareStatement(update.sql, params, update.paramNames, conn);
            return preparedStatement.executeUpdate();
        } catch (Exception e) {
            throw new MyBatisException(e);
        }
    }

    public <T> T selectOne(String statement) {
        return this.selectOne(statement, null);
    }

    public <T> T selectOne(String statement, Object params) {
        Mapper mapper = new Mapper();
        SqlSelect select = (SqlSelect) getMapping(statement, config, mapper);
        Cache cache = getCache(mapper);
        ResultMap resultMap = null;
        String parameterType = select.parameterType;
        String returnType = select.resultType;
        String resultMapType = select.resultMapType;
        if (!resultMapType.isEmpty()) {
            resultMap = getResultMap(resultMapType, config);
        }

        try {
            if (params != null && !params.getClass().getSimpleName().equals(convertClassName(parameterType)))
                throw new MyBatisException("Params type is not matching the statement parameter type!");

            PreparedStatement preparedStatement = prepareStatement(select.sql, params, select.paramNames, conn);
            if (select.useCache) {
                if (cache == null)
                    throw new MyBatisException("Mapping is set to use a cache but there is no caching set for the mapper");
                T result = (T) cache.get(preparedStatement.toString());
                if (result != null)
                    return result;
            }
            ResultSet resultSet = preparedStatement.executeQuery();
            ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
            Class<?> returnTypeClass = Class.forName(returnType);

            if (!resultSet.next())
                return null;

            T resultObj = null;
            if (resultMap != null) {
                resultObj = (T) constructObject(resultSet, resultSetMetaData, resultMap, returnTypeClass);
            } else {
                HashMap<String, Field> fieldNames = new HashMap<>();
                for (Field declaredField : returnTypeClass.getDeclaredFields()) {
                    String normalized = normalize(declaredField.getName());
                    fieldNames.put(normalized, declaredField);
                }
                resultObj = (T) constructObject(resultSet, resultSetMetaData, fieldNames, returnTypeClass);
            }


            if (resultSet.next()) {
                throw new IllegalStateException("Select query returned more than 1 entries! Use selectList");
            }
            if (select.useCache)
                cache.set(preparedStatement.toString(), resultObj);
            return resultObj;
        } catch (Exception e) {
            throw new MyBatisException(e);
        }
    }

    private String convertClassName(String name) {
        return switch (name) {
            case "int" -> "Integer";
            case "long" -> "Long";
            case "boolean" -> "Boolean";
            case "double" -> "Double";
            case "char" -> "Character";
            case "byte" -> "Byte";
            case "float" -> "Float";
            default -> name;
        };
    }

    public <T> List<T> selectList(String id) {
        return this.selectList(id, null);
    }

    public <T> List<T> selectList(String id, Object params) {
        Mapper mapper = new Mapper();
        SqlSelect select = (SqlSelect) getMapping(id, config, mapper);
        Cache cache = getCache(mapper);

        ResultMap resultMap = null;
        String parameterType = select.parameterType;
        String returnType = select.resultType;
        String resultMapType = select.resultMapType;
        if (!resultMapType.isEmpty()) {
            resultMap = getResultMap(resultMapType, config);
        }

        if (params != null && !params.getClass().getSimpleName().equals(convertClassName(parameterType)))
            throw new MyBatisException("Params type is not matching the statement parameter type!");

        try {
            PreparedStatement preparedStatement = prepareStatement(select.sql, params, select.paramNames, conn);
            if (select.useCache) {
                if (cache == null)
                    throw new MyBatisException("Mapping is set to use a cache but there is no caching set for the mapper");
                List<T> result = (List<T>) cache.get(preparedStatement.toString());
                if (result != null)
                    return result;
            }
            ResultSet resultSet = preparedStatement.executeQuery();
            ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
            Class<?> returnTypeClass = Class.forName(returnType);
            ArrayList<T> objects = new ArrayList<>();

            if (resultMap != null) {
                while (resultSet.next()) {
                    T resultObj = (T) constructObject(resultSet, resultSetMetaData, resultMap, returnTypeClass);
                    objects.add(resultObj);
                }
                return objects;
            }

            HashMap<String, Field> fieldNames = new HashMap<>();
            for (Field declaredField : returnTypeClass.getDeclaredFields()) {
                String normalized = normalize(declaredField.getName());
                fieldNames.put(normalized, declaredField);
            }

            while (resultSet.next()) {
                T resultObj = (T) constructObject(resultSet, resultSetMetaData, fieldNames, returnTypeClass);
                objects.add(resultObj);
            }
            if (select.useCache)
                cache.set(preparedStatement.toString(), objects);
            return objects;
        } catch (Exception e) {
            throw new MyBatisException(e);
        }
    }

    private Cache getCache(Mapper mapper) {
        Cache cache = caches.get(mapper.namespace);
        if (cache == null && mapper.cacheFactory != null) {
            cache = mapper.cacheFactory.getCache();
            caches.put(mapper.namespace, cache);
        }
        return cache;
    }

    public Connection getConnection() {
        return this.conn;
    }

    public Configuration getConfiguration() {
        return this.config;
    }

    @Override
    public void close() throws IOException {
        try {
            conn.close();
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }
}
