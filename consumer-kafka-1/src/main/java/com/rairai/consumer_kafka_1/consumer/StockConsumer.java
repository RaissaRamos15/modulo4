package com.rairai.consumer_kafka_1.consumer;

import com.rairai.consumer_kafka_1.model.Order;
import com.rairai.consumer_kafka_1.service.StockService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;


@Component
public class StockConsumer {

    private static final Logger logger = LoggerFactory.getLogger(StockConsumer.class);

    private final StockService stockService;

    public StockConsumer(StockService stockService) {
        this.stockService = stockService;
    }


    @KafkaListener(topics = "pedidos", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(ConsumerRecord<String, Order> record) {
        try {
            String key = record.key();
            Order order = record.value();

            if (order == null) {
                logger.warn("Recebido registro com valor nulo. offset={}, partition={}, key={}",
                        record.offset(), record.partition(), key);
                return;
            }

            logger.info("Recebido pedido para reserva. key={}, orderId={}, partition={}, offset={}",
                    key, order.getId(), record.partition(), record.offset());

            boolean reserved = stockService.reserve(order);
            if (reserved) {
                logger.info("Estoque reservado para pedido {} (key={})", order.getId(), key);
            } else {
                logger.warn("Falha ao reservar estoque para pedido {} (key={}). Verifique disponibilidade.", order.getId(), key);
            }
        } catch (Exception ex) {
            logger.error("Erro ao processar mensagem de estoque. partition={}, offset={}. erro={}",
                    record.partition(), record.offset(), ex.getMessage(), ex);
        }
    }
}
