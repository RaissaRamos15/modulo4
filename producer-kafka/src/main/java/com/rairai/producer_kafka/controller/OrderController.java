package com.rairai.producer_kafka.controller;

import com.rairai.producer_kafka.model.Order;
import com.rairai.producer_kafka.services.ProducerService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/pedidos")
public class OrderController {

    private final ProducerService producerService;

    public OrderController(ProducerService producerService) {
        this.producerService = producerService;
    }

    @PostMapping
    public ResponseEntity<Order> criarPedido(@Valid @RequestBody Order order) {
        producerService.sendOrder(order);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }
}
