package com.snovasys.ledgerline.security;

import com.snovasys.ledgerline.repository.UserRepository;
import java.util.UUID;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

public class UserTokenValidator implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error INVALID_TOKEN =
            new OAuth2Error("invalid_token", "The access token is no longer valid.", null);

    private final OAuth2TokenValidator<Jwt> timestampAndIssuerValidator;
    private final UserRepository userRepository;

    public UserTokenValidator(
            OAuth2TokenValidator<Jwt> timestampAndIssuerValidator,
            UserRepository userRepository) {
        this.timestampAndIssuerValidator = timestampAndIssuerValidator;
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        OAuth2TokenValidatorResult standardValidation = timestampAndIssuerValidator.validate(jwt);
        if (standardValidation.hasErrors()) {
            return standardValidation;
        }

        try {
            UUID userId = UUID.fromString(jwt.getSubject());
            Number tokenVersion = jwt.getClaim("ver");
            String role = jwt.getClaimAsString("role");
            var user = userRepository.findById(userId);
            if (tokenVersion == null
                    || user.isEmpty()
                    || !user.get().isEnabled()
                    || tokenVersion.longValue() != user.get().getTokenVersion()
                    || !user.get().getRole().name().equals(role)) {
                return OAuth2TokenValidatorResult.failure(INVALID_TOKEN);
            }
        } catch (IllegalArgumentException exception) {
            return OAuth2TokenValidatorResult.failure(INVALID_TOKEN);
        }
        return OAuth2TokenValidatorResult.success();
    }
}
