#!/usr/bin/env bash
# Deploys everything and records the real output of every demo step in evidence/*.txt (+ PNG renders).
# Usage: evidence/tools/run-aws-evidence.sh
set -uo pipefail
cd "$(dirname "$0")/../.."
C=evidence/tools/capture.sh
export AWS_PAGER=""

$C 10-deploy "Deploy - Lambda functions + both REST APIs (deploy/deploy.sh)" "./deploy/deploy.sh"
source deploy/outputs.env
source deploy/config.sh >/dev/null

# ------------------------------------------------------------------ Part II: invoke Lambda directly
inv() {  # name title function event-file
  $C "$1" "$2" "aws lambda invoke --function-name $3 --cli-binary-format raw-in-base64-out --payload file://$4 target/out.json; echo '--- payload:'; cat $4; echo '--- response:'; cat target/out.json; echo"
}
inv 11-lambda-create-1      "Part II - direct Lambda invoke: create order (input 1)"         orders-create-direct events/direct/create-order-1.json
inv 12-lambda-create-2      "Part II - direct Lambda invoke: create order (input 2)"         orders-create-direct events/direct/create-order-2.json
inv 13-lambda-create-invalid "Part II - direct Lambda invoke: invalid input -> Lambda error" orders-create-direct events/direct/create-order-invalid.json
inv 14-lambda-get           "Part II - direct Lambda invoke: get order"                      orders-get-direct    events/direct/get-order.json
inv 15-lambda-get-missing   "Part II - direct Lambda invoke: missing order -> Lambda error"  orders-get-direct    events/direct/get-order-missing.json
inv 17-lambda-extra-fields  "Part II - serialization experiment: unknown fields, client id, quantity as string" orders-create-direct events/direct/create-order-extra-fields.json
inv 18-lambda-case          "Part II - serialization experiment: property names in other case" orders-create-direct events/direct/create-order-case.json
inv 19-lambda-not-object    "Part II - serialization experiment: JSON that is not an object"  orders-create-direct events/direct/create-order-not-object.json
inv 16-lambda-find          "Part II - direct Lambda invoke: orders of a customer (List<Order> -> JSON array)" orders-find-direct events/direct/find-by-customer.json

# ------------------------------------------------------------------ Architecture A: what API Gateway sends to Lambda
MAPPING_ID=$(echo "$MAPPING_URL" | sed -E 's#https://([^.]+)\..*#\1#')
PROXY_ID=$(echo "$PROXY_URL" | sed -E 's#https://([^.]+)\..*#\1#')
res_id() { aws apigateway get-resources --rest-api-id "$1" --query "items[?path=='$2'].id | [0]" --output text; }

$C 20-mapping-test-invoke "Arch A - API Gateway test-invoke GET /orders/{id}: request after mapping template" \
  "MSYS_NO_PATHCONV=1 aws apigateway test-invoke-method --rest-api-id $MAPPING_ID --resource-id $(res_id "$MAPPING_ID" '/orders/{id}') --http-method GET --path-with-query-string /orders/ORD-42 --query log --output text | sed 's/^.* UTC [0-9]* : //' | awk '/^Method request path/ || /^Endpoint request body after transformations/,/^Sending request/ {print} /^Endpoint response body before/ || /^Method response body after/ || /^Successfully completed/ {print}' | grep -v -e '^Sending request' -e '^Endpoint request headers'"

# ------------------------------------------------------------------ Architecture A: HTTP calls
cu() {  # name title curl-args...
  local name="$1" title="$2"; shift 2
  $C "$name" "$title" "curl -s -i $*; echo"
}
J='-H "Content-Type: application/json"'
cu 21-mapping-get-200        "Arch A - GET /orders/ORD-42 -> 200"                 "$MAPPING_URL/orders/ORD-42"
cu 22-mapping-get-404        "Arch A - GET /orders/ORD-999 -> 404"                "$MAPPING_URL/orders/ORD-999"
cu 23-mapping-query-cust42   "Arch A - GET /orders?customerId=CUST-42 -> 200"     "'$MAPPING_URL/orders?customerId=CUST-42'"
cu 24-mapping-query-cust100  "Arch A - GET /orders?customerId=CUST-100 -> 200 []" "'$MAPPING_URL/orders?customerId=CUST-100'"
cu 25-mapping-query-400      "Arch A - GET /orders (no customerId) -> 400"        "$MAPPING_URL/orders"
cu 26-mapping-post-201       "Arch A - POST /orders -> 201 + Location"            "-X POST $J -d @events/direct/create-order-1.json $MAPPING_URL/orders"
cu 27-mapping-post-400       "Arch A - POST /orders quantity 0 -> 400 (business rule, Java)" "-X POST $J -d @events/direct/create-order-invalid.json $MAPPING_URL/orders"
cu 28-mapping-post-malformed "Arch A - POST /orders quantity of wrong type -> 400 (API Gateway body validator, Lambda never invoked)" "-X POST $J -d '{\"customerId\": \"CUST-42\", \"quantity\": \"one\"}' $MAPPING_URL/orders"
created_id() { grep -o '"id" *: *"[^"]*"' "evidence/$1.txt" | head -1 | sed -E 's/.*"([^"]*)"$/\1/'; }
A_ID=$(created_id 26-mapping-post-201)
cu 29-mapping-get-created    "Arch A - GET $A_ID just created by ANOTHER Lambda -> 404 (in-memory state is per function)" "$MAPPING_URL/orders/$A_ID"
cu 30-mapping-delete-204     "Arch A - DELETE /orders/ORD-42 -> 204"              "-X DELETE $MAPPING_URL/orders/ORD-42"
cu 31-mapping-delete-404     "Arch A - DELETE /orders/ORD-999 -> 404"             "-X DELETE $MAPPING_URL/orders/ORD-999"
cu 32-mapping-unknown-route  "Arch A - unknown route -> 404 (gateway response)"   "$MAPPING_URL/customers"

