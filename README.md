# BUY-02 — Monorepo microservices (Spring Boot + Angular)

Plateforme e-commerce démonstrative bâtie en microservices. Le back-end s’appuie sur Spring Boot 3.5/Spring Cloud 2025 et le front sur Angular, packagés avec Docker et orchestrés via Docker Compose. Un pipeline Jenkins assure build, qualité, intégration et déploiement.

## Aperçu de l’architecture
Services (ports exposés en local) et rôles:
- Discovery Service (Eureka) — 8761:8761
- Config Service (Spring Cloud Config Server) — 8888:8888
- API Gateway (Spring Cloud Gateway, HTTPS) — 5050:5050
- User Service — 8081:8081
- Product Service — 8082:8082
- Media Service — 8083:8083
- Order Service — 8084:8084
- Frontend (Angular via Nginx) — 4200:80 et 8443:443

Les services communiquent via l’API Gateway et Eureka. La configuration applicative est fournie par un repo Git distant (Spring Cloud Config Server).

## Prérequis
- Docker ≥ 24 et Docker Compose
- Accès à GitHub (token lecture) pour le Config Server
- Optionnel (dev local hors Docker): Java 21 + Maven 3.9, Node 20 + npm

## Démarrage rapide (Docker Compose)
1) Exporter votre token GitHub (lecture du repo de config):
```bash
export GITHUB_TOKEN="<votre_token_github>"
```
2) Démarrer l’ensemble:
```bash
docker compose up -d --build
```
3) Accéder aux services:
- Eureka: http://localhost:8761
- API Gateway (TLS): https://localhost:5050
- Frontend: http://localhost:4200 ou https://localhost:8443
- Health endpoints: `.../actuator/health` (backends), `/health` (frontend)

Astuce: la Gateway utilise un certificat auto-signé. Votre navigateur peut nécessiter une validation manuelle.

## Exécution avec images prébuildées
Si vous disposez d’images poussées sur un registre (via le pipeline), utilisez:
```bash
export IMAGE_VERSION="<numéro_build_ou_tag>"
export GITHUB_TOKEN="<votre_token_github>"
docker compose -f docker-compose-deploy.yml up -d
```

## Développement local (sans Docker)
Démarrer les services un par un dans cet ordre (nouveaux terminaux):
```bash
# 1) Discovery
cd discovery-service && mvn spring-boot:run

# 2) Config Service (nécessite GITHUB_TOKEN)
export GITHUB_TOKEN="<votre_token_github>"
export DOCKER_EUREKA_URL="http://localhost:8761/eureka"
cd ../config-service && mvn spring-boot:run

# 3) API Gateway
export DOCKER_CONFIG_SERVICE_URL="http://localhost:8888"
export DOCKER_EUREKA_URL="http://localhost:8761/eureka"
cd ../api-gateway && mvn spring-boot:run

# 4) Services métier (exemples)
export DOCKER_CONFIG_SERVICE_URL="http://localhost:8888"
export DOCKER_MEDIA_SERVICE_URL="http://localhost:8083/api/media"   # pour product-service
export DOCKER_PRODUCT_SERVICE_URL="http://localhost:8082/api/products"  # pour order-service
cd ../user-service && mvn spring-boot:run
cd ../product-service && mvn spring-boot:run
cd ../media-service && mvn spring-boot:run
cd ../order-service && mvn spring-boot:run

# 5) Frontend (dev)
cd ../buy-01-frontend
npm install
npm start
```
Remarque: certaines intégrations (ex: Media Service) peuvent nécessiter des variables supplémentaires (voir ci-dessous).

## Build & tests
- Back-end (par service):
```bash
mvn -f discovery-service/pom.xml clean verify
mvn -f config-service/pom.xml clean verify
mvn -f api-gateway/pom.xml clean verify
mvn -f product-service/pom.xml clean verify
mvn -f user-service/pom.xml clean verify
mvn -f media-service/pom.xml clean verify
mvn -f order-service/pom.xml clean verify
```
- Front-end Angular:
```bash
cd buy-01-frontend
npm ci
npm run test:headless
npm run build
```
- Build d’images Docker (exemples):
```bash
docker build -t safe-zone-eureka:latest discovery-service
# … répéter pour chaque service, ou laisser docker compose builder automatiquement
```

## CI/CD (Jenkins)
Le fichier `Jenkinsfile` orchestre:
1. Build & tests parallélisés (Angular + Maven)
2. Analyse SonarQube + Quality Gates
3. Construction des images Docker
4. Tests d’intégration via docker compose
5. Push des images sur Docker Hub
6. Déploiement local via `docker-compose-deploy.yml` + health check

Crédentials/paramètres requis dans Jenkins (exemples à adapter):
- `GITHUB_TOKEN` (Secret text): accès lecture du repo de config
- `dockerhub-credential` (Username/Password): push des images
- `SONAR_USER_TOKEN` (Secret text) et serveur Sonar nommé `safe-zone-mr-jenk`
- Variables pour Media Service: `MONGODB_URI`, `MONGODB_DATABASE`, `SUPABASE_PROJECT_URL`, `SUPABASE_API_KEY`, `SUPABASE_BUCKET_NAME`

Les images sont taguées avec un préfixe projet et le numéro de build, puis poussées en `:latest` et `:${BUILD_NUMBER}`.

## Configuration & variables d’environnement
- Config Server (repo Git): `https://github.com/mamadbah2/config-buy-01.git`
  - `GITHUB_TOKEN` requis
- Service Discovery: `DOCKER_EUREKA_URL` (par défaut `http://localhost:8761/eureka`)
- Config Client: `DOCKER_CONFIG_SERVICE_URL` (par défaut `http://localhost:8888`)
- Gateway CORS et SSL déjà configurés côté application
- Services inter-dépendants:
  - `DOCKER_MEDIA_SERVICE_URL` (ex: `http://media-service:8083/api/media`)
  - `DOCKER_PRODUCT_SERVICE_URL` (ex: `http://product-service:8082/api/products`)
- Media Service (selon implémentation): `MONGODB_URI`, `MONGODB_DATABASE`, `SUPABASE_PROJECT_URL`, `SUPABASE_API_KEY`, `SUPABASE_BUCKET_NAME`

## Observabilité
- Actuator exposé sur chaque service: `/actuator/health`, `/actuator/info`
- Health check Nginx (frontend): `/health`

## Sécurité & certificats
- API Gateway utilise un keystore PKCS12 embarqué (self-signed) et écoute en HTTPS sur 5050.
- Le frontend Nginx embarque des certificats auto-signés (`nginx.crt` / `nginx.key`) et expose 80/443.
- Utilisation en local: attendez-vous à des avertissements de certificat non approuvé.

## Commandes utiles
```bash
# Voir l’état des conteneurs
docker compose ps

# Logs en direct
docker compose logs -f --tail=100

# Arrêt et nettoyage
docker compose down -v --remove-orphans
```

## Structure (extrait)
- `discovery-service/`, `config-service/`, `api-gateway/`, `user-service/`, `product-service/`, `media-service/`, `order-service/` — microservices Spring Boot
- `buy-01-frontend/` — application Angular servie par Nginx
- `docker-compose.yml` — exécution locale avec build des images
- `docker-compose-deploy.yml` — exécution à partir d’images versionnées
- `Jenkinsfile` — pipeline CI/CD
