package Parsers;

import Anotations.Delete;
import Anotations.Insert;
import Anotations.Select;
import Anotations.Update;
import Cache.CacheFactory;
import Cache.CacheFactoryBuilder;
import ConfigurationModels.Mapper;
import Exceptions.MyBatisException;
import SqlMappingModels.*;
import Utility.Resources;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.Reader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;

import static Parsers.SqlParser.prepareSql;
import static SqlMappingModels.MappingTypeEnum.*;

class MapperParser {
    Mapper parse(String resource) throws Exception {
        Document doc = createDoc(resource);
        if (doc.getNodeName().equals("mapper"))
            return null;

        Mapper mapper = new Mapper();
        mapper.namespace = doc.getDocumentElement().getAttribute("namespace");
        mapper.mappings = parseMappings(doc);
        mapper.resultMaps = parseResultMaps(doc);
        mapper.cacheFactory = parseCache(doc);

        return mapper;
    }

    Mapper parseClassMapper(Class<?> type) throws ParserConfigurationException {
        Method[] declaredMethods = type.getDeclaredMethods();
        Mapper mapper = new Mapper();
        mapper.namespace = type.getName();
        for (Method method : declaredMethods) {
            SqlMapping mapping = new SqlMapping();
            mapping.id = method.getName();
            Parameter parameter = method.getParameters()[0];
            mapping.parameterType = parameter.getType().getName();
            mapping.paramNames.add(parameter.getName());
            Annotation[] annotations = method.getDeclaredAnnotations();
            if (annotations[0] instanceof Select) {
                mapping.mappingType = SELECT;
                mapping.sql = ((Select) annotations[0]).value();
            } else if (annotations[0] instanceof Update) {
                mapping.mappingType = UPDATE;
                mapping.sql = ((Update) annotations[0]).value();
            } else if (annotations[0] instanceof Delete) {
                mapping.mappingType = DELETE;
                mapping.sql = ((Delete) annotations[0]).value();
            } else if (annotations[0] instanceof Insert) {
                mapping.mappingType = INSERT;
                mapping.sql = ((Insert) annotations[0]).value();
            } else
                throw new ParserConfigurationException(type.getSimpleName() + " isn't a valid mapper");
        }
        return mapper;
    }

    private Document createDoc(String resource) throws Exception {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

        Document doc;
        try (Reader reader = Resources.getResourceAsReader(resource)) {
            doc = dBuilder.parse(new InputSource(reader));
        }
        doc.getDocumentElement().normalize();
        return doc;
    }

    private SqlMapping[] parseMappings(Document doc) {
        ArrayList<SqlMapping> mappings = new ArrayList<>();
        parseSelect(doc, mappings);
        parseDelete(doc, mappings);
        parseInsert(doc, mappings);
        parseUpdate(doc, mappings);
        return mappings.toArray(SqlMapping[]::new);
    }

    private CacheFactory parseCache(Document doc) {
        NodeList cacheNodes = doc.getElementsByTagName("cache");
        if (cacheNodes.getLength() < 1)
            return null;

        if (cacheNodes.getLength() > 1)
            throw new MyBatisException("There is more than one cache element in mapper " + doc.getDocumentElement().getAttribute("namespace"));

        Element cacheElement = (Element) cacheNodes.item(0);
        String eviction = cacheElement.getAttribute("eviction");
        int flushInterval = Integer.parseInt(cacheElement.getAttribute("flushInterval"));
        int size = Integer.parseInt(cacheElement.getAttribute("size"));
        boolean readOnly = Boolean.parseBoolean(cacheElement.getAttribute("readOnly"));

        CacheFactoryBuilder builder = CacheFactoryBuilder.getBuilder();
        builder.setEviction(eviction).setFlushInterval(flushInterval).setSize(size).setReadOnly(readOnly);
        return builder.build();
    }

    private void parseSelect(Document doc, ArrayList<SqlMapping> mappings) {
        NodeList selects = doc.getElementsByTagName("select");
        for (int i = 0; i < selects.getLength(); i++) {
            Element selectElement = (Element) selects.item(i);
            SqlSelect sqlSelect = new SqlSelect();

            getBasicInfo(selectElement, sqlSelect);
            sqlSelect.resultType = selectElement.getAttribute("resultType");
            sqlSelect.resultMapType = selectElement.getAttribute("resultMap");
            String useCache = selectElement.getAttribute("useCache");
            sqlSelect.useCache = useCache.equals("true") || useCache.isEmpty();
            mappings.add(sqlSelect);
        }
    }

    private void parseInsert(Document doc, ArrayList<SqlMapping> mappings) {
        NodeList inserts = doc.getElementsByTagName("insert");
        for (int i = 0; i < inserts.getLength(); i++) {
            Element insertElement = (Element) inserts.item(i);
            SqlInsert sqlInsert = new SqlInsert();

            getBasicInfo(insertElement, sqlInsert);
            sqlInsert.keyProperty = insertElement.getAttribute("keyProperty");
            String useGeneratedKeysString = insertElement.getAttribute("useGeneratedKeys");
            sqlInsert.useGeneratedKeys = useGeneratedKeysString.equals("true");
            mappings.add(sqlInsert);
        }
    }

    private void parseUpdate(Document doc, ArrayList<SqlMapping> mappings) {
        NodeList updates = doc.getElementsByTagName("update");
        for (int i = 0; i < updates.getLength(); i++) {
            Element updateElement = (Element) updates.item(i);
            SqlUpdate sqlUpdate = new SqlUpdate();

            getBasicInfo(updateElement, sqlUpdate);
            mappings.add(sqlUpdate);
        }
    }

    private void parseDelete(Document doc, ArrayList<SqlMapping> mappings) {
        NodeList deletes = doc.getElementsByTagName("delete");
        for (int i = 0; i < deletes.getLength(); i++) {
            Element deleteElement = (Element) deletes.item(i);
            SqlDelete sqlDelete = new SqlDelete();

            getBasicInfo(deleteElement, sqlDelete);
            mappings.add(sqlDelete);
        }
    }

    private void getBasicInfo(Element e, SqlMapping mapping) {
        mapping.mappingType = valueOf(e.getNodeName().toUpperCase());
        mapping.id = e.getAttribute("id");
        mapping.parameterType = e.getAttribute("parameterType");
        mapping.sql = e.getTextContent();
        mapping.flushCache = e.getAttribute("flushCache").equals("true");
        prepareSql(mapping);
    }

    private ResultMap[] parseResultMaps(Document doc) {
        ArrayList<ResultMap> resultMaps = new ArrayList<>();

        NodeList resultMapsNodes = doc.getElementsByTagName("resultMap");
        for (int i = 0; i < resultMapsNodes.getLength(); i++) {
            Element resultMapElement = (Element) resultMapsNodes.item(i);
            ResultMap resultMap = new ResultMap();

            resultMap.id = resultMapElement.getAttribute("id");
            resultMap.type = resultMapElement.getAttribute("type");

            NodeList childNodes = resultMapElement.getChildNodes();
            for (int j = 0; j < childNodes.getLength(); j++) {
                Node node = childNodes.item(j);
                String nodeName = node.getNodeName();
                if (nodeName.equals("id") || nodeName.equals("result")) {
                    Element element = (Element) node;
                    String property = element.getAttribute("property");
                    String column = element.getAttribute("column");
                    resultMap.mappings.put(column, property);
                }
            }
            resultMaps.add(resultMap);
        }
        return resultMaps.toArray(ResultMap[]::new);
    }
}
