#!/usr/bin/env bash
# =============================================================================
# EHRbase GraphQL E2E Test Script
# =============================================================================
#
# Runs a full end-to-end test of the GraphQL API against a running EHRbase instance.
# Uses real test data (configuration/src/test/resources/composition.json).
#
# Prerequisites:
#   docker compose -f docker-compose-dev.yml up -d
#   Wait for: curl -s http://localhost:8080/ehrbase/management/health → {"status":"UP"}
#
# Usage:
#   chmod +x tests/graphql-e2e-test.sh
#   ./tests/graphql-e2e-test.sh
#
# =============================================================================

set -euo pipefail

BASE_URL="${EHRBASE_URL:-http://localhost:8080/ehrbase}"
GQL_URL="${BASE_URL}/api/v2/graphql"
COMPOSITION_FILE="configuration/src/test/resources/composition.json"

PASS=0
FAIL=0

pass() { PASS=$((PASS + 1)); echo "[PASS] $1"; }
fail() { FAIL=$((FAIL + 1)); echo "[FAIL] $1"; echo "       $2"; }

gql() {
  curl -s -X POST "$GQL_URL" -H "Content-Type: application/json" -d "$1"
}

gql_file() {
  curl -s -X POST "$GQL_URL" -H "Content-Type: application/json" -d @"$1"
}

json_val() {
  echo "$1" | python3 -c "import sys,json; $2" 2>/dev/null
}

echo "============================================="
echo "  EHRbase GraphQL E2E Tests"
echo "  Endpoint: $GQL_URL"
echo "============================================="
echo ""

# ---------------------------------------------------------------------------
# Health check
# ---------------------------------------------------------------------------
echo "--- Checking EHRbase is running ---"
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "${BASE_URL}/management/health" 2>/dev/null || echo "000")
if [ "$HTTP_CODE" = "200" ]; then
  pass "EHRbase is healthy"
else
  echo "[FATAL] EHRbase not reachable at ${BASE_URL} (HTTP $HTTP_CODE)"
  echo "        Start it with: docker compose -f docker-compose-dev.yml up -d"
  exit 1
fi

# ---------------------------------------------------------------------------
# 1. Schema Introspection
# ---------------------------------------------------------------------------
echo ""
echo "--- 1. Schema Introspection ---"

R=$(gql '{"operationName":"IntrospectionQuery","query":"query IntrospectionQuery { __schema { queryType { name } mutationType { name } subscriptionType { name } types { kind name } } }"}')
TYPES=$(json_val "$R" "d=json.load(sys.stdin); print(len(d['data']['__schema']['types']))")

if [ -n "$TYPES" ] && [ "$TYPES" -gt 30 ] 2>/dev/null; then
  pass "Introspection returned $TYPES types"
else
  fail "Introspection failed" "$R"
fi

# Check custom scalars
for SCALAR in DateTime DateTimeRange JSON Long; do
  if json_val "$R" "d=json.load(sys.stdin); types=d['data']['__schema']['types']; exit(0 if any(t['name']=='$SCALAR' and t['kind']=='SCALAR' for t in types) else 1)"; then
    pass "Scalar: $SCALAR"
  else
    fail "Scalar missing: $SCALAR" ""
  fi
done

# Check mutations
R=$(gql '{"query":"{ __schema { mutationType { fields { name } } } }"}')
for MUT in createEhr createComposition updateComposition deleteComposition; do
  if json_val "$R" "d=json.load(sys.stdin); fields=d['data']['__schema']['mutationType']['fields']; exit(0 if any(f['name']=='$MUT' for f in fields) else 1)"; then
    pass "Mutation: $MUT"
  else
    fail "Mutation missing: $MUT" ""
  fi
done

# Check subscriptions
R=$(gql '{"query":"{ __schema { subscriptionType { fields { name } } } }"}')
for SUB in onCompositionChange onAuditEvent; do
  if json_val "$R" "d=json.load(sys.stdin); fields=d['data']['__schema']['subscriptionType']['fields']; exit(0 if any(f['name']=='$SUB' for f in fields) else 1)"; then
    pass "Subscription: $SUB"
  else
    fail "Subscription missing: $SUB" ""
  fi
