package edu.gatech.juniordesign.juniordesignpart2;

import java.util.ArrayList;

public class BusinessObject {
    private int iD;
    private String name;
    private String category;
    private String rating;
    private String extraDetails;
    private String aboutTheOwner;
    private ArrayList<String> reviews;
    private boolean isFavorited;
    private String description;
    private String image_url;

    //will need to store a profile photo here but I dont think we have any photots yet

    public BusinessObject(int iD, String name, String category, String rating, String extraDetails, String
                          aboutTheOwner, String description, String image_url, ArrayList<String> reviews) {
        this.iD = iD;
        this.name = name;
        this.category = category;
        this.rating = rating;
        this.extraDetails = extraDetails;
        this.aboutTheOwner = aboutTheOwner;
        this.reviews = reviews;
        this.description = description;
        this.image_url = image_url;
        this.isFavorited = false;

    }
    public void setIsFavorited(boolean isFavorited){ this.isFavorited = isFavorited; };

    public boolean getIsFavorited() { return this.isFavorited; };

    public int getID() {
        return this.iD;
    }

    public String getName() {
        return this.name;
    }

    public String getCategory() {
        return this.category;
    }

    public String getRating() {
        return this.rating;
    }

    public String getImage_url() {
        return this.image_url;
    }

    public String getDescription() {
        return this.description;
    }

    public String getExtraDetails() {
        return this.extraDetails;
    }

    public String getAboutTheOwner() {
        return this.aboutTheOwner;
    }

    //I suggest making a review class and using it in the array list to store strings, ratings and the user
    public ArrayList<String> getReviews() {
        return this.reviews;
    }

}
