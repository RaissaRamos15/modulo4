package com.rairai.consumer_kafka_2.service;

import com.rairai.consumer_kafka_2.model.Invoice;
import com.rairai.consumer_kafka_2.model.Order;
import com.rairai.consumer_kafka_2.model.OrderItem;
import com.rairai.consumer_kafka_2.model.InvoiceItem;
import com.rairai.consumer_kafka_2.model.InvoiceStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


@Service
public class InvoiceService {

    private static final Logger logger = LoggerFactory.getLogger(InvoiceService.class);

    private final Map<String, Invoice> invoicesByOrderId = new ConcurrentHashMap<>();


    public Invoice generateInvoice(Order order) {
        if (order == null) {
            logger.warn("Tentativa de gerar NF para pedido nulo.");
            return null;
        }

        String orderId = order.getId();
        Invoice existing = invoicesByOrderId.get(orderId);
        if (existing != null) {
            logger.info("Invoice já existente encontrada para orderId={} invoiceId={}", orderId, existing.getInvoiceId());
            return existing;
        }

        logger.info("Gerando invoice para orderId={} customer={}", orderId, order.getCustomer());

        Invoice invoice = buildInvoiceFromOrder(order);

        byte[] pdfBytes = renderPdf(invoice);
        invoice.setPdfContent(pdfBytes);

        invoice.setStatus(InvoiceStatus.ISSUED);
        invoice.setIssuedAt(Instant.now());

        invoicesByOrderId.put(orderId, invoice);

        logger.info("Invoice gerada: invoiceId={} orderId={} total={}", invoice.getInvoiceId(), orderId, invoice.getTotal());

        return invoice;
    }

   
    public Invoice getInvoiceByOrderId(String orderId) {
        return invoicesByOrderId.get(orderId);
    }

   
    public List<Invoice> listInvoices() {
        return Collections.unmodifiableList(new ArrayList<>(invoicesByOrderId.values()));
    }

    private Invoice buildInvoiceFromOrder(Order order) {
        Invoice invoice = new Invoice();
        invoice.setInvoiceId(UUID.randomUUID().toString());
        invoice.setOrderId(order.getId());
        invoice.setCustomer(order.getCustomer());
        invoice.setCreatedAt(Instant.now());

        List<InvoiceItem> items = new ArrayList<>();
        double calculatedTotal = 0.0;
        if (order.getItems() != null) {
            for (OrderItem oi : order.getItems()) {
                InvoiceItem ii = new InvoiceItem();
                ii.setProductId(oi.getProductId());
                ii.setQuantity(oi.getQuantity() != null ? oi.getQuantity() : 0);
                ii.setUnitPrice(oi.getPrice() != null ? oi.getPrice() : 0.0);
                ii.setLineTotal(ii.getQuantity() * ii.getUnitPrice());
                calculatedTotal += ii.getLineTotal();
                items.add(ii);
            }
        }

        invoice.setItems(items);
        invoice.setTotal(order.getTotal() != null ? order.getTotal() : calculatedTotal);

        invoice.setTax( round(invoice.getTotal() * 0.1) ); // exemplo: 10% imposto
        invoice.setTotalWithTax( round(invoice.getTotal() + invoice.getTax()) );

        return invoice;
    }

    private byte[] renderPdf(Invoice invoice) {
        StringBuilder sb = new StringBuilder();
        sb.append("NOTA FISCAL SIMULADA\n");
        sb.append("InvoiceId: ").append(invoice.getInvoiceId()).append("\n");
        sb.append("OrderId: ").append(invoice.getOrderId()).append("\n");
        sb.append("Customer: ").append(invoice.getCustomer()).append("\n");
        sb.append("Total: ").append(invoice.getTotal()).append("\n");
        sb.append("Tax: ").append(invoice.getTax()).append("\n");
        sb.append("TotalWithTax: ").append(invoice.getTotalWithTax()).append("\n");
        sb.append("IssuedAt: ").append(invoice.getIssuedAt()).append("\n");
        sb.append("Itens:\n");
        for (InvoiceItem it : invoice.getItems()) {
            sb.append(" - ").append(it.getProductId())
              .append(" x").append(it.getQuantity())
              .append(" @").append(it.getUnitPrice())
              .append(" = ").append(it.getLineTotal()).append("\n");
        }
        return sb.toString().getBytes();
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
    
}
