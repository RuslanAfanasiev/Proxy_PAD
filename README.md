# Web Proxy - Realizarea Transparenței în Distribuire

## Descriere

Implementare completă a unui **Web Proxy** pentru distribuirea transparentă a datelor conform cerințelor de laborator. Sistemul include:

- **Data Warehouse (DW)** - Servicii de stocare a datelor (movie_api)
- **Smart Proxy** - Reverse proxy cu caching și load balancing
- **Redis** - Sistem de caching și stocare conexiuni

## Arhitectură

```
Client → Smart Proxy → Load Balancer → Data Warehouse Nodes
             ↓
         Redis Cache
```

### Componente

#### 1. Data Warehouse (movie_api)
- **Port**: 9001 (node1), 9002 (node2)
- **Tehnologii**: Spring Boot, PostgreSQL, JPA
- **Funcționalități**:
  - CRUD operations pentru entitatea Movie
  - Suport pentru JSON/XML
  - Procesare concurentă (thread-per-request prin Spring Boot)

#### 2. Smart Proxy
- **Port**: 8080
- **Tehnologii**: Spring Boot, Redis
- **Funcționalități**:
  - **Smart-proxy**: Menținerea conexiunilor spre clienți
  - **Caching**: Memorare temporară răspunsuri (TTL: 5 minute)
  - **Load Balancing**: Algoritm Round-Robin pentru distribuirea cererilor
  - Suport pentru JSON/XML
  - Cache invalidation pentru operații de modificare (POST, PUT, DELETE)

## Cerințe Preliminare

1. **Java 21**
2. **Maven 3.x**
3. **PostgreSQL** (rulând pe port 5432)
4. **Redis** (rulând pe port 6379)

## Setup

### 1. Pornire PostgreSQL

```bash
# Creați baza de date
createdb proxydb

# Sau folosind psql
psql -c "CREATE DATABASE proxydb;"
```

### 2. Pornire Redis

```bash
# Linux/Mac
redis-server

# Sau folosind Docker
docker run -d -p 6379:6379 redis:latest
```

### 3. Build și Run

#### Pornire Data Warehouse Node 1 (port 9001)

```bash
cd movie_api
./mvnw clean install
./mvnw spring-boot:run
```

#### Pornire Data Warehouse Node 2 (port 9002)

```bash
# În alt terminal
cd movie_api
./mvnw spring-boot:run -Dspring-boot.run.profiles=node2
```

#### Pornire Smart Proxy (port 8080)

```bash
# În alt terminal
cd proxy
./mvnw clean install
./mvnw spring-boot:run
```

## Utilizare

### Endpoints (prin Proxy pe port 8080)

#### GET - Obținere toate filmele (cu caching)
```bash
# JSON (default)
curl http://localhost:8080/api/movies

# XML
curl -H "Accept: application/xml" http://localhost:8080/api/movies
```

#### GET - Obținere film după ID (cu caching)
```bash
curl http://localhost:8080/api/movies/1
```

#### POST - Creare film nou (invalidează cache-ul)
```bash
curl -X POST http://localhost:8080/api/movies \
  -H "Content-Type: application/json" \
  -d '{"title":"Inception","rating":8.8}'
```

#### PUT - Actualizare film (invalidează cache-ul)
```bash
curl -X PUT http://localhost:8080/api/movies/1 \
  -H "Content-Type: application/json" \
  -d '{"title":"Inception Updated","rating":9.0}'
```

#### DELETE - Ștergere film (invalidează cache-ul)
```bash
curl -X DELETE http://localhost:8080/api/movies/1
```

### Verificare Load Balancing

Faceți mai multe cereri și verificați header-ul `X-DW-Node` pentru a vedea Round-Robin în acțiune:

```bash
# Prima cerere → http://localhost:9001
curl -v http://localhost:8080/api/movies

# A doua cerere → http://localhost:9002
curl -v http://localhost:8080/api/movies

# A treia cerere → http://localhost:9001
curl -v http://localhost:8080/api/movies
```

