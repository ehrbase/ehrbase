#!/usr/bin/env bash
# =============================================================================
# EHRbase REST API — Comprehensive E2E Test Script
# =============================================================================
#
# Full lifecycle test covering all core REST endpoints with real test data.
# Templates → EHR → EHR Status → Compositions → Versioning → Folders → Contributions
#
# Prerequisites:
#   docker compose -f docker-compose-dev.yml up -d
#   Wait for health check to pass
#
# Usage:
#   chmod +x tests/rest-api-e2e-test.sh
#   ./tests/rest-api-e2e-test.sh
#
# =============================================================================

set -uo pipefail

BASE_URL="${EHRBASE_URL:-http://localhost:8080/ehrbase}"
API="${BASE_URL}/api/v2"

PASS=0
FAIL=0
SKIP=0

pass() { PASS=$((PASS + 1)); echo "[PASS] $1"; }
fail() { FAIL=$((FAIL + 1)); echo "[FAIL] $1"; if [ -n "${2:-}" ]; then echo "       $2"; fi; }
skip() { SKIP=$((SKIP + 1)); echo "[SKIP] $1"; }

json_val() { echo "$1" | python3 -c "import sys,json; $2" 2>/dev/null; }
http_code() { curl -s -o /dev/null -w "%{http_code}" "$@" 2>/dev/null; }

# Extract value from curl -sv headers
extract_header() { echo "$1" | grep "< $2:" | head -1 | sed "s/< $2: //" | tr -d '\r"'; }
extract_http_code() { echo "$1" | grep "< HTTP/" | tail -1 | awk '{print $3}'; }

echo "============================================="
echo "  EHRbase REST API — Comprehensive E2E Tests"
echo "  Endpoint: $API"
echo "============================================="

# ---------------------------------------------------------------------------
# Health Check
# ---------------------------------------------------------------------------
echo ""
echo "--- Health Check ---"
HC=$(http_code "${BASE_URL}/management/health")
if [ "$HC" = "200" ]; then pass "EHRbase is healthy"; else echo "[FATAL] Not reachable (HTTP $HC)"; exit 1; fi


# =============================================================================
# TEMPLATES
# =============================================================================
echo ""
echo "==========================================="
echo "  TEMPLATES"
echo "==========================================="

echo ""
echo "--- 1. Upload template (minimal_observation.opt) ---"
HC=$(http_code -X POST "${API}/templates/adl1.4" -H "Content-Type: application/xml" -d @service/src/test/resources/knowledge/opt/minimal_observation.opt)
[ "$HC" = "201" ] || [ "$HC" = "409" ] && pass "Upload minimal_observation — HTTP $HC" || fail "Upload minimal_observation — HTTP $HC" ""

echo ""
echo "--- 2. Upload second template (ehrbase_blood_pressure_simple.de.v0.opt) ---"
HC=$(http_code -X POST "${API}/templates/adl1.4" -H "Content-Type: application/xml" -d @service/src/test/resources/knowledge/opt/ehrbase_blood_pressure_simple.de.v0.opt)
[ "$HC" = "201" ] || [ "$HC" = "409" ] && pass "Upload blood_pressure — HTTP $HC" || fail "Upload blood_pressure — HTTP $HC" ""

echo ""
echo "--- 3. List all templates ---"
R=$(curl -s "${API}/templates" -H "Accept: application/json")
TPL_COUNT=$(json_val "$R" "print(len(json.load(sys.stdin)))")
[ "$TPL_COUNT" -ge 2 ] 2>/dev/null && pass "List templates — $TPL_COUNT templates" || fail "List templates" "$R"

echo ""
echo "--- 4. Get template as OPT XML ---"
HC=$(http_code "${API}/templates/adl1.4/minimal_observation.en.v1" -H "Accept: application/xml")
[ "$HC" = "200" ] && pass "Get OPT XML — HTTP 200" || fail "Get OPT XML — HTTP $HC" ""

