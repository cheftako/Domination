package com.linkedin.domination.api;

/**
 * A basic immutable planet class.
 */
public class Planet {

    public final int x;
    public final int y;
    public final int id;
    public final int owner;
    public final int population;
    public final Size size;

    public Planet(int x, int y, int id, int owner, int population, Size size) {
        this.x = x;
        this.y = y;
        this.id = id;
        this.owner = owner;
        this.population = population;
        this.size = size;
    }
}
