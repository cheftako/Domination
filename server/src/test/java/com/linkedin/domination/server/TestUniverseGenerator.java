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
        }

        Assert.assertEquals(playerCount, 3, "There should be 3 players");
    }
}
