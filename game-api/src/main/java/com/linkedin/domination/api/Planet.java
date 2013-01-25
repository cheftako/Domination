package com.linkedin.domination.api;

/**
 * A basic immutable planet class.
 */
public class Planet
{
    private final int x;
    private final int y;
    private final int id;
    private final int owner;
    private final int population;
    private final Size size;

    public Planet(int x, int y, int id, int owner, int population, Size size) {
        this.x = x;
        this.y = y;
        this.id = id;
        this.owner = owner;
        this.population = population;
        this.size = size;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getId() {
        return id;
    }

    public int getOwner() {
        return owner;
    }

    public int getPopulation() {
        return population;
    }

    public Size getSize() {
        return size;
    }

    public String toJson() {
        String json = "{";
        json += "\"planet\": ";
        json += id;
        json += ", \"player\": ";
        json += owner;
        json += ", \"ships\": ";
        json += population;
        json += ", \"turn\": {turn}},";
        return json;
    }
}
