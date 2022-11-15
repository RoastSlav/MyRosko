package ClassMappers;

import Cache.Cache;
import ConfigurationModels.Configuration;
import ConfigurationModels.Mapper;
import SqlMappingModels.SqlMapping;

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
    private final Configuration config;
    private final Connection conn;

    public ClassMapperFactory(Connection conn, Configuration config) {
        this.conn = conn;
        this.config = config;
    }

    public <T> T createMapper(Class<?> c, Mapper mapper) {
        var handler = new InvocationHandler() {
            Cache cache = null;

            public Object invoke(Object proxy, Method method, Object[] args) throws Exception {
                if (cache == null && mapper.cacheFactory != null)
                    cache = mapper.cacheFactory.getCache();

                SqlMapping mapping = null;
                for (SqlMapping m : mapper.mappings) {
                    if (m.id.equals(method.getName()))
                        mapping = m;
                }

                ArrayList<String> values = new ArrayList<>();
                String sql = prepareSql(mapping.sql, values);
                Map<String, Object> objectValues = new HashMap<>();
                Parameter[] parameters = method.getParameters();
                for (int j = 0; j < parameters.length; j++) {
                    Parameter parameter = parameters[j];
                    String name = parameter.getName();
                    objectValues.put(name, args[j]);
                }
                if (mapping.mappingType == SELECT) {
                    if (cache != null) {
                        T result = (T) cache.get(mapping.sql + args.toString());
                        if (result != null) {
                            return result;
                        }
                    }

                    Class<?> returnType = method.getReturnType();
                    Object o = selectObject(sql, values, returnType, objectValues);
                    if (cache != null)
                        cache.set(mapping.sql + args.toString(), o);
                    return o;
                } else {
                    if (cache != null)
                        cache.clearCache();
                    return executeUpdate(sql, values, objectValues);
                }
            }
        };
        return (T) Proxy.newProxyInstance(c.getClassLoader(), new Class[]{c}, handler);
    }

    private <T> T selectObject(String sql, List<String> values, Class<T> c, Map<String, Object> objectValues) throws Exception {
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

    private int executeUpdate(String sql, List<String> values, Map<String, Object> objectValues) throws Exception {
        PreparedStatement preparedStatement = prepareStatement(sql, objectValues, values, conn);
        return preparedStatement.executeUpdate();
    }
}
