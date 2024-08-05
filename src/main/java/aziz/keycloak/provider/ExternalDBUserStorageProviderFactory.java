package aziz.keycloak.provider;

import jakarta.persistence.EntityManager;
import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.storage.UserStorageProviderFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ExternalDBUserStorageProviderFactory implements UserStorageProviderFactory<ExternalDBUserStorageProvider> {
    public static final String PROVIDER_ID = "external-user-provider";

    protected static final List<ProviderConfigProperty> configProperties;

    public static final String PERSISTENCE_UNIT_PROPERTY = "persistence-unit-name";
    public static final String ROLE_MAPPINGS_PROPERTY = "role-mappings";

    protected static final String PERSISTENCE_UNIT_LABEL = "Persistence Unit Name";
    protected static final String ROLE_MAPPINGS_LABEL = "Role Mappings";

    protected static final String PERSISTENCE_UNIT_PROPERTY_HELP_TEXT =
        "The persistence unit name is used to specify the configuration details for acquiring an entity manager. " +
        "This will be needed to get an entity manager / session per Keycloak transaction in your user provider implementation. " +
        "This name corresponds to the <persistence-unit> element in the persistence.xml file, which should be located " +
        "in the META-INF directory of your provider.";

    protected static final String ROLE_MAPPINGS_HELP_TEXT =
        "Define a set of key value pairs to map roles from the external storage into realm roles. " +
        "The key is the role name in the external storage, and the value is the name " +
        "of the realm role in this realm.";

    static {
        configProperties = ProviderConfigurationBuilder.create()
                .property()
                .name(PERSISTENCE_UNIT_PROPERTY)
                .type(ProviderConfigProperty.STRING_TYPE)
                .label(PERSISTENCE_UNIT_LABEL)
                .helpText(PERSISTENCE_UNIT_PROPERTY_HELP_TEXT)
                .defaultValue("user-store")
                .required(true)
                .add()

                .property()
                .name(ROLE_MAPPINGS_PROPERTY)
                .type(ProviderConfigProperty.MAP_TYPE)
                .label(ROLE_MAPPINGS_LABEL)
                .helpText(ROLE_MAPPINGS_HELP_TEXT)
                .add()
                .build();
    }

    @Override
    public ExternalDBUserStorageProvider create(KeycloakSession keycloakSession, ComponentModel componentModel) {
        return new ExternalDBUserStorageProvider(
                keycloakSession,
                componentModel,
                getEntityManager(keycloakSession, componentModel),
                ProviderConfigUtil.getConfigMap(componentModel, ROLE_MAPPINGS_PROPERTY)
        );
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    /**
     * Called before a component is created or updated.  Allows you to validate the configuration
     *
     * @throws ComponentValidationException
     */
    @Override
    public void validateConfiguration(KeycloakSession session, RealmModel realm, ComponentModel config)
            throws ComponentValidationException {
        validatePersistenceUnitName(session, config);
        validateRoleMappings(realm, config);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    protected void validatePersistenceUnitName(KeycloakSession session, ComponentModel config) {
        if (!session.listProviderIds(JpaConnectionProvider.class).contains(config.get(PERSISTENCE_UNIT_PROPERTY))) {
            throw new ComponentValidationException("Failed to find JpaConnectionProvider for " +
                    "persistence unit: " + config.get(PERSISTENCE_UNIT_PROPERTY));
        }
    }

    protected void validateRoleMappings(RealmModel realm, ComponentModel config) {
        Map<String, Set<String>> roleMappings = ProviderConfigUtil.getConfigMap(config, ROLE_MAPPINGS_PROPERTY);
        Set<String> rolesNotFound = new HashSet<>();
        roleMappings.forEach((externalRoleName, mappedRealmRoles) -> {
            if (mappedRealmRoles != null) {
                mappedRealmRoles.forEach(realmRole -> {
                    if(realm.getRole(realmRole) == null)
                        rolesNotFound.add(realmRole);
                });
            }

        });
        if(!rolesNotFound.isEmpty()){
            throw new ComponentValidationException("Realm roles: " + rolesNotFound + " were not found");
        }
    }

    protected EntityManager getEntityManager(KeycloakSession keycloakSession, ComponentModel config) {
        return keycloakSession
                .getProvider(JpaConnectionProvider.class, config.get(PERSISTENCE_UNIT_PROPERTY))
                .getEntityManager();
    }

}
