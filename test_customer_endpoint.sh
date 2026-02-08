#!/bin/bash
# Script para probar el endpoint de clientes con parámetros PrimeNG
# Asegúrate de que el servidor esté corriendo antes de ejecutar este script

BASE_URL="http://localhost:8080/api/tenant-customers/tenant"
TENANT_ID=1

echo "========================================"
echo " PRUEBAS DE ENDPOINT - CLIENTES"
echo "========================================"
echo ""

echo "[TEST 1] Obtener primera página sin parámetros"
echo "URL: $BASE_URL/$TENANT_ID"
curl -s -X GET "$BASE_URL/$TENANT_ID" -H "Content-Type: application/json" | jq .
echo ""
echo ""
sleep 1

echo "[TEST 2] Paginación - página 0, 5 registros"
echo "URL: $BASE_URL/$TENANT_ID?page=0&size=5"
curl -s -X GET "$BASE_URL/$TENANT_ID?page=0&size=5" -H "Content-Type: application/json" | jq .
echo ""
echo ""
sleep 1

echo "[TEST 3] Ordenamiento - por nombre ascendente"
echo "URL: $BASE_URL/$TENANT_ID?sort=name,asc"
curl -s -X GET "$BASE_URL/$TENANT_ID?sort=name,asc" -H "Content-Type: application/json" | jq .
echo ""
echo ""
sleep 1

echo "[TEST 4] Ordenamiento - por email descendente"
echo "URL: $BASE_URL/$TENANT_ID?sort=email,desc"
curl -s -X GET "$BASE_URL/$TENANT_ID?sort=email,desc" -H "Content-Type: application/json" | jq .
echo ""
echo ""
sleep 1

echo "[TEST 5] Filtro por email - buscar \"gmail\""
echo "URL: $BASE_URL/$TENANT_ID?email=gmail"
curl -s -X GET "$BASE_URL/$TENANT_ID?email=gmail" -H "Content-Type: application/json" | jq .
echo ""
echo ""
sleep 1

echo "[TEST 6] Combinación completa - página 0, 20 registros, ordenar por createdAt desc, filtrar por email"
echo "URL: $BASE_URL/$TENANT_ID?page=0&size=20&sort=createdAt,desc&email=test"
curl -s -X GET "$BASE_URL/$TENANT_ID?page=0&size=20&sort=createdAt,desc&email=test" -H "Content-Type: application/json" | jq .
echo ""
echo ""
sleep 1

echo "[TEST 7] Sort inválido (debe usar orden por defecto)"
echo "URL: $BASE_URL/$TENANT_ID?sort=invalid-format"
curl -s -X GET "$BASE_URL/$TENANT_ID?sort=invalid-format" -H "Content-Type: application/json" | jq .
echo ""
echo ""
sleep 1

echo "[TEST 8] Page negativo (debe ajustarse a 0)"
echo "URL: $BASE_URL/$TENANT_ID?page=-1&size=10"
curl -s -X GET "$BASE_URL/$TENANT_ID?page=-1&size=10" -H "Content-Type: application/json" | jq .
echo ""
echo ""
sleep 1

echo "[TEST 9] Size excesivo (debe ajustarse a 10)"
echo "URL: $BASE_URL/$TENANT_ID?page=0&size=500"
curl -s -X GET "$BASE_URL/$TENANT_ID?page=0&size=500" -H "Content-Type: application/json" | jq .
echo ""
echo ""
sleep 1

echo "[TEST 10] Mapeo de campos - nombreCompleto"
echo "URL: $BASE_URL/$TENANT_ID?sort=nombreCompleto,asc"
curl -s -X GET "$BASE_URL/$TENANT_ID?sort=nombreCompleto,asc" -H "Content-Type: application/json" | jq .
echo ""
echo ""

echo "========================================"
echo " PRUEBAS COMPLETADAS"
echo "========================================"
echo ""
echo "Nota: Si algún test falla, verifica que:"
echo "1. El servidor esté corriendo en http://localhost:8080"
echo "2. Exista un tenant con ID = $TENANT_ID"
echo "3. Ese tenant tenga clientes registrados"
echo "4. Tengas instalado 'jq' para formatear JSON (opcional)"
echo ""
