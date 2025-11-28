package com.rairai.consumer_kafka_1.service;

import com.rairai.consumer_kafka_1.model.Order;
import com.rairai.consumer_kafka_1.model.OrderItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Serviço simples de gerenciamento de estoque para o consumidor de estoque.
 *
 * - Mantém um estoque em memória (produtoId -> quantidade disponível).
 * - Permite reservar produtos para um pedido de forma atômica (síncrona).
 * - Métodos utilitários para consulta e reposição de estoque (úteis para testes).
 *
 * Observação: este é um exemplo educacional. Em produção, o controle de estoque
 * precisa ser persistente, tolerante a falhas e coordenado (transações, locks distribuídos, etc).
 */
@Service
public class StockService {

    private static final Logger logger = LoggerFactory.getLogger(StockService.class);

    private final Map<String, Integer> stock = new ConcurrentHashMap<>();

    public StockService() {
        stock.put("p1", 10);
        stock.put("p2", 5);
        stock.put("p3", 0);
    }


    public synchronized boolean reserve(Order order) {
        if (order == null) {
            logger.warn("Pedido nulo recebido para reserva.");
            return false;
        }

        if (order.getItems() == null || order.getItems().isEmpty()) {
            logger.warn("Pedido {} não contém itens para reserva.", order.getId());
            return false;
        }

        logger.info("Tentando reservar estoque para pedido {}", order.getId());

        for (OrderItem item : order.getItems()) {
            String productId = item.getProductId();
            int required = item.getQuantity() != null ? item.getQuantity() : 0;
            int available = stock.getOrDefault(productId, 0);
            if (available < required) {
                logger.warn("Sem estoque suficiente para produto {}: necessário={}, disponível={}",
                        productId, required, available);
                return false;
            }
        }

        for (OrderItem item : order.getItems()) {
            String productId = item.getProductId();
            int required = item.getQuantity() != null ? item.getQuantity() : 0;
            int available = stock.getOrDefault(productId, 0);
            int newQty = available - required;
            stock.put(productId, newQty);
            logger.info("Reservado {} unidades do produto {} (novo estoque={})",
                    required, productId, newQty);
        }

        logger.info("Reserva concluída para pedido {}", order.getId());
        return true;
    }


    public int getStock(String productId) {
        return stock.getOrDefault(productId, 0);
    }


    public void addStock(String productId, int amount) {
        stock.merge(productId, amount, Integer::sum);
        logger.info("Estoque do produto {} ajustado em {} (novo={})",
                productId, amount, stock.getOrDefault(productId, 0));
    }


    public Map<String, Integer> snapshot() {
        return Collections.unmodifiableMap(new ConcurrentHashMap<>(stock));
    }
}
