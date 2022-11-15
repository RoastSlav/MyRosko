package SqlMappingModels;

import java.util.ArrayList;
import java.util.List;

public class SqlMapping {
    public String id;
    public MappingTypeEnum mappingType;
    public String parameterType;
    public String sql;
    public boolean flushCache;
    public List<String> paramNames = new ArrayList<>();
}
