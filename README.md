# Orders API — Serverless REST API with AWS Lambda and API Gateway

AREM — Microservicios y Arquitecturas sin Servidor · Escuela Colombiana de Ingeniería Julio Garavito

A small **Order Management API** written in plain Java 17 + Maven, deployed to **AWS Lambda** and exposed
through **Amazon API Gateway** using two different integration styles:

* **Architecture A** — API Gateway *non-proxy* (custom) integration with mapping templates.
* **Architecture B** — API Gateway *Lambda proxy* integration with a thin Java HTTP adapter.

---

## 1. API design (contract first)

### Resource model

An order:

```json
{
  "id": "ORD-001",
  "customerId": "CUST-42",
  "product": "MacBook Air",
  "quantity": 1
}
```

Every error, in every operation, uses the same JSON representation:

```json
{
  "error": "ORDER_NOT_FOUND",
  "message": "Order ORD-999 does not exist"
}
```

* `error` — stable, machine-readable code (clients can `switch` on it).
* `message` — human-readable explanation (may change, never parsed by clients).

| Error code           | HTTP | When                                                          |
|----------------------|------|---------------------------------------------------------------|
| `INVALID_ORDER`      | 400  | Body is missing/malformed or violates a validation rule       |
| `MISSING_CUSTOMER_ID`| 400  | `GET /orders` called without the `customerId` query parameter |
| `ORDER_NOT_FOUND`    | 404  | No order exists with the given id                             |
| `ROUTE_NOT_FOUND`    | 404  | Unknown path/method combination (proxy adapter only)          |
| `INTERNAL_ERROR`     | 500  | Any unexpected failure; no internal details are leaked        |

### Operations

#### Create an order — `POST /orders`

| Item              | Value |
|-------------------|-------|
| Path params       | — |
| Query params      | — |
| Request body      | `{"customerId": "CUST-42", "product": "MacBook Air", "quantity": 1}` |
| Success           | `201 Created` + created order (with the server-generated `id`) + `Location: /orders/{id}` header |
| Errors            | `400 INVALID_ORDER` (no body, malformed JSON, blank `customerId`/`product`, `quantity < 1`), `500 INTERNAL_ERROR` |

#### Retrieve an order — `GET /orders/{id}`

| Item              | Value |
|-------------------|-------|
| Path params       | `id` — order identifier, e.g. `ORD-42` |
| Query params      | — |
| Request body      | — |
| Success           | `200 OK` + the order |
| Errors            | `404 ORDER_NOT_FOUND`, `500 INTERNAL_ERROR` |

#### Find the orders of a customer — `GET /orders?customerId={customerId}`

| Item              | Value |
|-------------------|-------|
| Path params       | — |
| Query params      | `customerId` (required) |
| Request body      | — |
| Success           | `200 OK` + JSON array of orders (an **empty array** when the customer has no orders) |
| Errors            | `400 MISSING_CUSTOMER_ID`, `500 INTERNAL_ERROR` |

#### Delete an order — `DELETE /orders/{id}` (final challenge)

| Item              | Value |
|-------------------|-------|
| Path params       | `id` |
| Success           | `204 No Content` (empty body) |
| Errors            | `404 ORDER_NOT_FOUND`, `500 INTERNAL_ERROR` |

### Design questions

1. **What are the resources in your API?**
   One collection resource, `/orders`, and its item resource, `/orders/{id}`. Customers are not exposed as a
   resource in this version; they appear only as an attribute (`customerId`) used to filter the collection.

2. **Why `/orders/{id}` to retrieve a particular order?**
   The id *identifies* a single resource, so it belongs in the path: `/orders/ORD-42` is the URI of that order.
   It is stable, cacheable, bookmarkable, and the same URI is reused for other methods on the same resource
   (`DELETE /orders/{id}`, a future `PUT`). It is also the natural value for the `Location` header after a `POST`.

3. **Why is `customerId` a query parameter?**
   Searching by customer does not identify one resource; it *filters* the `/orders` collection. Query parameters
   are the conventional way to express optional filters/criteria over a collection and compose well
   (`?customerId=CUST-42&product=...` later) without inventing new resource hierarchies. A nested resource
   (`/customers/{id}/orders`) would also be valid, but it implies that customers are a first-class resource,
   which they are not in this API.

4. **What should `POST /orders` return on success?**
   `201 Created`, the representation of the created order including its server-generated `id`, and a
   `Location: /orders/{id}` header pointing to the new resource.

5. **What should the API return when an order does not exist?**
   `404 Not Found` with the error body `{"error":"ORDER_NOT_FOUND","message":"Order ORD-999 does not exist"}`.
   Not `200` with `null`, and not `500` — the request was valid, the resource simply is not there.

6. **What information should an error response contain?**
   A stable machine-readable code (`error`) and a human-readable description (`message`). The HTTP status code
   carries the category. It must **not** contain stack traces, class names or other internal details.

---

## 2. Part I — Application in plain Java

```
src/main/java/edu/eci/arem/orders/
├── domain/        Order, CreateOrderRequest, OrderNotFoundException, InvalidOrderException
└── application/   OrderService, OrderRepository (port), InMemoryOrderRepository
```

`OrderService` exposes:

```java
Order createOrder(CreateOrderRequest request);   // throws InvalidOrderException
Order getOrder(String id);                       // throws OrderNotFoundException
List<Order> getOrdersByCustomer(String customerId);
```

* Validation (business rules: required `customerId`/`product`, `quantity >= 1`) lives in `OrderService`.
* Failures are expressed with **domain exceptions**, never with status codes.
* Ids are generated by the repository (`ORD-43`, `ORD-44`, ...). Sample data: `ORD-001`, `ORD-002` (CUST-42)
  and `ORD-42` (CUST-7).

### Checkpoint 1

1. **Can the functionality be tested without AWS?** Yes. `OrderServiceTest` runs with plain JUnit 5 in
   milliseconds; no AWS account, SDK or emulator is involved.
2. **Does it require a web server?** No. There is no Tomcat/Jetty/Netty; `OrderService` is invoked as a
   normal Java object.
3. **Does it require Spring Boot?** No. Dependencies at this point: only JUnit (test scope).
4. **What does the generated JAR contain?** Only our compiled classes (`domain` and `application`
   packages) plus Maven metadata (`META-INF/MANIFEST.MF`, `pom.xml`, `pom.properties`) — ~17 KB, no
   third-party libraries.
5. **Which parts currently know about HTTP?** None. Not a single class imports anything HTTP related.