### Verificare Caching

```bash
# Prima cerere - Cache MISS (header: X-Cache: MISS)
curl -v http://localhost:8080/api/movies

# A doua cerere (în 5 minute) - Cache HIT (header: X-Cache: HIT)
curl -v http://localhost:8080/api/movies
```

## Caracteristici Implementate

### ✅ Etapa 1 - Data Warehouse

- [x] Comunicare concurentă prin HTTP (thread-per-request)
- [x] Suport pentru GET, POST, PUT, DELETE
- [x] Răspunsuri în format JSON/XML
- [x] Thread-safe operations (Spring Boot default)
- [x] Controller pentru maparea cererilor HTTP

### ✅ Etapa 2 - Smart Proxy

- [x] **Smart-proxy**: Menținerea conexiunilor (RestTemplate persistent)
- [x] **Caching**: 
  - Redis pentru stocare
  - TTL configurabil (default 5 minute)
  - Cache invalidation automat pentru POST/PUT/DELETE
- [x] **Load Balancing**:
  - Algoritm Round-Robin
  - Thread-safe cu AtomicInteger
  - Suport pentru multiple noduri DW

## Configurare

### Proxy (application.properties)

```properties
server.port=8080

# Redis
spring.data.redis.host=localhost
spring.data.redis.port=6379

# Data Warehouse nodes
proxy.datawarehouse.nodes=http://localhost:9001,http://localhost:9002

# Cache TTL (5 minute)
proxy.cache.ttl=300000
```

### Data Warehouse (application.properties)

```properties
server.port=9001
spring.datasource.url=jdbc:postgresql://localhost:5432/proxydb
spring.datasource.username=admin
spring.datasource.password=Cascaval24#
```

## Testare

### Test Complet

```bash
# 1. Creează filme
curl -X POST http://localhost:8080/api/movies \
  -H "Content-Type: application/json" \
  -d '{"title":"The Matrix","rating":8.7}'

curl -X POST http://localhost:8080/api/movies \
  -H "Content-Type: application/json" \
  -d '{"title":"Interstellar","rating":8.6}'

# 2. Obține toate filmele (Cache MISS)
curl -v http://localhost:8080/api/movies

# 3. Obține din nou (Cache HIT)
curl -v http://localhost:8080/api/movies

# 4. Actualizează un film (invalidează cache)
curl -X PUT http://localhost:8080/api/movies/1 \
  -H "Content-Type: application/json" \
  -d '{"title":"The Matrix Reloaded","rating":8.9}'

# 5. Verifică că cache-ul a fost invalidat
curl -v http://localhost:8080/api/movies
```

## Logs

Verificați logs pentru:
- **Load Balancing**: `Selected node: http://localhost:9001`
- **Caching**: `Cache HIT/MISS for key: GET:/api/movies`
- **Request Forwarding**: `Forwarding GET request to: http://localhost:9001/api/movies`

## Tehnologii

- **Spring Boot 3.5.7**
- **Java 21**
- **PostgreSQL** - Baza de date
- **Redis** - Caching
- **Lombok** - Reducere boilerplate
- **Jackson** - JSON/XML serialization

## Diagrame

### Flow Diagram

```
1. Client → GET /api/movies
2. Proxy → Check Redis Cache
3. Cache HIT? → Return cached response
4. Cache MISS? → LoadBalancer.getNextNode()
5. Proxy → Forward to DW (http://localhost:9001)
6. DW → Process & Return
7. Proxy → Cache response in Redis
8. Proxy → Return to Client
```

### Cache Invalidation

```
POST/PUT/DELETE → Proxy → Forward to DW → Success → Invalidate Cache
```

## Autori

Implementat conform specificațiilor laboratorului pentru:
- Web Proxy: Realizarea transparenței în distribuire
- Studiul protocolului HTTP în contextul distribuirii datelor