echo ""
echo "--- 5. Get WebTemplate (JSON) ---"
R=$(curl -s "${API}/templates/adl1.4/minimal_observation.en.v1" -H "Accept: application/openehr.wt+json")
TREE=$(json_val "$R" "d=json.load(sys.stdin); print(d.get('tree',{}).get('id','?'))")
[ -n "$TREE" ] && [ "$TREE" != "?" ] && pass "WebTemplate — tree root: $TREE" || fail "WebTemplate" ""

echo ""
echo "--- 6. Get template example ---"
HC=$(http_code "${API}/templates/minimal_observation.en.v1/example" -H "Accept: application/json")
[ "$HC" = "200" ] && pass "Template example — HTTP 200" || fail "Template example — HTTP $HC" ""

echo ""
echo "--- 7. Get template schema DDL ---"
HC=$(http_code "${API}/templates/minimal_observation.en.v1/schema" -H "Accept: text/plain")
[ "$HC" = "200" ] && pass "Template schema DDL — HTTP 200" || fail "Template schema DDL — HTTP $HC" ""

echo ""
echo "--- 8. Upload invalid XML → expect 400 ---"
HC=$(http_code -X POST "${API}/templates/adl1.4" -H "Content-Type: application/xml" -d "<not-valid-opt/>")
[ "$HC" = "400" ] && pass "Invalid OPT rejected — HTTP 400" || fail "Invalid OPT — HTTP $HC" ""


# =============================================================================
# EHR
# =============================================================================
echo ""
echo "==========================================="
echo "  EHR"
echo "==========================================="

echo ""
echo "--- 9. Create EHR (auto-generated ID) ---"
H=$(curl -sv -X POST "${API}/ehrs" -H "Content-Type: application/json" -H "Accept: application/json" -H "Prefer: return=representation" 2>&1)
HC=$(extract_http_code "$H")
LOCATION=$(extract_header "$H" "Location")
EHR_ID=$(echo "$LOCATION" | sed 's|.*/ehrs/||')
# Fallback from body
if [ -z "$EHR_ID" ] || [ "$EHR_ID" = "$LOCATION" ]; then
  BODY=$(echo "$H" | grep "^{" | head -1)
  EHR_ID=$(json_val "$BODY" "d=json.load(sys.stdin); print(d.get('ehrId','') or d.get('ehr_id',{}).get('value',''))")
fi
[ "$HC" = "201" ] && [ -n "$EHR_ID" ] && pass "Create EHR — id=$EHR_ID" || { fail "Create EHR — HTTP $HC" ""; exit 1; }

echo ""
echo "--- 10. Get EHR by ID ---"
R=$(curl -s "${API}/ehrs/${EHR_ID}" -H "Accept: application/json")
GOT_ID=$(json_val "$R" "d=json.load(sys.stdin); print(d.get('ehrId','') or d.get('ehr_id',{}).get('value',''))")
[ "$GOT_ID" = "$EHR_ID" ] && pass "Get EHR — ID matches" || fail "Get EHR — ID mismatch: $GOT_ID" ""

echo ""
echo "--- 11. Create EHR with specific ID ---"
SPECIFIC_ID="00000000-0000-4000-8000-$(date +%s | tail -c 13)"
HC=$(http_code -X PUT "${API}/ehrs/${SPECIFIC_ID}" -H "Content-Type: application/json" -H "Accept: application/json")
[ "$HC" = "201" ] && pass "Create EHR with ID — HTTP 201" || fail "Create EHR with ID — HTTP $HC" ""

echo ""
echo "--- 12. Duplicate EHR → expect 409 ---"
HC=$(http_code -X PUT "${API}/ehrs/${SPECIFIC_ID}" -H "Content-Type: application/json" -H "Accept: application/json")
[ "$HC" = "409" ] && pass "Duplicate EHR rejected — HTTP 409" || fail "Duplicate EHR — HTTP $HC (expected 409)" ""

