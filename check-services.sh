#!/bin/bash

# Script pour vérifier l'état de tous les microservices

# Couleurs pour les messages
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Fonction pour vérifier l'état d'un service
check_service() {
    local service_name=$1
    local url=$2
    local port=$3

    printf "%-25s " "$service_name"

    # Vérifier si le port est utilisé
    if command -v lsof &> /dev/null; then
        if lsof -ti:$port > /dev/null 2>&1; then
            printf "[${GREEN}PORT OK${NC}] "
        else
            printf "[${RED}PORT LIBRE${NC}] "
            echo ""
            return 1
        fi
    fi

    # Vérifier la santé du service
    if curl -k -s -f "$url" > /dev/null 2>&1; then
        echo -e "${GREEN}✓ EN LIGNE${NC}"
        return 0
    else
        echo -e "${RED}✗ HORS LIGNE${NC}"
        return 1
    fi
}

# Fonction principale
main() {
    echo ""
    echo -e "${BLUE}=========================================="
    echo "État des microservices"
    echo -e "==========================================${NC}"
    echo ""

    local total=0
    local running=0

    # Vérifier Eureka
    total=$((total + 1))
    if check_service "Eureka Server" "http://localhost:8761/actuator/health" 8761; then
        running=$((running + 1))
    fi

    # Vérifier Config Service
    total=$((total + 1))
    if check_service "Config Service" "http://localhost:8888/actuator/health" 8888; then
        running=$((running + 1))
    fi

    # Vérifier User Service
    total=$((total + 1))
    if check_service "User Service" "http://localhost:8081/actuator/health" 8081; then
        running=$((running + 1))
    fi

    # Vérifier Product Service
    total=$((total + 1))
    if check_service "Product Service" "http://localhost:8082/actuator/health" 8082; then
        running=$((running + 1))
    fi

    # Vérifier Media Service
    total=$((total + 1))
    if check_service "Media Service" "http://localhost:8083/actuator/health" 8083; then
        running=$((running + 1))
    fi

    # Vérifier Order Service
    if [ -d "order-service" ]; then
        total=$((total + 1))
        if check_service "Order Service" "http://localhost:8084/actuator/health" 8084; then
            running=$((running + 1))
        fi
    fi

    # Vérifier API Gateway
    total=$((total + 1))
    if check_service "API Gateway" "https://localhost:5050/actuator/health" 5050; then
        running=$((running + 1))
    fi

    # Vérifier Frontend
    if [ -d "buy-01-frontend" ]; then
        total=$((total + 1))
        printf "%-25s " "Frontend Angular"
        if command -v lsof &> /dev/null; then
            if lsof -ti:4200 > /dev/null 2>&1; then
                printf "[${GREEN}PORT OK${NC}] "
                echo -e "${GREEN}✓ EN LIGNE${NC}"
                running=$((running + 1))
            else
                printf "[${RED}PORT LIBRE${NC}] "
                echo -e "${RED}✗ HORS LIGNE${NC}"
            fi
        else
            if curl -s -f "http://localhost:4200" > /dev/null 2>&1; then
                echo -e "${GREEN}✓ EN LIGNE${NC}"
                running=$((running + 1))
            else
                echo -e "${RED}✗ HORS LIGNE${NC}"
            fi
        fi
    fi

    echo ""
    echo -e "${BLUE}==========================================${NC}"
    echo -e "Services en ligne: ${GREEN}$running${NC}/$total"
    echo -e "${BLUE}==========================================${NC}"
    echo ""

    if [ $running -eq $total ]; then
        echo -e "${GREEN}✓ Tous les services sont opérationnels !${NC}"
        echo ""
        return 0
    else
        echo -e "${YELLOW}⚠ Certains services ne sont pas disponibles${NC}"
        echo ""
        return 1
    fi
}

# Exécuter le script
main

