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
        Planet two = new Planet(0, 9, 0, 0, 0, Size.SMALL);
        Planet three = new Planet(0, 10, 0, 0, 0, Size.SMALL);
        Planet four = new Planet(1, 10, 0, 0, 0, Size.SMALL);

        Assert.assertEquals(Universe.getTimeToTravel(one, two), 1, "Less than 10 should be 1 jump apart");
        Assert.assertEquals(Universe.getTimeToTravel(one, three), 1, "Exactly 10 should be 1 jump apart");
        Assert.assertEquals(Universe.getTimeToTravel(one, four), 2, "Even slightly more than 10 should be 2 jumps");

    }
}
