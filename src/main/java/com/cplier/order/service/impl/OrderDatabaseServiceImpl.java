package com.cplier.order.service.impl;

import com.cplier.order.domain.Order;
import com.cplier.order.exception.PKNotFoundException;
import com.cplier.order.service.OrderDatabaseService;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public class OrderDatabaseServiceImpl implements OrderDatabaseService {

  private static final String SQL_ADD_NEW_ORDER =
      "INSERT INTO t_order(name, price) VALUES ($1, $2) RETURNING id";
  private static final String SQL_DELETE_ORDER_BY_ID = "DELETE FROM t_order WHERE id = $1";
  private static final String SQL_FIND_ORDER_BY_ID = "SELECT * FROM t_order WHERE id = $1";
  private static final String SQL_UPSERT_ORDER_BY_ID =
      "INSERT INTO t_order VALUES($1, $2, $3, $4) "
          + "ON CONFLICT(id) DO UPDATE SET name = $2, price = $3, time = $4";
  private static final String SQL_FIND_ALL_ORDERS = "SELECT * FROM t_order WHERE TRUE";

  private static final Logger LOGGER = LoggerFactory.getLogger(OrderDatabaseServiceImpl.class);

  private final PgPool pgConnectionPool;

  public OrderDatabaseServiceImpl(
      io.vertx.pgclient.PgPool pgPool, Handler<AsyncResult<OrderDatabaseService>> resultHandler) {
    this.pgConnectionPool = new PgPool(pgPool);
    pgConnectionPool
        .rxGetConnection()
        .flatMap(
            pgConnection ->
                pgConnection.rxPrepare(SQL_FIND_ALL_ORDERS).doAfterTerminate(pgConnection::close))
        .subscribe(
            result -> resultHandler.handle(Future.succeededFuture(this)),
            throwable -> {
              LOGGER.error("Can not open a database connection", throwable);
              resultHandler.handle(Future.failedFuture(throwable));
            });
  }

  private JsonArray extract(RowSet<Row> rowRowSet) {
    JsonArray jsonArray = new JsonArray();
    for (Row row : rowRowSet) {
      JsonObject jsonObject = new JsonObject();
      jsonObject.put("id", row.getLong("id"));
      jsonObject.put("name", row.getString("name"));
      jsonObject.put("price", row.getDouble("price"));
      OffsetDateTime time = row.getOffsetDateTime("time");
      if (time != null) {
        jsonObject.put("time", time.format(DateTimeFormatter.ISO_DATE));
      }
      jsonArray.add(jsonObject);
    }
    return jsonArray;
  }

  @Override
  public void createOrder(Order order, Handler<AsyncResult<JsonObject>> resultHandler) {
    pgConnectionPool
        .getDelegate()
        .preparedQuery(SQL_ADD_NEW_ORDER)
        .execute(
            Tuple.of(order.getName(), order.getPrice()),
            ar -> {
              if (ar.succeeded()) {
                for (Row row : ar.result()) {
                  order.setId(row.getLong("id"));
                  order.setTime(OffsetDateTime.now());
                }
                JsonObject jsonObject = order.toJson();
                resultHandler.handle(Future.succeededFuture(jsonObject));

              } else {
                LOGGER.error("Failed to add a new order into database", ar.cause());
                resultHandler.handle(Future.failedFuture(ar.cause()));
              }
            });
  }

  @Override
  public void getAllOrder(Handler<AsyncResult<JsonArray>> resultHandler) {
    pgConnectionPool
        .getDelegate()
        .preparedQuery(SQL_FIND_ALL_ORDERS)
        .execute(
            ar -> {
              if (ar.succeeded()) {
                JsonArray jsonArray = extract(ar.result());
                resultHandler.handle(Future.succeededFuture(jsonArray));
              } else {
                LOGGER.error("Failed to get the filtered orders", ar.cause());
                resultHandler.handle(Future.failedFuture(ar.cause()));
              }
            });
  }

  @Override
  public void getOrderById(Long id, Handler<AsyncResult<JsonObject>> resultHandler) {
    pgConnectionPool
        .getDelegate()
        .preparedQuery(SQL_FIND_ORDER_BY_ID)
        .execute(
            Tuple.of(id),
            ar -> {
              if (ar.succeeded()) {
                JsonArray jsonArray = extract(ar.result());
                if (!jsonArray.isEmpty()) {
                  JsonObject jsonObject = jsonArray.getJsonObject(0);
                  resultHandler.handle(Future.succeededFuture(jsonObject));
                } else {
                  resultHandler.handle(
                      Future.failedFuture(new PKNotFoundException(404, "id not found")));
                }
              } else {
                LOGGER.error("Failed to get order by id {}", id, ar.cause());
                resultHandler.handle(Future.failedFuture(ar.cause()));
              }
            });
  }

  @Override
  public void updateOrderById(
      Long id, Order order, Handler<AsyncResult<JsonObject>> resultHandler) {
    pgConnectionPool
        .getDelegate()
        .preparedQuery(SQL_UPSERT_ORDER_BY_ID)
        .execute(
            Tuple.of(id, order.getName(), order.getPrice(), order.getTime()),
            ar -> {
              if (ar.succeeded()) {
                JsonObject jsonObject = extract(ar.result()).getJsonObject(0);
                resultHandler.handle(Future.succeededFuture(jsonObject));

              } else {
                LOGGER.error("Failed to update order by id {}", id, ar.cause());
                resultHandler.handle(Future.failedFuture(ar.cause()));
              }
            });
  }

  @Override
  public void deleteOrderById(Long id, Handler<AsyncResult<Void>> resultHandler) {
    pgConnectionPool
        .getDelegate()
        .preparedQuery(SQL_DELETE_ORDER_BY_ID)
        .execute(
            Tuple.of(id),
            ar -> {
              if (ar.succeeded()) {
                resultHandler.handle(Future.succeededFuture());

              } else {
                LOGGER.error("Failed to delete order by id {}", id, ar.cause());
                resultHandler.handle(Future.failedFuture(ar.cause()));
              }
            });
  }
}
