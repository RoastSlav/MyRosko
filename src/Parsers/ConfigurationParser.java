package Parsers;

import ConfigurationModels.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class ConfigurationParser {
    Document doc;
    Properties properties;

    public ConfigurationParser(Reader reader) throws ParserConfigurationException, IOException, SAXException {
        this(reader, null);
    }

    public ConfigurationParser(Reader reader, Properties properties) throws ParserConfigurationException, IOException, SAXException {
        this.properties = properties;
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        try (reader) {
            doc = dBuilder.parse(new InputSource(reader));
        }
        doc.getDocumentElement().normalize();
    }

    private static List<Element> getChildrenByTagName(Element parent, String name) {
        List<Element> nodeList = new ArrayList<>();
        for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNodeType() == Node.ELEMENT_NODE && name.equals(child.getNodeName()))
                nodeList.add((Element) child);
        }

        return nodeList;
    }

    private static Element getChildByTagName(Element parent, String name) throws ParserConfigurationException {
        List<Element> childrenByTagName = getChildrenByTagName(parent, name);
        if (childrenByTagName.size() > 1)
            throw new ParserConfigurationException("There is more than one: " + name);

        if (childrenByTagName.size() < 1)
            return null;

        return childrenByTagName.get(0);
    }

    public Configuration parse() throws Exception {
        Configuration configuration = new Configuration();
        if (!doc.getDocumentElement().getNodeName().equals("configuration"))
            throw new ParserConfigurationException("The file is not a configuration file");

        Properties parsedProps = parseProperties(doc.getDocumentElement());
        if (this.properties != null)
            parsedProps.putAll(this.properties);
        configuration.properties = parsedProps;
        configuration.typeAliases = parseTypeAliases(doc.getDocumentElement());
        configuration.environments = parseEnvironments(doc.getDocumentElement(), configuration.properties);
        configuration.mappers = parseMappers(doc.getDocumentElement());
        return configuration;
    }

    private Mapper[] parseMappers(Element conf) throws Exception {
        ArrayList<Mapper> mappers = new ArrayList<>();
        Element mappersElement = getChildByTagName(conf, "mappers");
        if (mappersElement == null)
            throw new ParserConfigurationException("The mappers element is missing from the configuration file");

        List<Element> mapperElements = getChildrenByTagName(mappersElement, "mapper");
        MapperParser parser = new MapperParser();
        for (Element element : mapperElements) {
            String resource = element.getAttribute("resource");
            String classResource = element.getAttribute("class");
            Mapper mapper;
            try {
                if (!resource.isEmpty()) {
                    mapper = parser.parse(resource);
                } else if (!classResource.isEmpty()) {
                    mapper = parser.parseClassMapper(Class.forName(classResource));
                } else
                    throw new ParserConfigurationException("There was an error parsing a mapper");
            } catch (Exception e) {
                throw new ParserConfigurationException("There was an error parsing a mapper: " + resource);
            }
            mappers.add(mapper);
        }

        return mappers.toArray(Mapper[]::new);
    }

    private Properties parseProperties(Element conf) throws IOException, ParserConfigurationException {
        Properties properties = new Properties();
        Element propertiesElement = getChildByTagName(conf, "properties");
        if (propertiesElement == null)
            throw new ParserConfigurationException("The properties element is missing from the configuration file");

        List<Element> propertyElements = getChildrenByTagName(propertiesElement, "property");
        for (Element propertyElement : propertyElements) {
            String name = propertyElement.getAttribute("name");
            String value = propertyElement.getAttribute("value");
            if (name.isEmpty() || value.isEmpty()) {
                String resource = propertyElement.getAttribute("resource");
                if (!resource.isEmpty() && Files.exists(Path.of(resource)))
                    properties.load(new FileInputStream(resource));
            } else
                properties.setProperty(name, value);
        }

        return properties;
    }

    private TypeAlias[] parseTypeAliases(Element conf) throws ParserConfigurationException {
        ArrayList<TypeAlias> aliases = new ArrayList<>();
        Element typeAliasesElement = getChildByTagName(conf, "typeAliases");
        if (typeAliasesElement == null)
            throw new ParserConfigurationException("The typeAliases element is missing from the configuration file");
        ;

        List<Element> typeAliasElements = getChildrenByTagName(typeAliasesElement, "typeAlias");
        for (Element typeAliasElement : typeAliasElements) {
            String type = typeAliasElement.getAttribute("type");
            String alias = typeAliasElement.getAttribute("alias");
            TypeAlias typeAlias = new TypeAlias(type, alias);
            aliases.add(typeAlias);
        }
        return aliases.toArray(TypeAlias[]::new);
    }

    private Environments parseEnvironments(Element conf, Properties props) throws ParserConfigurationException {
        ArrayList<Environment> environments = new ArrayList<>();
        Element environmentsElement = getChildByTagName(conf, "environments");
        if (environmentsElement == null)
            throw new ParserConfigurationException("The environments element is missing from the configuration file");

        String defaultEnvId = environmentsElement.getAttribute("default");
        Environment defaultEnv = null;

        List<Element> environmentElements = getChildrenByTagName(environmentsElement, "environment");
        for (Element environmentElement : environmentElements) {
            Environment env = new Environment();
            env.id = environmentElement.getAttribute("id");
            if (env.id.equals(defaultEnvId))
                defaultEnv = env;

            List<Element> transManElements = getChildrenByTagName(environmentElement, "transactionManager");
            env.transactionManager = transManElements.get(0).getAttribute("type");
            env.dataSource = parseDataSource(environmentElement, props);
            environments.add(env);
        }

        Environment[] environmentsArray = environments.toArray(Environment[]::new);
        return new Environments(environmentsArray, defaultEnv);
    }

    private DataSource parseDataSource(Element environment, Properties props) throws ParserConfigurationException {
        DataSource dataSource = new DataSource();
        Element dataSourceElement = getChildByTagName(environment, "dataSource");
        if (dataSourceElement == null)
            throw new ParserConfigurationException("The dataSource element is missing from the configuration file");
        dataSource.type = dataSourceElement.getAttribute("type");

        List<Element> propertyElements = getChildrenByTagName(dataSourceElement, "property");
        for (Element propertyElement : propertyElements) {
            String name = propertyElement.getAttribute("name");
            String value = propertyElement.getAttribute("value");
            if (value.startsWith("#{") && value.endsWith("}"))
                value = props.getProperty(value.substring(2, value.length() - 1));

            dataSource.properties.setProperty(name, value);
        }
        return dataSource;
    }
}
