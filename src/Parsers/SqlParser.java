package Parsers;

import SqlMappingModels.SqlMapping;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SqlParser {
    private static final Pattern valueNamesPattern = Pattern.compile("#\\{([a-zA-Z1-9_]+)}");

    public static void prepareSql(SqlMapping mapping) {
        Matcher matcher = valueNamesPattern.matcher(mapping.sql);
        mapping.sql = matcher.replaceAll((match) -> {
            String paramName = match.group(1);
            mapping.paramNames.add(paramName);
            return "?";
        });
    }

    public static String prepareSql(String sql, List<String> values) {
        Matcher matcher = valueNamesPattern.matcher(sql);
        return matcher.replaceAll((match) -> {
            String paramName = match.group(1);
            values.add(paramName);
            return "?";
        });
    }
}
