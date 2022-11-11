package Parsers;

import ConfigurationModels.Mapper;
import SqlMappingModels.*;
import Utility.Resources;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;

class MapperParser {
    Mapper parse(String resource) throws Exception {
        Document doc = createDoc(resource);
        if (doc.getNodeName().equals("mapper"))
            return null;

        Mapper mapper = new Mapper();
        mapper.namespace =  doc.getDocumentElement().getAttribute("namespace");
        mapper.mappings = parseMappings(doc);
        mapper.resultMaps = parseResultMaps(doc);

        return mapper;
    }

    private Document createDoc(String resource) throws Exception {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

        Document doc;
        try(Reader reader = Resources.getResourceAsReader(resource)) {
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

    private void parseSelect(Document doc, ArrayList<SqlMapping> mappings) {
        NodeList selects = doc.getElementsByTagName("select");
        for (int i = 0; i < selects.getLength(); i++) {
            Element selectElement = (Element) selects.item(i);
            SqlSelect sqlSelect = new SqlSelect();

            getBasicInfo(selectElement, sqlSelect);
            sqlSelect.resultType = selectElement.getAttribute("resultType");
            sqlSelect.resultMapType = selectElement.getAttribute("resultMap");
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
        mapping.mappingType = MappingTypeEnum.valueOf(e.getNodeName().toUpperCase());
        mapping.id = e.getAttribute("id");
        mapping.parameterType = e.getAttribute("parameterType");
        mapping.sql = e.getTextContent();
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
