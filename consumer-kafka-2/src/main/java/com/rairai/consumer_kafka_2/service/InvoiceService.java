package com.rairai.consumer_kafka_2.service;

import com.rairai.consumer_kafka_2.model.Order;
import com.rairai.consumer_kafka_2.model.OrderItem;
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

    /**
     * Recupera a invoice gerada para um dado pedido.
     *
     * @param orderId id do pedido
     * @return Invoice ou null se não existir
     */
    public Invoice getInvoiceByOrderId(String orderId) {
        return invoicesByOrderId.get(orderId);
    }

    /**
     * Lista todas as invoices geradas.
     *
     * @return lista imutável de invoices
     */
    public List<Invoice> listInvoices() {
        return Collections.unmodifiableList(new ArrayList<>(invoicesByOrderId.values()));
    }

    // Constrói um objeto Invoice a partir dos dados do pedido
    private Invoice buildInvoiceFromOrder(Order order) {
        Invoice invoice = new Invoice();
        invoice.setInvoiceId(UUID.randomUUID().toString());
        invoice.setOrderId(order.getId());
        invoice.setCustomer(order.getCustomer());
        invoice.setCreatedAt(Instant.now());

        // Copia itens e calcula total (se order.total não estiver presente)
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

        // Observação fiscal / campos simulados
        invoice.setTax( round(invoice.getTotal() * 0.1) ); // exemplo: 10% imposto
        invoice.setTotalWithTax( round(invoice.getTotal() + invoice.getTax()) );

        return invoice;
    }

    // Simula renderização de PDF gerando um array de bytes com conteúdo placeholder
    private byte[] renderPdf(Invoice invoice) {
        // Em um caso real, aqui você chamaria um gerador de PDF e retornaria os bytes.
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

    // ---------- Classes internas representando a Invoice (modelo simplificado) ----------

    public enum InvoiceStatus {
        PENDING,
        ISSUED,
        CANCELLED
    }

    /**
     * Representa uma invoice gerada a partir de um pedido.
     */
    public static class Invoice {
        private String invoiceId;
        private String orderId;
        private String customer;
        private List<InvoiceItem> items;
        private double total;
        private double tax;
        private double totalWithTax;
        private Instant createdAt;
        private Instant issuedAt;
        private InvoiceStatus status = InvoiceStatus.PENDING;
        private byte[] pdfContent;

        public String getInvoiceId() {
            return invoiceId;
        }

        public void setInvoiceId(String invoiceId) {
            this.invoiceId = invoiceId;
        }

        public String getOrderId() {
            return orderId;
        }

        public void setOrderId(String orderId) {
            this.orderId = orderId;
        }

        public String getCustomer() {
            return customer;
        }

        public void setCustomer(String customer) {
            this.customer = customer;
        }

        public List<InvoiceItem> getItems() {
            return items;
        }

        public void setItems(List<InvoiceItem> items) {
            this.items = items;
        }

        public double getTotal() {
            return total;
        }

        public void setTotal(double total) {
            this.total = total;
        }

        public double getTax() {
            return tax;
        }

        public void setTax(double tax) {
            this.tax = tax;
        }

        public double getTotalWithTax() {
            return totalWithTax;
        }

        public void setTotalWithTax(double totalWithTax) {
            this.totalWithTax = totalWithTax;
        }

        public Instant getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(Instant createdAt) {
            this.createdAt = createdAt;
        }

        public Instant getIssuedAt() {
            return issuedAt;
        }

        public void setIssuedAt(Instant issuedAt) {
            this.issuedAt = issuedAt;
        }

        public InvoiceStatus getStatus() {
            return status;
        }

        public void setStatus(InvoiceStatus status) {
            this.status = status;
        }

        public byte[] getPdfContent() {
            return pdfContent;
        }

        public void setPdfContent(byte[] pdfContent) {
            this.pdfContent = pdfContent;
        }
    }

    /**
     * Representa uma linha de item na Invoice.
     */
    public static class InvoiceItem {
        private String productId;
        private int quantity;
        private double unitPrice;
        private double lineTotal;

        public String getProductId() {
            return productId;
        }

        public void setProductId(String productId) {
            this.productId = productId;
        }

        public int getQuantity() {
            return quantity;
        }

        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }

        public double getUnitPrice() {
            return unitPrice;
        }

        public void setUnitPrice(double unitPrice) {
            this.unitPrice = unitPrice;
        }

        public double getLineTotal() {
            return lineTotal;
        }

        public void setLineTotal(double lineTotal) {
            this.lineTotal = lineTotal;
        }
    }
}