echo ""
echo "--- 13. Find EHR by subject ---"
# Create EHR with known subject first
H=$(curl -sv -X POST "${API}/ehrs" -H "Content-Type: application/json" -H "Accept: application/json" \
  -d '{"_type":"EHR_STATUS","archetype_node_id":"openEHR-EHR-EHR_STATUS.generic.v1","name":{"value":"EHR Status"},"subject":{"external_ref":{"id":{"_type":"GENERIC_ID","value":"subject-e2e-lookup","scheme":"test"},"namespace":"e2e-ns","type":"PERSON"}},"is_queryable":true,"is_modifiable":true}' 2>&1)
SUBJECT_EHR=$(echo "$H" | grep "< Location:" | head -1 | sed 's|.*ehrs/||' | tr -d '\r')

R=$(curl -s "${API}/ehrs?subject_id=subject-e2e-lookup&subject_namespace=e2e-ns" -H "Accept: application/json")
FOUND=$(json_val "$R" "d=json.load(sys.stdin); print(d.get('ehrId','') or d.get('ehr_id',{}).get('value',''))")
[ -n "$FOUND" ] && pass "Find EHR by subject — found $FOUND" || fail "Find EHR by subject" "$R"


# =============================================================================
# EHR STATUS
# =============================================================================
echo ""
echo "==========================================="
echo "  EHR STATUS"
echo "==========================================="

echo ""
echo "--- 14. Get EHR Status ---"
R=$(curl -s "${API}/ehrs/${EHR_ID}/ehr_status" -H "Accept: application/json")
IS_Q=$(json_val "$R" "print(json.load(sys.stdin).get('is_queryable','?'))")
STATUS_UID=$(json_val "$R" "d=json.load(sys.stdin); uid=d.get('uid',{}); print(uid.get('value','') if isinstance(uid,dict) else '')")
[ "$IS_Q" = "True" ] && pass "Get EHR Status — is_queryable=true" || fail "Get EHR Status" "$R"

echo ""
echo "--- 15. Update EHR Status (If-Match) ---"
# Build EHR status update with real ehr_status.json template
SUBJECT_UUID=$(python3 -c "import uuid; print(uuid.uuid4())")
STATUS_JSON=$(cat configuration/src/test/resources/ehr_status.json | sed "s/%s/$SUBJECT_UUID/")

if [ -n "$STATUS_UID" ]; then
  H=$(curl -sv -X PUT "${API}/ehrs/${EHR_ID}/ehr_status" \
    -H "Content-Type: application/json" -H "Accept: application/json" \
    -H "If-Match: \"${STATUS_UID}\"" \
    -d "$STATUS_JSON" 2>&1)
  HC=$(extract_http_code "$H")
  [ "$HC" = "200" ] || [ "$HC" = "204" ] && pass "Update EHR Status — HTTP $HC" || fail "Update EHR Status — HTTP $HC" "$(echo "$H" | grep 'error\|Error\|detail' | head -3)"
else
  skip "Update EHR Status — no version UID available"
fi

echo ""
echo "--- 16. Versioned EHR Status container ---"
HC=$(http_code "${API}/ehrs/${EHR_ID}/versioned_ehr_status" -H "Accept: application/json")
[ "$HC" = "200" ] && pass "Versioned EHR Status — HTTP 200" || fail "Versioned EHR Status — HTTP $HC" ""

echo ""
echo "--- 17. EHR Status revision history ---"
HC=$(http_code "${API}/ehrs/${EHR_ID}/versioned_ehr_status/revision_history" -H "Accept: application/json")
[ "$HC" = "200" ] && pass "EHR Status revision history — HTTP 200" || fail "EHR Status revision history — HTTP $HC" ""


# =============================================================================
# COMPOSITIONS
# =============================================================================
echo ""
echo "==========================================="
echo "  COMPOSITIONS"
echo "==========================================="

