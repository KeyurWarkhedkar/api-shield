# API-Shield 🛡️

A high-performance API gateway middleware built with Spring Boot and Redis that provides intelligent rate limiting, dynamic caching, and traffic management to protect backend services from abuse and reduce external API costs.

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Redis](https://img.shields.io/badge/Redis-7.x-red.svg)](https://redis.io/)
[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.oracle.com/java/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

## 🎯 Problem Statement

Modern applications face critical challenges:
- **API Abuse**: Malicious or poorly implemented clients can overwhelm backend services
- **Cost Inefficiency**: Repeated calls to external APIs incur unnecessary expenses
- **Performance Bottlenecks**: Every request hitting upstream services adds latency
- **Lack of Visibility**: No centralized traffic monitoring and control

API-Shield solves these problems by acting as an intelligent middleware layer between clients and backend services.

## ✨ Key Features

### 🚦 Multi-Layer Protection
- **Traffic Monitoring**: Real-time request counting and pattern analysis
- **Smart Caching**: Automatic detection and caching of frequently accessed GET endpoints
- **Rate Limiting**: Token bucket algorithm with Redis-backed distributed rate limiting
- **Request Forwarding**: Transparent proxy to external APIs

### 📊 Performance Highlights
| Metric | Value | Impact |
|--------|-------|--------|
| **Cache Hit Ratio** | 87.5% | 9 out of 10 requests served from cache |
| **API Load Reduction** | 98.5% | Prevented ~395 out of 400 external API calls |
| **Median Latency** | 15ms | Lightning-fast cached responses |
| **95th Percentile** | 49ms | Consistent performance under load |
| **Burst Traffic Handling** | 11% throttled | Effective rate limiting without affecting legitimate traffic |

### 🔧 Technical Capabilities
- Distributed rate limiting using Redis
- Hot endpoint detection with LRU cache eviction
- Configurable rate limits per client/endpoint
- RESTful metrics endpoint for monitoring
- Filter chain architecture for extensibility

## 🏗️ Architecture

```
┌──────────┐
│  Client  │
└────┬─────┘
     │
     ▼
┌─────────────────────────────────────┐
│      API-Shield Middleware          │
│                                     │
│  ┌──────────────────────────────┐  │
│  │  ApiTrafficFilter            │  │
│  │  (Traffic Counting)          │  │
│  └──────────┬───────────────────┘  │
│             ▼                       │
│  ┌──────────────────────────────┐  │
│  │  DynamicCacheFilter          │  │
│  │  (Hot Endpoint Caching)      │  │
│  └──────────┬───────────────────┘  │
│             ▼                       │
│  ┌──────────────────────────────┐  │
│  │  RateLimiterProxyController  │  │
│  │  (Rate Limiting + Proxy)     │  │
│  └──────────┬───────────────────┘  │
│             │                       │
│  ┌──────────▼───────────────────┐  │
│  │      Redis Cache             │  │
│  │  - Rate limit tokens         │  │
│  │  - Cached responses          │  │
│  └──────────────────────────────┘  │
└─────────────┬───────────────────────┘
              ▼
     ┌────────────────┐
     │  External API  │
     └────────────────┘
```

## 🚀 Quick Start

### Prerequisites
- Java 17 or higher
- Redis 7.x running locally or accessible remotely
- Maven 3.6+

### Installation

1. **Clone the repository**
```bash
git clone https://github.com/KeyurWarkhedkar/api-shield.git
cd api-shield
```

2. **Configure Redis connection**

Edit `src/main/resources/application.properties`:
```properties
# -----------------------------
# Redis Configuration
# -----------------------------
spring.redis.host=localhost
spring.redis.port=6379
spring.redis.timeout=2000  

# External API base URL
proxy.external.base-url=http://localhost:8081

# -----------------------------
# Rate Limiting Configuration
# -----------------------------
rate.limit.max-requests=5
rate.limit.window-seconds=10

# -----------------------------
# Cache Configuration
# -----------------------------
cache.ttl-seconds=60

# -----------------------------
# Server Configuration
# -----------------------------
server.port=8080
```

3. **Build the project**
```bash
mvn clean install
```

4. **Run the application**
```bash
mvn spring-boot:run
```

The API-Shield will start on `http://localhost:8080`

## 📖 Usage

### Making Requests

API-Shield acts as a transparent proxy. Any request to the `/proxy` endpoint will pass through the middleware:

```bash
# This request goes through API-Shield filters
curl http://localhost:8080/proxy/api/users/123

# Response headers show cache status
# X-Cache: HIT (cached) or MISS (forwarded to external API)
# X-RateLimit-Remaining: 8
```

### Rate Limiting

When rate limit is exceeded, you'll receive:
```json
{
  "status": 429,
  "message": "Rate limit exceeded. Try again later.",
  "retryAfter": 60
}
```

### Monitoring

Access real-time metrics:
```bash
curl http://localhost:8080/metrics
```

Response:
```json
{
  "totalRequests": 400,
  "cacheHits": 350,
  "forwardedRequests": 6,
  "throttledRequests": 44
}
```

## 🔬 Performance Testing

Tested with Apache JMeter under concurrent load:

**Test Configuration:**
- **Concurrent Users**: 400
- **Request Rate**: ~20 req/sec
- **Test Duration**: 20 seconds

**Results:**

| Metric | Value |
|--------|-------|
| Total Requests | 400 |
| Cache Hits | 350 (87.5%) |
| Forwarded to API | 6 (1.5%) |
| Throttled (429) | 44 (11%) |
| Median Latency | 15ms |
| 95th Percentile | 49ms |
| Throughput | 20.07 req/sec |

**Key Takeaways:**
- ✅ **98.5% reduction in external API calls** (cost savings)
- ✅ **15ms median response time** for cached requests
- ✅ **11% burst traffic successfully throttled** without affecting legitimate users
- ✅ **Zero cache inconsistencies** across all requests

## 🛠️ Configuration

### Rate Limiting Strategies

**Token Bucket Algorithm** (Default)
```properties
rate.limiter.algorithm=TOKEN_BUCKET
rate.limiter.tokens.per.minute=60
rate.limiter.burst.capacity=10
```

### Cache Configuration

**Hot Endpoint Detection**
```properties
# Minimum requests to be considered "hot"
cache.hot.threshold=50

# Cache TTL
cache.ttl.minutes=5

```


## 🎯 Use Cases

### 1. **Microservices Gateway**
Deploy API-Shield as a gateway for internal microservices to prevent cascading failures and reduce inter-service traffic.

### 2. **Third-Party API Protection**
Wrap expensive external APIs (payment gateways, AI services, maps APIs) to minimize costs through caching.

### 3. **Public API Rate Limiting**
Expose your own APIs with built-in rate limiting to prevent abuse and ensure fair usage.

### 4. **Development/Testing**
Use as a mock gateway during development to simulate rate limits and caching behavior.

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request


## 👨‍💻 Author

**Keyur Warkhedkar**
- Email: keyurwarkhedkar@gmail.com
- LinkedIn: https://www.linkedin.com/in/keyurwarkhedkar
- GitHub: https://github.com/KeyurWarkhedkar


## 📚 References

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Redis Documentation](https://redis.io/documentation)
- [Rate Limiting Algorithms](https://en.wikipedia.org/wiki/Rate_limiting)
- [Token Bucket Algorithm](https://en.wikipedia.org/wiki/Token_bucket)

---

