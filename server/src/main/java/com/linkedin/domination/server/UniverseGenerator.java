package com.linkedin.domination.server;

import com.linkedin.domination.api.Planet;
import com.linkedin.domination.api.Size;
import com.linkedin.domination.api.Universe;

import java.util.*;

/**
 * Generate a random universe. Planets are set to be at least 2 moves from each other. The northern most planet
 * is claimed by player 1. The south west is claimed by player 2. The south east is claimed by player 3.
 */
public class UniverseGenerator {

    public static double MIN_PLANET_DISTANCE = 80;   // Minimum planet distance, in pixels

    private Random _random;
    private int _size;
    private int _width;
    private int _height;
    private int _playerStartSize;

    public UniverseGenerator(int numberOfPlanets, int width, int height, int shipsPerPlayer) {
        _random = new Random();
        _size = numberOfPlanets;
        _width = width;
        _height = height;
        _playerStartSize = shipsPerPlayer;
    }

    public Universe createUniverse()
    {
        Map<Integer, Planet> planets = new HashMap<Integer, Planet>(_size);

        for (int i = 0; i < _size; i++)
        {
            boolean foundGoodPlanet = false;
            while(!foundGoodPlanet)
            {
                Planet planet = createRandomPlanet(i, _width, _height);
                if (isPlanetValid(planet, planets.values()))
                {
                    planets.put(planet.getId(), planet);
                    foundGoodPlanet = true;
                }
            }
        }

        Planet northIdeal = new Planet(_width / 2, _height, 0, 0, 0, Size.SMALL);
        Planet swIdeal = new Planet(0, 0, 0, 0, 0, Size.SMALL);
        Planet seIdeal = new Planet(_width, 0, 0, 0, 0, Size.SMALL);

        Planet north = getClosestPlanet(northIdeal, planets.values());
        Planet southWest = getClosestPlanet(swIdeal, planets.values());
        Planet southEast = getClosestPlanet(seIdeal, planets.values());

        planets.put(north.getId(), clonePlanetWithNewOwner(1, north));
        planets.put(southWest.getId(), clonePlanetWithNewOwner(2, southWest));
        planets.put(southEast.getId(), clonePlanetWithNewOwner(3, southEast));

        return new Universe(planets);
    }

    private Planet clonePlanetWithNewOwner(int owner, Planet planet)
    {
        return new Planet(planet.getX(), planet.getY(), planet.getId(), owner, _playerStartSize, Size.getSizeForNumber(_playerStartSize));
    }

    private Planet getClosestPlanet(Planet ideal, Collection<Planet> planets)
    {
        Planet closest = null;
        int nearestDistance = Integer.MAX_VALUE;

        for(Planet planet : planets)
        {
            int distanceFromIdeal = Universe.getTimeToTravel(planet, ideal);
            if (distanceFromIdeal < nearestDistance)
            {
                nearestDistance = distanceFromIdeal;
                closest = planet;
            }
        }

        return closest;
    }

    private boolean isPlanetValid(Planet planet, Collection<Planet> planets)
    {
        for (Planet existingPlanet : planets)
        {
            if (Universe.getTimeToTravel(planet, existingPlanet) < 2 || Universe.getPlanetDistance(planet, existingPlanet) < MIN_PLANET_DISTANCE)
            {
                return false;
            }
        }
        return true;
    }


    private Planet createRandomPlanet(int id, int width, int height)
    {

        //80% small, 15% med, 5% large
        int x = _random.nextInt(width);
        int y = _random.nextInt(height);
        int size = getRandomPlanetSize();

        return new Planet(x, y, id, 0, size, Size.getSizeForNumber(size));
    }

    private int getRandomPlanetSize()
    {
        int basicSize = _random.nextInt(100);

        if (basicSize < 80)
        {
            return _random.nextInt(20);
        } else if (basicSize < 95)
        {
            return _random.nextInt(30) + 20;
        } else
        {
            return _random.nextInt(40) + 50;
        }
    }
}
