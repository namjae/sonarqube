package org.sonar.server.platform.ws;

import com.google.common.io.Resources;
import org.sonar.api.platform.ServerUpgradeStatus;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.platform.monitoring.DatabaseMonitor;
import org.sonar.server.platform.monitoring.DatabaseMonitorMBean;

public class ServerMigrateWsAction implements ServerWsAction {

  private final ServerUpgradeStatus serverUpgradeStatus;
  private final DatabaseMonitorMBean databaseMonitor;
  private final DBMigration dbMigration;

  public ServerMigrateWsAction(ServerUpgradeStatus serverUpgradeStatus,
                               DatabaseMonitorMBean databaseMonitor,
                               DBMigration dbMigration) {
    this.serverUpgradeStatus = serverUpgradeStatus;
    this.databaseMonitor = databaseMonitor;
    this.dbMigration = dbMigration;
  }

  @Override
  public void define(WebService.NewController controller) {
    controller.createAction("migrate")
        .setDescription("Migrate the database to match the current version of SonarQube")
        .setSince("5.2")
        .setPost(true)
        .setHandler(this)
        .setResponseExample(Resources.getResource(this.getClass(), "example-migrate.json"));
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    if (serverUpgradeStatus.isUpgraded())  {
      writeNoMigrationResponse(response);
    }
    if (databaseMonitor.attributes().get(DatabaseMonitor.DatabaseAttributes.PRODUCT))
  }
}
