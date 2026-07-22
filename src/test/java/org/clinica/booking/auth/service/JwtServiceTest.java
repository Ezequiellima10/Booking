package org.clinica.booking.auth.service;

import org.clinica.booking.auth.entity.Role;
import org.clinica.booking.auth.entity.User;
import org.clinica.booking.config.JwtConfig;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET =
            "4Lwkyf7eLpQkwEI7Uj/GY0yg3GCmTyt4u1nuN8hSKFs0m9ZtLNBKf4BwP1jw+aM9Ekk9KdZ8wr0fTN91c5zb5Q==";

    private User buildUser(long id) {
        return User.builder()
                .id(id)
                .email("patient@example.com")
                .passwordHash("hashed")
                .firstName("Ana")
                .lastName("Gomez")
                .role(Role.PATIENT)
                .build();
    }

    @Test
    void generateToken_thenExtractUserId_returnsSameId() {
        JwtService jwtService = new JwtService(new JwtConfig(SECRET, Duration.ofHours(1)));
        User user = buildUser(42L);

        String token = jwtService.generateToken(user);
        Optional<Long> userId = jwtService.extractUserId(token);

        assertThat(userId).contains(42L);
    }

    @Test
    void extractUserId_malformedToken_returnsEmpty() {
        JwtService jwtService = new JwtService(new JwtConfig(SECRET, Duration.ofHours(1)));

        Optional<Long> userId = jwtService.extractUserId("not-a-valid-token");

        assertThat(userId).isEmpty();
    }

    @Test
    void extractUserId_expiredToken_returnsEmpty() {
        JwtService jwtService = new JwtService(new JwtConfig(SECRET, Duration.ofSeconds(-1)));
        User user = buildUser(42L);

        String token = jwtService.generateToken(user);
        Optional<Long> userId = jwtService.extractUserId(token);

        assertThat(userId).isEmpty();
    }

    @Test
    void extractUserId_tokenSignedWithDifferentKey_returnsEmpty() {
        JwtService signer = new JwtService(new JwtConfig(SECRET, Duration.ofHours(1)));
        String otherSecret = "36dw8AxPziLZOVf46pP5bDeUpPkwrng+6M6TkOOVDQBgKbBdIC/06xBW6sb1cnfUAt7LpujFXsUaMGeuT7EI4Q==";
        JwtService verifier = new JwtService(new JwtConfig(otherSecret, Duration.ofHours(1)));

        String token = signer.generateToken(buildUser(42L));

        assertThat(verifier.extractUserId(token)).isEmpty();
    }
}