package db.migration;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V7__expire_billing_for_withdrawn_members extends BaseJavaMigration {

  @Override
  public void migrate(Context context) throws SQLException {
    Connection connection = context.getConnection();
    // An empty schema has no withdrawn members to backfill before Hibernate starts.
    if (!memberTableExists(connection)) {
      return;
    }

    try (Statement statement = connection.createStatement()) {
      statement.executeUpdate(
          """
          UPDATE applydays_subscription s
          SET status = 'EXPIRED', next_billing_date = NULL
          WHERE s.status IN ('ACTIVE', 'CANCELED')
            AND EXISTS (SELECT 1 FROM member m WHERE m.id = s.member_id AND m.deleted = true)
          """);
      statement.executeUpdate(
          """
          UPDATE applydays_payment_method p
          SET deleted = true
          WHERE p.deleted = false
            AND EXISTS (SELECT 1 FROM member m WHERE m.id = p.member_id AND m.deleted = true)
          """);
    }
  }

  private boolean memberTableExists(Connection connection) throws SQLException {
    DatabaseMetaData metadata = connection.getMetaData();
    String tableName = metadata.storesUpperCaseIdentifiers() ? "MEMBER" : "member";
    try (ResultSet tables =
        metadata.getTables(connection.getCatalog(), connection.getSchema(), tableName, null)) {
      return tables.next();
    }
  }
}
