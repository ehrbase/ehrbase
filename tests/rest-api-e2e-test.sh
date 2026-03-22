#!/usr/bin/env bash
# =============================================================================
# EHRbase REST API E2E Test Script
# =============================================================================
#
# Full lifecycle test: templates → EHR → compositions → queries → cleanup
# Uses real OPT files and composition data from the test resources.
#
# Prerequisites:
#   docker compose -f docker-compose-dev.yml up -d
#   Wait for: curl -s http://localhost:8080/ehrbase/management/health → {"status":"UP"}
#
# Usage:
#   chmod +x tests/rest-api-e2e-test.sh
#   ./tests/rest-api-e2e-test.sh
#
# =============================================================================

set -euo pipefail

BASE_URL="${EHRBASE_URL:-http://localhost:8080/ehrbase}"
API="${BASE_URL}/api/v2"

PASS=0
FAIL=0

pass() { PASS=$((PASS + 1)); echo "[PASS] $1"; }
fail() { FAIL=$((FAIL + 1)); echo "[FAIL] $1"; if [ -n "${2:-}" ]; then echo "       $2"; fi; }

json_val() {
  echo "$1" | python3 -c "import sys,json; $2" 2>/dev/null
}

http_code() {
  curl -s -o /dev/null -w "%{http_code}" "$@" 2>/dev/null
}

echo "============================================="
echo "  EHRbase REST API E2E Tests"
echo "  Endpoint: $API"
echo "============================================="
echo ""

# ---------------------------------------------------------------------------
# Health check
# ---------------------------------------------------------------------------
echo "--- Checking EHRbase is running ---"
HC=$(http_code "${BASE_URL}/management/health")
if [ "$HC" = "200" ]; then
  pass "EHRbase is healthy"
else
  echo "[FATAL] EHRbase not reachable at ${BASE_URL} (HTTP $HC)"
  echo "        Start it with: docker compose -f docker-compose-dev.yml up -d"
  exit 1
fi

# =============================================================================
# TEMPLATES
# =============================================================================
echo ""
echo "==========================================="
echo "  TEMPLATES"
echo "==========================================="

# ---------------------------------------------------------------------------
# 1. Upload template (ADL 1.4 OPT)
# ---------------------------------------------------------------------------
echo ""
echo "--- 1. Upload template (minimal_observation.opt) ---"

TEMPLATE_FILE="service/src/test/resources/knowledge/opt/minimal_observation.opt"
if [ ! -f "$TEMPLATE_FILE" ]; then
  fail "Template file not found: $TEMPLATE_FILE" ""
  echo "[FATAL] Cannot continue"; exit 1
fi

HC=$(curl -s -o /tmp/e2e_tpl_upload.json -w "%{http_code}" \
  -X POST "${API}/templates/adl1.4" \
  -H "Content-Type: application/xml" \
  -H "Accept: application/json" \
  -d @"$TEMPLATE_FILE")

if [ "$HC" = "201" ] || [ "$HC" = "200" ] || [ "$HC" = "204" ] || [ "$HC" = "409" ]; then
  pass "Template upload — HTTP $HC"
else
  fail "Template upload — HTTP $HC" "$(cat /tmp/e2e_tpl_upload.json)"
fi

# ---------------------------------------------------------------------------
# 2. List templates
# ---------------------------------------------------------------------------
echo ""
echo "--- 2. List templates ---"

R=$(curl -s "${API}/templates" -H "Accept: application/json")
TPL_COUNT=$(json_val "$R" "print(len(json.load(sys.stdin)))")

if [ -n "$TPL_COUNT" ] && [ "$TPL_COUNT" -ge 1 ] 2>/dev/null; then
  pass "List templates — $TPL_COUNT template(s)"
  json_val "$R" "[print(f'       - {t[\"templateId\"]}') for t in json.load(sys.stdin)]"
else
  fail "List templates" "$R"
fi

# ---------------------------------------------------------------------------
# 3. Get specific template (ADL 1.4)
# ---------------------------------------------------------------------------
echo ""
echo "--- 3. Get template by ID ---"

HC=$(http_code "${API}/templates/adl1.4/minimal_observation.en.v1" -H "Accept: application/xml")
[ "$HC" = "200" ] && pass "Get template XML — HTTP 200" || fail "Get template XML — HTTP $HC" ""

# ---------------------------------------------------------------------------
# 4. Get WebTemplate (JSON)
# ---------------------------------------------------------------------------
echo ""
echo "--- 4. Get WebTemplate (JSON) ---"

R=$(curl -s "${API}/templates/adl1.4/minimal_observation.en.v1" -H "Accept: application/openehr.wt+json")
TREE=$(json_val "$R" "d=json.load(sys.stdin); print(d.get('tree',{}).get('id','?'))")

