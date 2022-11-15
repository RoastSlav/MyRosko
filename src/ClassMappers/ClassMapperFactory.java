package ClassMappers;

import Anotations.Delete;
import Anotations.Insert;
import Anotations.Select;
import Anotations.Update;
import Cache.Cache;
import ConfigurationModels.Configuration;
import ConfigurationModels.Mapper;
import SqlMappingModels.ClassMapperStatement;
import SqlMappingModels.MappingTypeEnum;
import SqlMappingModels.SqlMapping;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static Parsers.SqlParser.prepareSql;
import static SqlMappingModels.MappingTypeEnum.SELECT;
import static StatementUtility.ObjectBuilder.constructObject;
import static StatementUtility.StatementBuilder.prepareStatement;
import static Utility.StringUtility.normalize;

public class ClassMapperFactory {
    Connection conn;
    public Configuration config;

    public ClassMapperFactory(Connection conn, Configuration config) {
        this.conn = conn;
        this.config = config;
    }

    public <T> T createMapper(Class<?> c, Mapper mapper) {
        var handler = new InvocationHandler() {
            public Object invoke(Object proxy, Method method, Object[] args) throws Exception {
                SqlMapping mapping = null;
                for (SqlMapping m : mapper.mappings) {
                    if (m.id.equals(method.getName()))
                        mapping = m;
                }

                if (mapping.mappingType == SELECT) {
                    Class<?> returnType = method.getReturnType();
                    ClassMapperStatement statement = getStatement(mapping.sql, method.getName());
                    return selectObject(statement.sql, statement.values, returnType, args, method);
                } else {
                    ClassMapperStatement statement = getStatement(mapping.sql, method.getName());
                    return executeUpdate(statement.sql, statement.values, args[0]);
                }
            }
        };
        return (T) Proxy.newProxyInstance(c.getClassLoader(), new Class[]{c}, handler);
    }

    private <T> T selectObject(String sql, List<String> values, Class<T> c, Object[] params, Method m) throws Exception {
        Map<String, Object> objectValues = new HashMap<>();
        Parameter[] parameters = m.getParameters();
        for (int j = 0; j < parameters.length; j++) {
            Parameter parameter = parameters[j];
            String name = parameter.getName();
            objectValues.put(name, params[j]);
        }

        PreparedStatement preparedStatement = prepareStatement(sql, objectValues, values, conn);
        ResultSet resultSet = preparedStatement.executeQuery();
        ResultSetMetaData resultSetMetaData = resultSet.getMetaData();

        HashMap<String, Field> fieldNames = new HashMap<>();
        for (Field declaredField : c.getDeclaredFields()) {
            String normalized = normalize(declaredField.getName());
            fieldNames.put(normalized, declaredField);
        }

        resultSet.next();
        T object = constructObject(resultSet, resultSetMetaData, fieldNames, c);
        if (resultSet.next()) {
            throw new IllegalStateException("Select query returned more than 1 entries! Use selectList");
        }
        return object;
    }

    private int executeUpdate(String sql, List<String> values, Object params) throws Exception {
        PreparedStatement preparedStatement = prepareStatement(sql, params, values, conn);
        return preparedStatement.executeUpdate();
    }

    private ClassMapperStatement getStatement(String sql, String methodName) {
        ArrayList<String> values = new ArrayList<>();
        sql = prepareSql(sql, values);
        ClassMapperStatement statement = new ClassMapperStatement(sql, null, null, values);
        return statement;
    }
}
