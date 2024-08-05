package aziz.keycloak.domain;

import jakarta.persistence.*;

@NamedQueries({
        @NamedQuery(name="getUserByUsername", query="select u from ExternalUserEntity u where u.username = :username"),
        @NamedQuery(name="getUserByEmail", query="select u from ExternalUserEntity u where u.email = :email"),
        @NamedQuery(name="getUserCount", query="select count(u) from ExternalUserEntity u"),
        @NamedQuery(name="getAllUsers", query="select u from ExternalUserEntity u"),
        @NamedQuery(name="getAllUsersWithAnyRole", query="select u from ExternalUserEntity u where u.role.name in :roles"),
        @NamedQuery(name="searchForUser", query="select u from ExternalUserEntity u where " +
                "( lower(u.username) like :search or u.email like :search ) order by u.username"),
})
@Entity
@Table(name = "user_entity")
public class ExternalUserEntity {
    @Id
    @Column(name = "id", insertable = false, updatable = false)
    private String id;

    @Column(name = "email", insertable = false, updatable = false)
    private String email;

    @Column(name = "username", insertable = false, updatable = false)
    private String username;

    @Column(name = "password", insertable = false, updatable = false)
    private String password;

    @ManyToOne
    @JoinColumn(name = "role_id", updatable = false, insertable = false)
    private ExternalUserRoleEntity role;

    @Column(name = "first_name", insertable = false, updatable = false)
    private String firstName;

    @Column(name = "last_name", insertable = false, updatable = false)
    private String lastName;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Long createdAt;

    public String getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public ExternalUserRoleEntity getRole() {
        return role;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public Long getCreatedAt() {
        return createdAt;
    }
}
