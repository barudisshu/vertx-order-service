package com.cplier.order;

import com.cplier.order.verticle.HttpRxVerticle;
import com.cplier.order.service.OrderDatabaseVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Vertx;

public class OrderApplication {

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();

    vertx.deployVerticle(
        OrderDatabaseVerticle.class.getName(),
        new DeploymentOptions()
            .setConfig(
                new JsonObject()
                    .put("library.db.eb.address", "library.db.queue")
                    .put("postgresql.host", "localhost")
                    .put("postgresql.port", 5432)
                    .put("postgresql.database", "postgres")
                    .put("postgresql.username", "postgres")
                    .put("postgresql.password", "postgres")
                    .put("postgresql.pool.maxsize", 10)));
    vertx.deployVerticle(
        HttpRxVerticle.class.getName(),
        new DeploymentOptions()
            .setConfig(
                new JsonObject()
                    .put("http.server.port", 8080)
                    .put("library.db.eb.address", "library.db.queue")));
  }
}
