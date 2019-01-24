package edu.gatech.juniordesign.juniordesignpart2;

import android.support.annotation.Nullable;
import android.util.Log;

import java.security.SecureRandom;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

final class DatabaseModel {
    private DatabaseConnector db;
    private boolean dbInitialized;
    @Nullable
    private User currentUser;
    private static DatabaseModel model;
    private static final int SALT_SIZE = 32;
    private PasswordHasher hasher;

    private DatabaseModel() {
        try {
            db = new DatabaseConnector(
            );
            dbInitialized = true;
            currentUser = null;
            hasher = new PasswordHasher();
        } catch (SQLException e) {
            Log.e("DatabaseModel", e.getMessage());
            dbInitialized = false;
        }
    }

    static void checkInitialization() {
        if ((model == null) || !model.dbInitialized) {
            DatabaseModel.initialize();
        }
    }

    /**
     * This method initializes the model and tries to create a database connection.
     */
    static void initialize() {
        model = new DatabaseModel();
    }

    /**
     * This method gets the current DatabaseModel.
     * @return DatabaseModel currently being maintained.
     */
    static DatabaseModel getInstance() {return model;}

    /**
     * This method returns whether the Database connection has been initialized.
     * @return Boolean whether the Database connection has been initialized
     */
    boolean isDbInitialized() {return dbInitialized;}

    /**
     * This method gets the DatabaseConnector for the model.
     * @return The DatabaseConnector
     */
    DatabaseConnector getConnector() {return db;}

    /**
     * This method gets the current user's username.
     * @return String of current user's username
     */
    @Nullable
    User getCurrentUser() {return currentUser;}

    /**
     * This method clears the current user
     */
    void clearCurrentUser() {
        currentUser = null;
    }

    /**
     * Sets the current user to the one passed in.
     * @param newCurrentUser The new current user
     */
    void setCurrentUser(@Nullable User newCurrentUser) {
        currentUser = newCurrentUser;
    }

    /**
     * This method logs the user in with the passed in credentials if correct. It also sets the
     * current user if the login is successful.
     * @param email The email to login with
     * @param password The password (in plaintext) to login with
     * @return boolean of the result of the login
     */
    boolean login(String email, String password) {
        DatabaseModel.checkInitialization();
        String getUsersText = "SELECT * FROM tb_entity WHERE email=?";
        ResultSet results;
        boolean loginStatus;
        try {
            PreparedStatement statement = db.getStatement(getUsersText);
            statement.setString(1, email);
            results = db.query(statement);
            if (!results.next()) {
                // No entries in DB for passed in username
                results.close();
                loginStatus = false;
            } else {
                String dbPass = results.getString("password");
                int salt = results.getInt("salt");
                String hashPass = hasher.getSecurePassword(Integer.toString(salt),
                        password);
                if (dbPass.equals(hashPass)) {
                    Log.i("login", "auth success");
                    loginStatus = true;
                    String first_name = results.getString("first_name");
                    String last_name = results.getString("last_name");
                    boolean isAdmin = results.getBoolean("is_admin");
                    currentUser = new User(email, first_name, last_name, isAdmin);
                } else {
                    Log.i("login", "auth failed");
                    loginStatus = false;
                }
                results.close();
            }
            return loginStatus;
        } catch (SQLException e) {
            Log.e("login", e.getMessage());
            return false;
        }
    }

    /**
     * This method registers a user in the database. After registration they will instantly be able
     * to log in to the system.
     *
     * NOTE: this method does not do any input validation. You should check
     * to make sure usernames, passwords, profile names, and home locations match your specific
     * criteria before calling this method.
     * @param email The username under which to register the user
     * @param password The password to use for the user's account
     * @param first_name The first name to display for the user
     * @param last_name The last name to display for the user
     * @param isAdmin A boolean for whether to make the user an administrator.
     * @return An integer code indicating the success of the registration. 0 if registration
     *         succeeds, 1 if the registration fails because the username is already taken, and
     *         2 if a SQLException occurs during registration;
     */
    int registerUser(String email, String password, String first_name,
                     String last_name, boolean isAdmin) {
        DatabaseModel.checkInitialization();
        SecureRandom saltShaker = new SecureRandom();
        try {
            PreparedStatement checkStatement = db.getStatement(
                    "SELECT * FROM tb_entity WHERE email = ?");
            checkStatement.setString(1, email);
            ResultSet checkResults = db.query(checkStatement);
            if (checkResults.next()) { //Check for username already in use
                return 1;
            } else {
                int salt = saltShaker.nextInt(SALT_SIZE);
                String hashedPass = hasher.getSecurePassword(Integer.toString(salt),
                        password);
                String registrationText = "INSERT INTO tb_entity(email, password, first_name, "
                        + "last_name, salt, is_admin) VALUES(?, ?, ?, ?, ?, ?)";
                PreparedStatement registerStatement = db.getStatement(registrationText);
                registerStatement.setString(1, email);
                registerStatement.setString(2, hashedPass);
                registerStatement.setString(3, first_name);
                registerStatement.setString(4, last_name);
                registerStatement.setInt(5, salt);
                registerStatement.setBoolean(6, isAdmin);
                db.update(registerStatement);
                Log.d("Register User", "Success for email = " + email);
                return 0;
            }
        } catch (SQLException e) {
            Log.e("Register User", e.getMessage());
            return 2;

        }
    }

    /**
     * This method will change the user's password if they pass in the correct homeLocation for
     * that particular user.
     * @param userName Username of the user to reset the password.
     * @param first_name first name for authentication.
     * @param last_name first name for authentication.
     * @param newPassword Password to change the password to.
     * @return An integer code indicating the success of the update. 0 if update
     *         succeeds, 1 if the update fails because they passed in the incorrect homeLocation,
     *         and 2 if a SQLException occurs during the update.
     *
    int changePassword( String userName, String first_name, String last_name, String newPassword ) {
        try {
            //Get values from passed in User
            String qText = "SELECT salt, homeLocation FROM users WHERE userName = ?";
            PreparedStatement stmt = db.getStatement(qText);
            stmt.setString(1, userName);
            ResultSet results = db.query(stmt);
            results.next();
            String salt = results.getString("salt");
            String homeLoc = results.getString("homeLocation");
            results.close();
            //Here is where we "Authenticate" the user. If they correctly answer the homeLocation,
            //they can change the password
            if (homeLocation.equals(homeLoc)) {
                //Update the password
                String newPass = hasher.getSecurePassword(salt, newPassword);
                String qText2 = "UPDATE users SET password = ? WHERE userName = ?";
                PreparedStatement stmt2 = db.getStatement(qText2);
                stmt2.setString(1, newPass);
                stmt2.setString(2, userName);
                Log.d("Change Password", "Attempting...");
                db.update(stmt2);
                Log.d("Change Password", "Success!");
                return 0;
            } else {
                Log.d("Change Password", "Failed attempt");
                return 1;
            }

        } catch (SQLException e) {
            Log.e("Change Password", e.getMessage(), e);
            return 2;
        }
    }*/

}
