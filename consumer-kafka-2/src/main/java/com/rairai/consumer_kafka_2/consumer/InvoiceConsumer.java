package com.rairai.consumer_kafka_2.consumer;

import com.rairai.consumer_kafka_2.model.Order;
import com.rairai.consumer_kafka_2.service.InvoiceService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;


@Component
public class InvoiceConsumer {

    private static final Logger logger = LoggerFactory.getLogger(InvoiceConsumer.class);

    private final InvoiceService invoiceService;

    public InvoiceConsumer(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @KafkaListener(topics = "pedidos", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(ConsumerRecord<String, Order> record) {
        try {
            String key = record.key();
            Order order = record.value();

            if (order == null) {
                logger.warn("Recebido registro com valor nulo. partition={}, offset={}, key={}",
                        record.partition(), record.offset(), key);
                return;
            }

            logger.info("Recebendo pedido para emissão de NF. key={}, orderId={}, partition={}, offset={}",
                    key, order.getId(), record.partition(), record.offset());

            InvoiceService.Invoice invoice = invoiceService.generateInvoice(order);
            if (invoice != null) {
                logger.info("NF emitida com sucesso. orderId={}, invoiceId={}, issuedAt={}",
                        order.getId(), invoice.getInvoiceId(), invoice.getIssuedAt());
            } else {
                logger.warn("A geração da NF retornou null para orderId={}. Verifique o serviço.", order.getId());
            }

        } catch (Exception ex) {
            logger.error("Erro ao processar mensagem de NF. partition={}, offset={}, error={}",
                    record.partition(), record.offset(), ex.getMessage(), ex);
        }
    }
}
