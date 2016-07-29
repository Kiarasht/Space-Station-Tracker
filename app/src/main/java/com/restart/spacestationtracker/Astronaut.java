package com.restart.spacestationtracker;

public class Astronaut {
    private String name;
    private String role;
    private String image;
    private String wiki;

    public Astronaut() {
    }

    public String getName() {
        return name;
    }

    public String getImage() {
        return image;
    }

    public String getWiki() {
        return wiki;
    }

    public String getRole() {
        return role;
    }

    /**
     * Moves the commander Astronaut to the first on the list
     *
     * @param astronauts List of astronauts so be modified
     */
    public static void commanderFirst(Astronaut[] astronauts) {
        int index = 0;

        for (int i = 0; i < astronauts.length; ++i) {
            if (astronauts[i] != null && astronauts[i].getRole().equals("Commander")) {
                index = i;
                break;
            }
        }

        if (index != 0) {
            Astronaut temp = astronauts[0];
            astronauts[0] = astronauts[index];
            astronauts[index] = temp;
        }
    }
}
