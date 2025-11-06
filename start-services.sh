#!/bin/bash

# Script de lancement des microservices dans le bon ordre
# Ordre: Eureka → Config → Services métier → API Gateway → Frontend

set -e

# Couleurs pour les messages
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
MAX_RETRIES=30
RETRY_INTERVAL=5

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

# Fonction pour vérifier si un service est en cours d'exécution
check_service_health() {
    local service_name=$1
    local health_url=$2
    local retries=0

    log_info "Vérification de la santé de $service_name..."

    while [ $retries -lt $MAX_RETRIES ]; do
        if curl -k -s -f "$health_url" > /dev/null 2>&1; then
            log_success "$service_name est prêt !"
            return 0
        fi

        retries=$((retries + 1))
        if [ $retries -lt $MAX_RETRIES ]; then
            echo -n "."
            sleep $RETRY_INTERVAL
        fi
    done

    log_error "$service_name n'a pas démarré dans le délai imparti"
    return 1
}

# Fonction pour démarrer un service Spring Boot
start_spring_service() {
    local service_name=$1
    local service_dir=$2
    local health_url=$3

    log_info "Démarrage de $service_name..."

    cd "$service_dir" || {
        log_error "Impossible d'accéder au répertoire $service_dir"
        return 1
    }

    # Vérifier si le service n'est pas déjà en cours d'exécution
    if check_service_health "$service_name" "$health_url" 2>/dev/null; then
        log_warning "$service_name est déjà en cours d'exécution"
        cd - > /dev/null
        return 0
    fi

    # Compiler et démarrer le service
    log_info "Compilation de $service_name..."
    ./mvnw clean install -DskipTests > /dev/null 2>&1 &

    local build_pid=$!
    wait $build_pid

    if [ $? -ne 0 ]; then
        log_error "Échec de la compilation de $service_name"
        cd - > /dev/null
        return 1
    fi

    log_info "Lancement de $service_name en arrière-plan..."
    nohup ./mvnw spring-boot:run > "nohup-$service_name.out" 2>&1 &

    cd - > /dev/null

    # Attendre que le service soit prêt
    if check_service_health "$service_name" "$health_url"; then
        return 0
    else
        return 1
    fi
}

# Fonction pour démarrer le frontend Angular
start_frontend() {
    log_info "Démarrage du Frontend Angular..."

    cd buy-01-frontend || {
        log_error "Impossible d'accéder au répertoire buy-01-frontend"
        return 1
    }

    # Vérifier si npm est installé
    if ! command -v npm &> /dev/null; then
        log_error "npm n'est pas installé"
        cd - > /dev/null
        return 1
    fi

    # Installer les dépendances si nécessaire
    if [ ! -d "node_modules" ]; then
        log_info "Installation des dépendances npm..."
        npm install > /dev/null 2>&1
    fi

    # Démarrer le serveur de développement
    log_info "Lancement du serveur Angular en arrière-plan..."
    nohup npm start > nohup-frontend.out 2>&1 &

    cd - > /dev/null

    sleep 10
    log_success "Frontend démarré"
    return 0
}

# Fonction principale
main() {
    log_info "=========================================="
    log_info "Démarrage de l'architecture microservices"
    log_info "=========================================="
    echo ""

    # Aller dans le répertoire du projet
    SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
    cd "$SCRIPT_DIR" || exit 1

    # 1. Démarrer Eureka (Discovery Service)
    log_info "ÉTAPE 1/6: Lancement d'Eureka Discovery Service"
    if ! start_spring_service "Eureka Server" "discovery-service" "http://localhost:8761/actuator/health"; then
        log_error "Échec du démarrage d'Eureka. Abandon."
        exit 1
    fi
    echo ""

    # 2. Démarrer Config Service
    log_info "ÉTAPE 2/6: Lancement du Config Service"
    if ! start_spring_service "Config Service" "config-service" "http://localhost:8888/actuator/health"; then
        log_error "Échec du démarrage du Config Service. Abandon."
        exit 1
    fi
    echo ""

    # 3. Démarrer les services métier en parallèle
    log_info "ÉTAPE 3/6: Lancement des services métier"

    # Media Service
    log_info "3.1 - Démarrage du Media Service"
    if ! start_spring_service "Media Service" "media-service" "http://localhost:8083/actuator/health"; then
        log_warning "Échec du démarrage du Media Service, mais on continue..."
    fi
    echo ""

    # User Service
    log_info "3.2 - Démarrage du User Service"
    if ! start_spring_service "User Service" "user-service" "http://localhost:8081/actuator/health"; then
        log_warning "Échec du démarrage du User Service, mais on continue..."
    fi
    echo ""

    # Product Service
    log_info "3.3 - Démarrage du Product Service"
    if ! start_spring_service "Product Service" "product-service" "http://localhost:8082/actuator/health"; then
        log_warning "Échec du démarrage du Product Service, mais on continue..."
    fi
    echo ""

    # Order Service (si existe)
    if [ -d "order-service" ]; then
        log_info "3.4 - Démarrage de l'Order Service"
        if ! start_spring_service "Order Service" "order-service" "http://localhost:8084/actuator/health"; then
            log_warning "Échec du démarrage de l'Order Service, mais on continue..."
        fi
        echo ""
    fi

    # 4. Démarrer API Gateway
    log_info "ÉTAPE 4/6: Lancement de l'API Gateway"
    if ! start_spring_service "API Gateway" "api-gateway" "https://localhost:5050/actuator/health"; then
        log_error "Échec du démarrage de l'API Gateway. Abandon."
        exit 1
    fi
    echo ""

    # 5. Démarrer le Frontend (optionnel)
    log_info "ÉTAPE 5/6: Lancement du Frontend Angular"
    if [ -d "buy-01-frontend" ]; then
        start_frontend
    else
        log_warning "Répertoire frontend non trouvé, passage..."
    fi
    echo ""

    # Résumé
    log_info "=========================================="
    log_success "Tous les services sont démarrés !"
    log_info "=========================================="
    echo ""
    log_info "URLs des services :"
    log_info "  • Eureka Dashboard:    http://localhost:8761"
    log_info "  • Config Service:      http://localhost:8888"
    log_info "  • User Service:        http://localhost:8081"
    log_info "  • Product Service:     http://localhost:8082"
    log_info "  • Media Service:       http://localhost:8083"
    if [ -d "order-service" ]; then
        log_info "  • Order Service:       http://localhost:8084"
    fi
    log_info "  • API Gateway:         https://localhost:5050"
    if [ -d "buy-01-frontend" ]; then
        log_info "  • Frontend:            http://localhost:4200"
    fi
    echo ""
    log_info "Pour arrêter tous les services, exécutez: ./stop-services.sh"
    echo ""
}

# Exécuter le script
main

