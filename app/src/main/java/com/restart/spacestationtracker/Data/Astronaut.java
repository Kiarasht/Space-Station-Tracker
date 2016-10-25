package com.restart.spacestationtracker.data;

import android.support.annotation.NonNull;

@SuppressWarnings("unused")
public class Astronaut implements Comparable<Astronaut> {

    // Data needed to represent a single astronaut
    private String name, image, country, countryLink, launchDate, role, location, bio, wiki, twitter;
    private int careerDays;

    public Astronaut(String name, String image, String country, String countryLink, String launchDate, String role, String location, String bio, String wiki, String twitter, int careerDays) {
        this.name = name;
        this.image = image;
        this.country = country;
        this.countryLink = countryLink;
        this.launchDate = launchDate;
        this.role = role;
        this.location = location;
        this.bio = bio;
        this.wiki = wiki;
        this.twitter = twitter;
        this.careerDays = careerDays;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCountryLink() {
        return countryLink;
    }

    public void setCountryLink(String countryLink) {
        this.countryLink = countryLink;
    }

    public String getLaunchDate() {
        return launchDate;
    }

    public void setLaunchDate(String launchDate) {
        this.launchDate = launchDate;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getWiki() {
        return wiki;
    }

    public void setWiki(String wiki) {
        this.wiki = wiki;
    }

    public String getTwitter() {
        return twitter;
    }

    public void setTwitter(String twitter) {
        this.twitter = twitter;
    }

    public int getCareerDays() {
        return careerDays;
    }

    public void setCareerDays(int careerDays) {
        this.careerDays = careerDays;
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
