package com.hotelease.service;

import com.hotelease.model.Role;
import com.hotelease.model.User;
import com.hotelease.repository.RoleRepository;
import com.hotelease.repository.UserRepository;
import com.hotelease.util.PasswordEncoder;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class AuthService {

    private static final List<String> DEFAULT_ROLES = Arrays.asList(
            "GUEST",
            "RECEPTIONIST",
            "HOUSEKEEPING",
            "MANAGER",
            "ADMIN"
    );

    private static final String DEFAULT_REGISTRATION_ROLE = "GUEST";
    private static final String DEFAULT_ADMIN_PASSWORD = "admin@123";
    private static final String DEFAULT_ADMIN_PHONE = "0000000000";
    private static final List<DefaultUserSpec> DEFAULT_USERS = List.of(
            new DefaultUserSpec("guest1", "guest1@hotelease.local", "guest@123", "9991110001", List.of("GUEST")),
            new DefaultUserSpec("guest2", "guest2@hotelease.local", "guest@123", "9991110002", List.of("GUEST")),
            new DefaultUserSpec("receptionist1", "receptionist1@hotelease.local", "employee@123", "9991111001", List.of("RECEPTIONIST")),
            new DefaultUserSpec("receptionist2", "receptionist2@hotelease.local", "employee@123", "9991111002", List.of("RECEPTIONIST")),
            new DefaultUserSpec("housekeeping1", "housekeeping1@hotelease.local", "employee@123", "9991112001", List.of("HOUSEKEEPING")),
            new DefaultUserSpec("housekeeping2", "housekeeping2@hotelease.local", "employee@123", "9991112002", List.of("HOUSEKEEPING")),
            new DefaultUserSpec("manager1", "manager1@hotelease.local", "employee@123", "9991113001", List.of("MANAGER")),
            new DefaultUserSpec("manager2", "manager2@hotelease.local", "employee@123", "9991113002", List.of("MANAGER"))
    );

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public AuthService(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    public void initializeDefaults() {
        Role adminRole = null;
        for (String roleName : DEFAULT_ROLES) {
            Role role = roleRepository.findByName(roleName)
                    .orElseGet(() -> roleRepository.save(new Role(null, roleName)));
            if ("ADMIN".equals(roleName)) {
                adminRole = role;
            }
        }

        if (adminRole == null) {
            throw new IllegalStateException("Admin role could not be initialized");
        }

        final Role finalAdminRole = adminRole;
        userRepository.findByUsername("admin")
                .map(existing -> refreshDefaultAdmin(existing, finalAdminRole))
                .orElseGet(() -> createDefaultAdmin(finalAdminRole));

        DEFAULT_USERS.forEach(this::ensureDefaultUser);
    }

    public Optional<User> authenticate(String username, String rawPassword) {
        if (username == null || rawPassword == null) {
            return Optional.empty();
        }
        return userRepository.findByUsername(username)
                .filter(User::isActive)
                .filter(user -> PasswordEncoder.matches(rawPassword, user.getPasswordHash()));
    }

    public User registerUser(String username, String email, String phone, String rawPassword) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username is required");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("Phone number is required");
        }
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }

        userRepository.findByUsername(username).ifPresent(u -> {
            throw new IllegalArgumentException("Username already exists");
        });
        userRepository.findByEmail(email).ifPresent(u -> {
            throw new IllegalArgumentException("Email already registered");
        });

        User user = new User();
        user.setUsername(username.trim());
        user.setEmail(email.trim());
        user.setPhone(phone.trim());
        user.setPasswordHash(PasswordEncoder.hash(rawPassword));
        user.setActive(true);
        user.addRole(getOrCreateRole(DEFAULT_REGISTRATION_ROLE));

        return userRepository.save(user);
    }

    public User registerEmployee(String username, String email, String phone, String rawPassword, String roleName) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username is required");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("Phone number is required");
        }
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }
        if (roleName == null || roleName.isBlank()) {
            throw new IllegalArgumentException("Employee role is required");
        }

        String normalizedRole = roleName.trim().toUpperCase();
        if (DEFAULT_REGISTRATION_ROLE.equalsIgnoreCase(normalizedRole)) {
            throw new IllegalArgumentException("Guests should register through the guest portal.");
        }
        if (DEFAULT_ROLES.stream().noneMatch(r -> r.equalsIgnoreCase(normalizedRole))) {
            throw new IllegalArgumentException("Unsupported employee role: " + roleName);
        }

        userRepository.findByUsername(username).ifPresent(u -> {
            throw new IllegalArgumentException("Username already exists");
        });
        userRepository.findByEmail(email).ifPresent(u -> {
            throw new IllegalArgumentException("Email already registered");
        });

        User user = new User();
        user.setUsername(username.trim());
        user.setEmail(email.trim());
        user.setPhone(phone.trim());
        user.setPasswordHash(PasswordEncoder.hash(rawPassword));
        user.setActive(true);
        user.addRole(getOrCreateRole(normalizedRole));

        return userRepository.save(user);
    }

    private User createDefaultAdmin(Role adminRole) {
        User admin = new User();
        admin.setUsername("admin");
        admin.setPasswordHash(PasswordEncoder.hash(DEFAULT_ADMIN_PASSWORD));
        admin.setEmail("admin@hotelease.local");
        admin.setPhone(DEFAULT_ADMIN_PHONE);
        admin.setActive(true);
        admin.addRole(adminRole);
        return userRepository.save(admin);
    }

    private User refreshDefaultAdmin(User admin, Role adminRole) {
        boolean needsSave = false;

        if (!PasswordEncoder.matches(DEFAULT_ADMIN_PASSWORD, admin.getPasswordHash())) {
            admin.setPasswordHash(PasswordEncoder.hash(DEFAULT_ADMIN_PASSWORD));
            needsSave = true;
        }

        if (!admin.isActive()) {
            admin.setActive(true);
            needsSave = true;
        }

        boolean hasAdminRole = admin.getRoles().stream()
                .map(Role::getName)
                .anyMatch(name -> "ADMIN".equalsIgnoreCase(name));
        if (!hasAdminRole) {
            admin.addRole(adminRole);
            needsSave = true;
        }

        if (needsSave) {
            return userRepository.save(admin);
        }
        return admin;
    }

    private Role getOrCreateRole(String roleName) {
        return roleRepository.findByName(roleName)
                .orElseGet(() -> roleRepository.save(new Role(null, roleName)));
    }

    private User ensureDefaultUser(DefaultUserSpec spec) {
        return userRepository.findByUsername(spec.username())
                .map(existing -> refreshDefaultUser(existing, spec))
                .orElseGet(() -> createDefaultUser(spec));
    }

    private User createDefaultUser(DefaultUserSpec spec) {
        User user = new User();
        user.setUsername(spec.username());
        user.setEmail(spec.email());
        user.setPhone(spec.phone());
        user.setPasswordHash(PasswordEncoder.hash(spec.password()));
        user.setActive(true);
        spec.roles().forEach(roleName -> user.addRole(getOrCreateRole(roleName)));
        return userRepository.save(user);
    }

    private User refreshDefaultUser(User user, DefaultUserSpec spec) {
        boolean needsSave = false;

        if (!PasswordEncoder.matches(spec.password(), user.getPasswordHash())) {
            user.setPasswordHash(PasswordEncoder.hash(spec.password()));
            needsSave = true;
        }

        if (!spec.email().equalsIgnoreCase(user.getEmail())) {
            user.setEmail(spec.email());
            needsSave = true;
        }

        if (spec.phone() != null && !spec.phone().equalsIgnoreCase(user.getPhone())) {
            user.setPhone(spec.phone());
            needsSave = true;
        }

        if (!user.isActive()) {
            user.setActive(true);
            needsSave = true;
        }

        for (String roleName : spec.roles()) {
            boolean hasRole = user.getRoles().stream()
                    .map(Role::getName)
                    .anyMatch(name -> roleName.equalsIgnoreCase(name));
            if (!hasRole) {
                user.addRole(getOrCreateRole(roleName));
                needsSave = true;
            }
        }

        if (needsSave) {
            return userRepository.save(user);
        }
        return user;
    }

    private record DefaultUserSpec(String username, String email, String password, String phone, List<String> roles) {
    }
}
