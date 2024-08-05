package aziz.keycloak.provider;

import aziz.keycloak.adapter.ExternalUserAdapter;
import aziz.keycloak.domain.ExternalUserEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputUpdater;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.models.*;
import org.keycloak.models.cache.CachedUserModel;
import org.keycloak.models.cache.OnUserCache;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.user.UserCountMethodsProvider;
import org.keycloak.storage.user.UserLookupProvider;
import org.keycloak.storage.user.UserQueryProvider;
import org.mindrot.jbcrypt.BCrypt;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ExternalDBUserStorageProvider implements
        UserStorageProvider,
        UserLookupProvider,
        UserQueryProvider,
        UserCountMethodsProvider,
        CredentialInputValidator,
        CredentialInputUpdater,
        OnUserCache {
    private static final Logger logger = Logger.getLogger(ExternalDBUserStorageProvider.class);

    protected EntityManager entityManager;


    protected ComponentModel model;

    protected KeycloakSession session;

    // map of loaded users in this transaction
    protected Map<String, UserModel> loadedUsers = new HashMap<>();

    public static final String PASSWORD_CACHE_KEY = ExternalUserAdapter.class.getName() + ".password";

    protected final Map<String, Set<String>> roleMappings;

    public ExternalDBUserStorageProvider(KeycloakSession session,
                                         ComponentModel model,
                                         EntityManager entityManager,
                                         Map<String, Set<String>> roleMappings) {
        this.session = session;
        this.model = model;
        this.entityManager = entityManager;
        this.roleMappings = roleMappings;
    }

    @Override
    public void close() {
       entityManager.close();
    }

    @Override
    public UserModel getUserById(RealmModel realm, String id) {
        String externalId = StorageId.externalId(id);
        ExternalUserAdapter externalUserAdapter;
        if(loadedUsers.get(externalId) != null) {
            return loadedUsers.get(externalId);
        }
        ExternalUserEntity entity = entityManager.find(ExternalUserEntity.class, externalId);
        if (entity != null) {
            externalUserAdapter = new ExternalUserAdapter(session, realm, model, entity, roleMappings);
            loadedUsers.put(externalId, externalUserAdapter);
            return externalUserAdapter;
        }
        return null;
    }

    @Override
    public UserModel getUserByUsername(RealmModel realm, String username) {
        TypedQuery<ExternalUserEntity> query = entityManager
                .createNamedQuery("getUserByUsername", ExternalUserEntity.class);
        query.setParameter("username", username);
        List<ExternalUserEntity> result = query.getResultList();
        if (!result.isEmpty()) {
            return new ExternalUserAdapter(session, realm, model, result.getFirst(), roleMappings);
        }
        return null;
    }

    @Override
    public UserModel getUserByEmail(RealmModel realm, String email) {
        TypedQuery<ExternalUserEntity> query = entityManager
                .createNamedQuery("getUserByEmail", ExternalUserEntity.class);
        query.setParameter("email", email);
        List<ExternalUserEntity> result = query.getResultList();
        if (!result.isEmpty()) {
            return new ExternalUserAdapter(session, realm, model, result.getFirst(), roleMappings);
        }
        return null;
    }

    @Override
    public Stream<UserModel> searchForUserStream(RealmModel realm, Map<String, String> params, Integer firstResult, Integer maxResults) {
        String search = params.get(UserModel.SEARCH);
        TypedQuery<ExternalUserEntity> query = entityManager
                .createNamedQuery("searchForUser", ExternalUserEntity.class);
        String lower = search != null && !search.equals("*") ? search.toLowerCase() : "";
        query.setParameter("search", "%" + lower + "%");
        if (firstResult != null) {
            query.setFirstResult(firstResult);
        }
        if (maxResults != null) {
            query.setMaxResults(maxResults);
        }
        return query
                .getResultStream()
                .map(entity -> new ExternalUserAdapter(session, realm, model, entity, roleMappings));
    }

    @Override
    public Stream<UserModel> getRoleMembersStream(RealmModel realm, RoleModel role) {
        return UserQueryProvider.super.getRoleMembersStream(realm, role);
    }

    @Override
    public Stream<UserModel> getRoleMembersStream(RealmModel realm, RoleModel role, Integer firstResult, Integer maxResults) {
        //1. find all external roles mapped to this realm role
        Set<String> externalRoles = getExternalRolesMappedToRealmRole(role);

        //2. find external users assigned any of those roles
        TypedQuery<ExternalUserEntity> query = entityManager
                .createNamedQuery("getAllUsersWithAnyRole", ExternalUserEntity.class);

        query.setParameter("roles", externalRoles);
        if (firstResult != null) {
            query.setFirstResult(firstResult);
        }
        if (maxResults != null) {
            query.setMaxResults(maxResults);
        }
        return query
                .getResultStream()
                .map(entity -> new ExternalUserAdapter(session, realm, model, entity, roleMappings));
    }

    @Override
    public Stream<UserModel> getGroupMembersStream(RealmModel realm, GroupModel group, Integer firstResult, Integer maxResults) {
        return Stream.empty();
    }

    @Override
    public Stream<UserModel> searchForUserByUserAttributeStream(RealmModel realm, String attrName, String attrValue) {
        return Stream.empty();
    }

    @Override
    public boolean supportsCredentialType(String credentialType) {
        return credentialType.equals(PasswordCredentialModel.TYPE);
    }

    @Override
    public boolean updateCredential(RealmModel realm, UserModel user, CredentialInput input) {
        if(supportsCredentialType(input.getType()) && (input instanceof UserCredentialModel)) {
            throw new ModelException("Federated user password is read only.");
        }
        return false;
    }

    @Override
    public void disableCredentialType(RealmModel realm, UserModel user, String credentialType) {
        //no-op
    }

    @Override
    public Stream<String> getDisableableCredentialTypesStream(RealmModel realm, UserModel user) {
        return Stream.empty();
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        if (!this.supportsCredentialType(credentialType)) {
            return false;
        } else {
            return user.credentialManager()
                    .getStoredCredentialsByTypeStream(credentialType)
                    .findAny().isPresent();
        }
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput credentialInput) {
        if (!(credentialInput instanceof UserCredentialModel)) {
            logger.debug("Expected instance of UserCredentialModel for CredentialInput");
            return false;
        } else if (credentialInput.getChallengeResponse() == null) {
            logger.debugv("Input password was null for user {0} ", user.getUsername());
            return false;
        } else {
            String hashedUserPassword = getPassword(user);
            if (hashedUserPassword == null) {
                logger.debugv("No hashedUserPassword stored for user {0} ", user.getUsername());
                return false;
            } else {
                return BCrypt.checkpw(credentialInput.getChallengeResponse(), hashedUserPassword);
            }
        }
    }

    protected String getPassword(UserModel userModel) {
        String password = null;
        if (userModel instanceof CachedUserModel) {
            logger.trace("credentials fetched from cache");
            password = (String)((CachedUserModel) userModel).getCachedWith().get(PASSWORD_CACHE_KEY);
        } else if (userModel instanceof ExternalUserAdapter) {
            logger.trace("credentials fetched from model");
            password = ((ExternalUserAdapter)userModel).getPassword();
        }
        return password;
    }

    @Override
    public void onCache(RealmModel realm, CachedUserModel user, UserModel delegate) {
        String password = ((ExternalUserAdapter)delegate).getPassword();
        if (password != null) {
            logger.trace("caching credentials");
            user.getCachedWith().put(PASSWORD_CACHE_KEY, password);
        }
    }

    @Override
    public int getUsersCount(RealmModel realm, boolean includeServiceAccount) {
        Object count = entityManager.createNamedQuery("getUserCount")
                .getSingleResult();
        return ((Number)count).intValue();
    }

    private Set<String> getExternalRolesMappedToRealmRole(RoleModel role) {
        return roleMappings
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue().contains(role.getName()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }
}
