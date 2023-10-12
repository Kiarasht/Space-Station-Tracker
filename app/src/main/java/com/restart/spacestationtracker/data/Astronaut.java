package com.restart.spacestationtracker.data;

import androidx.annotation.NonNull;

/**
 * Class of type Astronaut. Every single astronaut gets one.
 */
public class Astronaut {

    // Data needed to represent a single astronaut
    private final String name;
    private final String image;
    private final boolean isIss;
    private final String flagCode;
    private final int launchDate;
    private final String role;
    private final String location;
    private final String bio;
    private final String wiki;
    private final String twitter;
    private final String facebook;
    private final String instagram;

    /**
     * Instantiates a new Astronaut.
     *
     * @param name       The name of the astronaut
     * @param image      The url to a profile image of the astronaut
     * @param isIss      If astronaut is at ISS or Tiangong-2
     * @param flagCode   The url to a country flag the astronaut was born in
     * @param launchDate The date where the astronaut was launched
     * @param role       The role the astronaut has on the aircraft
     * @param location   The location astronaut is located at. (Often times it's ISS)
     * @param wiki       The wiki url of the astronaut
     * @param twitter    The twitter url of the astronaut
     * @param facebook   The facebook url of the astronaut
     * @param instagram  The instagram url of the astronaut
     */
    public Astronaut(String name, String image, boolean isIss, String flagCode, int launchDate, String role, String location, String bio, String wiki, String twitter, String facebook, String instagram) {
        this.name = name;
        this.image = image;
        this.isIss = isIss;
        this.flagCode = flagCode;
        this.launchDate = launchDate;
        this.role = role;
        this.location = location;
        this.bio = bio;
        this.wiki = wiki;
        this.twitter = twitter;
        this.facebook = facebook;
        this.instagram = instagram;
    }

    /**
     * Gets name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets image.
     *
     * @return the image
     */
    public String getImage() {
        return image;
    }

    /**
     * Gets if astronaut from Iss.
     *
     * @return the image
     */
    public boolean getIsIss() {
        return isIss;
    }

    /**
     * Gets country link.
     *
     * @return the country link
     */
    public String getFlagCode() {
        return flagCode;
    }

    /**
     * Gets launch date.
     *
     * @return the launch date
     */
    public int getLaunchDate() {
        return launchDate;
    }

    /**
     * Gets role.
     *
     * @return the role
     */
    public String getRole() {
        return role;
    }

    /**
     * Gets location.
     *
     * @return the location
     */
    public String getLocation() {
        return location;
    }

    /**
     * Gets bio.
     *
     * @return the bio
     */
    public String getBio() {
        return bio;
    }

    /**
     * Gets wiki.
     *
     * @return the wiki
     */
    public String getWiki() {
        return wiki;
    }

    /**
     * Gets twitter.
     *
     * @return the twitter
     */
    public String getTwitter() {
        return twitter;
    }

    /**
     * Gets facebook.
     *
     * @return the facebook
     */
    public String getFacebook() {
        return facebook;
    }

    /**
     * Gets instagram.
     *
     * @return the instagram
     */
    public String getInstagram() {
        return instagram;
    }
}
