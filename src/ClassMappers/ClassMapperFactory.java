package ClassMappers;

import Anotations.Delete;
import Anotations.Insert;
import Anotations.Select;
import Anotations.Update;
import SqlMappingModels.ClassMapperStatement;

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
import static StatementUtility.ObjectBuilder.constructObject;
import static StatementUtility.StatementBuilder.prepareStatement;
import static Utility.StringUtility.normalize;

public class ClassMapperFactory {
    Connection conn;

    public ClassMapperFactory(Connection conn) {
        this.conn = conn;
    }

    public <T> T createMapper(Class<?> c) {
        var handler = new InvocationHandler() {
            public Object invoke(Object proxy, Method method, Object[] args) throws Exception {
                Annotation[] annotations = method.getDeclaredAnnotations();
                if (annotations[0] instanceof Select) {
                    Class<?> returnType = method.getReturnType();
                    String sql = ((Select) annotations[0]).value();

                    ClassMapperStatement statement = getStatement(sql, method.getName(), c);
                    return selectObject(statement.sql, statement.values, returnType, args, method);
                } else if (annotations[0] instanceof Insert ||
                        annotations[0] instanceof Update ||
                        annotations[0] instanceof Delete) {
                    String sql = ((Insert) annotations[0]).value();

                    ClassMapperStatement statement = getStatement(sql, method.getName(), c);
                    return executeUpdate(statement.sql, statement.values, args[0]);
                }
                return null;
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

    private ClassMapperStatement getStatement(String sql, String methodName, Class<?> c) {
        ArrayList<String> values = new ArrayList<>();
        sql = prepareSql(sql, values);
        ClassMapperStatement statement = new ClassMapperStatement(sql, null, null, values);
        return statement;
    }
}
