#!/bin/bash

# Script pour arrêter tous les microservices

set -e

# Couleurs pour les messages
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Fonction pour afficher les messages
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Fonction pour arrêter un service sur un port spécifique
stop_service_on_port() {
    local port=$1
    local service_name=$2

    log_info "Arrêt de $service_name (port $port)..."

    # Trouver le PID du processus utilisant le port
    local pid=$(lsof -ti:$port 2>/dev/null)

    if [ -z "$pid" ]; then
        log_warning "$service_name n'est pas en cours d'exécution sur le port $port"
        return 0
    fi

    # Arrêter le processus
    kill -15 $pid 2>/dev/null || kill -9 $pid 2>/dev/null

    # Attendre que le processus se termine
    local retries=0
    while [ $retries -lt 10 ]; do
        if ! lsof -ti:$port > /dev/null 2>&1; then
            log_success "$service_name arrêté"
            return 0
        fi
        sleep 1
        retries=$((retries + 1))
    done

    log_warning "$service_name pourrait ne pas s'être arrêté correctement"
}

# Fonction pour arrêter les processus Maven Spring Boot
stop_maven_processes() {
    log_info "Arrêt de tous les processus Maven Spring Boot..."

    # Trouver et tuer tous les processus mvn spring-boot:run
    local pids=$(pgrep -f "mvn.*spring-boot:run" 2>/dev/null || true)

    if [ -z "$pids" ]; then
        log_warning "Aucun processus Maven Spring Boot trouvé"
        return 0
    fi

    for pid in $pids; do
        kill -15 $pid 2>/dev/null || kill -9 $pid 2>/dev/null
    done

    sleep 2
    log_success "Processus Maven arrêtés"
}

# Fonction pour arrêter les processus Java
stop_java_processes() {
    log_info "Recherche de processus Java des microservices..."

    # Services à rechercher
    local services=("discovery-service" "config-service" "user-service" "product-service" "media-service" "order-service" "api-gateway")

    for service in "${services[@]}"; do
        local pids=$(pgrep -f "$service.*jar" 2>/dev/null || true)

        if [ -n "$pids" ]; then
            log_info "Arrêt de $service (PID: $pids)..."
            for pid in $pids; do
                kill -15 $pid 2>/dev/null || kill -9 $pid 2>/dev/null
            done
        fi
    done

    sleep 2
    log_success "Processus Java arrêtés"
}

# Fonction pour arrêter les processus npm
stop_npm_processes() {
    log_info "Arrêt des processus npm/Angular..."

    local pids=$(pgrep -f "node.*angular" 2>/dev/null || pgrep -f "ng serve" 2>/dev/null || true)

    if [ -z "$pids" ]; then
        log_warning "Aucun processus npm trouvé"
        return 0
    fi

    for pid in $pids; do
        kill -15 $pid 2>/dev/null || kill -9 $pid 2>/dev/null
    done

    sleep 2
    log_success "Processus npm arrêtés"
}

# Fonction principale
main() {
    log_info "=========================================="
    log_info "Arrêt de l'architecture microservices"
    log_info "=========================================="
    echo ""

    # Vérifier si lsof est disponible
    if ! command -v lsof &> /dev/null; then
        log_warning "lsof n'est pas installé, utilisation d'une méthode alternative"
        LSOF_AVAILABLE=false
    else
        LSOF_AVAILABLE=true
    fi

    # Arrêter les services par port (dans l'ordre inverse du démarrage)
    if [ "$LSOF_AVAILABLE" = true ]; then
        stop_service_on_port 4200 "Frontend Angular"
        stop_service_on_port 5050 "API Gateway"
        stop_service_on_port 8084 "Order Service"
        stop_service_on_port 8083 "Media Service"
        stop_service_on_port 8082 "Product Service"
        stop_service_on_port 8081 "User Service"
        stop_service_on_port 8888 "Config Service"
        stop_service_on_port 8761 "Eureka Server"
        echo ""
    fi

    # Arrêter tous les processus Maven
    stop_maven_processes
    echo ""

    # Arrêter tous les processus Java des microservices
    stop_java_processes
    echo ""

    # Arrêter les processus npm
    stop_npm_processes
    echo ""

    log_info "=========================================="
    log_success "Tous les services sont arrêtés !"
    log_info "=========================================="
}

# Exécuter le script
main

