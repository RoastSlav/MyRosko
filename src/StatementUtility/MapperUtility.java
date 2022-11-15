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

    public static SqlMapping getMapping(String id, Mapper mapper) {
        for (SqlMapping mapping : mapper.mappings) {
            if (mapping.id.equals(id))
                return mapping;
        }
        throw new MyBatisException(id + " mapping does not exist in " + mapper.namespace);
    }

    public static SqlMapping getMapping(String id, Configuration config, Mapper outputMapper) {
        for (Mapper mapper : config.mappers) {
            for (SqlMapping map : mapper.mappings) {
                if (map.id.equals(id)) {
                    outputMapper.namespace = mapper.namespace;
                    outputMapper.mappings = mapper.mappings;
                    outputMapper.resultMaps = mapper.resultMaps;
                    outputMapper.cacheFactory = mapper.cacheFactory;
                    return map;
                }
            }
        }
        throw new MyBatisException(id + " mapping does not exist");
    }

    public static Mapper getMapper(String id, Configuration config) {
        for (Mapper mapper : config.mappers) {
            if (mapper.namespace.equals(id))
                return mapper;
        }
        throw new MyBatisException("Mapper with this namespace: " + id + " does not exist");
    }
}
