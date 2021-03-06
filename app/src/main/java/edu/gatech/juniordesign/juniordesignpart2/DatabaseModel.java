package edu.gatech.juniordesign.juniordesignpart2;

import android.support.annotation.Nullable;
import android.util.Log;

import java.security.SecureRandom;
import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

final class DatabaseModel {
    private DatabaseConnector db;
    private boolean dbInitialized;
    @Nullable
    private User currentUser;
    private static DatabaseModel model;
    private static final int SALT_SIZE = 32;
    private PasswordHasher hasher;
    private static String selectedCategory;
    private static ArrayList<BusinessListItem> businessList;
    private static int selectedBusiness;
    private BusinessObject selectedBusinessObject;
    private boolean toggle;
    private static ArrayList<String> categories;
    private String[] addresses;
    private int business_id;
    private ArrayList<Review> reviews;

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

    ArrayList<Review> getReviewsForSelected() {
        return this.reviews;
    }

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

    int getBusiness_id() {return this.business_id;}

    String[] getAddresses() {return this.addresses;}

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
        String getUsersText = "SELECT * FROM tb_entity WHERE email=? AND is_inactive is NULL ";
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
                    String entity = results.getString("entity");
                    String first_name = results.getString("first_name");
                    String last_name = results.getString("last_name");
                    boolean isAdmin = results.getBoolean("is_admin");
                    currentUser = new User(email, first_name, last_name, isAdmin, entity);
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
    boolean facebookLogin( String firstName, String lastName, String email, String facebookID ) {
        DatabaseModel.checkInitialization();
        String getUsersText = "SELECT * FROM tb_entity WHERE email = ? AND is_inactive is NULL";
        ResultSet results;
        boolean loginStatus;
        try {
            PreparedStatement statement = db.getStatement(getUsersText);
            statement.setString(1, email);
            results = db.query(statement);
            if (!results.next()) {
                Log.i("Facebook", "No entry in DB for "+ email);
                // No entries in DB for passed in username
                int fbsalt = new SecureRandom().nextInt(SALT_SIZE);
                String hashPass = hasher.getSecurePassword(Integer.toString(fbsalt),
                        facebookID);
                String registrationText = "INSERT INTO tb_entity(email, facebook_id, first_name, "
                        + "last_name, fb_salt, is_admin) VALUES(?, ?, ?, ?, ?, false)";
                PreparedStatement registerStatement = db.getStatement(registrationText);
                registerStatement.setString(1, email);
                registerStatement.setString(2, hashPass);
                registerStatement.setString(3, firstName);
                registerStatement.setString(4, lastName);
                registerStatement.setInt(5, fbsalt);
                db.update(registerStatement);
                results = db.query(statement);
                String entity = results.getString("entity");
                String first_name = results.getString("first_name");
                String last_name = results.getString("last_name");
                boolean isAdmin = results.getBoolean("is_admin");
                setCurrentUser(new User(email, first_name, last_name, isAdmin, entity));
                Log.d("Facebook", "Registered email = " + email + " with FB");
                return true;
            } else if (results.getString("facebook_id") == null) {
                Log.i("Facebook", "entry in DB for "+ email +" but no FBID");
                int fbsalt = new SecureRandom().nextInt(SALT_SIZE);
                String hashfb_id = hasher.getSecurePassword(Integer.toString(fbsalt),
                        facebookID);
                String add_fb_text = "" +
                        "UPDATE tb_entity " +
                        "SET fb_salt = ?, " +
                        "facebook_id = ? " +
                        "WHERE entity = ? ";
                PreparedStatement add_fb_statement = db.getStatement(add_fb_text);
                add_fb_statement.setInt(1, fbsalt);
                add_fb_statement.setString(2, hashfb_id);
                add_fb_statement.setInt(3, results.getInt("entity"));
                db.update(add_fb_statement);
                String entity = results.getString("entity");
                String first_name = results.getString("first_name");
                String last_name = results.getString("last_name");
                boolean isAdmin = results.getBoolean("is_admin");
                setCurrentUser(new User(email, first_name, last_name, isAdmin, entity));
                Log.d("Facebook", "Added FBID to " + email);
                return true;
            }
            else if(results.getString("facebook_id").equals(hasher.getSecurePassword(Integer.toString(results.getInt("fb_salt")),
                    facebookID))){
                Log.i("Facebook", "FBID for "+ email);
                String entity = results.getString("entity");
                String first_name = results.getString("first_name");
                String last_name = results.getString("last_name");
                boolean isAdmin = results.getBoolean("is_admin");
                currentUser = new User(email, first_name, last_name, isAdmin, entity);
                results.close();
            } else {
                Log.i("Facebook", "Authentication Failure");
                return false;
            }

            return true;
        } catch (SQLException e) {
            Log.e("Facebook", e.getMessage());
            return false;
        }
    }

