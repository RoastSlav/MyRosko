import SqlSession.*;
import Utility.Resources;

import java.io.Reader;
import java.sql.SQLException;

public class Main {
    public static void main(String[] args) throws SQLException {
        String resource = "mybatis-config.xml";
        SqlSessionFactory sessionFactory = getSessionFactory(resource);
        try (SqlSession sqlSession = sessionFactory.openSession()) {

        } catch (Exception e) {
            throw new SQLException("The session did not close successfully");
        }
    }

    public static SqlSessionFactory getSessionFactory(String resource){
        try (Reader reader = Resources.getResourceAsReader(resource)) {
            return new SqlSessionFactoryBuilder().build(reader);
        } catch (Exception e) {
            throw new IllegalStateException("The resource was not loaded successfully");
        }
    }
}
