package org.sonar.server.platform.ws;

public interface DBMigration {
  /**
   * Starts a DB migration to the schema that matches the current version of SonarQube.
   * <p>
   * This method throws an {@link IllegalStateException} when called as a db migration is already running. To avoid
   * getting an exception, one should check the value returned by {@link #running()} before calling this method.
   * </p>
   *
   * @throws IllegalStateException if the DB migration is already running
   */
  void start() throws IllegalStateException;

  /**
   * Indicates whether a DB migration is running.
   *
   * @return a boolean
   */
  boolean running();

  /**
   * Indicates whether a migration has been started since the last boot of SonarQube and if it ended successfully.
   * <p>
   * This flag is {@code false} when SonarQube is started and reset to {@code false} whenever {@link #start()} is
   * called.
   * </p>
   *
   * @return a boolean
   */
  boolean succeeded();
}
