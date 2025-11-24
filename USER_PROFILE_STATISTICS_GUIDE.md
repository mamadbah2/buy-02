# Guide des Statistiques de Profil Utilisateur

Ce guide explique comment utiliser les nouveaux endpoints de statistiques pour afficher les informations de profil des utilisateurs, incluant les produits les plus achetés, les meilleures ventes (pour les vendeurs), et le montant total dépensé.

## Architecture

Le système de statistiques de profil utilisateur a été implémenté dans le `order-service` avec les composants suivants :

### DTOs créés

1. **ProductStatisticsDto** - Contient les statistiques d'un produit :
   - `productId` : Identifiant du produit
   - `productName` : Nom du produit
   - `totalQuantity` : Quantité totale vendue/achetée
   - `totalRevenue` : Revenu total généré
   - `orderCount` : Nombre de commandes contenant ce produit

2. **UserProfileStatisticsDto** - Contient les statistiques complètes d'un utilisateur :
   - `userId` : Identifiant de l'utilisateur
   - `totalSpent` : Montant total dépensé
   - `totalOrders` : Nombre total de commandes
   - `mostPurchasedProducts` : Liste des produits les plus achetés
   - `bestSellingProducts` : Liste des produits les mieux vendus (si vendeur)

## Endpoints disponibles

### 1. Obtenir toutes les statistiques d'un utilisateur

**Endpoint :** `GET /api/orders/statistics/user/{userId}`

Retourne un profil statistique complet incluant :
- Le montant total dépensé
- Le nombre total de commandes
- Les 5 produits les plus achetés
- Les 5 produits les mieux vendus (si l'utilisateur est vendeur)

**Exemple de requête :**
```bash
curl -X GET "http://localhost:8084/api/orders/statistics/user/user123" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Exemple de réponse :**
```json
{
  "userId": "user123",
  "totalSpent": 1500.50,
  "totalOrders": 25,
  "mostPurchasedProducts": [
    {
      "productId": "prod1",
      "productName": "Laptop Dell",
      "totalQuantity": 3,
      "totalRevenue": 3000.00,
      "orderCount": 3
    },
    {
      "productId": "prod2",
      "productName": "Mouse Logitech",
      "totalQuantity": 5,
      "totalRevenue": 150.00,
      "orderCount": 2
    }
  ],
  "bestSellingProducts": [
    {
      "productId": "prod10",
      "productName": "Keyboard Mechanical",
      "totalQuantity": 50,
      "totalRevenue": 5000.00,
      "orderCount": 45
    }
  ]
}
```

### 2. Obtenir les produits les plus achetés par un utilisateur

**Endpoint :** `GET /api/orders/statistics/user/{userId}/most-purchased?limit={limit}`

Retourne la liste des produits les plus achetés par un utilisateur, triés par quantité décroissante.

**Paramètres :**
- `userId` : Identifiant de l'utilisateur (path parameter)
- `limit` : Nombre de produits à retourner (query parameter, défaut: 5)

**Exemple de requête :**
```bash
curl -X GET "http://localhost:8084/api/orders/statistics/user/user123/most-purchased?limit=10" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Exemple de réponse :**
```json
[
  {
    "productId": "prod1",
    "productName": "Laptop Dell",
    "totalQuantity": 3,
    "totalRevenue": 3000.00,
    "orderCount": 3
  },
  {
    "productId": "prod2",
    "productName": "Mouse Logitech",
    "totalQuantity": 5,
    "totalRevenue": 150.00,
    "orderCount": 2
  }
]
```

### 3. Obtenir les produits les mieux vendus d'un vendeur

**Endpoint :** `GET /api/orders/statistics/seller/{sellerId}/best-selling?limit={limit}`

Retourne la liste des produits les mieux vendus pour un vendeur spécifique, triés par revenu décroissant.

**Paramètres :**
- `sellerId` : Identifiant du vendeur (path parameter)
- `limit` : Nombre de produits à retourner (query parameter, défaut: 5)

**Exemple de requête :**
```bash
curl -X GET "http://localhost:8084/api/orders/statistics/seller/seller123/best-selling?limit=10" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Exemple de réponse :**
```json
[
  {
    "productId": "prod10",
    "productName": "Keyboard Mechanical",
    "totalQuantity": 50,
    "totalRevenue": 5000.00,
    "orderCount": 45
  },
  {
    "productId": "prod11",
    "productName": "Monitor 4K",
    "totalQuantity": 20,
    "totalRevenue": 8000.00,
    "orderCount": 20
  }
]
```

### 4. Obtenir le montant total dépensé par un utilisateur

**Endpoint :** `GET /api/orders/statistics/user/{userId}/total-spent`

Retourne le montant total dépensé par un utilisateur (exclut les commandes en statut "CART").

**Paramètres :**
- `userId` : Identifiant de l'utilisateur (path parameter)

**Exemple de requête :**
```bash
curl -X GET "http://localhost:8084/api/orders/statistics/user/user123/total-spent" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Exemple de réponse :**
```json
1500.50
```

## Notes importantes

### Filtrage des commandes
- Toutes les statistiques **excluent** les commandes avec le statut "CART" (panier en cours)
- Seules les commandes finalisées sont comptabilisées

### Performance
- Les endpoints utilisent des caches avec un max-age de 300 secondes (5 minutes)
- Pour les vendeurs avec beaucoup de produits, envisagez de limiter le nombre de résultats retournés

### Sécurité
- Ces endpoints nécessitent une authentification JWT valide
- Assurez-vous que l'utilisateur a le droit d'accéder aux statistiques demandées

## Intégration Frontend

### Exemple d'affichage du profil utilisateur en Angular

```typescript
// user-profile.component.ts
import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-user-profile',
  templateUrl: './user-profile.component.html'
})
export class UserProfileComponent implements OnInit {
  statistics: any;
  userId: string = 'current-user-id'; // À récupérer depuis le service d'authentification

  constructor(private http: HttpClient) {}

  ngOnInit() {
    this.loadStatistics();
  }

  loadStatistics() {
    this.http.get(`http://localhost:8084/api/orders/statistics/user/${this.userId}`)
      .subscribe(data => {
        this.statistics = data;
      });
  }
}
```

```html
<!-- user-profile.component.html -->
<div class="user-profile">
  <h2>Mon Profil</h2>
  
  <div class="stats-summary">
    <div class="stat-card">
      <h3>Montant Total Dépensé</h3>
      <p>{{ statistics?.totalSpent | currency:'EUR' }}</p>
    </div>
    <div class="stat-card">
      <h3>Nombre de Commandes</h3>
      <p>{{ statistics?.totalOrders }}</p>
    </div>
  </div>

  <div class="most-purchased">
    <h3>Mes Produits Favoris</h3>
    <ul>
      <li *ngFor="let product of statistics?.mostPurchasedProducts">
        {{ product.productName }} - {{ product.totalQuantity }} achetés
      </li>
    </ul>
  </div>

  <div class="best-selling" *ngIf="statistics?.bestSellingProducts?.length > 0">
    <h3>Mes Meilleures Ventes</h3>
    <ul>
      <li *ngFor="let product of statistics?.bestSellingProducts">
        {{ product.productName }} - {{ product.totalQuantity }} vendus ({{ product.totalRevenue | currency:'EUR' }})
      </li>
    </ul>
  </div>