done

# ---------------------------------------------------------------------------
# 2. Mutation — createEhr
# ---------------------------------------------------------------------------
echo ""
echo "--- 2. createEhr ---"

R=$(gql '{"query":"mutation { createEhr(subjectId: \"e2e-test-001\", subjectNamespace: \"ehrbase-e2e\") { id subjectId subjectNamespace creationDate isModifiable isQueryable } }"}')
EHR_ID=$(json_val "$R" "print(json.load(sys.stdin)['data']['createEhr']['id'])")

if [ -n "$EHR_ID" ]; then
  pass "createEhr — id=$EHR_ID"
else
  fail "createEhr" "$R"
  echo "[FATAL] Cannot continue without EHR ID"; exit 1
fi

SUBJECT=$(json_val "$R" "print(json.load(sys.stdin)['data']['createEhr']['subjectId'])")
[ "$SUBJECT" = "e2e-test-001" ] && pass "subjectId matches" || fail "subjectId" "$SUBJECT"

MODIFIABLE=$(json_val "$R" "print(json.load(sys.stdin)['data']['createEhr']['isModifiable'])")
[ "$MODIFIABLE" = "True" ] && pass "isModifiable=true" || fail "isModifiable" "$MODIFIABLE"

# ---------------------------------------------------------------------------
# 3. Mutation — createComposition (real test file)
# ---------------------------------------------------------------------------
echo ""
echo "--- 3. createComposition (real composition.json) ---"

if [ ! -f "$COMPOSITION_FILE" ]; then
  fail "Test file not found: $COMPOSITION_FILE" ""
  echo "[FATAL] Cannot continue"; exit 1
fi

python3 - "$EHR_ID" << 'PYEOF' > /tmp/ehrbase_e2e_create.json
import json, sys
with open('configuration/src/test/resources/composition.json') as f:
    comp = json.load(f)
print(json.dumps({
    'query': 'mutation($ehrId: ID!, $templateId: String!, $data: JSON!) { createComposition(ehrId: $ehrId, templateId: $templateId, data: $data) { compositionId ehrId templateId version committedAt } }',
    'variables': {'ehrId': sys.argv[1], 'templateId': 'minimal_evaluation.en.v1', 'data': json.dumps(comp)}
}))
PYEOF

R=$(gql_file /tmp/ehrbase_e2e_create.json)
COMP_ID=$(json_val "$R" "print(json.load(sys.stdin)['data']['createComposition']['compositionId'])")
VERSION=$(json_val "$R" "print(json.load(sys.stdin)['data']['createComposition']['version'])")

if [ -n "$COMP_ID" ] && [ "$VERSION" = "1" ]; then
  pass "createComposition — id=$COMP_ID, version=$VERSION"
else
  fail "createComposition" "$(echo "$R" | python3 -m json.tool 2>/dev/null)"
  echo "[FATAL] Cannot continue"; exit 1
fi

# ---------------------------------------------------------------------------
# 4. Mutation — updateComposition
# ---------------------------------------------------------------------------
echo ""
echo "--- 4. updateComposition (version 1 → 2) ---"

python3 - "$COMP_ID" << 'PYEOF' > /tmp/ehrbase_e2e_update.json
import json, sys
with open('configuration/src/test/resources/composition.json') as f:
    comp = json.load(f)
comp['name']['value'] = 'Minimal - Updated via E2E test'
print(json.dumps({
    'query': 'mutation($compositionId: ID!, $version: Int!, $data: JSON!) { updateComposition(compositionId: $compositionId, version: $version, data: $data) { compositionId version } }',
    'variables': {'compositionId': sys.argv[1], 'version': 1, 'data': json.dumps(comp)}
}))
PYEOF

R=$(gql_file /tmp/ehrbase_e2e_update.json)
UPD_VERSION=$(json_val "$R" "print(json.load(sys.stdin)['data']['updateComposition']['version'])")

if [ "$UPD_VERSION" = "2" ]; then
  pass "updateComposition — version=$UPD_VERSION"
else
  fail "updateComposition" "$(echo "$R" | python3 -m json.tool 2>/dev/null)"
fi

