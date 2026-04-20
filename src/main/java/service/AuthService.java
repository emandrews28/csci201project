package service;

import dao.UserDAO;
import model.User;
import util.PasswordUtil;

public class AuthService {

    private final UserDAO userDAO;

    public AuthService() {
        this.userDAO = new UserDAO();
    }

    /**
     * Register a new user
     */
    public User register(String username, String email, String password) {

        // Basic validation
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email cannot be empty");
        }

        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }

        // Check if username/email already exists
        if (userDAO.isUsernameTaken(username)) {
            throw new IllegalArgumentException("Username already taken");
        }

        if (userDAO.isEmailTaken(email)) {
            throw new IllegalArgumentException("Email already registered");
        }

        // Hash password
        String hashedPassword = PasswordUtil.hashPassword(password);

        // Create user object
        User newUser = new User(username, email, hashedPassword);

        // Save to database
        User createdUser = userDAO.createUser(newUser);

        if (createdUser == null) {
            throw new RuntimeException("Failed to create user");
        }

        return createdUser;
    }

    /**
     * Login user
     * Username and password are CASE-SENSITIVE
     */
    public User login(String username, String password) {

        if (username == null || password == null) {
            return null;
        }

        // Fetch user by username (case-sensitive in PostgreSQL by default)
        User user = userDAO.findByUsername(username);

        if (user == null) {
            return null; // user not found
        }

        // Compare password (BCrypt is case-sensitive)
        boolean passwordMatches = PasswordUtil.checkPassword(password, user.getPasswordHash());

        if (!passwordMatches) {
            return null; // incorrect password
        }

        return user; // successful login
    }
}