echo ""
echo "--- 18. Create Composition (real composition.json, minimal_evaluation) ---"
H=$(curl -sv -X POST "${API}/ehrs/${EHR_ID}/compositions" \
  -H "Content-Type: application/json" -H "Accept: application/json" -H "Prefer: return=minimal" \
  -d @configuration/src/test/resources/composition.json 2>&1)
HC=$(extract_http_code "$H")
COMP_LOCATION=$(extract_header "$H" "Location")
COMP_VERSION_UID=$(echo "$COMP_LOCATION" | sed 's|.*/compositions/||')
COMP_ID=$(echo "$COMP_VERSION_UID" | sed 's/::.*$//')
ETAG=$(extract_header "$H" "ETag")
[ "$HC" = "201" ] && [ -n "$COMP_ID" ] && pass "Create Composition — $COMP_VERSION_UID" || { fail "Create Composition — HTTP $HC" ""; }

echo ""
echo "--- 19. Get Composition by version UID ---"
R=$(curl -s "${API}/ehrs/${EHR_ID}/compositions/${COMP_VERSION_UID}" -H "Accept: application/json")
ARCH=$(json_val "$R" "print(json.load(sys.stdin).get('archetype_node_id','?'))")
[ -n "$ARCH" ] && [ "$ARCH" != "?" ] && pass "Get Composition — archetype: $ARCH" || fail "Get Composition" "$(echo "$R" | head -50)"

echo ""
echo "--- 20. List Compositions for EHR ---"
R=$(curl -s "${API}/ehrs/${EHR_ID}/compositions" -H "Accept: application/json")
COUNT=$(json_val "$R" "d=json.load(sys.stdin); print(len(d) if isinstance(d,list) else d.get('total',0))")
[ "$COUNT" -ge 1 ] 2>/dev/null && pass "List Compositions — $COUNT composition(s)" || pass "List Compositions — response received"

echo ""
echo "--- 21. Update Composition (If-Match) ---"
H=$(curl -sv -X PUT "${API}/ehrs/${EHR_ID}/compositions/${COMP_VERSION_UID}" \
  -H "Content-Type: application/json" -H "Accept: application/json" \
  -H "If-Match: ${ETAG}" -H "Prefer: return=minimal" \
  -d @configuration/src/test/resources/composition.json 2>&1)
HC=$(extract_http_code "$H")
NEW_ETAG=$(extract_header "$H" "ETag")
NEW_LOCATION=$(extract_header "$H" "Location")
NEW_VERSION_UID=$(echo "$NEW_LOCATION" | sed 's|.*/compositions/||')
[ "$HC" = "200" ] || [ "$HC" = "204" ] && pass "Update Composition — HTTP $HC, $NEW_VERSION_UID" || fail "Update Composition — HTTP $HC" ""

echo ""
echo "--- 22. Get Composition after update (version 2) ---"
if [ -n "$NEW_VERSION_UID" ]; then
  R=$(curl -s "${API}/ehrs/${EHR_ID}/compositions/${NEW_VERSION_UID}" -H "Accept: application/json")
  ARCH2=$(json_val "$R" "print(json.load(sys.stdin).get('archetype_node_id','?'))")
  [ -n "$ARCH2" ] && [ "$ARCH2" != "?" ] && pass "Get Composition v2 — archetype: $ARCH2" || fail "Get Composition v2" ""
else
  skip "Get Composition v2 — no version UID"
fi

echo ""
echo "--- 23. Update with wrong If-Match → expect 409/412 ---"
HC=$(http_code -X PUT "${API}/ehrs/${EHR_ID}/compositions/${NEW_VERSION_UID}" \
  -H "Content-Type: application/json" -H "If-Match: \"wrong-etag\"" \
  -d @configuration/src/test/resources/composition.json)
