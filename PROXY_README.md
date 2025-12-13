# Reverse Proxy Implementation - Proxy PAD

This project implements a reverse proxy system with caching (Redis) and load balancing (Round-Robin) for distributed data warehouse access.

## Architecture

```
Client → Proxy (8080) → Load Balancer → [DW1 (9001), DW2 (9003)]
                ↓
              Redis Cache
```

## Components

### 1. Proxy Service (Port 8080)
- **ProxyController**: Handles HTTP requests (GET, POST, PUT, DELETE)
- **ProxyService**: Coordinates caching and load balancing
- **LoadBalancer**: Round-Robin algorithm for distributing requests
- **CacheService**: Redis-based caching with TTL
- **HttpClient**: HTTP communication with data warehouses

### 2. Data Warehouses
- **movie_api** (Port 9001): Primary data warehouse
- **movie_api_9003** (Port 9003): Secondary data warehouse for load balancing

## Features

### Smart Proxy
- Transparent request forwarding
- Thread-per-request model (provided by Spring Boot)
- Support for GET, POST, PUT, DELETE methods

### Caching
- Redis-based response caching
- Default TTL: 60 seconds
- Cache invalidation on data modifications (POST, PUT, DELETE)
- Cache keys generated from request path

### Load Balancing
- Round-Robin algorithm
- Thread-safe implementation using AtomicInteger
- Distributes load across multiple data warehouse instances

## Running the System

### Prerequisites
- Java 21
- PostgreSQL database running on port 5432
- Redis server running on port 6379
- Database: `proxydb` with credentials (admin/Cascaval24#)

### Start Services

1. **Start PostgreSQL**
```bash
# Ensure PostgreSQL is running with database 'proxydb'
```

2. **Start Redis**
```bash
redis-server
```

3. **Start Data Warehouse 1 (Port 9001)**
```bash
cd movie_api
mvn spring-boot:run
```

4. **Start Data Warehouse 2 (Port 9003)**
```bash
# Note: movie_api_9003 uses the same codebase as movie_api, just different port
cd movie_api
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=9003
```

5. **Start Proxy (Port 8080)**
```bash
cd proxy
mvn spring-boot:run
```

## API Endpoints

All requests should be sent to the proxy on port 8080:

### Get All Movies
```bash
GET http://localhost:8080/api/movies
```

### Get Movie by ID
```bash
GET http://localhost:8080/api/movies/{id}
```

### Create Movie
```bash
POST http://localhost:8080/api/movies
Content-Type: application/json

{
  "title": "Movie Title",
  "rating": 8.5
}
```

### Update Movie
```bash
PUT http://localhost:8080/api/movies/{id}
Content-Type: application/json

{
  "title": "Updated Title",
  "rating": 9.0
}
```

### Delete Movie
```bash
DELETE http://localhost:8080/api/movies/{id}
```

## Testing Load Balancing

Make multiple requests to observe Round-Robin distribution:

```bash
# Request 1 → DW1 (9001)
curl http://localhost:8080/api/movies

# Request 2 → DW2 (9003)
curl http://localhost:8080/api/movies

# Request 3 → DW1 (9001)
curl http://localhost:8080/api/movies
```

Check the proxy logs to see which data warehouse handled each request.

## Testing Caching

```bash
# First request - Cache MISS (retrieves from DW)
curl http://localhost:8080/api/movies

# Second request within 60s - Cache HIT (retrieves from Redis)
curl http://localhost:8080/api/movies

# Modify data - invalidates cache
curl -X POST http://localhost:8080/api/movies \
  -H "Content-Type: application/json" \
  -d '{"title":"New Movie","rating":8.0}'

# Next request - Cache MISS (cache was invalidated)
curl http://localhost:8080/api/movies
```

## Configuration

### Proxy Configuration (`proxy/src/main/resources/application.properties`)
```properties
server.port=8080
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.cache.redis.time-to-live=60000
datawarehouse.endpoints=http://localhost:9001,http://localhost:9003
```

## Implementation Details

### Thread Safety
- **LoadBalancer**: Uses `AtomicInteger` for thread-safe Round-Robin index
- **CacheService**: Redis operations are thread-safe by design
- **Spring Boot**: Thread-per-request model handles concurrent requests

### Cache Strategy
- **GET requests**: Check cache first, store on miss
- **POST/PUT/DELETE**: Invalidate related cache entries
- **Pattern matching**: Cache invalidation uses pattern matching for related entries

## Project Structure

```
Proxy_PAD/
├── proxy/                      # Reverse proxy service
│   └── src/main/java/com/example/proxy/
│       ├── controller/         # HTTP request handlers
│       ├── service/            # Business logic
│       ├── http/               # HTTP client
│       └── config/             # Configuration classes
├── movie_api/                  # Data warehouse 1 (port 9001)
├── movie_api_9003/             # Data warehouse 2 (port 9003)
├── sync_node/                  # Synchronization service
└── common/                     # Shared models
```

## Requirements Met

✅ HTTP protocol study and implementation  
✅ Concurrent request processing (thread-per-request)  
✅ Thread-safe collections (AtomicInteger for Round-Robin)  
✅ Controller-based HTTP operation mapping  
✅ Smart proxy for connection management  
✅ Redis-based caching with TTL  
✅ Round-Robin load balancing  
✅ Support for GET, POST, PUT, DELETE methods  
✅ Multiple data warehouse instances  

## Technologies Used

- **Spring Boot 3.5.7**: Framework
- **Redis**: Caching layer
- **PostgreSQL**: Database
- **Lombok**: Reduce boilerplate
- **RestTemplate**: HTTP client
- **Jackson**: JSON processing

## Notes

- The proxy uses Spring Boot's embedded Tomcat which provides thread-per-request model
- Redis must be running for caching to work; if Redis is down, requests will bypass cache
- Both data warehouses share the same PostgreSQL database for data consistency
- Cache TTL can be configured in application.properties
