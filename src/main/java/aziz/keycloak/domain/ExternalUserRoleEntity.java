package aziz.keycloak.domain;

import jakarta.persistence.*;
@NamedQueries({
        @NamedQuery(name="getAllRoles", query="select r from ExternalUserRoleEntity r"),
})
@Entity
@Table(name = "role_entity")
public class ExternalUserRoleEntity {
    @Id
    @Column(name = "id", insertable = false, updatable = false)
    private String id;

    @Column(name = "name", insertable = false, updatable = false)
    private String name;

    public String getName() {
        return name;
    }
}
