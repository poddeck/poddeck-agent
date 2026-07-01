FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

COPY build/libs/agent-*.jar agent.jar
COPY docker-entrypoint.sh /app/docker-entrypoint.sh

# Upgrade libexpat to patch CVE-2026-45186 (fixed in 2.8.1-r0) and
# p11-kit to patch CVE-2026-2100 (fixed in 0.26.2-r0).
RUN apk add --no-cache procps && \
    apk upgrade --no-cache libcrypto3 libssl3 openssl libexpat p11-kit p11-kit-trust && \
    chmod +x /app/docker-entrypoint.sh && \
    chown -R 1000:1000 /app

USER 1000

HEALTHCHECK --interval=30s --timeout=5s --start-period=15s --retries=3 \
  CMD pgrep -f "java.*agent.jar" || exit 1

ENTRYPOINT ["/app/docker-entrypoint.sh"]
