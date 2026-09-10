package com.amancay.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.amancay.dto.UpdateUserRequest;
import com.amancay.dto.UserDto;
import com.amancay.entity.Role;
import com.amancay.entity.User;
import com.amancay.exceptions.UserNotFoundException;
import com.amancay.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock
    private UserRepository userRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository);
    }

    @Test
    void provisionsNewUserWithBuyerRole() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserDto result = userService.getOrProvision(id, "buyer@amancay.com", "Ada");

        assertThat(result.id()).isEqualTo(id);
        assertThat(result.email()).isEqualTo("buyer@amancay.com");
        assertThat(result.name()).isEqualTo("Ada");
        assertThat(result.role()).isEqualTo(Role.BUYER);
    }

    @Test
    void returnsExistingUserWithoutDuplicating() {
        UUID id = UUID.randomUUID();
        User existing = existingUser(id, "buyer@amancay.com", "Ada");
        when(userRepository.findById(id)).thenReturn(Optional.of(existing));

        UserDto result = userService.getOrProvision(id, "buyer@amancay.com", "Ignored");

        assertThat(result.id()).isEqualTo(id);
        assertThat(result.name()).isEqualTo("Ada");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updatesEmailWhenItChangedInTheToken() {
        UUID id = UUID.randomUUID();
        User existing = existingUser(id, "old@amancay.com", "Ada");
        when(userRepository.findById(id)).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserDto result = userService.getOrProvision(id, "new@amancay.com", "Ada");

        assertThat(result.email()).isEqualTo("new@amancay.com");
        assertThat(existing.getEmail()).isEqualTo("new@amancay.com");
    }

    @Test
    void updatesNameViaUpdateProfile() {
        UUID id = UUID.randomUUID();
        User existing = existingUser(id, "buyer@amancay.com", "Ada");
        when(userRepository.findById(id)).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserDto result = userService.updateProfile(id, new UpdateUserRequest("Grace"));

        assertThat(result.name()).isEqualTo("Grace");
        assertThat(existing.getName()).isEqualTo("Grace");
    }

    @Test
    void throwsWhenUpdatingMissingUser() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateProfile(id, new UpdateUserRequest("Grace")))
                .isInstanceOf(UserNotFoundException.class);
    }

    private User existingUser(UUID id, String email, String name) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setName(name);
        user.setRole(Role.BUYER);
        user.setActive(true);
        return user;
    }
}
