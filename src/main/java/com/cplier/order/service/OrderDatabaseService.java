package com.cplier.order.service;

import com.cplier.order.domain.Order;
import com.cplier.order.service.impl.OrderDatabaseServiceImpl;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgPool;

@ProxyGen
@VertxGen
public interface OrderDatabaseService {

  @GenIgnore
  static void create(
      PgPool pgPool, Handler<AsyncResult<OrderDatabaseService>> asyncResultHandler) {
      new OrderDatabaseServiceImpl(pgPool, asyncResultHandler);
  }

  @GenIgnore
  static com.cplier.order.service.reactivex.OrderDatabaseService createProxy(Vertx vertx, String address) {
    return new com.cplier.order.service.reactivex.OrderDatabaseService(new OrderDatabaseServiceVertxEBProxy(vertx, address));
  }

  void createOrder(Order order, Handler<AsyncResult<JsonObject>> resultHandler);

  void getAllOrder(Handler<AsyncResult<JsonArray>> resultHandler);

  void getOrderById(Long id, Handler<AsyncResult<JsonObject>> resultHandler);

  void updateOrderById(Long id, Order order, Handler<AsyncResult<JsonObject>> resultHandler);

  void deleteOrderById(Long id, Handler<AsyncResult<Void>> resultHandler);
}
