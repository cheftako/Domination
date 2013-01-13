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
        return getTimeToTravel(_planets.get(planetOne), _planets.get(planetTwo));
    }

    public static int getTimeToTravel(Planet first, Planet second)
    {
        if (first == null || second == null)
        {
            return 0;
        }

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

    public Map<Integer, Planet> getPlanetMap()
    {
        return _planets;
    }

    public Collection<Planet> getPlanets()
    {
        return _planets.values();
    }
}
