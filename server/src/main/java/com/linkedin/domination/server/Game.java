package com.linkedin.domination.server;

import com.linkedin.domination.api.*;

import java.util.*;

/**
 * 1. Movement
 * 2. Combat
 * 3. Growth
 */
public class Game {
    private final Universe _universe;
    private final List<Fleet> _currentFleets;
    private final Map<Integer, Player> _players;
    private final Map<Player, Integer> _playerIds;
    private final int _maxTurns;
    private int _turnNumber;

    public Game(Universe _universe, Map<Integer, Player> players, int maxTurns) {
        this._universe = _universe;
        this._currentFleets = new LinkedList<Fleet>();
        this._players = players;
        this._maxTurns = maxTurns;
        this._turnNumber = 0;

        this._playerIds = new HashMap<Player, Integer>(_players.size());

    }

    public Universe start()
    {
        List<Event> lastTurnEvents = new ArrayList<Event>();

        while(!isOver())
        {
            List<Event> thisTurnEvents = new ArrayList<Event>();
            Map<Planet, List<Fleet>> conflictMap = new HashMap<Planet, List<Fleet>>();

            // Get user commands
            for(Player player : _players.values())
            {
                List<Move> playerMoves = player.makeMove(lastTurnEvents);
                List<Fleet> playerFleets = getFleetsForPlayer(player, playerMoves);
                _currentFleets.addAll(playerFleets);
                List<Event> playerEvents = getEventsForFleets(playerFleets);
                thisTurnEvents.addAll(playerEvents);
            }

            // movement
            for(Fleet fleet : _currentFleets)
            {
                fleet.incrementTurn();

                // check if landing on a friendly planet
                if(fleet.hasArrived())
                {
                    if(isFleetAtFriendlyPlanet(fleet)) {
                        // TODO: reinforce planet & generate event?
                    } else {
                        // combat setup
                        Planet conflictPlanet = fleet.get_destination();
                        List<Fleet> conflict = conflictMap.containsKey(conflictPlanet) ? conflictMap.get(conflictPlanet) : new ArrayList<Fleet>();
                        conflict.add(fleet);
                        conflictMap.put(conflictPlanet, conflict);
                    }
                }
            }

            List<Event> combatEvents = combat(conflictMap);
            thisTurnEvents.addAll(combatEvents);
            lastTurnEvents = thisTurnEvents;

        }

        return _universe;
    }

    private boolean isOver()
    {
        if(this._turnNumber >= _maxTurns) return true;
        Set<Integer> playerSet = new HashSet<Integer>();

        for(Planet planet : _universe.getPlanets())
        {
            playerSet.add(planet.getOwner());
            if(playerSet.size() > 1) return false;
        }

        return true;
    }

    private Collection<Fleet> condenseFleets(List<Fleet> fleets)
    {
        Map<Integer, Fleet> condenser = new HashMap<Integer, Fleet>();
        for(Fleet fleet : fleets)
        {
            Fleet condensingFleet = condenser.get(fleet.getOwner());
            if (condensingFleet == null) {
                condenser.put(fleet.getOwner(), fleet);
            } else {
                Fleet condensed = new Fleet(fleet.get_origin(),
                        fleet.get_destination(),
                        fleet.getTurnsRemaining(),
                        fleet.getOwner(),
                        fleet.getSize() + condensingFleet.getSize());
                condenser.put(fleet.getOwner(), fleet);
            }
        }
        return condenser.values();
    }
    /**
     * Breaking stuff!!
     * @param conflictMap
     * @return
     */
    private List<Event> combat(Map<Planet, List<Fleet>> conflictMap)
    {
        List<Event> combatEvents = new ArrayList<Event>(conflictMap.size());

        for(Planet battleGround : conflictMap.keySet())
        {
            List<Fleet> rawFleets = conflictMap.get(battleGround);

            // TODO : Since we condense we lose origins
            Collection<Fleet> fleetsInvolved = condenseFleets(rawFleets);

            int battleSplit = fleetsInvolved.size(); // size before adding planet
            Fleet planetFleet = new Fleet(null, battleGround, 0, battleGround.getOwner(), battleGround.getPopulation());
            fleetsInvolved.add(planetFleet);

            List<Integer> playersInvolved = new ArrayList<Integer>(fleetsInvolved.size());
            for(Fleet fleet : fleetsInvolved) {
                playersInvolved.add(fleet.getOwner());
            }

            Map<Fleet, Event> battleEvents = new HashMap<Fleet, Event>(fleetsInvolved.size());
            for(Fleet fleet : fleetsInvolved)
            {
                Event landing = new LandingEvent(fleet.get_origin().getId(),
                        fleet.get_destination().getId(),
                        Size.getSizeForNumber(fleet.getSize()),
                        fleet.getSize(),
                        fleet.getSize(),
                        playersInvolved
                        );
                battleEvents.put(fleet, landing);
            }


            while(fleetsInvolved.size() > 1)
            {
                List<Fleet> nextRoundFleets = new ArrayList<Fleet>(fleetsInvolved.size());
                for(Fleet fleet : fleetsInvolved)
                {
                    int size = fleet.getSize();
                    for(Fleet otherFleet : fleetsInvolved)
                    {
                        if (fleet != otherFleet)
                        {
                            size = size - (otherFleet.getSize() / battleSplit);
                        }
                    }

                    if(size > 0) {
                        fleet.setSize(size);
                        nextRoundFleets.add(fleet);
                        LandingEvent fleetUpdate = (LandingEvent) battleEvents.get(fleet);
                        fleetUpdate.setShipRemainingCount(size);
                    } else {
                        LandingEvent fleetUpdate = (LandingEvent) battleEvents.get(fleet);
                        fleetUpdate.setShipRemainingCount(0); // all your ships died.. you lost.
                    }

                    // update event

                }

                fleetsInvolved = nextRoundFleets;
            }

            if(fleetsInvolved.isEmpty()) {
                // Neutral just got a planet
                Planet everyoneLost = makePlanetWithNewOwnerAndSize(battleGround, 0, 0);
                updatePlanet(everyoneLost);
            } else {
                // new owner
                Fleet winner = fleetsInvolved.iterator().next();
                int winnerSize = winner.getSize();
                if(winner.getOwner() != battleGround.getOwner())
                {
                    winnerSize--;
                }
                if (winnerSize > 0)
                {
                    Planet winningPlanet = makePlanetWithNewOwnerAndSize(battleGround, winner.getOwner(), winnerSize);
                    updatePlanet(winningPlanet);
                } else {
                    Planet everyoneStillLost = makePlanetWithNewOwnerAndSize(battleGround, 0, 0);
                    updatePlanet(everyoneStillLost);
                }
            }

            // Add to events
            combatEvents.addAll(battleEvents.values());
        }

        return combatEvents;
    }

