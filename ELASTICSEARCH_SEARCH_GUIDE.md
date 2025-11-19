# 🔍 Documentation - Recherche de Produits avec ElasticSearch

## ✨ Fonctionnalité de recherche

L'endpoint `/api/products/search` permet de rechercher des produits par texte (nom ou description) avec des filtres optionnels de prix basés sur ElasticSearch.

---

## 🚀 Endpoint de recherche

### GET /api/products/search

Recherche des produits avec filtres optionnels.

#### 🔧 Paramètres de requête

| Paramètre | Type | Requis | Valeur par défaut | Description |
|-----------|------|--------|-------------------|-------------|
| `q` | String | ❌ Non | - | Texte à rechercher dans le nom ou la description |
| `minPrice` | Double | ❌ Non | - | Prix minimum (inclus) |
| `maxPrice` | Double | ❌ Non | - | Prix maximum (inclus) |
| `page` | int | ❌ Non | `0` | Numéro de page (commence à 0) |
| `size` | int | ❌ Non | `20` | Nombre d'éléments par page |
| `sortBy` | String | ❌ Non | `id` | Champ de tri |
| `sortDirection` | String | ❌ Non | `DESC` | Direction (ASC ou DESC) |

---

## 📋 Exemples d'utilisation

### 1. Recherche simple par texte

```bash
# Rechercher "iphone"
GET http://localhost:8082/api/products/search?q=iphone

# Rechercher "samsung"
GET http://localhost:8082/api/products/search?q=samsung
```

### 2. Recherche avec prix minimum

```bash
# Produits contenant "iphone" avec prix >= 100€
GET http://localhost:8082/api/products/search?q=iphone&minPrice=100

# Tous les produits avec prix >= 500€
GET http://localhost:8082/api/products/search?minPrice=500
```

### 3. Recherche avec prix maximum

```bash
# Produits "laptop" avec prix <= 1000€
GET http://localhost:8082/api/products/search?q=laptop&maxPrice=1000

# Tous les produits avec prix <= 50€
GET http://localhost:8082/api/products/search?maxPrice=50
```

### 4. Recherche avec intervalle de prix

```bash
# iPhone entre 100€ et 500€
GET http://localhost:8082/api/products/search?q=iphone&minPrice=100&maxPrice=500

# Tous les produits entre 20€ et 100€
GET http://localhost:8082/api/products/search?minPrice=20&maxPrice=100
```

### 5. Recherche avec pagination et tri

```bash
# Recherche "phone", triée par prix croissant, 10 résultats par page
GET http://localhost:8082/api/products/search?q=phone&sortBy=price&sortDirection=ASC&size=10

# Produits entre 100-500€, triés par nom
GET http://localhost:8082/api/products/search?minPrice=100&maxPrice=500&sortBy=name&sortDirection=ASC
```

---

## 📤 Format de réponse

La réponse est un objet `Page` identique aux autres endpoints paginés :

```json
{
  "content": [
    {
      "id": "65abc...",
      "name": "iPhone 14 Pro",
      "description": "Dernier modèle Apple...",
      "price": 1299.99,
      "quantity": 15,
      "userId": "seller-id",
      "medias": [
        {
          "id": "media-id",
          "url": "https://picsum.photos/...",
          "productId": "65abc..."
        }
      ]
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20,
    "sort": {
      "sorted": true,
      "unsorted": false
    }
  },
  "totalPages": 5,
  "totalElements": 87,
  "numberOfElements": 20,
  "first": true,
  "last": false,
  "empty": false
}
```

---

## 💻 Exemples de code

### JavaScript/TypeScript

```typescript
// Service de recherche de produits
class ProductSearchService {
  private baseUrl = 'http://localhost:8082/api/products';

  // Recherche simple
  async search(query: string, page = 0, size = 20) {
    const params = new URLSearchParams({
      q: query,
      page: page.toString(),
      size: size.toString()
    });
    
    const response = await fetch(`${this.baseUrl}/search?${params}`);
    return await response.json();
  }

  // Recherche avec filtres de prix
  async searchWithPriceFilter(
    query: string,
    minPrice?: number,
    maxPrice?: number,
    page = 0,
    size = 20
  ) {
    const params = new URLSearchParams({
      page: page.toString(),
      size: size.toString()
    });
    
    if (query) params.append('q', query);
    if (minPrice !== undefined) params.append('minPrice', minPrice.toString());
    if (maxPrice !== undefined) params.append('maxPrice', maxPrice.toString());
    
    const response = await fetch(`${this.baseUrl}/search?${params}`);
    return await response.json();
  }

  // Recherche avec tri personnalisé
  async searchWithSort(
    query: string,
    sortBy: string,
    sortDirection: 'ASC' | 'DESC',
    page = 0,
    size = 20
  ) {
    const params = new URLSearchParams({
      q: query,
      sortBy,
      sortDirection,
      page: page.toString(),
      size: size.toString()
    });
    
    const response = await fetch(`${this.baseUrl}/search?${params}`);
    return await response.json();
  }
}

// Utilisation
const searchService = new ProductSearchService();

// Rechercher des iPhones
const iphones = await searchService.search('iphone');

// Rechercher des produits entre 100€ et 500€
const midRangeProducts = await searchService.searchWithPriceFilter(
  '', 100, 500
);

// Rechercher des laptops pas chers, triés par prix
const cheapLaptops = await searchService.searchWithPriceFilter(
  'laptop', undefined, 800
);
```

