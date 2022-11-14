package SqlMappingModels;

import java.util.List;

public class ClassMapperStatement<P, R> {
    public String sql;
    public Class<P> parameterType;
    public Class<R> returnType;
    public List<String> values;

    public ClassMapperStatement(String sql, Class<P> parameterType, Class<R> returnType, List<String> values) {
        this.sql = sql;
        this.parameterType = parameterType;
        this.returnType = returnType;
        this.values = values;
    }
}
