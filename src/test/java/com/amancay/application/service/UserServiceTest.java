package com.amancay.application.service;

import static com.amancay.application.service.fake.Admins.ADMIN_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.amancay.application.service.fake.Admins;
import com.amancay.application.service.fake.InMemoryUserRepository;
import com.amancay.domain.exception.InactiveUserException;
import com.amancay.domain.exception.SelfRoleChangeException;
import com.amancay.domain.exception.UserNotFoundException;
import com.amancay.domain.model.PageQuery;
import com.amancay.domain.model.PageResult;
import com.amancay.domain.model.Role;
import com.amancay.domain.model.User;

class UserServiceTest {

    private static final PageQuery PAGE = new PageQuery(0, 20);

    private final InMemoryUserRepository users = new InMemoryUserRepository();
    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(users, Admins.guard());
    }

    @Test
    void provisionsNewUserWithBuyerRole() {
        UUID id = UUID.randomUUID();

        User result = userService.getOrProvision(id, "buyer@amancay.com", "Ada");

        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getEmail()).isEqualTo("buyer@amancay.com");
        assertThat(result.getName()).isEqualTo("Ada");
        assertThat(result.getRole()).isEqualTo(Role.BUYER);
        assertThat(users.count("insertIfAbsent")).isEqualTo(1);
    }

    @Test
    void concurrentProvisionReadsTheWinningUserWithoutReplacingTheirRole() {
        UUID id = UUID.randomUUID();
        // Otro request gano la carrera entre el findById y el insert: el insert no hace nada.
        InMemoryUserRepository racing = new InMemoryUserRepository() {
            @Override
            public void insertIfAbsent(UUID userId, String email, String name) {
                calls.add("insertIfAbsent");
                create(userId, email, name, Role.ADMIN, true);
            }
        };

        assertThat(new UserService(racing, Admins.guard()).getOrProvision(id, "buyer@amancay.com", "Ada").getRole())
                .isEqualTo(Role.ADMIN);
        assertThat(racing.count("save")).isZero();
    }

    @Test
    void existingUserKeepsTheirRoleAndIsNotSavedAgain() {
        UUID id = UUID.randomUUID();
        users.create(id, "buyer@amancay.com", "Ada", Role.ADMIN, true);

        assertThat(userService.getOrProvision(id, "buyer@amancay.com", "Ada").getRole()).isEqualTo(Role.ADMIN);
        assertThat(users.count("insertIfAbsent")).isZero();
        assertThat(users.count("save")).isZero();
    }

    @Test
    void rejectsInactiveBuyersAndAdminsBeforeSynchronizingTheirProfile() {
        for (Role role : Role.values()) {
            UUID id = UUID.randomUUID();
            users.create(id, "old@amancay.com", "Ada", role, false);

            assertThatThrownBy(() -> userService.getOrProvision(id, "new@amancay.com", "Grace"))
                    .isInstanceOf(InactiveUserException.class);
            assertThat(users.stored(id).getEmail()).isEqualTo("old@amancay.com");
        }
        assertThat(users.count("save")).isZero();
    }

    @Test
    void returnsExistingUserWithoutReplacingTheirName() {
        UUID id = UUID.randomUUID();
        users.create(id, "buyer@amancay.com", "Ada", Role.BUYER, true);

        User result = userService.getOrProvision(id, "buyer@amancay.com", "Ignored");

        assertThat(result.getName()).isEqualTo("Ada");
        assertThat(users.count("save")).isZero();
    }

    @Test
    void fillsTheNameFromTheTokenOnlyWhenItWasMissing() {
        UUID id = UUID.randomUUID();
        users.create(id, "buyer@amancay.com", null, Role.BUYER, true);

        assertThat(userService.getOrProvision(id, "buyer@amancay.com", "Ada").getName()).isEqualTo("Ada");
        assertThat(users.stored(id).getName()).isEqualTo("Ada");
    }

    @Test
    void updatesEmailWhenItChangedInTheToken() {
        UUID id = UUID.randomUUID();
        users.create(id, "old@amancay.com", "Ada", Role.BUYER, true);

        User result = userService.getOrProvision(id, "new@amancay.com", "Ada");

        assertThat(result.getEmail()).isEqualTo("new@amancay.com");
        assertThat(users.stored(id).getEmail()).isEqualTo("new@amancay.com");
    }

    @Test
    void updatesNameViaUpdateProfile() {
        UUID id = UUID.randomUUID();
        users.create(id, "buyer@amancay.com", "Ada", Role.BUYER, true);

        User result = userService.updateName(id, "Grace");

        assertThat(result.getName()).isEqualTo("Grace");
        assertThat(users.stored(id).getName()).isEqualTo("Grace");
    }

    @Test
    void throwsWhenUpdatingMissingUser() {
        assertThatThrownBy(() -> userService.updateName(UUID.randomUUID(), "Grace"))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void listsAllUsersWhenQueryIsBlank() {
        UUID id = UUID.randomUUID();
        users.create(id, "a@amancay.com", "Ada", Role.BUYER, true);

        PageResult<User> result = userService.list(ADMIN_ID, "  ", PAGE);

        assertThat(result.content()).extracting(User::getId).containsExactly(id);
        assertThat(users.count("search")).isZero();
    }

    @Test
    void searchesUsersByEmailOrNameNewestFirst() {
        users.create(UUID.randomUUID(), "ada@amancay.com", "Ada", Role.BUYER, true);
        users.create(UUID.randomUUID(), "b@amancay.com", "Adalberto", Role.BUYER, true);
        users.create(UUID.randomUUID(), "c@amancay.com", "Carla", Role.BUYER, true);

        PageResult<User> result = userService.list(ADMIN_ID, " ada ", PAGE);

        assertThat(result.totalElements()).isEqualTo(2L);
        assertThat(result.content()).extracting(User::getName).containsExactly("Adalberto", "Ada");
    }

    @Test
    void adminPromotesAnotherUser() {
        UUID targetId = UUID.randomUUID();
        users.create(targetId, "b@amancay.com", "Bob", Role.BUYER, true);

        assertThat(userService.changeRole(ADMIN_ID, targetId, Role.ADMIN).getRole()).isEqualTo(Role.ADMIN);
        assertThat(users.stored(targetId).getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    void adminCannotRemoveTheirOwnAdminRole() {
        UUID adminId = ADMIN_ID;
        users.create(adminId, "a@amancay.com", "Ada", Role.ADMIN, true);

        assertThatThrownBy(() -> userService.changeRole(adminId, adminId, Role.BUYER))
                .isInstanceOf(SelfRoleChangeException.class);
        assertThat(users.count("save")).isZero();
    }

    @Test
    void changingRoleOfUnknownUserIsNotFound() {
        assertThatThrownBy(() -> userService.changeRole(ADMIN_ID, UUID.randomUUID(), Role.ADMIN))
                .isInstanceOf(UserNotFoundException.class);
    }
}
