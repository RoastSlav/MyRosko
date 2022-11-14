package StatementUtility;

import ConfigurationModels.Configuration;
import ConfigurationModels.Mapper;
import Exceptions.MyBatisException;
import SqlMappingModels.ResultMap;
import SqlMappingModels.SqlMapping;

public class MapperUtility {
    public static SqlMapping getMapping(String id, Configuration config) {
        for (Mapper mapper : config.mappers) {
            for (SqlMapping mapping : mapper.mappings) {
                if (mapping.id.equals(id))
                    return mapping;
            }
        }
        throw new MyBatisException(id + " mapping does not exist");
    }

    public static ResultMap getResultMap(String id, Configuration config) {
        for (Mapper mapper : config.mappers) {
            for (ResultMap map : mapper.resultMaps) {
                if (map.id.equals(id))
                    return map;
            }
        }
        throw new MyBatisException(id + " result map does not exist");
    }
}
