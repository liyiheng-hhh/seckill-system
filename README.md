# seckill-system
商品库存与秒杀系统

### 系统架构草图
系统采用微服务架构设计，将核心功能拆分为四个独立服务：用户服务（负责用户管理和认证）、商品服务（管理商品信息）、订单服务（处理订单创建和支付逻辑）、库存服务（管理库存扣减和秒杀高并发场景）。这些服务通过RESTful API或消息队列（如Kafka）进行通信，前端可以通过API Gateway（如Spring Cloud Gateway）统一访问。数据库使用MySQL，缓存使用Redis以支持秒杀的高并发。整个系统可部署在Docker容器中，使用Kubernetes进行编排。

### API接口
- 用户服务 (User Service)
  - POST /users/register：用户注册
    - 请求体：{ "username": "string", "password": "string", "email": "string" }
    - 响应：201 Created，{ "userId": "long", "message": "注册成功" }

  - POST /users/login：用户登录
    - 请求体：{ "username": "string", "password": "string" }
    - 响应：200 OK，{ "token": "JWT string", "userId": "long" }

  - GET /users/{userId}：获取用户信息（需认证）
    - 响应：200 OK，{ "username": "string", "email": "string" }


- 商品服务 (Product Service)
  - POST /products：添加商品（管理员权限）
    - 请求体：{ "name": "string", "description": "string", "price": "double" }
    - 响应：201 Created，{ "productId": "long" }

  - GET /products/{productId}：获取商品详情
    - 响应：200 OK，{ "name": "string", "description": "string", "price": "double", "stock": "int" (从库存服务查询) }

  - GET /products：查询商品列表（支持分页）
    - 查询参数：page=1&size=10
    - 响应：200 OK，[ { "productId": "long", "name": "string", ... } ]


- 订单服务 (Order Service)
  - POST /orders：创建订单（需认证）
    - 请求体：{ "userId": "long", "productId": "long", "quantity": "int" }
    - 响应：201 Created，{ "orderId": "long", "status": "PENDING" }（调用库存服务扣减库存）

  - GET /orders/{orderId}：获取订单详情
    - 响应：200 OK，{ "orderId": "long", "productId": "long", "quantity": "int", "status": "string" }

  - PUT /orders/{orderId}/pay：支付订单
    - 请求体：{ "paymentMethod": "string" }
    - 响应：200 OK，{ "status": "PAID" }


- 库存服务 (Inventory Service)
  - GET /inventory/{productId}：查询库存
    - 响应：200 OK，{ "productId": "long", "stock": "int" }

  - PUT /inventory/{productId}/deduct：扣减库存（支持秒杀，使用乐观锁或Redis）
    - 请求体：{ "quantity": "int" }
    - 响应：200 OK，{ "success": "boolean", "remainingStock": "int" }（如果库存不足，返回false）

  - POST /seckill/{productId}：秒杀接口（高并发优化）
    - 请求体：{ "userId": "long", "quantity": 1 }（限购1件）
    - 响应：200 OK，{ "success": "boolean", "orderId": "long" }（集成订单服务）
   

### 数据库ER图

### 技术选型
编程语言：Java（成熟、社区支持强，适合企业级系统；备选GoLang for 库存服务以支持高并发）。

框架：Spring Boot（快速开发微服务，支持RESTful API、依赖注入）；MyBatis（ORM框架，灵活SQL管理，优于JPA在复杂查询）。

数据库：MySQL（关系型，ACID支持，适合交易场景）；Redis（缓存和分布式锁，用于秒杀库存扣减，防止数据库瓶颈）。

中间件：Kafka（消息队列，用于服务间异步通信，如订单创建后通知库存）；Spring Cloud（微服务治理，包括Gateway、Eureka注册中心）；Docker & Kubernetes（容器化和部署）。

其他：JWT（认证）、Lombok（简化代码）、Swagger（API文档）。选型理由：Spring生态完整，易扩展；Redis+Kafka处理秒杀高并发（QPS>1000）；MySQL确保数据一致性。

---

## 快速启动

### 环境要求

- JDK 17+
- Maven 3.8+
- MySQL 8.0+

### 1. 初始化数据库

```bash
mysql -u root -p < scripts/init.sql
```

或手动执行 `scripts/init.sql` 中的 SQL。默认数据库名 `seckill`，请根据 `application.yml` 修改连接信息（用户名/密码）。

### 2. 编译项目

```bash
mvn clean install
```

### 3. 启动服务

各服务独立运行，端口如下：

| 服务 | 端口 | 启动命令 |
|------|------|----------|
| 用户服务 | 8081 | `mvn -pl seckill-user-service spring-boot:run` |
| 商品服务 | 8082 | `mvn -pl seckill-product-service spring-boot:run` |
| 订单服务 | 8083 | `mvn -pl seckill-order-service spring-boot:run` |
| 库存服务 | 8084 | `mvn -pl seckill-inventory-service spring-boot:run` |

### 4. API 文档

启动用户服务后访问：http://localhost:8081/swagger-ui.html

---

## 用户注册登录 API 示例

### 注册

```bash
curl -X POST http://localhost:8081/users/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"123456","email":"test@example.com"}'
```

响应示例：`{"userId":1,"message":"注册成功"}`

### 登录

```bash
curl -X POST http://localhost:8081/users/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"123456"}'
```

响应示例：`{"token":"eyJhbGciOiJIUzI1NiJ9...","userId":1}`

### 获取用户信息（需认证）

```bash
curl -X GET http://localhost:8081/users/1 \
  -H "Authorization: Bearer <登录返回的token>"
```

响应示例：`{"username":"testuser","email":"test@example.com"}`
