package com.ericsson.cces.order.domain;

import io.vertx.codegen.annotations.DataObject;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.vertx.core.json.JsonObject;

import java.time.OffsetDateTime;

@DataObject(generateConverter = true)
@JsonPropertyOrder({"id", "name", "price", "time"})
public class Order {

  @JsonProperty("id")
  private Long id;

  @JsonProperty("name")
  private String name;

  @JsonProperty("price")
  private double price;

  @JsonProperty("time")
  private OffsetDateTime time;

  public Order() {}

  public Order(Order other) {
    this.id = other.id;
    this.name = other.name;
    this.price = other.price;
    this.time = other.time;
  }

  public Order(String json) {
    this(new JsonObject(json));
  }

  public Order(JsonObject jsonObject) {
    OrderConverter.fromJson(jsonObject, this);
  }

  public Order(Long id, String name, double price, OffsetDateTime time) {
    this.id = id;
    this.name = name;
    this.price = price;
    this.time = time;
  }

  public JsonObject toJson() {
    JsonObject jsonObject = new JsonObject();
    OrderConverter.toJson(this, jsonObject);
    return jsonObject;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public double getPrice() {
    return price;
  }

  public void setPrice(double price) {
    this.price = price;
  }

  public OffsetDateTime getTime() {
    return time;
  }

  public void setTime(OffsetDateTime time) {
    this.time = time;
  }
}
