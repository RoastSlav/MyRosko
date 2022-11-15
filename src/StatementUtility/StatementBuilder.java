package StatementUtility;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static Utility.StringUtility.normalize;

public class StatementBuilder {
    public static PreparedStatement prepareStatement(String sql, Object params, List<String> paramNames, Connection conn) throws Exception {
        Map<String, Object> fieldNames = getFieldValues(params.getClass(), params);
        return preparedStatement(sql, fieldNames, paramNames, conn);
    }

    public static PreparedStatement preparedStatement(String sql, Map<String, Object> params, List<String> paramNames, Connection conn) throws Exception {
        Class<?> paramsClass = params == null ? null : params.getClass();
        PreparedStatement preparedStatement = conn.prepareStatement(sql);
        if (setToPreparedStatement(params, paramsClass, 1, preparedStatement)) {
            if (!paramNames.get(0).equals("value"))
                throw new IllegalArgumentException("Parameter " + paramNames.get(0) + " does not exist in the params object");
            return preparedStatement;
        }

        for (int i = 0; i < paramNames.size(); i++) {
            String paramName = paramNames.get(i);
            Object value = params.get(normalize(paramName));
            if (value == null)
                throw new IllegalArgumentException("Parameter " + paramNames.get(0) + " does not exist in the params object");
            Class<?> paramClass = value == null ? null : value.getClass();
            setToPreparedStatement(value, paramClass, i + 1, preparedStatement);
        }
        return preparedStatement;
    }

    private static Map<String, Object> getFieldValues(Class<?> paramsClass, Object params) throws IllegalAccessException {
        HashMap<String, Object> fieldNames = new HashMap<>();
        for (Field declaredField : paramsClass.getDeclaredFields()) {
            String normalized = normalize(declaredField.getName());
            declaredField.setAccessible(true);
            fieldNames.put(normalized, declaredField.get(params));
        }
        return fieldNames;
    }

    private static boolean setToPreparedStatement(Object value, Class<?> valueClass, int position, PreparedStatement preparedStatement) throws Exception {
        if (value == null) {
            preparedStatement.setNull(position, Types.NULL);
            return true;
        }
        if (valueClass.equals(Integer.class)) {
            preparedStatement.setInt(position, (int) value);
            return true;
        }
        if (valueClass.equals(Long.class)) {
            preparedStatement.setLong(position, (long) value);
            return true;
        }
        if (valueClass.equals(Short.class)) {
            preparedStatement.setShort(position, (short) value);
            return true;
        }
        if (valueClass.equals(Double.class)) {
            preparedStatement.setDouble(position, (double) value);
            return true;
        }
        if (valueClass.equals(Float.class)) {
            preparedStatement.setFloat(position, (float) value);
            return true;
        }
        if (valueClass.equals(Byte.class)) {
            preparedStatement.setByte(position, (byte) value);
            return true;
        }
        if (valueClass.equals(Character.class)) {
            preparedStatement.setString(position, (String) value);
            return true;
        }
        if (valueClass.equals(Boolean.class)) {
            preparedStatement.setBoolean(position, (boolean) value);
            return true;
        }
        if (valueClass.equals(String.class)) {
            preparedStatement.setString(position, (String) value);
            return true;
        }
        return false;
    }
}
