package com.ericsson.cces.order.exception;

import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ServiceException;

public class PKNotFoundException extends ServiceException {

  public PKNotFoundException(int failureCode, String message) {
    super(failureCode, message);
  }

  public PKNotFoundException(int failureCode, String message, JsonObject debugInfo) {
    super(failureCode, message, debugInfo);
  }
}
