package com.linkedin.domination.api;

/**
 * Created with IntelliJ IDEA.
 * User: wfender
 * Date: 1/12/13
 * Time: 5:04 PM
 * To change this template use File | Settings | File Templates.
 */
public interface Event
{
    public enum EventType
    {
        LAUNCH,
        LANDING
    };

    public EventType getEventType();
    public Integer getFromPlanet();
    public Integer getToPlanet();
    public Size gtFleetSize();
}
