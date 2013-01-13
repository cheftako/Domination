package com.linkedin.domination.server;

import com.linkedin.domination.api.Event;
import com.linkedin.domination.api.Size;

import java.util.List;

/**
 * A Landing event..
 */
public class LandingEvent implements Event {

    private int _origin;
    private int _destination;
    private Size _size;
    private int _shipSentCount;
    private int _shipRemainingCount;
    private List<Integer> _playersInvolved;
    private int _owner;

    public LandingEvent(int _origin,
                        int _destination,
                        Size _size,
                        int shipsSent,
                        int shipsRemaining,
                        List<Integer> playersInvolved,
                        int owner) {
        this._origin = _origin;
        this._destination = _destination;
        this._size = _size;
        this._shipSentCount = shipsSent;
        this._shipRemainingCount = shipsRemaining;
        this._playersInvolved = playersInvolved;
        this._owner = owner;
    }

    @Override
    public EventType getEventType() {
        return EventType.LANDING;
    }

    @Override
    public Integer getFromPlanet() {
        return _origin;
    }

    @Override
    public Integer getToPlanet() {
        return _destination;
    }

    @Override
    public Size getFleetSize() {
        return _size;
    }

    @Override
    public int getFleetOwner() {
        return _owner;
    }

    @Override
    public int getSentShipCount() {
        return _shipSentCount;
    }

    @Override
    public int getAfterBattleShipCount() {
        return _shipRemainingCount;
    }

    public void setShipRemainingCount(int remaining)
    {
        this._shipRemainingCount = remaining;
    }
}