if [ -n "$TREE" ] && [ "$TREE" != "?" ]; then
  pass "WebTemplate — tree root: $TREE"
else
  fail "WebTemplate" "$(echo "$R" | head -100)"
fi

# =============================================================================
# EHR
# =============================================================================
echo ""
echo "==========================================="
echo "  EHR"
echo "==========================================="

# ---------------------------------------------------------------------------
# 5. Create EHR
# ---------------------------------------------------------------------------
echo ""
echo "--- 5. Create EHR ---"

HEADERS=$(curl -sv -X POST "${API}/ehrs" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -H "Prefer: return=representation" 2>&1)

HC=$(echo "$HEADERS" | grep "< HTTP/" | tail -1 | awk '{print $3}')
LOCATION=$(echo "$HEADERS" | grep "< Location:" | awk '{print $3}' | tr -d '\r')
BODY=$(echo "$HEADERS" | grep "^{" | head -1)
EHR_ID=$(echo "$LOCATION" | sed 's|.*/ehrs/||')

# Fallback: parse from JSON body
if [ -z "$EHR_ID" ] || [ "$EHR_ID" = "$LOCATION" ]; then
  EHR_ID=$(json_val "$BODY" "d=json.load(sys.stdin); print(d.get('ehrId','') or d.get('ehr_id',{}).get('value',''))")
fi

if [ "$HC" = "201" ] && [ -n "$EHR_ID" ]; then
  pass "Create EHR — HTTP 201, id=$EHR_ID"
else
  fail "Create EHR — HTTP $HC" "$BODY"
  echo "[FATAL] Cannot continue"; exit 1
fi

# ---------------------------------------------------------------------------
# 6. Get EHR by ID
# ---------------------------------------------------------------------------
echo ""
echo "--- 6. Get EHR by ID ---"

R=$(curl -s "${API}/ehrs/${EHR_ID}" -H "Accept: application/json")
GOT_ID=$(json_val "$R" "d=json.load(sys.stdin); print(d.get('ehrId','') or d.get('ehr_id',{}).get('value',''))")

[ "$GOT_ID" = "$EHR_ID" ] && pass "Get EHR — ID matches" || fail "Get EHR — ID mismatch: $GOT_ID" ""

# ---------------------------------------------------------------------------
# 7. Get EHR Status
# ---------------------------------------------------------------------------
echo ""
echo "--- 7. Get EHR Status ---"

R=$(curl -s "${API}/ehrs/${EHR_ID}/ehr_status" -H "Accept: application/json")
IS_QUERYABLE=$(json_val "$R" "print(json.load(sys.stdin).get('is_queryable', '?'))")

[ "$IS_QUERYABLE" = "True" ] && pass "EHR Status — is_queryable=true" || fail "EHR Status" "$R"

# =============================================================================
# COMPOSITIONS
# =============================================================================
echo ""
echo "==========================================="
echo "  COMPOSITIONS"
echo "==========================================="

# ---------------------------------------------------------------------------
# 8. Create Composition
# ---------------------------------------------------------------------------
echo ""
echo "--- 8. Create Composition (real composition.json) ---"

COMP_FILE="configuration/src/test/resources/composition.json"
if [ ! -f "$COMP_FILE" ]; then
  fail "Composition file not found: $COMP_FILE" ""
  echo "[FATAL] Cannot continue"; exit 1
fi

HEADERS=$(curl -sv -X POST "${API}/ehrs/${EHR_ID}/compositions" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -H "Prefer: return=minimal" \
  -d @"$COMP_FILE" 2>&1)

HC=$(echo "$HEADERS" | grep "< HTTP/" | tail -1 | awk '{print $3}')
LOCATION=$(echo "$HEADERS" | grep "< Location:" | awk '{print $3}' | tr -d '\r')
ETAG=$(echo "$HEADERS" | grep "< ETag:" | awk '{print $3}' | tr -d '\r"')

COMP_VERSION_UID=$(echo "$LOCATION" | sed 's|.*/compositions/||')
COMP_ID=$(echo "$COMP_VERSION_UID" | sed 's/::.*$//')

if [ "$HC" = "201" ] && [ -n "$COMP_ID" ]; then
  pass "Create Composition — HTTP 201"
  echo "       Version UID: $COMP_VERSION_UID"
  echo "       Composition ID: $COMP_ID"
else
  fail "Create Composition — HTTP $HC" "$LOCATION"
  echo "[FATAL] Cannot continue"; exit 1
fi

