package com.restart.spacestationtracker.data;

import android.support.annotation.NonNull;

/**
 * Class of type Astronaut. Every single astronaut gets one.
 */
public class Astronaut implements Comparable<Astronaut> {

    // Data needed to represent a single astronaut
    private String name, image, countryLink, launchDate, role, location, bio, wiki, twitter;

    /**
     * Instantiates a new Astronaut.
     *
     * @param name        The name of the astronaut
     * @param image       The url to a profile image of the astronaut
     * @param countryLink The url to a country flag the astronaut was born in
     * @param launchDate  The date where the astronaut was launched
     * @param role        The role the astronaut has on the aircraft
     * @param location    The location astronaut is located at. (Often times it's ISS)
     * @param bio         The bio associated with the astronaut
     * @param wiki        The wiki url of the astronaut
     * @param twitter     The twitter url of the astronaut
     */
    public Astronaut(String name, String image, String countryLink, String launchDate, String role, String location, String bio, String wiki, String twitter) {
        this.name = name;
        this.image = image;
        this.countryLink = countryLink;
        this.launchDate = launchDate;
        this.role = role;
        this.location = location;
        this.bio = bio;
        this.wiki = wiki;
        this.twitter = twitter;
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
     * Gets country link.
     *
     * @return the country link
     */
    public String getCountryLink() {
        return countryLink;
    }

    /**
     * Gets launch date.
     *
     * @return the launch date
     */
    public String getLaunchDate() {
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
     * Sorts astronauts by their location first (ISS, Tiangong-2, etc...) then by their roles
     * (Commander, Flight Engineer, etc...)
     *
     * @param astronaut An astronaut that needs to be compared with current astronaut
     * @return Smaller, equal, or bigger?
     */
    @Override
    public int compareTo(@NonNull Astronaut astronaut) {
        if (this.getLocation().compareTo(astronaut.getLocation()) != 0) {
            return this.getLocation().compareTo(astronaut.getLocation());
        } else {
            return this.getRole().compareTo(astronaut.getRole());
        }
    }
}
