# Shared deployment settings (sourced by deploy.sh / teardown.sh / invoke scripts).
AWS_REGION="${AWS_REGION:-$(aws configure get region || echo us-east-1)}"
export AWS_REGION AWS_DEFAULT_REGION="$AWS_REGION"
ACCOUNT_ID="${ACCOUNT_ID:-$(aws sts get-caller-identity --query Account --output text)}"

FN_PREFIX="orders"
ROLE_NAME="orders-api-lambda-role"
STAGE="prod"
JAR="target/orders-api-1.0.0.jar"

# function-name=handler
FUNCTIONS=(
  "$FN_PREFIX-create-direct=edu.eci.arem.orders.lambda.direct.CreateOrderHandler::handleRequest"
  "$FN_PREFIX-get-direct=edu.eci.arem.orders.lambda.direct.GetOrderHandler::handleRequest"
  "$FN_PREFIX-find-direct=edu.eci.arem.orders.lambda.direct.FindOrdersByCustomerHandler::handleRequest"
  "$FN_PREFIX-delete-direct=edu.eci.arem.orders.lambda.direct.DeleteOrderHandler::handleRequest"
  "$FN_PREFIX-proxy=edu.eci.arem.orders.lambda.proxy.ApiGatewayProxyHandler::handleRequest"
)
MAPPING_FUNCTIONS=("$FN_PREFIX-create-direct" "$FN_PREFIX-get-direct" "$FN_PREFIX-find-direct" "$FN_PREFIX-delete-direct")
PROXY_FUNCTIONS=("$FN_PREFIX-proxy")
