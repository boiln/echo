package mgo.echo.data.repository;

import java.sql.Connection;
import java.sql.SQLException;

import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;

public class EchoConnectionProvider implements ConnectionProvider {
    private static final long serialVersionUID = 1L;

    @Override
    public boolean isUnwrappableAs(Class<?> unwrapType) {
        return false;
    }

    @Override
    public <T> T unwrap(Class<T> unwrapType) {
        return null;
    }

    @Override
    public void closeConnection(Connection conn) throws SQLException {
        DbManager.close(conn);
    }

    @Override
    public Connection getConnection() throws SQLException {
        return DbManager.get();
    }

    @Override
    public boolean supportsAggressiveRelease() {
        return false;
    }
}