# ---------------------------------------------------------------------------
# 5. Mutation — deleteComposition
# ---------------------------------------------------------------------------
echo ""
echo "--- 5. deleteComposition (version 2) ---"

R=$(gql "{\"query\":\"mutation { deleteComposition(compositionId: \\\"${COMP_ID}\\\", version: 2) }\"}")
DELETED=$(json_val "$R" "print(json.load(sys.stdin)['data']['deleteComposition'])")

if [ "$DELETED" = "True" ]; then
  pass "deleteComposition — returned true"
else
  fail "deleteComposition" "$(echo "$R" | python3 -m json.tool 2>/dev/null)"
fi

# ---------------------------------------------------------------------------
# 6. Dynamic Query — template view
# ---------------------------------------------------------------------------
echo ""
echo "--- 6. Dynamic query (compMinimalEvaluationEnV1s) ---"

# Create 3 compositions for query testing
for i in 1 2 3; do
  python3 - "$EHR_ID" << 'PYEOF' > /tmp/ehrbase_e2e_create.json
import json, sys
with open('configuration/src/test/resources/composition.json') as f:
    comp = json.load(f)
print(json.dumps({
    'query': 'mutation($ehrId: ID!, $templateId: String!, $data: JSON!) { createComposition(ehrId: $ehrId, templateId: $templateId, data: $data) { compositionId } }',
    'variables': {'ehrId': sys.argv[1], 'templateId': 'minimal_evaluation.en.v1', 'data': json.dumps(comp)}
}))
PYEOF
  gql_file /tmp/ehrbase_e2e_create.json > /dev/null
done

R=$(gql '{"query":"{ compMinimalEvaluationEnV1s(first: 5) { edges { node { ehrId compositionId quantityMagnitude quantityUnits } cursor } pageInfo { hasNextPage hasPreviousPage } } }"}')
ERRORS=$(json_val "$R" "print(len(json.load(sys.stdin).get('errors',[])))")
EDGES=$(json_val "$R" "print(len(json.load(sys.stdin)['data']['compMinimalEvaluationEnV1s']['edges']))")

[ "$ERRORS" = "0" ] && pass "Basic query — no errors, $EDGES edges" || fail "Basic query" "$(echo "$R" | python3 -m json.tool 2>/dev/null)"

# ---------------------------------------------------------------------------
# 7. Query — filter by ehrId
# ---------------------------------------------------------------------------
echo ""
echo "--- 7. Filter by ehrId ---"

R=$(gql "{\"query\":\"{ compMinimalEvaluationEnV1s(filter: { ehrId: { eq: \\\"${EHR_ID}\\\" } }, first: 10) { edges { node { ehrId } } } }\"}")
EDGES=$(json_val "$R" "print(len(json.load(sys.stdin)['data']['compMinimalEvaluationEnV1s']['edges']))")

[ "$EDGES" = "3" ] && pass "Filter by ehrId — $EDGES edges" || fail "Filter by ehrId — expected 3, got $EDGES" ""

# ---------------------------------------------------------------------------
# 8. Query — pagination
# ---------------------------------------------------------------------------
echo ""
echo "--- 8. Pagination ---"

R=$(gql '{"query":"{ compMinimalEvaluationEnV1s(first: 1) { edges { node { compositionId } cursor } pageInfo { hasNextPage endCursor } } }"}')
HAS_NEXT=$(json_val "$R" "print(json.load(sys.stdin)['data']['compMinimalEvaluationEnV1s']['pageInfo']['hasNextPage'])")
CURSOR=$(json_val "$R" "print(json.load(sys.stdin)['data']['compMinimalEvaluationEnV1s']['pageInfo']['endCursor'])")

[ "$HAS_NEXT" = "True" ] && pass "Page 1 — hasNextPage=true, cursor=$CURSOR" || fail "Page 1 hasNextPage" "$HAS_NEXT"

# Page 2
R=$(gql "{\"query\":\"{ compMinimalEvaluationEnV1s(first: 1, after: \\\"${CURSOR}\\\") { pageInfo { hasPreviousPage } } }\"}")
HAS_PREV=$(json_val "$R" "print(json.load(sys.stdin)['data']['compMinimalEvaluationEnV1s']['pageInfo']['hasPreviousPage'])")

