package com.schemaforge.service;

import com.schemaforge.dto.AuthResponse;
import com.schemaforge.dto.LoginRequest;
import com.schemaforge.dto.RegisterRequest;
import com.schemaforge.exception.ConflictException;
import com.schemaforge.model.User;
import com.schemaforge.repository.UserRepository;
import com.schemaforge.security.JwtUtil;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    /** Hash compared against when the username does not exist, so both paths cost one bcrypt check. */
    private final String dummyHash;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.dummyHash = passwordEncoder.encode("schemaforge-timing-equaliser");
    }

    public AuthResponse register(RegisterRequest request) {
        String username = request.getUsername().trim();
        if (userRepository.findByUsername(username).isPresent()) {
            throw new ConflictException("Username already exists.");
        }

        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(request.getPassword()))
                .build();
        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Username already exists.");
        }

        return new AuthResponse(jwtUtil.generateToken(username));
    }

    public AuthResponse login(LoginRequest request) {
        String username = request.getUsername().trim();
        Optional<User> userOpt = userRepository.findByUsername(username);
        String hash = userOpt.map(User::getPassword).orElse(dummyHash);
        boolean matches = passwordEncoder.matches(request.getPassword(), hash);
        if (userOpt.isPresent() && matches) {
            return new AuthResponse(jwtUtil.generateToken(username));
        }
        throw new BadCredentialsException("Invalid username or password.");
    }
}
