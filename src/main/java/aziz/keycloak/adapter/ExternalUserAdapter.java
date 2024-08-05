package aziz.keycloak.adapter;

import aziz.keycloak.domain.ExternalUserEntity;
import org.jboss.logging.Logger;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.storage.ReadOnlyException;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.adapter.AbstractUserAdapterFederatedStorage;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public class ExternalUserAdapter extends AbstractUserAdapterFederatedStorage {
    protected final ExternalUserEntity externalUserEntity;

    protected final String keycloakUserId;

    protected final Map<String, Set<String>> roleMappings;

    private static final Logger logger = Logger.getLogger(ExternalUserAdapter.class);

    public ExternalUserAdapter(KeycloakSession session,
                               RealmModel realm,
                               ComponentModel storageProviderModel,
                               ExternalUserEntity externalUserEntity,
                               Map<String, Set<String>> roleMappings) {
        super(session, realm, storageProviderModel);
        this.externalUserEntity = externalUserEntity;
        this.keycloakUserId = StorageId.keycloakId(storageProviderModel, externalUserEntity.getId());
        this.roleMappings = roleMappings;
    }

    @Override
    public String getId() {
        return keycloakUserId;
    }

    @Override
    public String getUsername() {
        return externalUserEntity.getUsername();
    }

    @Override
    public void setUsername(String username) {
        throwReadOnlyException("username");
    }

    @Override
    public String getFirstName() {
        return externalUserEntity.getFirstName();
    }

    @Override
    public void setFirstName(String firstName) {
        throwReadOnlyException("first name");
    }

    @Override
    public String getLastName() {
        return externalUserEntity.getLastName();
    }

    @Override
    public void setLastName(String lastName) {
        throwReadOnlyException("last name");
    }

    @Override
    public String getEmail() {
        return externalUserEntity.getEmail();
    }

    @Override
    public void setEmail(String email) {
        throwReadOnlyException("email");
    }

    @Override
    public String getFirstAttribute(String name) {
        return switch (name) {
            case UserModel.USERNAME -> getUsername();
            case UserModel.LAST_NAME -> getLastName();
            case UserModel.FIRST_NAME -> getFirstName();
            case UserModel.EMAIL -> getEmail();
            default -> super.getFirstAttribute(name);
        };
    }

    @Override
    public void setSingleAttribute(String name, String value) {
        switch (name) {
            case UserModel.USERNAME:
            case UserModel.LAST_NAME:
            case UserModel.FIRST_NAME:
            case UserModel.EMAIL: throwReadOnlyException(name);
            default: super.setSingleAttribute(name, value); //allow non-external managed attributes to be managed.
        }
    }

    @Override
    public Set<RoleModel> getRoleMappingsInternal() {
        String userRole = externalUserEntity.getRole().getName();
        Set<RoleModel> roles = new HashSet<>();
        Set<String> mappedRealmRoles = roleMappings.getOrDefault(userRole, new HashSet<>());
        for(String mappedRole : mappedRealmRoles) {
            RoleModel roleModel = realm.getRole(mappedRole);
            if(roleModel != null) {
                roles.add(roleModel);
            } else{
                logger.warn("Mapping between external user role: <" + userRole + "> and realm role: <" + mappedRole + "> " +
                        "cannot be done as the realm role doesn't exist" );
            }
        }
        return roles;
    }

    @Override
    public void setAttribute(String name, List<String> values) {
        switch (name) {
            case UserModel.USERNAME:
            case UserModel.LAST_NAME:
            case UserModel.FIRST_NAME:
            case UserModel.EMAIL: throwReadOnlyException(name);
            default: super.setAttribute(name, values);
        }
    }

    @Override
    public Map<String, List<String>> getAttributes() {
        MultivaluedHashMap<String, String> attributes = getFederatedStorage().getAttributes(realm, this.getId());
        if (attributes == null) {
            attributes = new MultivaluedHashMap<>();
        }
        attributes.putAll(
                Map.of(
                        UserModel.USERNAME, List.of(getUsername()),
                        UserModel.EMAIL, List.of(getEmail()),
                        UserModel.FIRST_NAME, List.of(getFirstName()),
                        UserModel.LAST_NAME, List.of(getLastName())
                )
        );
        return attributes;
    }

    @Override
    public Stream<String> getAttributeStream(String name) {
        return switch (name) {
            case UserModel.USERNAME -> Stream.of(getUsername());
            case UserModel.LAST_NAME -> Stream.of(getLastName());
            case UserModel.FIRST_NAME -> Stream.of(getFirstName());
            case UserModel.EMAIL -> Stream.of(getEmail());
            default -> super.getAttributeStream(name);
        };
    }

    @Override
    public Long getCreatedTimestamp() {
        return externalUserEntity.getCreatedAt();
    }

    @Override
    public void setCreatedTimestamp(Long createdTimestamp) {
        throwReadOnlyException("created timestamp");
    }

    public String getPassword() {
        return externalUserEntity.getPassword();
    }

    private void throwReadOnlyException(String attributeName) {
        throw new ReadOnlyException("Federated user's attribute: " + attributeName + " is read-only");
    }
}