### cURL

```bash
# Recherche simple
curl -X GET "http://localhost:8082/api/products/search?q=iphone" \
  -H "Content-Type: application/json"

# Avec filtres de prix
curl -X GET "http://localhost:8082/api/products/search?q=phone&minPrice=100&maxPrice=500" \
  -H "Content-Type: application/json"

# Avec tri
curl -X GET "http://localhost:8082/api/products/search?q=laptop&sortBy=price&sortDirection=ASC" \
  -H "Content-Type: application/json"

# Compter les résultats
curl -X GET "http://localhost:8082/api/products/search?q=samsung" \
  -H "Content-Type: application/json" | jq '.totalElements'
```

---

## 🎯 Cas d'usage

### 1. Barre de recherche utilisateur

```typescript
// Recherche dynamique pendant la saisie
const handleSearch = async (searchText: string) => {
  if (searchText.length >= 3) {
    const results = await searchService.search(searchText, 0, 10);
    displayResults(results.content);
  }
};
```

### 2. Filtrage par prix dans un catalogue

```typescript
// Filtres de la page catalogue
const applyPriceFilter = async (min: number, max: number) => {
  const results = await searchService.searchWithPriceFilter(
    currentSearchQuery,
    min,
    max,
    0,
    24
  );
  updateCatalog(results);
};
```

### 3. Page de résultats de recherche

```typescript
// Recherche avec tous les filtres
const searchResults = await fetch(
  `${baseUrl}/search?` + new URLSearchParams({
    q: 'smartphone',
    minPrice: '200',
    maxPrice: '800',
    sortBy: 'price',
    sortDirection: 'ASC',
    page: '0',
    size: '24'
  })
);
```

### 4. Suggestions de produits

```typescript
// Trouver des produits similaires
const findSimilar = async (productName: string) => {
  const results = await searchService.search(productName, 0, 5);
  return results.content.filter(p => p.name !== productName);
};
```

---

## 🔧 Configuration ElasticSearch

### docker-compose.yml

Ajoutez ElasticSearch à votre configuration Docker :

```yaml
services:
  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.11.0
    container_name: elasticsearch
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false
      - "ES_JAVA_OPTS=-Xms512m -Xmx512m"
    ports:
      - "9200:9200"
      - "9300:9300"
    volumes:
      - elasticsearch-data:/usr/share/elasticsearch/data
    networks:
      - app-network

  product-service:
    # ... configuration existante
    environment:
      - ELASTICSEARCH_HOST=elasticsearch
      - ELASTICSEARCH_PORT=9200
    depends_on:
      - elasticsearch
      - mongodb

volumes:
  elasticsearch-data:
    driver: local
```

### Démarrage local (développement)

```bash
# Démarrer ElasticSearch avec Docker
docker run -d \
  --name elasticsearch \
  -p 9200:9200 \
  -p 9300:9300 \
  -e "discovery.type=single-node" \
  -e "xpack.security.enabled=false" \
  docker.elastic.co/elasticsearch/elasticsearch:8.11.0

# Vérifier qu'ElasticSearch est démarré
curl http://localhost:9200
```

---

## 📊 Indexation des données

Les produits sont automatiquement indexés dans ElasticSearch lors :

1. **Création d'un produit** → Ajouté à l'index
2. **Mise à jour d'un produit** → Réindexé
3. **Suppression d'un produit** → Retiré de l'index

### Réindexation manuelle (si nécessaire)

Si vous avez déjà des produits dans MongoDB et que vous démarrez ElasticSearch pour la première fois, vous devrez réindexer :

```java
// À ajouter dans ProductSeed ou créer un endpoint admin
@Autowired
private ProductRepo productRepo;

@Autowired
private ProductSearchRepo productSearchRepo;

public void reindexAllProducts() {
    List<Product> allProducts = productRepo.findAll();
    productSearchRepo.saveAll(allProducts);
    log.info("✅ {} produits réindexés dans ElasticSearch", allProducts.size());
}
```

---

