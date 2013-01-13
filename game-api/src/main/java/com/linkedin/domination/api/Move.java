package com.linkedin.domination.api;

/**
 * Created with IntelliJ IDEA.
 * User: wfender
 * Date: 1/12/13
 * Time: 4:54 PM
 * To change this template use File | Settings | File Templates.
 */
public class Move
{
    public enum FleetType
    {
      NONE,     // 0  %
      SCOUTING, // 25 %
      RAIDING,  // 50 %
      ASSAULT,  // 75 %
      HORDE     // 100 %
    }

    private Integer _fromPlanet;
    private Integer _toPlanet;
    private FleetType _size;

    public Move(Planet from, Planet to, FleetType size)
    {
        this(from.id, to.id, size);
    }

    public Move(Integer fromPlanet, Integer toPlanet, FleetType size)
    {
        _fromPlanet = fromPlanet;
        _toPlanet = toPlanet;
        _size = size;
    }

    public Integer getFromPlanet() {
        return _fromPlanet;
    }

    public void setFromPlanet(Integer fromPlanet) {
        _fromPlanet = fromPlanet;
    }

    public Integer getToPlanet() {
        return _toPlanet;
    }

    public void setToPlanet(Integer toPlanet) {
        _toPlanet = toPlanet;
    }

    public FleetType getSize() {
        return _size;
    }

    public void setSize(FleetType size) {
        _size = size;
    }


}