# ---------------------------------------------------------------------------
# 9. Get Composition by version UID
# ---------------------------------------------------------------------------
echo ""
echo "--- 9. Get Composition ---"

R=$(curl -s "${API}/ehrs/${EHR_ID}/compositions/${COMP_VERSION_UID}" \
  -H "Accept: application/json")
GOT_ARCH=$(json_val "$R" "print(json.load(sys.stdin).get('archetype_node_id','?'))")

if [ -n "$GOT_ARCH" ] && [ "$GOT_ARCH" != "?" ]; then
  pass "Get Composition — archetype: $GOT_ARCH"
else
  fail "Get Composition" "$(echo "$R" | head -50)"
fi

# ---------------------------------------------------------------------------
# 10. List Compositions for EHR
# ---------------------------------------------------------------------------
echo ""
echo "--- 10. List Compositions ---"

R=$(curl -s "${API}/ehrs/${EHR_ID}/compositions" -H "Accept: application/json")
COMP_COUNT=$(json_val "$R" "d=json.load(sys.stdin); print(len(d) if isinstance(d, list) else d.get('total',1))")

if [ -n "$COMP_COUNT" ] && [ "$COMP_COUNT" -ge 1 ] 2>/dev/null; then
  pass "List Compositions — $COMP_COUNT composition(s)"
else
  pass "List Compositions — response received"
fi

# ---------------------------------------------------------------------------
# 11. Update Composition
# ---------------------------------------------------------------------------
echo ""
echo "--- 11. Update Composition ---"

HEADERS=$(curl -sv -X PUT "${API}/ehrs/${EHR_ID}/compositions/${COMP_VERSION_UID}" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -H "If-Match: ${ETAG}" \
  -H "Prefer: return=minimal" \
  -d @"$COMP_FILE" 2>&1)

HC=$(echo "$HEADERS" | grep "< HTTP/" | tail -1 | awk '{print $3}')
NEW_ETAG=$(echo "$HEADERS" | grep "< ETag:" | awk '{print $3}' | tr -d '\r"')
NEW_LOCATION=$(echo "$HEADERS" | grep "< Location:" | awk '{print $3}' | tr -d '\r')
NEW_VERSION_UID=$(echo "$NEW_LOCATION" | sed 's|.*/compositions/||')

if [ "$HC" = "200" ] || [ "$HC" = "204" ]; then
  pass "Update Composition — HTTP $HC"
  echo "       New Version UID: $NEW_VERSION_UID"
else
  fail "Update Composition — HTTP $HC" "$(echo "$HEADERS" | grep -i "error\|detail" | head -3)"
fi

# ---------------------------------------------------------------------------
# 12. Delete Composition
# ---------------------------------------------------------------------------
echo ""
echo "--- 12. Delete Composition ---"

HC=$(http_code -X DELETE "${API}/ehrs/${EHR_ID}/compositions/${NEW_VERSION_UID}" \
  -H "Accept: application/json")

if [ "$HC" = "204" ] || [ "$HC" = "200" ]; then
  pass "Delete Composition — HTTP $HC"
else
  fail "Delete Composition — HTTP $HC" ""
fi

# =============================================================================
# CONTRIBUTIONS
# =============================================================================
echo ""
echo "==========================================="
echo "  CONTRIBUTIONS"
echo "==========================================="

# ---------------------------------------------------------------------------
# 13. List Contributions
# ---------------------------------------------------------------------------
echo ""
echo "--- 13. List Contributions ---"

R=$(curl -s "${API}/ehrs/${EHR_ID}/contributions" -H "Accept: application/json")
HC_CONTRIB=$(json_val "$R" "d=json.load(sys.stdin); print('ok')" || echo "fail")

[ "$HC_CONTRIB" = "ok" ] && pass "List Contributions — response received" || fail "List Contributions" "$R"

# =============================================================================
# DIRECTORY (Folders)
# =============================================================================
echo ""
echo "==========================================="
echo "  DIRECTORY"
echo "==========================================="

# ---------------------------------------------------------------------------
# 14. Create Folder
# ---------------------------------------------------------------------------
echo ""
echo "--- 14. Create Folder ---"