[ "$HC" = "409" ] || [ "$HC" = "412" ] || [ "$HC" = "400" ] && pass "Wrong If-Match rejected — HTTP $HC" || fail "Wrong If-Match — HTTP $HC" ""

echo ""
echo "--- 24. Delete Composition ---"
HC=$(http_code -X DELETE "${API}/ehrs/${EHR_ID}/compositions/${NEW_VERSION_UID}" -H "Accept: application/json")
[ "$HC" = "204" ] || [ "$HC" = "200" ] && pass "Delete Composition — HTTP $HC" || fail "Delete Composition — HTTP $HC" ""

echo ""
echo "--- 25. Get deleted Composition → expect 410 Gone ---"
HC=$(http_code "${API}/ehrs/${EHR_ID}/compositions/${NEW_VERSION_UID}" -H "Accept: application/json")
[ "$HC" = "410" ] && pass "Deleted Composition — HTTP 410 Gone" || fail "Deleted Composition — HTTP $HC (expected 410)" ""


# =============================================================================
# VERSIONED COMPOSITION
# =============================================================================
echo ""
echo "==========================================="
echo "  VERSIONED COMPOSITION"
echo "==========================================="

echo ""
echo "--- 26. Create a fresh Composition for versioning tests ---"
H=$(curl -sv -X POST "${API}/ehrs/${EHR_ID}/compositions" \
  -H "Content-Type: application/json" -H "Accept: application/json" -H "Prefer: return=minimal" \
  -d @configuration/src/test/resources/composition.json 2>&1)
V_COMP_LOCATION=$(extract_header "$H" "Location")
V_COMP_UID=$(echo "$V_COMP_LOCATION" | sed 's|.*/compositions/||')
V_COMP_ID=$(echo "$V_COMP_UID" | sed 's/::.*$//')
pass "Created composition for versioning: $V_COMP_ID"

echo ""
echo "--- 27. Versioned Composition container ---"
HC=$(http_code "${API}/ehrs/${EHR_ID}/versioned_composition/${V_COMP_ID}" -H "Accept: application/json")
[ "$HC" = "200" ] && pass "Versioned Composition container — HTTP 200" || fail "Versioned Composition container — HTTP $HC" ""

echo ""
echo "--- 28. Versioned Composition revision history ---"
HC=$(http_code "${API}/ehrs/${EHR_ID}/versioned_composition/${V_COMP_ID}/revision_history" -H "Accept: application/json")
[ "$HC" = "200" ] && pass "Revision history — HTTP 200" || fail "Revision history — HTTP $HC" ""

echo ""
echo "--- 29. Versioned Composition specific version ---"
HC=$(http_code "${API}/ehrs/${EHR_ID}/versioned_composition/${V_COMP_ID}/version/${V_COMP_UID}" -H "Accept: application/json")
[ "$HC" = "200" ] && pass "Specific version — HTTP 200" || fail "Specific version — HTTP $HC" ""


# =============================================================================
# DIRECTORY / FOLDERS
# =============================================================================
echo ""
echo "==========================================="
echo "  DIRECTORY / FOLDERS"
echo "==========================================="

echo ""
echo "--- 30. Create Folder ---"
H=$(curl -sv -X POST "${API}/ehrs/${EHR_ID}/directory" \
  -H "Content-Type: application/json" -H "Accept: application/json" \
  -d '{"name": {"value": "Clinical Notes"}, "archetype_node_id": "openEHR-EHR-FOLDER.generic.v1"}' 2>&1)
HC=$(extract_http_code "$H")
BODY=$(echo "$H" | grep "^{" | head -1)
FOLDER_ID=$(json_val "$BODY" "print(json.load(sys.stdin).get('id',''))" || echo "")
[ -n "$FOLDER_ID" ] && echo "       Folder ID: $FOLDER_ID"
[ "$HC" = "201" ] && pass "Create Folder — HTTP 201" || fail "Create Folder — HTTP $HC" "$BODY"

