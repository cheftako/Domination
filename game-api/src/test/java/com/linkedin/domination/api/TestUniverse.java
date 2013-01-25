package com.linkedin.domination.api;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tests for the Universe class.
 */
public class TestUniverse {

    @Test
    public void testDistances()
    {
        Planet one = new Planet(0, 0, 0, 0, 0, Size.SMALL);
        Planet two = new Planet(0, Universe.DISTANCE_FOR_TURN - 1, 0, 0, 0, Size.SMALL);
        Planet three = new Planet(0, Universe.DISTANCE_FOR_TURN, 0, 0, 0, Size.SMALL);
        Planet four = new Planet(1, Universe.DISTANCE_FOR_TURN, 0, 0, 0, Size.SMALL);

        Assert.assertEquals(Universe.getTimeToTravel(one, two), 1, "Less than " + Universe.DISTANCE_FOR_TURN + " should be 1 jump apart");
        Assert.assertEquals(Universe.getTimeToTravel(one, three), 1, "Exactly " + Universe.DISTANCE_FOR_TURN + " should be 1 jump apart");
        Assert.assertEquals(Universe.getTimeToTravel(one, four), 2, "Even slightly more than " + Universe.DISTANCE_FOR_TURN + " should be 2 jumps");

    }
}
