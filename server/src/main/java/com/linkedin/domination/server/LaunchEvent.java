package com.linkedin.domination.server;

import com.linkedin.domination.api.Event;
import com.linkedin.domination.api.Size;

/**
 * A Launch event.
 */
public class LaunchEvent implements Event {

    private final Integer _origin;
    private final Integer _destination;
    private final Size _size;
    private final int _owner;
    private final int _sentShipCount;

    public LaunchEvent(Integer _origin, Integer _destination, Size _size, int owner, int sentShipCount) {
        this._origin = _origin;
        this._destination = _destination;
        this._size = _size;
        this._owner = owner;
        this._sentShipCount = sentShipCount;
    }

    @Override
    public EventType getEventType() {
        return EventType.LAUNCH;
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
        return _sentShipCount;
    }

    @Override
    public int getAfterBattleShipCount() {
        return -1;
    }
}
