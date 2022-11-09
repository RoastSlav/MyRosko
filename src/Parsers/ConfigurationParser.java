package Parsers;

import ConfigurationModels.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
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
        try(reader) {
            doc = dBuilder.parse(new InputSource(reader));
        }
        doc.getDocumentElement().normalize();
    }

    public Configuration parse() throws IOException {
        Configuration configuration = new Configuration();
        if (!doc.getDocumentElement().getNodeName().equals("configuration"))
            return null;

        Properties parsedProps = parseProperties();
        if (parsedProps != null && this.properties != null)
            parsedProps.putAll(this.properties);
        else if (parsedProps == null && this.properties != null)
            parsedProps = this.properties;
        configuration.properties = parsedProps;

        configuration.typeAliases = parseTypeAliases();
        Environment[] environments = parseEnvironments(configuration.properties);
        configuration.environments = new Environments(environments);
        configuration.mappers = parseMappers();
        return configuration;
    }

    private Mapper[] parseMappers() {
        ArrayList<Mapper> mappers = new ArrayList<>();
        NodeList mappersNode = doc.getElementsByTagName("mappers");
        Element mappersElement = (Element) mappersNode.item(0);
        if (mappersElement == null)
            return new Mapper[0];

        NodeList mappersNodeList = mappersElement.getElementsByTagName("mapper");
        MapperParser parser = new MapperParser();
        for (int i = 0; i < mappersNodeList.getLength(); i++) {
            Element mapperElement = (Element) mappersNodeList.item(i);
            String resource = mapperElement.getAttribute("resource");
            Mapper mapper;
            try {
                mapper = parser.parse(resource);
            } catch (Exception e) {
                throw new RuntimeException("Could not parse mapper");
            }
            mappers.add(mapper);
        }

        return mappers.toArray(Mapper[]::new);
    }

    private Properties parseProperties() throws IOException {
        Properties properties = new Properties();
        NodeList nodeList = doc.getElementsByTagName("properties");
        Element propsElement = (Element) nodeList.item(0);
        if (propsElement == null)
            return null;

        NodeList childNodes = propsElement.getElementsByTagName("property");
        for (int i = 0; i < childNodes.getLength(); i++) {
            Element propElement = (Element) childNodes.item(i);
            String name = propElement.getAttribute("name");
            String value = propElement.getAttribute("value");
            properties.setProperty(name, value);
        }

        String resource = propsElement.getAttribute("resource");
        if (!resource.isEmpty() && Files.exists(Path.of(resource)))
            properties.load(new FileInputStream(resource));

        return properties;
    }

    private TypeAlias[] parseTypeAliases() {
        ArrayList<TypeAlias> aliases = new ArrayList<>();
        NodeList nodeList = doc.getElementsByTagName("typeAliases");
        Element typeAliasesElement = (Element) nodeList.item(0);
        if (typeAliasesElement == null)
            return null;

        NodeList typeAliases = typeAliasesElement.getElementsByTagName("typeAlias");
        for (int i = 0; i < typeAliases.getLength(); i++) {
            Element aliasElement = (Element) typeAliases.item(i);
            String type = aliasElement.getAttribute("type");
            String alias = aliasElement.getAttribute("alias");
            TypeAlias typeAlias = new TypeAlias(type, alias);
            aliases.add(typeAlias);
        }
        return aliases.toArray(TypeAlias[]::new);
    }

    private Environment[] parseEnvironments(Properties props) {
        ArrayList<Environment> environments = new ArrayList<>();
        NodeList nodeList = doc.getElementsByTagName("environments");
        Element environmentsElement = (Element) nodeList.item(0);
        if (environmentsElement == null)
            return null;

        NodeList environmentNodes = environmentsElement.getElementsByTagName("environment");
        for (int i = 0; i < environmentNodes.getLength(); i++) {
            Environment env = new Environment();
            Element environment = (Element) environmentNodes.item(i);
            env.id = environment.getAttribute("id");

            NodeList transManNode = environment.getElementsByTagName("transactionManager");
            Element transMan = (Element) transManNode.item(0);
            env.transactionManager = transMan.getAttribute("type");
            env.dataSource = parseDataSource(environment, props);
            environments.add(env);
        }
        return environments.toArray(Environment[]::new);
    }

    private DataSource parseDataSource(Element environment, Properties props) {
        DataSource dataSource = new DataSource();
        NodeList dataSourceNode = environment.getElementsByTagName("dataSource");
        Element dataSourceElement = (Element) dataSourceNode.item(0);
        dataSource.type = dataSourceElement.getAttribute("type");

        NodeList properties = dataSourceElement.getElementsByTagName("property");
        for (int j = 0; j < properties.getLength(); j++) {
            Element prop = (Element) properties.item(j);
            String name = prop.getAttribute("name");
            String value = prop.getAttribute("value");
            if (value.startsWith("#{") && value.endsWith("}"))
                value = props.getProperty(value.substring(2, value.length() - 1));

            dataSource.properties.setProperty(name, value);
        }
        return dataSource;
    }
}
