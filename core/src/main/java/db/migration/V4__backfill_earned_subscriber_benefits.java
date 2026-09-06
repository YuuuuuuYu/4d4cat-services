package db.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V4__backfill_earned_subscriber_benefits extends BaseJavaMigration {

  @Override
  public void migrate(Context context) throws SQLException {
    Connection connection = context.getConnection();
    if (!"PostgreSQL".equals(connection.getMetaData().getDatabaseProductName())
        || !applicationTableExists(connection)) {
      return;
    }

    try (PreparedStatement statement =
        connection.prepareStatement(
            """
            INSERT INTO applydays_member_benefit (
                id,
                member_id,
                benefit_type,
                created_at,
                updated_at
            )
            SELECT
                vr.member_id,
                vr.member_id,
                'SUBSCRIBER_ACCESS',
                CURRENT_TIMESTAMP,
                CURRENT_TIMESTAMP
            FROM application a
            JOIN verification_request vr ON vr.application_id = a.id
            JOIN member m ON m.id = vr.member_id
            WHERE a.deleted = FALSE
              AND a.verification_status = 'APPROVED'
              AND vr.status = 'APPROVED'
              AND m.deleted = FALSE
              AND m.role IN ('REVIEWER', 'SUBSCRIBER')
              AND EXISTS (
                  SELECT 1
                  FROM jsonb_array_elements(a.hiring_process) step
                  WHERE step ->> 'stepType' = 'DOCUMENT'
                    AND step ->> 'status' = 'PASSED'
              )
            GROUP BY vr.member_id
            HAVING COUNT(*) >= 10
            ON CONFLICT (member_id, benefit_type) DO NOTHING
            """)) {
      statement.executeUpdate();
    }
  }

  private boolean applicationTableExists(Connection connection) throws SQLException {
    try (ResultSet tables = connection.getMetaData().getTables(null, null, "application", null)) {
      return tables.next();
    }
  }
}
