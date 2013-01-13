package com.linkedin.domination.server;

import com.linkedin.domination.api.Planet;

/**
 * Created with IntelliJ IDEA.
 * User: cmiller
 * Date: 1/12/13
 * Time: 8:14 PM
 * To change this template use File | Settings | File Templates.
 */
public class Fleet {

    private Planet _origin;
    private Planet _destination;
    private int turnsRemaining;
    private int owner;
    private int size;

    public Fleet(Planet _origin, Planet _destination, int turnsRemaining, int owner, int size) {
        this._origin = _origin;
        this._destination = _destination;
        this.turnsRemaining = turnsRemaining;
        this.owner = owner;
        this.size = size;
    }

    public Planet get_origin() {
        return _origin;
    }

    public void set_origin(Planet _origin) {
        this._origin = _origin;
    }

    public Planet get_destination() {
        return _destination;
    }

    public void set_destination(Planet _destination) {
        this._destination = _destination;
    }

    public int getTurnsRemaining() {
        return turnsRemaining;
    }

    public void setTurnsRemaining(int turnsRemaining) {
        this.turnsRemaining = turnsRemaining;
    }

    public void incrementTurn() {
        this.turnsRemaining--;
    }

    public int getOwner() {
        return owner;
    }

    public void setOwner(int owner) {
        this.owner = owner;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public boolean hasArrived()
    {
        return this.turnsRemaining == 0;
    }
}