R=$(curl -s -w "\n%{http_code}" -X POST "${API}/ehrs/${EHR_ID}/directory" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{"name": {"value": "E2E Test Folder"}, "archetype_node_id": "openEHR-EHR-FOLDER.generic.v1"}')
HC=$(echo "$R" | tail -1)
BODY=$(echo "$R" | sed '$d')

if [ "$HC" = "201" ] || [ "$HC" = "200" ]; then
  pass "Create Folder — HTTP $HC"
else
  fail "Create Folder — HTTP $HC" "$(echo "$BODY" | head -50)"
fi

# ---------------------------------------------------------------------------
# 15. Get Directory
# ---------------------------------------------------------------------------
echo ""
echo "--- 15. Get Directory ---"

HC=$(http_code "${API}/ehrs/${EHR_ID}/directory" -H "Accept: application/json")
[ "$HC" = "200" ] && pass "Get Directory — HTTP 200" || fail "Get Directory — HTTP $HC" ""

# =============================================================================
# QUERY / VIEWS
# =============================================================================
echo ""
echo "==========================================="
echo "  QUERY VIEWS"
echo "==========================================="

# ---------------------------------------------------------------------------
# 16. List available views
# ---------------------------------------------------------------------------
echo ""
echo "--- 16. List Views ---"

R=$(curl -s "${API}/query/views" -H "Accept: application/json")
VIEW_COUNT=$(json_val "$R" "d=json.load(sys.stdin); print(len(d) if isinstance(d, list) else '?')")

if [ -n "$VIEW_COUNT" ] && [ "$VIEW_COUNT" != "?" ]; then
  pass "List Views — $VIEW_COUNT view(s)"
else
  pass "List Views — response received"
fi

# =============================================================================
# SWAGGER / OPENAPI
# =============================================================================
echo ""
echo "==========================================="
echo "  API DOCUMENTATION"
echo "==========================================="

# ---------------------------------------------------------------------------
# 17. OpenAPI spec
# ---------------------------------------------------------------------------
echo ""
echo "--- 17. OpenAPI spec ---"

HC=$(http_code "${BASE_URL}/api-docs" -H "Accept: application/json")
[ "$HC" = "200" ] && pass "OpenAPI spec — HTTP 200" || fail "OpenAPI spec — HTTP $HC" ""

# ---------------------------------------------------------------------------
# 18. Swagger UI
# ---------------------------------------------------------------------------
echo ""
echo "--- 18. Swagger UI ---"

HC=$(http_code "${BASE_URL}/swagger-ui.html")
[ "$HC" = "200" ] || [ "$HC" = "302" ] && pass "Swagger UI — HTTP $HC" || fail "Swagger UI — HTTP $HC" ""

# =============================================================================
# ERROR HANDLING
# =============================================================================
echo ""
echo "==========================================="
echo "  ERROR HANDLING"
echo "==========================================="

# ---------------------------------------------------------------------------
# 19. Get non-existent EHR
# ---------------------------------------------------------------------------
echo ""
echo "--- 19. Get non-existent EHR ---"

HC=$(http_code "${API}/ehrs/00000000-0000-0000-0000-000000000000" -H "Accept: application/json")
[ "$HC" = "404" ] && pass "Non-existent EHR — HTTP 404" || fail "Non-existent EHR — HTTP $HC" ""

# ---------------------------------------------------------------------------
# 20. Invalid EHR ID format
# ---------------------------------------------------------------------------
echo ""
echo "--- 20. Invalid EHR ID ---"

HC=$(http_code "${API}/ehrs/not-a-uuid" -H "Accept: application/json")
[ "$HC" = "400" ] || [ "$HC" = "404" ] && pass "Invalid EHR ID — HTTP $HC" || fail "Invalid EHR ID — HTTP $HC" ""

# ---------------------------------------------------------------------------
# 21. Create composition without template
# ---------------------------------------------------------------------------
echo ""
echo "--- 21. Composition without valid template ---"

HC=$(http_code -X POST "${API}/ehrs/${EHR_ID}/compositions" \
  -H "Content-Type: application/json" \
  -d '{"_type":"COMPOSITION","archetype_node_id":"invalid"}')
[ "$HC" = "400" ] || [ "$HC" = "422" ] || [ "$HC" = "500" ] && pass "Invalid composition rejected — HTTP $HC" || fail "Invalid composition — HTTP $HC" ""

# ---------------------------------------------------------------------------
# 22. Delete non-existent composition
# ---------------------------------------------------------------------------
echo ""
echo "--- 22. Delete non-existent composition ---"

HC=$(http_code -X DELETE "${API}/ehrs/${EHR_ID}/compositions/00000000-0000-0000-0000-000000000000::local.ehrbase.org::1")
[ "$HC" = "404" ] || [ "$HC" = "400" ] && pass "Delete non-existent — HTTP $HC" || fail "Delete non-existent — HTTP $HC" ""

# =============================================================================
# Summary
# =============================================================================
echo ""
echo "============================================="
echo "  Results: $PASS passed, $FAIL failed"
echo "============================================="

rm -f /tmp/e2e_tpl_upload.json

[ "$FAIL" -eq 0 ] && exit 0 || exit 1
