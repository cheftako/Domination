package com.linkedin.domination.server;

import com.linkedin.domination.api.Event;
import com.linkedin.domination.api.Planet;
import com.linkedin.domination.api.Player;
import com.linkedin.domination.api.Universe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: wfender
 * Date: 1/27/13
 * Time: 10:32 PM
 * To change this template use File | Settings | File Templates.
 */
public class JsonWatcher implements Watcher
{
    private Universe _universe;
    private Map<Integer, List<Event>> _turnEventMap = new HashMap<Integer, List<Event>>();
    private Map<Integer, Player> _players = new HashMap<Integer, Player>();
    private Map<Integer, Integer> _lastAlive = new HashMap<Integer, Integer>();
    private Map<Integer, Integer> _places = new HashMap<Integer, Integer>();
    private String _initialPlanetState;
    private int _lastTurn = 0;
    private int _livePlayers = 3;

    public void setUniverse(Universe universe)
    {
        _universe = universe;
        _initialPlanetState = initialPlanetState();
    }

    public void setEvent(Event event, int turn)
    {
        List<Event> events = _turnEventMap.get(turn);
        if (events == null)
        {
            events = new ArrayList<Event>();
            _turnEventMap.put(turn, events);
        }
        events.add(event);
        if (turn > _lastTurn)
        {
            checkForDeadPlayers();
            _lastTurn = turn;
        }
    }

    public void setPlayer(int playerNbr, Player player)
    {
       _players.put(playerNbr, player);
    }

    private void checkForDeadPlayers() {
      Map<Integer, Boolean> isDead = new HashMap<Integer, Boolean>();
      for (Planet p : _universe.getPlanets()) {
        isDead.put(p.getOwner(), false);
      }
      for (Integer i : _players.keySet()) {
        if (isDead.get(i) == null && !_lastAlive.containsKey(i)) {
          // They are dead, but weren't before
          _lastAlive.put(i, _lastTurn);
          _places.put(i, _livePlayers);
          _livePlayers -= 1;
        }
      }
    }

    public void gameOver()
    {
        checkForDeadPlayers();
        System.out.println("{");
        printPlayers();
        printPlanets();
        printEvents();
        System.out.println("}");
    }

    private void printPlayers()
    {
        System.out.println(quote("players") + ": [");
        for (Integer playerId : _players.keySet())
        {
            Integer place = _places.containsKey(playerId) ? _places.get(playerId) : 1;
            System.out.println("  {\"id\": " + playerId +
                    ", \"name\": " + quote(_players.get(playerId).getPlayerName()) +
                    ", \"place\": " + quote(place.toString()) +
                    "},");
        }
        System.out.println("],");
    }

    private String quote(String item)
    {
        return "\"" + item + "\"";
    }

    private String braces(String item)
    {
        return "{" + item +  "}";
    }

    private String initialPlanetState()
    {
        StringBuffer result = new StringBuffer(quote("planets"));
        result.append(": [\n");
        for(Planet planet : _universe.getPlanets())
        {
            result.append("{");
            result.append(quote("id")).append(": ").append(planet.getId()).append(", ");
            result.append(quote("owner")).append(": ").append(planet.getOwner()).append(", ");
            result.append(quote("ships")).append(": ").append(planet.getPopulation()).append(", ");
            result.append(quote("x")).append(": ").append(planet.getX()).append(", ");
            result.append(quote("y")).append(": ").append(planet.getY());
            result.append("},\n");
        }
        result.append("],");
        return result.toString();
    }

    private void printPlanets()
    {
        System.out.println(_initialPlanetState);
    }

    private void printEvents()
    {
        System.out.println(quote("events") + ": [");
        // Ugly hack to get the turns to print in order
        for (int cntr = 0; cntr <= _lastTurn; cntr++)
        {
            List<Event> events = _turnEventMap.get(cntr);
            if (events == null)
            {
                continue;
            }
            for (Event event : events)
            {
                if (event instanceof LaunchEvent)
                {
                    printLaunchEvent((LaunchEvent)event, cntr);
                }
                else if (event instanceof LandingEvent)
                {
                    printLandingEvent((LandingEvent)event, cntr);
                }
            }
        }
        System.out.println("]");
    }

    private void printLaunchEvent(LaunchEvent launchEvent, int turn)
    {
        System.out.println(braces(
            quote("destination") + ": " + launchEvent.getToPlanet() + ", " +
            quote("duration") + ": " + launchEvent.getFlightDuration() + ", " +
            quote("origin") + ": " + launchEvent.getFromPlanet() + ", " +
            quote("player") + ": " + launchEvent.getFleetOwner() + ", " +
            quote("ships") + ": " + launchEvent.getSentShipCount() + ", " +
            quote("turn") + ": " + turn) + ",");
    }

    private void printLandingEvent(LandingEvent landingEvent, int turn)
    {
        if (landingEvent.getAfterBattleShipCount() <= 0)
        {
            return;
        }
        System.out.println(braces(
            quote("planet") + ": " + landingEvent.getToPlanet() + ", " +
            quote("player") + ": " + landingEvent.getFleetOwner() + ", " +
            quote("ships") + ": " + landingEvent.getAfterBattleShipCount() + ", " +
            quote("turn") + ": " + turn) + ",");
    }
}