    public void setSelectedCategory(String category)
    {
        this.selectedCategory = category;
    }

    public String getSelectedCategory()
    {
        return this.selectedCategory;
    }

    public static ArrayList<BusinessListItem> getBusinessList() { return businessList; }

    public void setSelectedBusiness(int businessPK)
    {
        this.selectedBusiness = businessPK;
    }

    public int getSelectedBusiness()
    {
        return this.selectedBusiness;
    }

    public BusinessObject getSelectedBusinessObject() {
        return this.selectedBusinessObject;
    }

    public void setSelectedBusinessObject(BusinessObject b) {
        this.selectedBusinessObject = b;
    }

    ArrayList<String> getCategoryList() {
        return this.categories;
    }

    public void setToggle(boolean toggle){ this.toggle = toggle; }

    public void setAddresses(String[] addresses) { this.addresses = addresses; }

    public void setBusiness_id(int id) { this.business_id = id; }

    public boolean queryBusinessDetails()
    {
        DatabaseModel.checkInitialization();
        Log.i("BusinessDetails", "here");
        try {
            PreparedStatement checkStatement = db.getStatement(
                    "SELECT b.business," +
                            " b.name," +
                            " b.avg_rating," +
                            " c.description, " +
                            " b.description, " +
                            " b.image_url " +
                            " FROM tb_business b " +
                    "LEFT JOIN tb_business_category bc ON b.business = bc.business " +
                    "LEFT JOIN tb_category c ON bc.category = c.category " +
                    "WHERE b.business = CAST(? AS int)");
            checkStatement.setString(1, String.valueOf(selectedBusiness));
            ResultSet checkResults = db.query(checkStatement);
            while ( checkResults.next() ) {
                //TODO : fix to get the remaining arguments
                BusinessObject b_o = new BusinessObject(checkResults.getInt("business"),
                        checkResults.getString("name"), checkResults.getString(4),
                        checkResults.getString("avg_rating"), null, null,
                        checkResults.getString(5), checkResults.getString("image_url"),
                        null);
                setSelectedBusinessObject(b_o);
                Log.i("BusinessDetails", checkResults.getInt(1) + ": "
                        + checkResults.getString(2) + ":" + checkResults.getString(4)
                        + ": " + checkResults.getString(3));
            }
            Log.i("BusinessDetails", "below ");
            if (!Guest.isGuestUser()) {
                PreparedStatement favoritesStatement = db.getStatement(
                        "SELECT * from tb_entity_favorites " +
                                "WHERE entity = CAST(? AS int) " +
                                "AND business = CAST(? AS int) ");
                Log.i("BusinessDetails", "is not guest ");
                favoritesStatement.setString(1, getCurrentUser().getEntity());
                favoritesStatement.setString(2, String.valueOf(selectedBusiness));
                Log.i("BusinessDetails", checkStatement.toString() + ", " + favoritesStatement.toString());
                ResultSet favoritesResults = db.query(favoritesStatement);
                while (favoritesResults.next()) {
                    selectedBusinessObject.setIsFavorited(true);
                }
            } else {
                Log.i("BusinessDetails", "is guest " );
            }
        } catch (SQLException e) {
            Log.e("BusinessDetails", e.getMessage());
        }
        return true;
    }

