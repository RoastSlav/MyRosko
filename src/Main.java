import SqlSession.*;
import Utility.Resources;

import java.io.IOException;
import java.io.Reader;
import java.sql.SQLException;

public class Main {
    public static void main(String[] args) throws Exception {
        String resource = "mybatis-config.xml";
        SqlSessionFactory sessionFactory = getSessionFactory(resource);
        try (SqlSession sqlSession = sessionFactory.openSession()) {

        } catch (Exception e) {
            throw new SQLException("The session did not close successfully");
        }
    }

    public static SqlSessionFactory getSessionFactory(String resource) throws IOException {
        try (Reader reader = Resources.getResourceAsReader(resource)) {
            return new SqlSessionFactoryBuilder().build(reader);
        }
    }
}
