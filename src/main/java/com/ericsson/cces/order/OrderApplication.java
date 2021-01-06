package com.ericsson.cces.order;

import com.ericsson.cces.order.service.OrderDatabaseVerticle;
import com.ericsson.cces.order.verticle.HttpRxVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Vertx;

public class OrderApplication {

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();

    vertx.deployVerticle(
        new OrderDatabaseVerticle(),
        new DeploymentOptions()
            .setConfig(
                new JsonObject()
                    .put("library.db.eb.address", "library.db.queue")
                    .put("postgresql.host", "localhost")
                    .put("postgresql.port", 5432)
                    .put("postgresql.database", "postgres")
                    .put("postgresql.username", "postgres")
                    .put("postgresql.password", "postgres")
                    .put("postgresql.pool.maxsize", 20)));
    vertx.deployVerticle(
        new HttpRxVerticle(),
        new DeploymentOptions()
            .setConfig(
                new JsonObject()
                    .put("http.server.port", 8080)
                    .put("library.db.eb.address", "library.db.queue")));
  }
}
