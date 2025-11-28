package com.rairai.consumer_kafka_1.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;


public class Order implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String customer;
    private List<OrderItem> items;
    private Double total;
    private Instant createdAt;

    public Order() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = Instant.now();
    }

    public Order(String id, String customer, List<OrderItem> items, Double total, Instant createdAt) {
        this.id = id;
        this.customer = customer;
        this.items = items;
        this.total = total;
        this.createdAt = createdAt;
    }


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCustomer() {
        return customer;
    }

    public void setCustomer(String customer) {
        this.customer = customer;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Order order = (Order) o;
        return Objects.equals(id, order.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Order{" +
                "id='" + id + '\'' +
                ", customer='" + customer + '\'' +
                ", items=" + items +
                ", total=" + total +
                ", createdAt=" + createdAt +
                '}';
    }
}
