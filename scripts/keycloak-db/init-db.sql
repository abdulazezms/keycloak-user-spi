CREATE DATABASE keycloak_db;
CREATE USER keycloak_user WITH ENCRYPTED PASSWORD 'keycloak_password';
GRANT ALL PRIVILEGES ON DATABASE keycloak_db TO keycloak_user;
ALTER DATABASE keycloak_db OWNER TO keycloak_user;
