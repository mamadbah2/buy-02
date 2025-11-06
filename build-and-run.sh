#!/bin/bash

# Script simple pour build et run les microservices dans l'ordre
# Ordre: Eureka → Config → User → Product → Media → Order → API Gateway

set -e

echo "================================================"
echo "BUILD ET RUN DES MICROSERVICES"
echo "================================================"
echo ""

# 1. Eureka Discovery Service
echo "[1/7] Build et run Eureka Discovery Service..."
cd discovery-service
./mvnw clean install -DskipTests
./mvnw spring-boot:run &
cd ..
sleep 30
echo ""

# 2. Config Service
echo "[2/7] Build et run Config Service..."
cd config-service
./mvnw clean install -DskipTests
./mvnw spring-boot:run &
cd ..
sleep 20
echo ""

# 3. User Service
echo "[3/7] Build et run User Service..."
cd user-service
./mvnw clean install -DskipTests
./mvnw spring-boot:run &
cd ..
sleep 15
echo ""

# 4. Product Service
echo "[4/7] Build et run Product Service..."
cd product-service
./mvnw clean install -DskipTests
./mvnw spring-boot:run &
cd ..
sleep 15
echo ""

# 5. Media Service
echo "[5/7] Build et run Media Service..."
cd media-service
./mvnw clean install -DskipTests
./mvnw spring-boot:run &
cd ..
sleep 15
echo ""

# 6. Order Service
echo "[6/7] Build et run Order Service..."
cd order-service
./mvnw clean install -DskipTests
./mvnw spring-boot:run &
cd ..
sleep 15
echo ""

# 7. API Gateway
echo "[7/7] Build et run API Gateway..."
cd api-gateway
./mvnw clean install -DskipTests
./mvnw spring-boot:run &
cd ..
sleep 15
echo ""

echo "================================================"
echo "TOUS LES SERVICES SONT LANCÉS"
echo "================================================"
echo ""
echo "URLs:"
echo "  - Eureka:        http://localhost:8761"
echo "  - Config:        http://localhost:8888"
echo "  - User Service:  http://localhost:8081"
echo "  - Product:       http://localhost:8082"
echo "  - Media:         http://localhost:8083"
echo "  - Order:         http://localhost:8084"
echo "  - API Gateway:   https://localhost:5050"
echo ""
echo "Pour arrêter: ./stop-services.sh"
echo ""

