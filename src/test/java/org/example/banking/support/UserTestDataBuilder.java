package org.example.banking.support;

import org.example.banking.constants.accounts.Role;
import org.example.banking.entities.User;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

// id is null by default so Hibernate assigns it normally on persist — pre-setting a
// non-null id here makes Hibernate treat the entity as detached rather than new, and
// persist()/save() throws. Call withId() only for pure in-memory tests that never
// persist and need a concrete id to compare (e.g. Account.requireOwner checks).
public final class UserTestDataBuilder {

    private UUID id;
    private String email = "jane.doe+" + UUID.randomUUID() + "@example.com";
    private String firstName = "Jane";
    private String lastName = "Doe";
    private String password = "{noop}password";
    private Role role = Role.USER;
    private boolean enabled = true;

    private UserTestDataBuilder() {
    }

    public static UserTestDataBuilder aUser() {
        return new UserTestDataBuilder();
    }

    public UserTestDataBuilder withId(UUID id) {
        this.id = id;
        return this;
    }

    public UserTestDataBuilder withEmail(String email) {
        this.email = email;
        return this;
    }

    public UserTestDataBuilder withRole(Role role) {
        this.role = role;
        return this;
    }

    public UserTestDataBuilder admin() {
        return withRole(Role.ADMIN);
    }

    public UserTestDataBuilder disabled() {
        this.enabled = false;
        return this;
    }

    public User build() {
        User user = User.create(email, firstName, lastName, password);
        if (id != null) {
            ReflectionTestUtils.setField(user, "id", id);
        }
        user.updateRole(role);
        if (!enabled) {
            user.enableUser(false);
        }
        return user;
    }
}
