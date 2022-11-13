package SqlSession;

import ClassMappers.ClassMapperFactory;
import ConfigurationModels.Configuration;
import ConfigurationModels.Mapper;
import Exceptions.MyBatisException;
import SqlMappingModels.*;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Field;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static StatementUtility.MapperUtility.getMapping;
import static StatementUtility.ObjectBuilder.constructObject;
import static StatementUtility.StatementBuilder.*;
import static Utility.StringUtility.normalize;

public class SqlSession implements AutoCloseable {
    private final Connection conn;
    private final Configuration config;
    protected SqlSession(Connection conn, Configuration config) {
        this.conn = conn;
        this.config = config;
    }

    public <T> T getMapper(Class<T> type) {
        boolean registeredMapper = false;
        for (Mapper mapper : config.mappers) {
            if (mapper.namespace.equals(type.getName())) {
                registeredMapper = true;
                break;
            }
        }

        if (!registeredMapper)
            throw new MyBatisException("Could not create mapper for class " + type.getName());

        ClassMapperFactory factory = new ClassMapperFactory(conn);
        return factory.createMapper(type);
    }

    public int delete(String statement) {
        return this.delete(statement, null);
    }

    public int delete(String statement, Object params) {
        SqlDelete delete = (SqlDelete) getMapping(statement, config);
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
        SqlInsert insert = (SqlInsert) getMapping(statement, config);
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
        SqlUpdate update = (SqlUpdate) getMapping(statement, config);
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
        SqlSelect select = (SqlSelect) getMapping(statement, config);
        String parameterType = select.parameterType;
        String returnType = select.resultType;

        try {
            if (params != null && !params.getClass().getSimpleName().equals(convertClassName(parameterType)))
                throw new MyBatisException("Params type is not matching the statement parameter type!");

            PreparedStatement preparedStatement = prepareStatement(select.sql, params, select.paramNames, conn);
            ResultSet resultSet = preparedStatement.executeQuery();
            ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
            Class<?> returnTypeClass = Class.forName(returnType);
            HashMap<String, Field> fieldNames= new HashMap<>();
            for (Field declaredField : returnTypeClass.getDeclaredFields()) {
                String normalized = normalize(declaredField.getName());
                fieldNames.put(normalized, declaredField);
            }

            if (!resultSet.next())
                return null;

            T object = (T) constructObject(resultSet, resultSetMetaData, fieldNames, returnTypeClass);
            if (resultSet.next()) {
                throw new IllegalStateException("Select query returned more than 1 entries! Use selectList");
            }
            return object;
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
        SqlSelect select = (SqlSelect) getMapping(id, config);
        String parameterType = select.parameterType;
        String returnType = select.resultType;

        if (params != null && !params.getClass().getSimpleName().equals(convertClassName(parameterType)))
            throw new MyBatisException("Params type is not matching the statement parameter type!");

        try {
            PreparedStatement preparedStatement = prepareStatement(select.sql, params, select.paramNames, conn);
            ResultSet resultSet = preparedStatement.executeQuery();
            ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
            Class<?> returnTypeClass = Class.forName(returnType);
            HashMap<String, Field> fieldNames= new HashMap<>();
            for (Field declaredField : returnTypeClass.getDeclaredFields()) {
                String normalized = normalize(declaredField.getName());
                fieldNames.put(normalized, declaredField);
            }
            ArrayList<T> objects = new ArrayList<>();
            while(resultSet.next()) {
                T resultObj = (T) constructObject(resultSet, resultSetMetaData, fieldNames, returnTypeClass);
                objects.add(resultObj);
            }
            return objects;
        } catch (Exception e) {
            throw new MyBatisException(e);
        }
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