## 🧪 Tests

### Test de l'endpoint de recherche

```bash
# 1. Vérifier qu'ElasticSearch est accessible
curl http://localhost:9200

# 2. Créer quelques produits de test (ou utiliser le seeding)

# 3. Tester la recherche simple
curl "http://localhost:8082/api/products/search?q=test"

# 4. Tester avec filtres de prix
curl "http://localhost:8082/api/products/search?q=product&minPrice=10&maxPrice=100"

# 5. Vérifier le nombre de résultats
curl "http://localhost:8082/api/products/search?q=phone" | jq '.totalElements'

# 6. Tester sans query (liste filtrée par prix)
curl "http://localhost:8082/api/products/search?minPrice=50&maxPrice=200"
```

---

## 🔍 Fonctionnement de la recherche

### Recherche textuelle

ElasticSearch recherche le texte dans :
- **name** (nom du produit)
- **description** (description du produit)

La recherche est **insensible à la casse** et utilise un **analyseur standard** qui :
- Tokenise le texte
- Met en minuscules
- Supprime les accents

### Filtres de prix

| Paramètres | Condition SQL équivalente |
|------------|--------------------------|
| `minPrice` | `price >= minPrice` |
| `maxPrice` | `price <= maxPrice` |
| `minPrice` & `maxPrice` | `price BETWEEN minPrice AND maxPrice` |

### Combinaison recherche + filtres

```
Résultats = (nom CONTAINS query OR description CONTAINS query) 
            AND (price >= minPrice) 
            AND (price <= maxPrice)
```

---

## 📈 Performances

### Avantages d'ElasticSearch

| Aspect | MongoDB seul | Avec ElasticSearch |
|--------|-------------|-------------------|
| Recherche textuelle | Lent (full scan) | Très rapide (index inversé) |
| Recherche partielle | Regex lent | Rapide avec analyseurs |
| Tri + filtre | Peut être lent | Optimisé |
| Temps de réponse | 500-2000ms | 10-100ms |

### Recommandations

- **Petites bases (<1000 produits)** : MongoDB seul peut suffire
- **Moyennes bases (1000-10000)** : ElasticSearch recommandé
- **Grandes bases (>10000)** : ElasticSearch indispensable

---

## 🐛 Dépannage

### Erreur : Connection refused to ElasticSearch

**Cause** : ElasticSearch n'est pas démarré

**Solution** :
```bash
docker start elasticsearch
# ou
docker-compose up -d elasticsearch
```

### Erreur : No results found

**Causes possibles** :
1. Les produits ne sont pas indexés
2. La recherche ne correspond à rien

**Solutions** :
```bash
# Vérifier l'index ElasticSearch
curl "http://localhost:9200/products/_search?pretty"

# Réindexer si nécessaire (via endpoint admin ou code)
```

### Recherche retourne des résultats inattendus

**Cause** : Analyseur trop permissif

**Solution** : Ajuster la configuration dans `elasticsearch-settings.json`

---

## ✅ Résumé

### Nouveau endpoint créé

```
GET /api/products/search?q={query}&minPrice={min}&maxPrice={max}
```

### Fichiers créés/modifiés

1. ✅ **pom.xml** - Ajout de spring-boot-starter-data-elasticsearch
2. ✅ **Product.java** - Annotations ElasticSearch
3. ✅ **ProductSearchRepo.java** - Repository ElasticSearch
4. ✅ **ProductService.java** - Méthode search()
5. ✅ **ProductServiceImpl.java** - Implémentation search()
6. ✅ **ProductController.java** - Endpoint /search
7. ✅ **ProductControllerImpl.java** - Implémentation endpoint
8. ✅ **ElasticSearchConfig.java** - Configuration ElasticSearch
9. ✅ **application.properties** - Propriétés ES
10. ✅ **elasticsearch-settings.json** - Configuration analyseurs

### Capacités de recherche

- ✅ Recherche textuelle dans nom et description
- ✅ Filtre par prix minimum
- ✅ Filtre par prix maximum
- ✅ Filtre par intervalle de prix
- ✅ Pagination des résultats
- ✅ Tri personnalisable
- ✅ Insensible à la casse
- ✅ Performances optimisées

---

## 🚀 Prochaines étapes possibles

1. **Recherche avancée** : Ajouter des filtres par catégorie, disponibilité
2. **Autocomplétion** : Suggestions pendant la frappe
3. **Recherche fuzzy** : Tolérance aux fautes d'orthographe
4. **Highlighting** : Mettre en évidence les termes recherchés
5. **Facettes** : Compteurs par catégorie de prix, etc.
6. **Synonymes** : "téléphone" = "phone" = "smartphone"

La base est maintenant en place pour toutes ces améliorations ! 🎉

