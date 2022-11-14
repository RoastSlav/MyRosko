package StatementUtility;

import SqlMappingModels.ResultMap;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.HashMap;

import static Utility.StringUtility.normalize;

public class ObjectBuilder {
    public static <T> T constructObject(ResultSet resultSet, ResultSetMetaData metaData, HashMap<String, Field> fieldNames, Class<T> c) throws Exception {
        Constructor<T> declaredConstructor = c.getDeclaredConstructor();
        declaredConstructor.setAccessible(true);
        T obj = declaredConstructor.newInstance();

        int columnCount = metaData.getColumnCount();
        for (int i = 1; i <= columnCount; i++) {
            String normalizedName = normalize(metaData.getColumnName(i));
            Field field = fieldNames.get(normalizedName);
            if (field == null) {
                continue;
            }
            Object value = resultSet.getObject(i, field.getType());
            field.setAccessible(true);
            if (value == null)
                continue;
            field.set(obj, value);
        }
        return obj;
    }

    public static <T> T constructObject(ResultSet resultSet, ResultSetMetaData metaData, ResultMap map, Class<T> c) throws Exception{
        Constructor<T> declaredConstructor = c.getDeclaredConstructor();
        declaredConstructor.setAccessible(true);
        T obj = declaredConstructor.newInstance();

        int columnCount = metaData.getColumnCount();
        for (int i = 0; i < columnCount; i++) {
            String columnName = metaData.getColumnName(i);
            String propertyName = map.mappings.get(columnName);
            Object value = resultSet.getObject(columnName);
            c.getDeclaredField(propertyName).set(obj, value);
        }
        return obj;
    }
}