    private void updatePlanet(Planet planet)
    {
        _universe.getPlanetMap().put(planet.getId(), planet);
    }

    private Planet makePlanetWithNewOwnerAndSize(Planet planet, int owner, int size)
    {
        Planet terra = new Planet(planet.getX(),
                planet.getY(),
                planet.getId(),
                owner,
                size,
                Size.getSizeForNumber(size));

        return terra;
    }

    private boolean isFleetAtFriendlyPlanet(Fleet fleet)
    {
        Planet destination = fleet.get_destination();
        return destination.getOwner() == fleet.getOwner();
    }

    private List<Event> getEventsForFleets(List<Fleet> fleets)
    {
        List<Event> launchEvents = new ArrayList<Event>(fleets.size());

        for(Fleet fleet : fleets)
        {
            LaunchEvent event = new LaunchEvent(fleet.get_origin().getId(),
                    fleet.get_destination().getId(),
                    Size.getSizeForNumber(fleet.getSize()));
            launchEvents.add(event);
        }
        return launchEvents;
    }

    private List<Fleet> getFleetsForPlayer(Player player, List<Move> moves)
    {
        List<Fleet> playerFleets = new ArrayList<Fleet>(moves.size());

        for(Move move : moves)
        {
            if(moveValidForPlayer(player, move))
            {
                int fleetSize = getFleetSize(move);
                if (fleetSize > 0)
                {
                    Planet origin = _universe.getPlanetMap().get(move.getFromPlanet());
                    Planet destination = _universe.getPlanetMap().get(move.getToPlanet());

                    Fleet fleet = new Fleet(origin,
                            destination,
                            Universe.getTimeToTravel(origin, destination),
                            _playerIds.get(player),
                            fleetSize);
                    playerFleets.add(fleet);
                }
            }
        }

        return playerFleets;
    }

    private int getFleetSize(Move move)
    {
        Planet origin = _universe.getPlanetMap().get(move.getFromPlanet());
        if (origin == null) return 0;

        int shipsOnPlanet = origin.getPopulation();
        int scouting = shipsOnPlanet / 4;

        if (shipsOnPlanet == 0) return 0;
        switch (move.getSize()) {
            case NONE:
                return 0;
            case SCOUTING: // .25
                return scouting;
            case RAIDING: // .5
                return scouting * 2;
            case ASSAULT: // .75
                return  scouting * 3;
            default:
                return shipsOnPlanet;
        }
    }

    private boolean moveValidForPlayer(Player player, Move move)
    {
        Planet origin = _universe.getPlanetMap().get(move.getFromPlanet());
        Planet destination = _universe.getPlanetMap().get(move.getToPlanet());
        if (origin == null || destination == null) return false;
        return origin.getOwner() == _playerIds.get(player);
    }
}