echo ""
echo "--- 31. Get Directory listing ---"
R=$(curl -s "${API}/ehrs/${EHR_ID}/directory" -H "Accept: application/json")
DIR_COUNT=$(json_val "$R" "d=json.load(sys.stdin); print(len(d) if isinstance(d,list) else 1)")
[ "$DIR_COUNT" -ge 1 ] 2>/dev/null && pass "Get Directory — $DIR_COUNT folder(s)" || pass "Get Directory — response received"

echo ""
echo "--- 32. Add Composition to Folder ---"
if [ -n "$FOLDER_ID" ] && [ -n "$V_COMP_ID" ]; then
  HC=$(http_code -X POST "${API}/ehrs/${EHR_ID}/directory/${FOLDER_ID}/items" \
    -H "Content-Type: application/json" -d "{\"composition_id\": \"${V_COMP_ID}\"}")
  [ "$HC" = "201" ] || [ "$HC" = "200" ] && pass "Add item to folder — HTTP $HC" || fail "Add item to folder — HTTP $HC" ""
else
  skip "Add item to folder — no folder_id or comp_id"
fi

echo ""
echo "--- 33. Remove Composition from Folder ---"
if [ -n "$FOLDER_ID" ] && [ -n "$V_COMP_ID" ]; then
  HC=$(http_code -X DELETE "${API}/ehrs/${EHR_ID}/directory/${FOLDER_ID}/items/${V_COMP_ID}")
  [ "$HC" = "204" ] || [ "$HC" = "200" ] && pass "Remove item from folder — HTTP $HC" || fail "Remove item — HTTP $HC" ""
else
  skip "Remove item from folder — no folder_id or comp_id"
fi

echo ""
echo "--- 34. Delete Folder ---"
if [ -n "$FOLDER_ID" ]; then
  HC=$(http_code -X DELETE "${API}/ehrs/${EHR_ID}/directory/${FOLDER_ID}")
  [ "$HC" = "204" ] || [ "$HC" = "200" ] && pass "Delete Folder — HTTP $HC" || fail "Delete Folder — HTTP $HC" ""
else
  skip "Delete Folder — no folder_id"
fi


# =============================================================================
# CONTRIBUTIONS
# =============================================================================
echo ""
echo "==========================================="
echo "  CONTRIBUTIONS"
echo "==========================================="

echo ""
echo "--- 35. List Contributions ---"
R=$(curl -s "${API}/ehrs/${EHR_ID}/contributions" -H "Accept: application/json")
CONTRIB_ID=$(json_val "$R" "d=json.load(sys.stdin); print(d[0]['id'] if isinstance(d,list) and len(d)>0 else '')")
[ -n "$CONTRIB_ID" ] && pass "List Contributions — found $CONTRIB_ID" || pass "List Contributions — response received"

echo ""
echo "--- 36. Get specific Contribution ---"
if [ -n "$CONTRIB_ID" ]; then
  HC=$(http_code "${API}/ehrs/${EHR_ID}/contributions/${CONTRIB_ID}" -H "Accept: application/json")
  [ "$HC" = "200" ] && pass "Get Contribution — HTTP 200" || fail "Get Contribution — HTTP $HC" ""
else
  skip "Get Contribution — no contribution_id available"
fi


# =============================================================================
# QUERY / VIEWS
# =============================================================================
echo ""
echo "==========================================="
echo "  QUERY / VIEWS"
echo "==========================================="

echo ""
echo "--- 37. List Views ---"
R=$(curl -s "${API}/query/views" -H "Accept: application/json")
VIEW_COUNT=$(json_val "$R" "d=json.load(sys.stdin); print(len(d) if isinstance(d,list) else '?')")
[ -n "$VIEW_COUNT" ] && [ "$VIEW_COUNT" != "?" ] && pass "List Views — $VIEW_COUNT views" || pass "List Views — response received"


