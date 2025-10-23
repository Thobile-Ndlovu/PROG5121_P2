package com.mycompany.project1;

public class Login {
    private static String registeredUsername;
    private static String registeredPassword;
    private static String registeredFirstName;
    private static String registeredLastName;

    public static void setRegisteredUserDetails(String username, String password, String firstName, String lastName) {
        Login.registeredUsername = username;
        Login.registeredPassword = password;
        Login.registeredFirstName = firstName;
        Login.registeredLastName = lastName;
    }

    /**
     */
    public static boolean checkUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        // Must contain an underscore AND be 1 to 5 characters long.
        return username.contains("_") && username.length() >= 1 && username.length() <= 5;
    }

    public static boolean checkPasswordComplexity(String password) {
        if (password == null || password.trim().isEmpty()) {
            return false;
        }
        
        // Regex check for complexity:
        // ^(?=.*[A-Z])       # Must contain at least one uppercase letter
        // (?=.*[0-9])         # Must contain at least one digit
        // (?=.*[^a-zA-Z0-9])  # Must contain at least one special character (non-alphanumeric)
        // .{8,}$              # Must be at least 8 characters long
        return password.matches("^(?=.*[A-Z])(?=.*[0-9])(?=.*[^a-zA-Z0-9]).{8,}$");
    }

    /**
     * Must start with '+' followed by exactly 11 digits (e.g., +27721234567).
     */
    public static boolean validateCellphoneNumber(String cellphoneNumber) {
        if (cellphoneNumber == null || cellphoneNumber.trim().isEmpty()) {
            return false;
        }
        // Matches + followed by exactly 11 digits
        return cellphoneNumber.matches("^[+]\\d{11}$");
    }

    public static boolean loginUser(String username, String password) {
        return registeredUsername != null && registeredPassword != null &&
                registeredUsername.equals(username) && registeredPassword.equals(password);
    }

    public static String returnLoginStatus(boolean isLoginSuccessful) {
        if (isLoginSuccessful) {
            return "Welcome " + registeredFirstName + " " + registeredLastName + ", it is great to see you.";
        } else {
            return "Username or password incorrect, please try again.";
        }
    }
}