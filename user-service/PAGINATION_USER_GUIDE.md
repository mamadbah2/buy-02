# Guide de Pagination - User Service

## Endpoint Paginé

### GET `/api/users/custom`

Récupère une liste paginée d'utilisateurs avec des liens HATEOAS pour la navigation.

#### Paramètres de requête

| Paramètre | Type | Défaut | Description |
|-----------|------|--------|-------------|
| `page` | int | 0 | Numéro de la page (commence à 0) |
| `size` | int | 10 | Nombre d'éléments par page |
| `sortBy` | String | "id" | Champ pour le tri (ex: name, email, id) |
| `sortDirection` | String | "asc" | Direction du tri: `asc` ou `desc` |

#### Exemples de requêtes

```bash
# Page par défaut (page 0, size 10, tri par id asc)
GET https://localhost:5050/api/users/custom

# Page spécifique
GET https://localhost:5050/api/users/custom?page=2&size=5

# Tri par nom en ordre décroissant
GET https://localhost:5050/api/users/custom?sortBy=name&sortDirection=desc

# Combinaison de tous les paramètres
GET https://localhost:5050/api/users/custom?page=1&size=20&sortBy=email&sortDirection=asc
```

#### Réponse JSON

```json
{
  "_embedded": {
    "userResponseList": [
      {
        "id": "123",
        "name": "John Doe",
        "email": "john@example.com",
        "role": "USER",
        "avatar": null,
        "_links": {
          "self": {
            "href": "https://localhost:5050/api/users/123/custom"
          }
        }
      },
      {
        "id": "456",
        "name": "Jane Smith",
        "email": "jane@example.com",
        "role": "ADMIN",
        "avatar": null,
        "_links": {
          "self": {
            "href": "https://localhost:5050/api/users/456/custom"
          }
        }
      }
    ]
  },
  "_links": {
    "self": {
      "href": "https://localhost:5050/api/users/custom?page=1&size=10&sortBy=id&sortDirection=asc"
    },
    "first": {
      "href": "https://localhost:5050/api/users/custom?page=0&size=10&sortBy=id&sortDirection=asc"
    },
    "prev": {
      "href": "https://localhost:5050/api/users/custom?page=0&size=10&sortBy=id&sortDirection=asc"
    },
    "next": {
      "href": "https://localhost:5050/api/users/custom?page=2&size=10&sortBy=id&sortDirection=asc"
    },
    "last": {
      "href": "https://localhost:5050/api/users/custom?page=9&size=10&sortBy=id&sortDirection=asc"
    }
  }
}
```

## Liens HATEOAS

La réponse contient les liens suivants pour faciliter la navigation :

- **self** : Page courante
- **first** : Première page
- **prev** : Page précédente (seulement si disponible)
- **next** : Page suivante (seulement si disponible)
- **last** : Dernière page

## Endpoint Sans Pagination

### GET `/api/users`

Pour les cas où vous avez besoin de tous les utilisateurs sans pagination (ex: pour les autres microservices).

```bash
GET https://localhost:5050/api/users
```

#### Réponse JSON

```json
[
  {
    "id": "123",
    "name": "John Doe",
    "email": "john@example.com",
    "role": "USER",
    "avatar": null
  },
  {
    "id": "456",
    "name": "Jane Smith",
    "email": "jane@example.com",
    "role": "ADMIN",
    "avatar": null
  }
]
```

## Notes Importantes

1. **Numérotation des pages** : Les pages commencent à 0 (première page = 0)
2. **Taille maximale** : Aucune limite définie, mais recommandé de ne pas dépasser 100 éléments par page
3. **Champs triables** : id, name, email, role (attention à la casse)
4. **Performance** : Utilisez la pagination pour les grandes collections pour optimiser les performances