    public boolean toggleFavorited(){
        DatabaseModel.checkInitialization();
        Log.i("toggleFavorited", "here");
        if (toggle) {
            try {
                PreparedStatement checkStatement = db.getStatement("INSERT INTO tb_entity_favorites(entity, business) VALUES ( CAST(? AS int), CAST(? AS int) ) ");
                checkStatement.setString(1, getCurrentUser().getEntity());
                checkStatement.setString(2, String.valueOf(selectedBusiness));
                Log.i("toggleFavorited", checkStatement.toString());
                db.query(checkStatement);
            } catch (SQLException e) {
                Log.e("toggleFavorited", e.getMessage());
            }
        } else {
            try {
                PreparedStatement deleteStatement = db.getStatement("DELETE FROM tb_entity_favorites WHERE entity = CAST(? AS int) AND business = CAST(? AS int) ");
                deleteStatement.setString(1, getCurrentUser().getEntity());
                deleteStatement.setString(2, String.valueOf(selectedBusiness));
                Log.i("toggleFavorited", deleteStatement.toString());
                db.query(deleteStatement);
            } catch (SQLException e) {
                Log.e("toggleFavorited", e.getMessage());
            }
        }
        return true;
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
    public int registerUser(String email, String password, String first_name,
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
                checkResults = db.query(checkStatement);
                checkResults.next();
                String entity = checkResults.getString("entity");
                setCurrentUser(new User(email, first_name, last_name, isAdmin, entity));
                Log.d("Register User", "Success for email = " + email);
                return 0;
            }
        } catch (SQLException e) {
            Log.e("Register User", e.getMessage());
            return 2;

        }
    }

    boolean getCategories() {
        DatabaseModel.checkInitialization();
        ArrayList<String> categories = new ArrayList<String>();
        try {
            Log.i("getCategories", "category: made it inside try statement");
            ResultSet checkResults = db.query("SELECT description FROM tb_category");
            while ( checkResults.next() )
            {
                Log.i("getCategories", "category: "+ checkResults.getString(1));
                categories.add(checkResults.getString(1));
            }
            Log.i("getCategories", "categories retrieved: "+ categories);
            this.categories = categories;
            Log.i("getCategories", Integer.toString(this.categories.size()));
            return true;
        } catch(SQLException e) {
            Log.e("getCategories", e.getMessage());
            return false;
        }
    }

   public boolean removeUser(String email) {
       DatabaseModel.checkInitialization();
       try {
           PreparedStatement checkStatement = db.getStatement(
                   "UPDATE tb_entity SET is_inactive = now() where email = ?");
           Log.i("removeUser", "'UPDATE tb_entity SET is_inactive = now() where email = " + email + "'");
           checkStatement.setString(1, email);
           db.update(checkStatement);
           return true;
       } catch (SQLException e) {
           Log.e("removeUser", e.getMessage());
           return false;
       }
   }

    boolean queryBusinessList()
    {
        businessList = new ArrayList<BusinessListItem>();
        DatabaseModel.checkInitialization();
        Log.i("BusinessList", "here");
        try {
            String query = "SELECT b.business, b.name, avg_rating, array_agg(s.name), b.address_line_one, b.zip_code, b.city " +
                    "FROM tb_business b " +
                    "LEFT JOIN tb_business_subcategory bs " +
                    "ON b.business = bs.business " +
                    "LEFT JOIN tb_subcategory s " +
                    "ON bs.subcategory = s.subcategory " +
                    "LEFT JOIN tb_business_category bc " +
                    "ON b.business = bc.business ";
            if (!selectedCategory.equals("SEE ALL")){
                query = query + "WHERE bc.category = " +
                        "( SELECT category FROM tb_category WHERE description LIKE ? )";
            }
            query = query + "GROUP BY ( b.business, b.name, avg_rating ) ";

            PreparedStatement checkStatement = db.getStatement(query);
            if (!selectedCategory.equals("SEE ALL")){
                checkStatement.setString(1, selectedCategory);
            }
            Log.i("BusinessList", checkStatement.toString());
            ResultSet checkResults = db.query(checkStatement);
            while ( checkResults.next() )
            {
                String[] address = new String[3];
                address[0] = checkResults.getString(5);
                address[1] = checkResults.getString(6);
                address[2] = checkResults.getString(7);
                String rating = checkResults.getString(3);

                businessList.add( new BusinessListItem(checkResults.getInt(1), checkResults.getString(2), rating, address, (String[])checkResults.getArray(4).getArray() ) );
                Log.i("BusinessList", checkResults.getInt(1)+ ": " + checkResults.getString(2) + ", " + (String[])checkResults.getArray(4).getArray());
                Log.i("BusinessAddress", address[0] + " " + address[1] + " " + address[2]);
            }
        } catch (SQLException e) {
            Log.e("BusinessList", e.getMessage());
        }
        return true;
    }
    public int[] queryNumFavoritesAndReviews() {
        Log.i("queryNumFavoritesAndReviews", "here");
        int favs = 0;
        int rev = 0;
        try {
            PreparedStatement checkStatement = db.getStatement("SELECT COUNT(*) FROM tb_entity_favorites WHERE entity = CAST( ? AS int )");
            checkStatement.setString(1, getCurrentUser().getEntity());
            ResultSet checkResults = db.query(checkStatement);
            if (checkResults.next()) {
                favs = checkResults.getInt(1);
            }
            checkStatement = db.getStatement("SELECT COUNT(*) FROM tb_review WHERE entity = CAST( ? AS int )");
            checkStatement.setString(1, getCurrentUser().getEntity());
            checkResults = db.query(checkStatement);
            if (checkResults.next()) {
                rev = checkResults.getInt(1);
            }
        } catch (SQLException e){
            Log.e("queryNumFavoritesAndReviews", e.getMessage());
        }
        return new int[]{favs,rev};
    }

    boolean queryFavoritesBusinessList()
    {
        businessList = new ArrayList<BusinessListItem>();
        DatabaseModel.checkInitialization();
        Log.i("queryFavoritesBusinessList", "here");
        try {
            PreparedStatement checkStatement = db.getStatement("SELECT b.business, b.name, avg_rating, array_agg(s.name)" +
                    "FROM tb_business b " +
                    "LEFT JOIN tb_business_subcategory bs " +
                    "ON b.business = bs.business " +
                    "LEFT JOIN tb_subcategory s " +
                    "ON bs.subcategory = s.subcategory " +
                    "LEFT JOIN tb_business_category bc " +
                    "ON b.business = bc.business " +
                    "WHERE b.business in ( SELECT business FROM tb_entity_favorites WHERE entity = CAST( ? AS int ) ) " +
                    "GROUP BY ( b.business, b.name, avg_rating )");
            checkStatement.setString(1, getCurrentUser().getEntity());
            Log.i("queryFavoritesBusinessList", checkStatement.toString());
            ResultSet checkResults = db.query(checkStatement);
            while ( checkResults.next() )
            {
                businessList.add( new BusinessListItem(checkResults.getInt(1), checkResults.getString(2), checkResults.getString(3), (String[])checkResults.getArray(4).getArray() ) );
                Log.i("BusinessList", checkResults.getInt(1)+ ": " + checkResults.getString(2) + ", " + checkResults.getArray(4));
            }
        } catch (SQLException e) {
            Log.e("BusinessList", e.getMessage());
        } catch (Exception e) {
            Log.e("QueryReviewList", e.getMessage());
        }
        return true;
    }

    boolean submitReview(float rating) {
        try {
            PreparedStatement updateStatement = db.getStatement("" +
                    "UPDATE tb_business\n" +
                    "SET avg_rating = (SELECT (avg_rating * num_rating + ?) / (num_rating + 1) FROM tb_business WHERE business = ?),\n" +
                    "num_rating = (SELECT num_rating + 1 FROM tb_business WHERE business = ?)\n" +
                    "WHERE business = ?");
            updateStatement.setFloat(1, rating);
            updateStatement.setInt(2, getBusiness_id());
            updateStatement.setInt(3, getBusiness_id());
            updateStatement.setInt(4, getBusiness_id());
            db.update(updateStatement);
            return true;
        } catch (SQLException e) {
            Log.e("ReviewRating", e.getMessage());
            return false;
        }
    }

    boolean submitReview(float rating, String title, String review) {
        try {
            PreparedStatement updateStatement = db.getStatement("" +
                    "UPDATE tb_business\n" +
                    "SET avg_rating = (SELECT (avg_rating * num_rating + ?) / (num_rating + 1) FROM tb_business WHERE business = ?),\n" +
                    "num_rating = (SELECT num_rating + 1 FROM tb_business WHERE business = ?)\n" +
                    "WHERE business = ?;" +
                    "INSERT INTO tb_review (entity, business, rating, title, description) " +
                    "VALUES (?,?,?,?,?) ");
            updateStatement.setFloat(1, rating);
            updateStatement.setInt(2, getBusiness_id());
            updateStatement.setInt(3, getBusiness_id());
            updateStatement.setInt(4, getBusiness_id());
            updateStatement.setInt(5, Integer.valueOf(getCurrentUser().getEntity()));
            updateStatement.setInt(6, getBusiness_id());
            updateStatement.setFloat(7, rating);
            updateStatement.setString(8, title);
            updateStatement.setString(9, review);
            db.update(updateStatement);
            return true;
        } catch (SQLException e) {
            Log.e("ReviewRating", e.getMessage());
            return false;
        }
    }

    boolean queryReviewList() {
        ArrayList<Review> revs = new ArrayList<>();
        try {
            PreparedStatement checkStatement = db.getStatement("" +
                    "SELECT rating, title, description FROM tb_review WHERE business = ?");
            checkStatement.setInt(1, getBusiness_id());
            ResultSet checkResults = db.query(checkStatement);
            while ( checkResults.next() )
            {
                revs.add(new Review(checkResults.getString(2), checkResults.getString(3), checkResults.getFloat(1)));
                Log.i("QueryReviewList", checkResults.getString(3));
            }
        } catch (SQLException e) {
            Log.e("QueryReviewList", e.getMessage());
        }
        this.reviews = revs;
        return true;
    }

    boolean queryReviewListUser() {
        ArrayList<Review> revs = new ArrayList<>();
        try {
            PreparedStatement checkStatement = db.getStatement("" +
                    "SELECT rating, title, description, business, review FROM tb_review WHERE entity = ?");
            checkStatement.setInt(1, Integer.valueOf(getCurrentUser().getEntity()));
            ResultSet checkResults = db.query(checkStatement);
            while ( checkResults.next() )
            {
                revs.add(new Review(checkResults.getString(2), checkResults.getString(3), checkResults.getFloat(1), checkResults.getString(4), checkResults.getInt(5)));
            }
        } catch (SQLException e) {
            Log.e("QueryReviewList", e.getMessage());
        } catch (Exception e) {
            Log.e("QueryReviewList", e.getMessage());
        }
        this.reviews = revs;
        return true;
    }

    boolean deleteUserReview(int rev_id, float rating, String business) {
        DatabaseModel.checkInitialization();
        try {
            PreparedStatement checkStatement = db.getStatement(
                    "DELETE FROM tb_review WHERE review = ?");
            Log.i("removeReview", "'DELETE FROM tb_review WHERE review = " + rev_id + "'");
            checkStatement.setInt(1, rev_id);
            db.update(checkStatement);

            PreparedStatement updateStatement = db.getStatement("" +
                    "UPDATE tb_business\n" +
                    "SET avg_rating = (SELECT (avg_rating * num_rating - ?) / (num_rating - 1) FROM tb_business WHERE business = ?),\n" +
                    "num_rating = (SELECT num_rating - 1 FROM tb_business WHERE business = ?)\n" +
                    "WHERE business = ?");
            Log.i("removeReview", "'UPDATE tb_business SET avg_rating = " +
                    "(SELECT (avg_rating * num_rating - ?) / (num_rating - 1) FROM tb_business " +
                    "WHERE business = ?), num_rating = (SELECT num_rating - 1 FROM tb_business "+
                    "WHERE business = ?) WHERE business = ?'");
            updateStatement.setFloat(1, rating);
            updateStatement.setInt(2, Integer.valueOf(business));
            updateStatement.setInt(3, Integer.valueOf(business));
            updateStatement.setInt(4, Integer.valueOf(business));
            db.update(updateStatement);
            return true;
        } catch (SQLException e) {
            Log.e("removeReview", e.getMessage());
            return false;
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

