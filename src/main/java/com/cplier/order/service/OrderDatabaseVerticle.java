package com.cplier.order.service;

import io.vertx.core.Promise;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.serviceproxy.ServiceBinder;
import io.vertx.sqlclient.PoolOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OrderDatabaseVerticle extends AbstractVerticle {
  private static final String CONFIG_PG_HOST = "postgresql.host";
  private static final String CONFIG_PG_PORT = "postgresql.port";
  private static final String CONFIG_PG_DATABASE = "postgresql.database";
  private static final String CONFIG_PG_USERNAME = "postgresql.username";
  private static final String CONFIG_PG_PASSWORD = "postgresql.password";
  private static final String CONFIG_PG_POOL_MAX_SIZE = "postgresql.pool.maxsize";

  private static final String CONFIG_DB_EB_QUEUE = "library.db.eb.address";

  private static final Logger LOGGER = LoggerFactory.getLogger(OrderDatabaseVerticle.class);

  private PgPool pgPool;

  @Override
  public void start(Promise<Void> promise) {
    PgConnectOptions connectOptions =
        new PgConnectOptions()
            .setHost(config().getString(CONFIG_PG_HOST, "127.0.0.1"))
            .setPort(config().getInteger(CONFIG_PG_PORT, 5432))
            .setDatabase(config().getString(CONFIG_PG_DATABASE))
            .setUser(config().getString(CONFIG_PG_USERNAME))
            .setPassword(config().getString(CONFIG_PG_PASSWORD))
            .setIdleTimeout(30)
            .setReconnectAttempts(2)
            .setReconnectInterval(1000)
            .setPreparedStatementCacheMaxSize(10);

    // Pool Options
    PoolOptions poolOptions =
        new PoolOptions().setMaxSize(config().getInteger(CONFIG_PG_POOL_MAX_SIZE));

    this.pgPool = PgPool.pool(vertx.getDelegate(), connectOptions, poolOptions);

    String databaseEbAddress = config().getString(CONFIG_DB_EB_QUEUE);

    OrderDatabaseService.create(
        pgPool,
        result -> {
          if (result.succeeded()) {
            // register the database service
            new ServiceBinder(vertx.getDelegate())
                .setAddress(databaseEbAddress)
                .register(OrderDatabaseService.class, result.result())
                .exceptionHandler(
                    throwable -> {
                      LOGGER.error("Failed to establish PostgreSQL database service", throwable);
                      promise.fail(throwable);
                    })
                .completionHandler(
                    res -> {
                      LOGGER.info(
                          "PostgreSQL database service is successfully established in {}",
                          databaseEbAddress);
                      promise.complete();
                    });
          } else {
            LOGGER.error("Failed to initiate the connection to database", result.cause());
            promise.fail(result.cause());
          }
        });
  }

  @Override
  public void stop() {
    pgPool.close();
  }
}
