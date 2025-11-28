# Módulo 4 — Sistema de Produção/Consumo com Kafka

Este repositório contém um pequeno exemplo de integração com Apache Kafka usando três serviços:
- `producer-kafka` — API Spring Boot que publica pedidos no tópico Kafka.
- `consumer-kafka-1` — Consumidor responsável por controlar estoque (Stock).
- `consumer-kafka-2` — Consumidor responsável por gerar notas fiscais (Invoice).

O ambiente inteiro (Zookeeper + 3 brokers Kafka + aplicações) está orquestrado por `docker-compose.yaml` na raiz da pasta `modulo4`.

---

## Estrutura do projeto
- `modulo4/docker-compose.yaml` — composição Docker para Zookeeper, 3 brokers, um job de criação de tópico (observação abaixo), e containers das aplicações.
- `modulo4/producer-kafka` — aplicação producer (Spring Boot).
- `modulo4/consumer-kafka-1` — consumidor 1 (Stock).
- `modulo4/consumer-kafka-2` — consumidor 2 (Invoice).

Os serviços Spring Boot estão configurados para usar o tópico `pedidos` (veja `producer-kafka/src/main/java/.../config/KafkaConfig.java` que cria o tópico `pedidos`).

Observação: o serviço `create-topics` em `docker-compose.yaml` cria um tópico chamado `my-topic`. Isso parece ser um artefato — o código das aplicações usa `pedidos`. Recomendo remover/ajustar o job `create-topics` ou alterá-lo para criar `pedidos` para manter consistência.

---

## Pré-requisitos
- Docker & Docker Compose instalados (suporta Compose v2).
- Opcional: JDK + Maven caso queira construir as imagens localmente sem usar o `docker-compose` que já faz o build.

---

## Executando com Docker Compose (rápido)
No diretório `modulo4`, execute:

```bash
docker compose up --build
```

Isso irá:
1. Subir Zookeeper.
2. Subir 3 brokers Kafka (broker1, broker2, broker3).
3. Construir e subir as imagens das aplicações `producer`, `consumer-kafka-1` e `consumer-kafka-2`.
4. O `producer` cria o tópico `pedidos` à inicialização (via `KafkaConfig`).

Ports mapeadas principais (host -> container):
- Zookeeper: `2181:2181`
- Broker1: `9092:9092` (advertised `broker1:9092`)
- Broker2: `9093:9092` (advertised `broker2:9092`)
- Broker3: `9094:9092` (advertised `broker3:9092`)
- Producer HTTP: `8080:8080`
- Consumer 1 HTTP (se aplicável): `8081:8080`
- Consumer 2 HTTP (se aplicável): `8082:8080`

---

## Endpoints da API (Producer)
O `producer-kafka` expõe um endpoint REST para criar pedidos:

- POST `/api/pedidos` — cria e publica um `Order` no tópico `pedidos`.

Exemplo de requisição:

```bash
curl -X POST http://localhost:8080/api/pedidos \
  -H "Content-Type: application/json" \
  -d '{
    "customer": "João Silva",
    "items": [
      {"productId":"p1","quantity":2,"price":10.0},
      {"productId":"p2","quantity":1,"price":20.0}
    ],
    "total": 40.0
  }'
```

O `Order` tem o formato definido em `producer-kafka/src/main/java/com/rairai/producer_kafka/model/Order.java`.
O `ProducerService` usa a `order.id` como chave da mensagem ao enviar para Kafka.

---

## Consumidores
- `consumer-kafka-1` (StockConsumer)
  - Escuta o tópico `pedidos`.
  - Usa `StockService.reserve(order)` para reservar itens de um estoque em memória.
  - Configurações importantes: `spring.kafka.consumer.group-id` (padrão `consumer-group-1`), e `spring.listener.concurrency` (threads concorrentes).

- `consumer-kafka-2` (InvoiceConsumer)
  - Escuta o tópico `pedidos`.
  - Gera uma "Invoice" (nota fiscal simulada) via `InvoiceService.generateInvoice(order)` e mantém em memória.
  - Configurações importantes: `spring.kafka.consumer.group-id` (padrão `consumer-group-2`).

Ambos os consumidores usam `JsonDeserializer` para desserializar o payload para a classe `Order`.

---

## Variáveis de ambiente configuráveis (via `docker-compose` ou no host)
Principais variáveis usadas nos `docker-compose` service definitions:

- Para `producer`:
  - `KAFKA_BOOTSTRAP_SERVERS` — lista de brokers (ex: `broker1:9092,broker2:9092,broker3:9092`)
  - `TOPIC_NAME` — nome do tópico (o código atualmente cria `pedidos` por configuração interna)

- Para `consumer-kafka-*`:
  - `KAFKA_BOOTSTRAP_SERVERS`
  - `TOPIC_NAME`
  - `CONSUMER_GROUP` — grupo do consumidor (ex: `consumer-group-1`, `consumer-group-2`)

Observação: As aplicações Spring também têm propriedades `spring.kafka.*` dentro dos `application.properties`/`application.yml` (verificar código) que podem ser sobrescritas por variáveis de ambiente.

---

## Como testar / validar
1. Suba a stack com `docker compose up --build`.
2. Verifique logs:

```bash
docker compose logs -f producer
docker compose logs -f consumer-kafka-1
docker compose logs -f consumer-kafka-2
```
3. Envie um pedido para o producer (exemplo `curl` acima).
4. Observe nos logs dos consumidores as mensagens de processamento (reserva de estoque / geração de invoice).

---

## Build manual (sem docker compose auto-build)
Caso queira buildar localmente as imagens:

- Producer:
```bash
cd modulo4/producer-kafka
./mvnw package -DskipTests
docker build -t producer-kafka:local .
```

- Consumers: repetir para `consumer-kafka-1` e `consumer-kafka-2`.
