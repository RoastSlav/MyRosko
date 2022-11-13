package ConfigurationModels;

import Exceptions.MyBatisException;
import SqlMappingModels.ResultMap;
import SqlMappingModels.SqlMapping;

public class Mapper {
    public String namespace;
    public SqlMapping[] mappings;
    public ResultMap[] resultMaps;
}
