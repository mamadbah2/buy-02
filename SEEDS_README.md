# 🌱 Seeds - Données Fictives

Ce projet utilise **DataFaker** pour générer automatiquement des données fictives au démarrage des microservices.

## 📦 ProductSeed (product-service)

### Fonctionnalités
- Génère **500 produits fictifs** avec des données variées
- **12 catégories** de produits : electronics, clothing, food, books, furniture, sports, beauty, toys, home, garden, automotive, pets
- Données aléatoires réalistes :
  - Nom du produit adapté à sa catégorie
  - Description (10-25 mots)
  - Prix (5€ - 10 000€)
  - Quantité en stock (0-1000 unités)
  - UserId (UUID)

### Ordre d'exécution
- **@Order(1)** - S'exécute en premier

### Activation
Le seed s'active automatiquement si la base de données MongoDB est vide :
```java
if (productRepo.count() == 0) {
    seedProducts();
}
```

---

## 🖼️ MediaSeeding (media-service)

### Fonctionnalités
- Récupère automatiquement les produits depuis **product-service**
- Génère **2-5 images par produit** via [Picsum Photos](https://picsum.photos/)
- Images adaptées aux catégories de produits
- Formats variés : carré (600x600), paysage (800x600), portrait (600x800)

### Ordre d'exécution
- **@Order(2)** - S'exécute après ProductSeed

### URLs des images générées
Les images utilisent Picsum Photos avec des seeds uniques :
```
https://picsum.photos/seed/{hash}/{width}/{height}
```

Exemples :
- `https://picsum.photos/seed/12345/800/600` (paysage)
- `https://picsum.photos/seed/67890/600/600` (carré)
- `https://picsum.photos/seed/24680/600/800` (portrait)

### Correspondance produit-catégorie
Le seed analyse le nom du produit pour déterminer sa catégorie :
- **electronics** : smartphone, ordinateur, tablette
- **clothing** : shirt, pantalon, robe, veste
- **furniture** : canapé, table, chaise, lit
- **sports** : ballon, vélo, raquette
- **beauty** : parfum, crème, shampooing
- **toys** : jouet, peluche, puzzle

---

## 🚀 Utilisation

### Prérequis
1. **MongoDB** doit être démarré
2. **product-service** sur le port **8082** (ou variable d'environnement `PRODUCT_SERVICE_URL`)
3. **media-service** sur le port configuré

### Démarrage automatique

#### Option 1 : Démarrage séquentiel (recommandé)
```bash
# 1. Démarrer product-service en premier
cd product-service
./mvnw spring-boot:run

# 2. Attendre que product-service soit prêt (quelques secondes)

# 3. Démarrer media-service
cd ../media-service
./mvnw spring-boot:run
```

#### Option 2 : Via docker-compose
```bash
docker-compose up
```

### Vérification des données

#### Produits
```bash
# Récupérer tous les produits
curl http://localhost:8082/api/products

# Compter les produits
curl http://localhost:8082/api/products | jq length
```

#### Médias
```bash
# Récupérer les médias d'un produit
curl http://localhost:5050/api/media/product/{productId}
```

---

## 🎨 Personnalisation

### Modifier le nombre de produits
Dans `ProductSeed.java` :
```java
for (int i = 0; i < 500; i++) { // Changer 500 par le nombre voulu
```

### Modifier le nombre d'images par produit
Dans `MediaSeeding.java` :
```java
int imageCount = faker.number().numberBetween(2, 6); // Changer la plage
```

### Ajouter une nouvelle catégorie
1. Dans `ProductSeed.java`, ajouter la catégorie :
```java
private String getRandomCategory() {
    String[] categories = {
        "electronics", "clothing", ..., "nouvelle_categorie"
    };
}
```

2. Dans `generateProductName()`, ajouter le case :
```java
case "nouvelle_categorie" -> faker.options().option(
    "Produit A",
    "Produit B"
);
```

---

## ⚙️ Configuration avancée

### Variables d'environnement

#### media-service
```bash
# URL du product-service (par défaut: http://localhost:8082)
export PRODUCT_SERVICE_URL=http://product-service:8082
```

### Mode production
Pour désactiver les seeds en production, commentez `@Component` :
```java
// @Component
@RequiredArgsConstructor
public class ProductSeed implements CommandLineRunner {
```

Ou utilisez un profil Spring :
```java
@Component
@Profile("dev") // Ne s'active qu'en mode dev
public class ProductSeed implements CommandLineRunner {
```

---

## 📊 Statistiques

Avec la configuration par défaut :
- **500 produits** créés
- **~1500-2500 images** générées (2-5 par produit)
- **12 catégories** différentes
- Temps de génération : ~5-10 secondes

---

## 🐛 Dépannage

### Les produits ne sont pas créés
- Vérifier que MongoDB est démarré
- Vérifier que la base n'est pas déjà remplie (supprimez la collection `products`)

### Les médias ne sont pas créés
- Vérifier que product-service est accessible sur port 8082
- Vérifier les logs de media-service pour les erreurs de connexion
- S'assurer que product-service a fini de créer les produits

### Erreur "Connection refused"
```
❌ Erreur lors de la récupération des produits: Connection refused
```
→ product-service n'est pas démarré ou pas accessible. Démarrez-le en premier.

---

## 🔗 Liens utiles

- [DataFaker Documentation](https://www.datafaker.net/)
- [Picsum Photos](https://picsum.photos/)
- [Lorem Picsum - Documentation](https://picsum.photos/)