# ------------------------------------------------------------------ Architecture B: proxy event
MSYS_NO_PATHCONV=1 aws apigateway test-invoke-method --rest-api-id "$PROXY_ID" --resource-id "$(res_id "$PROXY_ID" '/orders')" \
  --http-method GET --path-with-query-string '/orders?customerId=CUST-42' --headers Accept=application/json \
  --query log --output text > target/proxy-test-invoke.log
python -I - <<'EOF'
import json, re
log = open('target/proxy-test-invoke.log', encoding='utf-8').read()
m = re.search(r'Endpoint request body after transformations: (\{.*?\})\s*\n\S', log, re.S)
event = json.loads(m.group(1))
identity = event.get('requestContext', {}).get('identity', {})
for key in ('accessKey', 'caller', 'user', 'userArn'):   # never publish credentials / IAM identifiers
    if identity.get(key):
        identity[key] = 'REDACTED'
with open('events/proxy/api-gateway-proxy-event.json', 'w', encoding='utf-8', newline='\n') as f:
    json.dump(event, f, indent=2)
    f.write('\n')
print('saved events/proxy/api-gateway-proxy-event.json')
EOF
$C 40-proxy-event "Arch B - event API Gateway sends to Lambda (proxy integration, real test-invoke)" \
  "cat events/proxy/api-gateway-proxy-event.json | python -I -c \"import json,sys; e=json.load(sys.stdin); print(json.dumps({k: e[k] for k in ['resource','path','httpMethod','pathParameters','queryStringParameters','headers','body','isBase64Encoded']}, indent=2))\""

# ------------------------------------------------------------------ Architecture B: HTTP calls
cu 41-proxy-post-201        "Arch B - POST /orders -> 201 + Location"                 "-X POST $J -d @events/direct/create-order-1.json $PROXY_URL/orders"
B_ID=$(created_id 41-proxy-post-201)
cu 42-proxy-get-created     "Arch B - GET $B_ID just created -> 200 (same Lambda, warm environment)" "$PROXY_URL/orders/$B_ID"
cu 43-proxy-get-200         "Arch B - GET /orders/ORD-42 -> 200"                      "$PROXY_URL/orders/ORD-42"
cu 44-proxy-get-404         "Arch B - GET /orders/ORD-999 -> 404"                     "$PROXY_URL/orders/ORD-999"
cu 45-proxy-query-cust42    "Arch B - GET /orders?customerId=CUST-42 -> 200"          "'$PROXY_URL/orders?customerId=CUST-42'"
cu 46-proxy-query-cust100   "Arch B - GET /orders?customerId=CUST-100 -> 200 []"      "'$PROXY_URL/orders?customerId=CUST-100'"
cu 47-proxy-query-400       "Arch B - GET /orders (no customerId) -> 400"             "$PROXY_URL/orders"
cu 48-proxy-post-400        "Arch B - POST /orders quantity 0 -> 400"                 "-X POST $J -d @events/direct/create-order-invalid.json $PROXY_URL/orders"
cu 49-proxy-post-malformed  "Arch B - POST /orders malformed JSON -> 400 (Java adapter)" "-X POST $J -d '{not json' $PROXY_URL/orders"
cu 50-proxy-delete-204      "Arch B - DELETE /orders/ORD-42 -> 204"                   "-X DELETE $PROXY_URL/orders/ORD-42"
cu 51-proxy-delete-again    "Arch B - GET + DELETE /orders/ORD-42 again -> 404"       "$PROXY_URL/orders/ORD-42; echo; echo; curl -s -i -X DELETE $PROXY_URL/orders/ORD-42"
cu 52-proxy-unknown-route   "Arch B - unknown route -> 404"                           "$PROXY_URL/customers"

# ------------------------------------------------------------------ deployed configuration, exported from AWS
mkdir -p apigateway/exported
for pair in "orders-api-mapping:$MAPPING_ID" "orders-api-proxy:$PROXY_ID"; do
  aws apigateway get-export --rest-api-id "${pair#*:}" --stage-name "$STAGE" --export-type swagger \
    --parameters extensions=integrations --accepts application/yaml "apigateway/exported/${pair%%:*}.exported.yaml" >/dev/null
done
$C 60-lambda-functions "Deployed Lambda functions" \
  "aws lambda list-functions --query \"Functions[?starts_with(FunctionName, 'orders-')].[FunctionName,Runtime,Handler,MemorySize]\" --output table"
$C 61-rest-apis "Deployed API Gateway REST APIs, resources and methods"   "aws apigateway get-rest-apis --query \"items[?starts_with(name, 'orders-api')].[name,id]\" --output table; for id in $MAPPING_ID $PROXY_ID; do echo \$id; aws apigateway get-resources --rest-api-id \$id --query \"items[?resourceMethods].[path, join(',', keys(resourceMethods))]\" --output table; done"

python -I evidence/tools/render.py
