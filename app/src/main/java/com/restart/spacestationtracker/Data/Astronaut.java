package com.restart.spacestationtracker.data;

import java.util.LinkedList;
import java.util.List;

public class Astronaut {

    // Data needed to represent a single astronaut
    private String name, role, image, wiki, status;

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

    public String getStatus() {
        return status;
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

    /**
     * Gets back a string array containing only astronaut names;
     *
     * @param astronauts Get the names from the Astronaut array
     * @return A string array holding the names
     */
    public static String[] getNames(Astronaut[] astronauts) {
        String[] names = new String[astronauts.length];

        for (int i = 0; i < astronauts.length; ++i) {
            names[i] = astronauts[i].getName();
        }

        return names;
    }

    /**
     * Remove any astronauts from the database which are not currently on duty.
     *
     * @param astronauts List of astronauts that need to be filtered
     * @return A new Astronaut array that contains only on duty astronauts
     */
    public static Astronaut[] offDuty(Astronaut[] astronauts) {
        if (astronauts == null || astronauts.length == 0) {
            return astronauts;
        }

        List<Astronaut> result = new LinkedList<>();
        for (Astronaut aAstronaut : astronauts) {
            if (aAstronaut.getStatus().equals("true")) {
                result.add(aAstronaut);
            }
        }

        astronauts = new Astronaut[result.size()];
        result.toArray(astronauts);
        return astronauts;
    }
}