[ "$HAS_PREV" = "True" ] && pass "Page 2 — hasPreviousPage=true" || fail "Page 2 hasPreviousPage" "$HAS_PREV"

# ---------------------------------------------------------------------------
# 9. Error handling — invalid UUID
# ---------------------------------------------------------------------------
echo ""
echo "--- 9. Error: invalid UUID ---"

R=$(gql '{"query":"mutation { deleteComposition(compositionId: \"not-a-uuid\", version: 1) }"}')
ERRORS=$(json_val "$R" "print(len(json.load(sys.stdin).get('errors',[])))")
[ "$ERRORS" -ge 1 ] 2>/dev/null && pass "Invalid UUID rejected" || fail "Invalid UUID not rejected" ""

# ---------------------------------------------------------------------------
# 10. Error handling — non-existent composition
# ---------------------------------------------------------------------------
echo ""
echo "--- 10. Error: non-existent composition ---"

R=$(gql '{"query":"mutation { updateComposition(compositionId: \"00000000-0000-0000-0000-000000000000\", version: 1, data: \"{}\") { compositionId } }"}')
ERRORS=$(json_val "$R" "print(len(json.load(sys.stdin).get('errors',[])))")
[ "$ERRORS" -ge 1 ] 2>/dev/null && pass "Non-existent composition rejected" || fail "Not rejected" ""

# ---------------------------------------------------------------------------
# 11. Error handling — query depth limit
# ---------------------------------------------------------------------------
echo ""
echo "--- 11. Error: query depth limit (max=10) ---"

R=$(gql '{"query":"{ __schema { types { fields { type { fields { type { fields { type { fields { type { fields { type { name } } } } } } } } } } } } }"}')
ERRORS=$(json_val "$R" "print(len(json.load(sys.stdin).get('errors',[])))")
[ "$ERRORS" -ge 1 ] 2>/dev/null && pass "Depth limit enforced" || fail "Depth limit not enforced" ""

# ---------------------------------------------------------------------------
# 12. Error handling — missing required argument
# ---------------------------------------------------------------------------
echo ""
echo "--- 12. Error: missing required argument ---"

R=$(gql '{"query":"mutation { createComposition(ehrId: \"test\", data: \"{}\") { compositionId } }"}')
ERRORS=$(json_val "$R" "print(len(json.load(sys.stdin).get('errors',[])))")
[ "$ERRORS" -ge 1 ] 2>/dev/null && pass "Missing required arg rejected" || fail "Not rejected" ""

# ---------------------------------------------------------------------------
# 13. Error handling — invalid syntax
# ---------------------------------------------------------------------------
echo ""
echo "--- 13. Error: invalid query syntax ---"

R=$(gql '{"query":"{ this is not valid graphql }"}')
ERRORS=$(json_val "$R" "print(len(json.load(sys.stdin).get('errors',[])))")
[ "$ERRORS" -ge 1 ] 2>/dev/null && pass "Invalid syntax rejected" || fail "Not rejected" ""

# ---------------------------------------------------------------------------
# 14. GraphiQL UI accessible
# ---------------------------------------------------------------------------
echo ""
echo "--- 14. GraphiQL UI ---"

HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "${BASE_URL}/api/v2/graphiql" 2>/dev/null)
[ "$HTTP_CODE" = "307" ] && pass "GraphiQL redirects (307)" || fail "GraphiQL HTTP $HTTP_CODE" ""

HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "${BASE_URL}/api/v2/graphiql?path=${BASE_URL}/api/v2/graphql" 2>/dev/null)
[ "$HTTP_CODE" = "200" ] && pass "GraphiQL page loads (200)" || fail "GraphiQL HTTP $HTTP_CODE" ""

# ---------------------------------------------------------------------------
# Summary
# ---------------------------------------------------------------------------
echo ""
echo "============================================="
echo "  Results: $PASS passed, $FAIL failed"
echo "============================================="

# Cleanup
rm -f /tmp/ehrbase_e2e_create.json /tmp/ehrbase_e2e_update.json

[ "$FAIL" -eq 0 ] && exit 0 || exit 1
