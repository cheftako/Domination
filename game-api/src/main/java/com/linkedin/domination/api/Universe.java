package com.linkedin.domination.api;

import java.util.Collection;
import java.util.Map;

/**
 * The universe as we know it. Contains List of all of the planets as you see them. There is also a helper method
 * to find the distance in turns between any two planets.
 */
public class Universe
{
    private final Map<Integer, Planet> _planets;
    public static final int DISTANCE_FOR_TURN = 20;

    public Universe(Map<Integer, Planet> _planets) {
        this._planets = _planets;
    }

    public int getTimeToTravel(int planetOne, int planetTwo)
    {
        return getTimeToTravel(_planets.get(planetOne), _planets.get(planetTwo));
    }

    public static double getPlanetDistance(Planet first, Planet second)
    {
        if (first == null || second == null)
        {
            return 0;
        }

        int a = first.getX() - second.getX();
        int b = first.getY() - second.getY();
        double c2 = a * a + b * b;
        return Math.sqrt(c2);
    }

    public static int getTimeToTravel(Planet first, Planet second)
    {
        return roundToNextTen(getPlanetDistance(first, second)) -1; // -1 because the fleet travels the turn it launches as well
    }

    private static int roundToNextTen(double c)
    {
        int turns = (int) c / DISTANCE_FOR_TURN;
        if (c % DISTANCE_FOR_TURN > 0){
            turns++;
        }
        return turns;
    }

    public Map<Integer, Planet> getPlanetMap()
    {
        return _planets;
    }

    public Collection<Planet> getPlanets()
    {
        return _planets.values();
    }

    @Override
    public String toString()
    {
        String result = new String();
        result += "***** UNIVERSE *****\n";
        for (Integer planetId : _planets.keySet())
        {
            Planet planet = _planets.get(planetId);
            result += "Planet(" + planetId + ") x=" + planet.getX() + ", y=" + planet.getY() + ", size=" + planet.getSize() + ", population=" + planet.getPopulation() + ", owner=" + planet.getOwner() + "\n";
        }
        result += "***** UNIVERSE *****\n";
        return result;
    }
}
