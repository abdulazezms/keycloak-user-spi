FROM quay.io/keycloak/keycloak:25.0.2

# Copy the provider
ADD --chown=keycloak:keycloak target/user-storage-spi-postgres.jar /opt/keycloak/providers

# Copy third party JARs (e.g., password hashing utilities for external credential store)
ADD --chown=keycloak:keycloak target/third-party/*.jar /opt/keycloak/providers

# Copy external DB properties
COPY quarkus.properties /opt/keycloak/conf/

# Copy cache config. Can be customized in a clustered stack
COPY cache-config.xml /opt/keycloak/conf/

# Configuration files are pre-parsed to reduce I/O when starting the server.
COPY custom-keycloak.conf /opt/keycloak/conf/

# Specify vendor at build time for optimized server startup.
# Note, certain vendors, such as Azure SQL and MariaDB Galera, do not support or rely on the XA transaction mechanism: https://github.com/yoannguion/keycloak/blob/fd6bde4df7b73b59b4f3cb5f5d01ff9ea2eaa9fd/docs/guides/server/db.adoc#using-database-vendors-with-xa-transaction-support
RUN /opt/keycloak/bin/kc.sh build --db=postgres --health-enabled=true --metrics-enabled=true --transaction-xa-enabled=true

ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]

CMD ["--config-file=/opt/keycloak/conf/custom-keycloak.conf" ,"start", "--optimized"]