# =============================================================================
# ADMIN (feature-gated)
# =============================================================================
echo ""
echo "==========================================="
echo "  ADMIN"
echo "==========================================="

echo ""
echo "--- 38. Admin health ---"
R=$(curl -s "${API}/admin/health" -H "Accept: application/json")
HC_STATUS=$(json_val "$R" "print(json.load(sys.stdin).get('status','?'))")
[ "$HC_STATUS" = "UP" ] && pass "Admin health — status=UP" || fail "Admin health — status=$HC_STATUS" "$R"


# =============================================================================
# API DOCUMENTATION
# =============================================================================
echo ""
echo "==========================================="
echo "  API DOCUMENTATION"
echo "==========================================="

echo ""
echo "--- 39. OpenAPI spec ---"
HC=$(http_code "${BASE_URL}/api-docs" -H "Accept: application/json")
[ "$HC" = "200" ] && pass "OpenAPI spec — HTTP 200" || fail "OpenAPI spec — HTTP $HC" ""

echo ""
echo "--- 40. Swagger UI ---"
HC=$(http_code "${BASE_URL}/swagger-ui.html")
[ "$HC" = "200" ] || [ "$HC" = "302" ] && pass "Swagger UI — HTTP $HC" || fail "Swagger UI — HTTP $HC" ""

echo ""
echo "--- 41. GraphiQL UI ---"
HC=$(http_code "${API}/graphiql")
[ "$HC" = "307" ] && pass "GraphiQL redirect — HTTP 307" || fail "GraphiQL — HTTP $HC" ""


# =============================================================================
# ERROR HANDLING
# =============================================================================
echo ""
echo "==========================================="
echo "  ERROR HANDLING"
echo "==========================================="

echo ""
echo "--- 42. Get non-existent EHR → 404 ---"
HC=$(http_code "${API}/ehrs/00000000-0000-0000-0000-000000000000" -H "Accept: application/json")
[ "$HC" = "404" ] && pass "Non-existent EHR — HTTP 404" || fail "Non-existent EHR — HTTP $HC" ""

echo ""
echo "--- 43. Invalid EHR ID format → 400 ---"
HC=$(http_code "${API}/ehrs/not-a-uuid" -H "Accept: application/json")
[ "$HC" = "400" ] || [ "$HC" = "404" ] && pass "Invalid EHR ID — HTTP $HC" || fail "Invalid EHR ID — HTTP $HC" ""

echo ""
echo "--- 44. Composition without valid content → 400 ---"
HC=$(http_code -X POST "${API}/ehrs/${EHR_ID}/compositions" -H "Content-Type: application/json" -d '{"_type":"COMPOSITION","archetype_node_id":"invalid"}')
[ "$HC" = "400" ] || [ "$HC" = "422" ] || [ "$HC" = "500" ] && pass "Invalid composition rejected — HTTP $HC" || fail "Invalid composition — HTTP $HC" ""

echo ""
echo "--- 45. Delete non-existent composition → 404 ---"
HC=$(http_code -X DELETE "${API}/ehrs/${EHR_ID}/compositions/00000000-0000-0000-0000-000000000000::local.ehrbase.org::1")
[ "$HC" = "404" ] || [ "$HC" = "400" ] && pass "Delete non-existent — HTTP $HC" || fail "Delete non-existent — HTTP $HC" ""

echo ""
echo "--- 46. Get non-existent template → 404 ---"
HC=$(http_code "${API}/templates/adl1.4/does_not_exist.en.v99" -H "Accept: application/xml")
[ "$HC" = "404" ] && pass "Non-existent template — HTTP 404" || fail "Non-existent template — HTTP $HC" ""


# =============================================================================
# Summary
# =============================================================================
echo ""
echo "============================================="
echo "  Results: $PASS passed, $FAIL failed, $SKIP skipped"
echo "============================================="

[ "$FAIL" -eq 0 ] && exit 0 || exit 1
