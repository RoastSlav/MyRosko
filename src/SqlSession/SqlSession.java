package SqlSession;

import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

public class SqlSession implements Closeable {
    private final Connection conn;
    protected SqlSession(Connection conn) {
        this.conn = conn;
    }

    @Override
    public void close() throws IOException {
        try {
            conn.close();
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }
}
