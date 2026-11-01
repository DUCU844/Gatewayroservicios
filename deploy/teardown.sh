#!/usr/bin/env bash
# Deletes every resource created by deploy.sh.
set -uo pipefail
export MSYS_NO_PATHCONV=1 AWS_PAGER=""
cd "$(dirname "$0")/.."
source deploy/config.sh

for api in orders-api-mapping orders-api-proxy; do
  id=$(aws apigateway get-rest-apis --query "items[?name=='$api'].id | [0]" --output text)
  [[ "$id" != "None" && -n "$id" ]] && aws apigateway delete-rest-api --rest-api-id "$id" && echo "deleted API $api"
done
for entry in "${FUNCTIONS[@]}"; do
  aws lambda delete-function --function-name "${entry%%=*}" && echo "deleted function ${entry%%=*}"
done
aws iam detach-role-policy --role-name "$ROLE_NAME" \
  --policy-arn arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole
aws iam delete-role --role-name "$ROLE_NAME" && echo "deleted role $ROLE_NAME"
