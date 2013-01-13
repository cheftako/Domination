package com.linkedin.domination.server;

import com.linkedin.domination.api.Planet;
import com.linkedin.domination.api.Universe;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Some basic tests for universe generation
 */
public class TestUniverseGenerator {

    @Test
    public void testUniverseCreation() {
        UniverseGenerator generator = new UniverseGenerator(40, 1000, 800, 40);
        Universe universe = generator.createUniverse();

        int playerCount = 0;

        for(Planet planet : universe.getPlanets())
        {
            if (planet.getOwner() != 0)
            {
                playerCount++;
            }

            for(Planet secondPlanet : universe.getPlanets())
            {
                // Yay nested for loops!
                if(planet != secondPlanet)
                {
                    int sizeBetween = Universe.getTimeToTravel(planet, secondPlanet);
                    Assert.assertTrue(sizeBetween >= 2, "Two planets were generated too close to each other");
                }
            }
        }

        Assert.assertEquals(playerCount, 3, "There should be 3 players");
        Assert.assertEquals(universe.getPlanets().size(), 40, "There should be 40 planets");
    }
}
