package com.cplier.order.verticle;

import com.cplier.order.domain.Order;
import com.cplier.order.exception.PKNotFoundException;
import com.cplier.order.service.reactivex.OrderDatabaseService;
import io.reactivex.Completable;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.http.HttpServer;
import io.vertx.reactivex.core.http.HttpServerResponse;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.handler.BodyHandler;

import java.util.Objects;

public class HttpRxVerticle extends AbstractVerticle {

  private static final String CONFIG_HTTP_SERVER_PORT = "http.server.port";
  private static final String CONFIG_DB_EB_QUEUE = "library.db.eb.address";

  @Override
  public Completable rxStart() {

    HttpServer server = vertx.createHttpServer();

    OrderDatabaseService orderDatabaseService =
        com.cplier.order.service.OrderDatabaseService.createProxy(
            vertx.getDelegate(), config().getString(CONFIG_DB_EB_QUEUE));

    // index, home
    final Router router = Router.router(vertx);

    // Enable to retrieve request bodies.
    router.route().handler(BodyHandler.create());

    router.get("/").handler(this::index);
    // create :order
    router.post("/order").handler(createOrder(orderDatabaseService));
    // get :order
    router.get("/order").handler(getAllOrder(orderDatabaseService));
    // get :order/{id}
    router.get("/order/:orderId").handler(getOrderById(orderDatabaseService));
    // put :order/{id}
    router.put("/order/:orderId").handler(updateOrderById(orderDatabaseService));
    // delete :order/{id}
    router.delete("/order/:orderId").handler(deleteOrderById(orderDatabaseService));

    router
        .route()
        .failureHandler(
            ctx -> {
              Throwable failure = ctx.failure();
              if (failure instanceof PKNotFoundException) {
                HttpServerResponse response = ctx.response();
                response.putHeader("content-type", "text/plain;charset=UTF-8");
                response.setStatusCode(404);
                response.end(failure.getMessage());
              }
            });

    return server
        .requestHandler(router)
        .rxListen(config().getInteger(CONFIG_HTTP_SERVER_PORT, 8080))
        .ignoreElement();
  }

  private void index(RoutingContext ctx) {
    HttpServerResponse response = ctx.response();
    response.putHeader("content-type", "text/plain;charset=UTF-8");
    response.setStatusCode(200);
    response.end("hello world");
  }

  private Handler<RoutingContext> createOrder(OrderDatabaseService orderDatabaseService) {
    return ctx -> {
      Order order = Json.decodeValue(ctx.getBodyAsString(), Order.class);
      orderDatabaseService.createOrder(
          order,
          ar -> {
            if (ar.succeeded()) {
              JsonObject jsonObject = ar.result();
              ctx.response()
                  .putHeader("Content-Type", "application/json")
                  .setStatusCode(201)
                  .end(jsonObject.encode());
            } else {
              ctx.fail(400, ar.cause());
            }
          });
    };
  }

  private Handler<RoutingContext> getAllOrder(OrderDatabaseService orderDatabaseService) {
    return ctx ->
        orderDatabaseService.getAllOrder(
            ar -> {
              if (ar.succeeded()) {
                JsonArray jsonArray = ar.result();
                ctx.response()
                    .putHeader("Content-Type", "application/json")
                    .setStatusCode(200)
                    .end(jsonArray.encode());
              } else {
                ctx.fail(404, ar.cause());
              }
            });
  }

  private Handler<RoutingContext> getOrderById(OrderDatabaseService orderDatabaseService) {
    return ctx -> {
      Objects.requireNonNull(ctx);
      Long orderId = Long.parseLong(ctx.pathParam("orderId"));
      orderDatabaseService.getOrderById(
          orderId,
          ar -> {
            if (ar.succeeded()) {
              JsonObject jsonObject = ar.result();
              ctx.response()
                  .putHeader("Content-Type", "application/json")
                  .setStatusCode(200)
                  .end(jsonObject.encode());
            } else {
              ctx.fail(400, ar.cause());
            }
          });
    };
  }

  private Handler<RoutingContext> updateOrderById(OrderDatabaseService orderDatabaseService) {
    return ctx -> {
      Objects.requireNonNull(ctx);
      Long orderId = Long.parseLong(ctx.pathParam("orderId"));
      orderDatabaseService.updateOrderById(
          orderId,
          Json.decodeValue(ctx.getBodyAsString(), Order.class),
          ar -> {
            if (ar.succeeded()) {
              JsonObject jsonObject = ar.result();
              ctx.response()
                  .putHeader("Content-Type", "application/json")
                  .setStatusCode(200)
                  .end(jsonObject.encode());
            } else {
              ctx.fail(400, ar.cause());
            }
          });
    };
  }

  private Handler<RoutingContext> deleteOrderById(OrderDatabaseService orderDatabaseService) {
    return ctx -> {
      Objects.requireNonNull(ctx);
      Long orderId = Long.parseLong(ctx.pathParam("orderId"));
      orderDatabaseService.deleteOrderById(
          orderId,
          ar -> {
            if (ar.succeeded()) {
              ctx.response().putHeader("Content-Type", "application/json").setStatusCode(202).end();
            } else {
              ctx.fail(400, ar.cause());
            }
          });
    };
  }
}