</div>
```

## Tests

### Tester les endpoints avec curl

1. **Obtenir les statistiques complètes :**
```bash
curl -X GET "http://localhost:8084/api/orders/statistics/user/YOUR_USER_ID" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

2. **Obtenir les produits les plus achetés :**
```bash
curl -X GET "http://localhost:8084/api/orders/statistics/user/YOUR_USER_ID/most-purchased?limit=3" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

3. **Obtenir les meilleures ventes :**
```bash
curl -X GET "http://localhost:8084/api/orders/statistics/seller/YOUR_SELLER_ID/best-selling?limit=3" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

4. **Obtenir le total dépensé :**
```bash
curl -X GET "http://localhost:8084/api/orders/statistics/user/YOUR_USER_ID/total-spent" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

## Améliorations futures possibles

1. **Filtrage par période** : Ajouter des paramètres pour filtrer les statistiques par date (dernière semaine, dernier mois, etc.)
2. **Graphiques** : Créer des endpoints pour retourner des données formatées pour des graphiques (évolution des ventes/achats dans le temps)
3. **Comparaison** : Ajouter la possibilité de comparer les statistiques entre différentes périodes
4. **Cache avancé** : Implémenter un système de cache avec Redis pour améliorer les performances
5. **Agrégation MongoDB** : Utiliser les pipelines d'agrégation MongoDB pour optimiser les requêtes

## Dépannage

### Problème : Aucune statistique retournée
- Vérifiez que l'utilisateur a des commandes avec un statut autre que "CART"
- Assurez-vous que le service product-service est accessible et fonctionne correctement

### Problème : Produits avec nom "Unknown Product"
- Cela indique que le product-service n'a pas pu retourner les détails du produit
- Vérifiez les logs du order-service pour voir les erreurs de communication avec product-service

### Problème : Performances lentes
- Envisagez de réduire la limite de produits retournés
- Ajoutez des index sur la collection orders dans MongoDB
- Implémentez un système de cache

