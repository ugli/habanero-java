package se.ugli.habanero.j.metadata;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import javax.sql.DataSource;

import se.ugli.habanero.j.HabaneroException;

public class MetaData {

    private final DataSource dataSource;

    private MetaData(final DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public static MetaData apply(final DataSource dataSource) {
        return new MetaData(dataSource);
    }

    public Optional<SqlType> getColumnType(final String tableName, final String columnName) {
        try (Connection connection = dataSource.getConnection()) {
            final DatabaseMetaData metaData = connection.getMetaData();
            try (ResultSet resultSet = metaData.getColumns(null, null, tableName.toUpperCase(),
                    columnName.toUpperCase())) {
                while (resultSet.next()) {
                    final int typeNumber = resultSet.getInt("DATA_TYPE");
                    return Optional.ofNullable(SqlType.applyTypeNumber(typeNumber));
                }
                return Optional.empty();
            }
        }
        catch (final SQLException e) {
            throw new HabaneroException(e);
        }
    }

    public DatabaseProductName getDatabaseProductName() {
        try (Connection connection = dataSource.getConnection()) {
            final DatabaseMetaData databaseMetaData = connection.getMetaData();
            final String productName = databaseMetaData.getDatabaseProductName();
            if (productName.equalsIgnoreCase("PostgreSQL"))
                return DatabaseProductName.POSTGRESQL;
            else if (productName.equalsIgnoreCase("H2"))
                return DatabaseProductName.H2;
            else if (productName.contains("DB2"))
                return DatabaseProductName.DB2;
            else
                throw new HabaneroException("Unknown product name: " + productName);
        }
        catch (final SQLException e) {
            throw new HabaneroException(e);
        }
    }

}
