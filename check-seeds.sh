#!/bin/bash

# Script pour vérifier les données générées par les seeds

echo "🔍 Vérification des seeds..."
echo ""

# Couleurs
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
PRODUCT_SERVICE_URL="http://localhost:8082"
MEDIA_SERVICE_URL="http://localhost:5050"

echo "📦 Vérification de product-service..."
PRODUCTS_COUNT=$(curl -s ${PRODUCT_SERVICE_URL}/api/products 2>/dev/null | jq length 2>/dev/null)

if [ -z "$PRODUCTS_COUNT" ]; then
    echo -e "${RED}❌ product-service n'est pas accessible${NC}"
    echo "   Vérifiez que le service tourne sur ${PRODUCT_SERVICE_URL}"
else
    echo -e "${GREEN}✅ ${PRODUCTS_COUNT} produits trouvés${NC}"

    # Afficher quelques exemples
    echo ""
    echo "📋 Exemples de produits :"
    curl -s ${PRODUCT_SERVICE_URL}/api/products 2>/dev/null | jq -r '.[:3] | .[] | "  - \(.name) (\(.price)€) - Stock: \(.quantity)"' 2>/dev/null
fi

echo ""
echo "🖼️  Vérification de media-service..."

if [ -z "$PRODUCTS_COUNT" ] || [ "$PRODUCTS_COUNT" -eq 0 ]; then
    echo -e "${YELLOW}⚠️  Impossible de vérifier les médias (pas de produits)${NC}"
else
    # Récupérer le premier produit
    FIRST_PRODUCT_ID=$(curl -s ${PRODUCT_SERVICE_URL}/api/products 2>/dev/null | jq -r '.[0].id' 2>/dev/null)

    if [ -n "$FIRST_PRODUCT_ID" ] && [ "$FIRST_PRODUCT_ID" != "null" ]; then
        MEDIA_COUNT=$(curl -s ${MEDIA_SERVICE_URL}/api/media/product/${FIRST_PRODUCT_ID} 2>/dev/null | jq length 2>/dev/null)

        if [ -z "$MEDIA_COUNT" ]; then
            echo -e "${RED}❌ media-service n'est pas accessible${NC}"
            echo "   Vérifiez que le service tourne sur ${MEDIA_SERVICE_URL}"
        else
            echo -e "${GREEN}✅ ${MEDIA_COUNT} images trouvées pour le premier produit${NC}"

            # Afficher les URLs des images
            echo ""
            echo "🖼️  URLs des images :"
            curl -s ${MEDIA_SERVICE_URL}/api/media/product/${FIRST_PRODUCT_ID} 2>/dev/null | jq -r '.[] | "  - \(.imageUrl)"' 2>/dev/null
        fi
    fi
fi

echo ""
echo "✨ Vérification terminée !"

