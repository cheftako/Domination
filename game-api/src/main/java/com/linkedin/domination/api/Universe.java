package com.linkedin.domination.api;

import java.util.Collection;
import java.util.Map;

/**
 * The universe as we know it. Contains List of all of the planets as you see them. There is also a helper method
 * to find the distance in turns between any two planets.
 */
public class Universe {

    private final Map<Integer, Planet> _planets;

    public Universe(Map<Integer, Planet> _planets) {
        this._planets = _planets;
    }

    public int getTimeToTravel(int planetOne, int planetTwo)
    {
        Planet first = _planets.get(planetOne);
        Planet second = _planets.get(planetTwo);

        if (first == null || second == null)
        {
            return 0;
        }

        int a =first.getX() - second.getX();
        int b = first.getY() - second.getY();
        double c2 = a * a + b * b;
        int c = (int) Math.sqrt(c2);
        return c / 10 + 1;
    }

    public int getTimeToTravel(Planet planetOne, Planet planetTwo)
    {
        if (planetOne == null || planetTwo == null)
        {
            return 0;
        }
        return getTimeToTravel(planetOne.getId(), planetTwo.getId());
    }

    public Map<Integer, Planet> getPlanetMap()
    {
        return _planets;
    }

    public Collection<Planet> getPlanets()
    {
        return _planets.values();
    }
}
