package ConfigurationModels;

import Cache.CacheFactory;
import SqlMappingModels.ResultMap;
import SqlMappingModels.SqlMapping;

public class Mapper {
    public String namespace;
    public SqlMapping[] mappings;
    public ResultMap[] resultMaps;
    public CacheFactory cacheFactory;
}
