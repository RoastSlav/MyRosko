package StatementUtility;

import ConfigurationModels.Configuration;
import ConfigurationModels.Mapper;
import Exceptions.MyBatisException;
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
}
