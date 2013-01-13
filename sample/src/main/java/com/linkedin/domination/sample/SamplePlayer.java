package com.linkedin.domination.sample;

import com.linkedin.domination.api.*;
import sun.jvmstat.perfdata.monitor.PerfIntegerMonitor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class SamplePlayer implements Player
{
    private Integer _me = null;

    private List<Integer> _targets = new ArrayList();
    private List<Integer> _flee = new ArrayList();

    private static final Integer MIN_LAUNCH_SIZE = 100;

    public SamplePlayer()
    {
        // Nothing to be done yet.
    }

    @Override
    public void initialize(Integer playerNbr)
    {
        _me = playerNbr;
    }

    @Override
    public List<Move> makeMove(Universe universe, List<Event> events)
    {
        // Dumb API - launch when any planet can do a RAID move and remain a large planet.
        // We also flee like frightened children (HORDE) any incoming battle we do not KNOW we can beat.
        List<Move> result = new ArrayList<Move>();
        Map<Integer, Planet> planetMap = universe.getPlanetMap();

        for (Event event : events)
        {
            Integer targetId = event.getToPlanet();
            Planet target = planetMap.get(targetId);
            if (planetHasOrders(result, targetId))
            {
                continue;
            }
            if (event.getEventType() == com.linkedin.domination.api.Event.EventType.LANDING)
            {
                // This client isn't bright enough to worry about this.
                // Just check to see if its a recent landing of ours.
                if (event.getFleetOwner() == _me && target.getOwner() == _me)
                {
                    // Recent conquest so check its safe and clear any state
                    if (_targets.contains(targetId))
                    {
                        _targets.remove(targetId);
                    }
                    if (_flee.contains(targetId))
                    {
                        Integer newHome = getTarget(universe, target, result);
                        _flee.remove(targetId);
                        if (newHome != null)
                        {
                            Move flee = new Move(targetId, newHome, Move.FleetType.HORDE);
                            result.add(flee);
                            _targets.add(newHome);
                        }
                    }
                }
                continue;
            }
            if (event.getFleetOwner() == _me)
            {
                // Ignoring
                continue;
            }
            // Am I the target?
            if (_targets.contains(targetId) && event.getFleetSize() == Size.LARGE)
            {
                _flee.add(targetId);
                continue;
            }
            if (target.getOwner() == _me && isFleetLarger(event.getFleetSize(), target.getSize()))
            {
                Integer newHome = getTarget(universe, target, result);
                if (newHome != null)
                {
                    Move flee = new Move(targetId, newHome, Move.FleetType.HORDE);
                    result.add(flee);
                    _targets.add(newHome);
                }
            }
        }

        Collection<Planet> planets = universe.getPlanets();
        for (Planet source : planets)
        {
            if (source.getOwner() != _me)
            {
                continue;
            }
            if (planetHasOrders(result, source.getId()))
            {
                continue;
            }
            if (source.getPopulation() >= MIN_LAUNCH_SIZE)
            {
                Integer target = getTarget(universe, source, result);
                if (target != null)
                {
                    Move attack = new Move(source.getId(), target, Move.FleetType.RAIDING);
                    result.add(attack);
                    _targets.add(target);
                }
            }
        }

        return new ArrayList();
    }

    private Integer getTarget(Universe universe, Planet source, List<Move> orders)
    {
        Integer current = null;
        Integer distance = Integer.MAX_VALUE;
        Collection<Planet> planets = universe.getPlanets();
        for (Planet option : planets)
        {
            if (option.getOwner() == _me)
            {
                continue;
            }
            if (_targets.contains(option.getId()))
            {
                continue;
            }
            if (planetHasOrders(orders, option.getId()))
            {
                continue;
            }
            // We have found a viable target, now lets find the closest.
            Integer optionDistance = universe.getTimeToTravel(source, option);
            if (optionDistance < distance)
            {
                current = option.getId();
            }
        }
        return current;
    }

    private boolean planetHasOrders(List<Move> orders, Integer planetId)
    {
        for (Move order : orders)
        {
            if (order.getToPlanet() == planetId)
            {
                return true;
            }
        }
        return false;
    }

    private boolean isFleetLarger(Size enemy, Size friendly)
    {
        switch(friendly)
        {
            case LARGE:
                return false;
            case MEDIUM:
                if (enemy == Size.LARGE)
                {
                    return true;
                }
                return false;
            case SMALL:
                if (enemy == Size.LARGE || enemy == Size.MEDIUM)
                {
                    return true;
                }
                return false;
        }
        return true;
    }
}
