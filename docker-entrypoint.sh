#!/bin/sh
set -e

if [ -z "$CORE_HOSTNAME" ]; then
  echo "ERROR: CORE_HOSTNAME environment variable is required (gRPC endpoint of the PodDeck control plane)"
  exit 1
fi

if [ -z "$CLUSTER_KEY" ]; then
  echo "ERROR: CLUSTER_KEY environment variable is required (agent key from PodDeck cluster creation)"
  exit 1
fi

cat > config.ini <<EOF
[communication]
hostname = ${CORE_HOSTNAME}
port = ${CORE_PORT:-10101}
cluster = ${CLUSTER_NAME:-default}
key = ${CLUSTER_KEY}

[telegraf]
namespace = ${TELEGRAF_NAMESPACE:-monitoring}
port = ${TELEGRAF_PORT:-9273}

[metric]
interval_seconds = ${METRIC_INTERVAL:-1}
EOF

exec java -jar agent.jar
