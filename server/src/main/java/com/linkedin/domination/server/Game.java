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
    private final List<Watcher> _watchers;
    private final int _maxTurns;
    private int _turnNumber;

    public Game(Universe universe, Map<Integer, Player> players, int maxTurns, List<Watcher> watchers) {
        _universe = universe;
        _currentFleets = new LinkedList<Fleet>();
        _players = players;
        _maxTurns = maxTurns;
        _watchers = watchers;
        _turnNumber = 0;

        for (Watcher watcher : _watchers)
        {
            watcher.setUniverse(_universe);
        }

        this._playerIds = new HashMap<Player, Integer>(_players.size());
        for (Integer playerId : players.keySet())
        {
            Player player = players.get(playerId);
            _playerIds.put(player, playerId);
            for (Watcher watcher : _watchers)
            {
                watcher.setPlayer(playerId, player);
            }
        }
    }

    public Universe start()
    {
        List<Event> lastTurnEvents = new ArrayList<Event>();

        while(!isOver())
        {
            //System.out.print(".");
            List<Event> thisTurnEvents = new ArrayList<Event>();
            Map<Planet, List<Fleet>> conflictMap = new HashMap<Planet, List<Fleet>>();

            // Get user commands
            for(Player player : _players.values())
            {
                Universe playerUniverse = makeUniverseForPlayer(_playerIds.get(player));
                // TODO: lastTurnEvents should be curated per player
                List<Move> playerMoves = player.makeMove(playerUniverse, lastTurnEvents);
                List<Fleet> playerFleets = getFleetsForPlayer(player, playerMoves);
                _currentFleets.addAll(playerFleets);
                List<Event> playerEvents = getEventsForFleets(playerFleets);
                //printLaunchEvents(playerEvents);
                thisTurnEvents.addAll(playerEvents);
            }

            // movement
            List<Fleet> removeFleets = new ArrayList<Fleet>();
            for(Fleet fleet : _currentFleets)
            {
                fleet.incrementTurn();

                // check if landing on a friendly planet
                if(fleet.hasArrived())
                {
                    if(isFleetAtFriendlyPlanet(fleet)) {
                        Planet friendlyPlanet = _universe.getPlanetMap().get(fleet.get_destination());
                        Planet updated = makePlanetWithNewOwnerAndSize(friendlyPlanet, fleet.getOwner(), friendlyPlanet.getPopulation() + fleet.getSize());
                        updatePlanet(updated);
                        List<Integer> playersInvolved = new ArrayList<Integer>(1);
                        playersInvolved.add(fleet.getOwner());
                        Event reinforce = new LandingEvent(fleet.get_origin(),
                            fleet.get_destination(),
                            Size.getSizeForNumber(fleet.getSize()),
                            fleet.getSize(),
                            friendlyPlanet.getPopulation() + fleet.getSize(),
                            playersInvolved,
                            fleet.getOwner()
                        );
                        notifyOfEvent(reinforce);
                        // System.out.println(updated.toJson().replace("{turn}", _turnNumber + ""));
                    } else {
                        // combat setup
                        Planet conflictPlanet = _universe.getPlanetMap().get(fleet.get_destination());
                        List<Fleet> conflict = conflictMap.containsKey(conflictPlanet) ? conflictMap.get(conflictPlanet) : new ArrayList<Fleet>();
                        conflict.add(fleet);
                        conflictMap.put(conflictPlanet, conflict);
                    }
                    removeFleets.add(fleet);
                }
            }

            _currentFleets.removeAll(removeFleets);

            List<Event> combatEvents = combat(conflictMap);
            //printCombatEvents(combatEvents);
            thisTurnEvents.addAll(combatEvents);

            // Growth
            grow();

            lastTurnEvents = thisTurnEvents;
            _turnNumber++;
        }

        //System.out.println("Game lasted " + _turnNumber + " turns");
        for (Watcher watcher : _watchers)
        {
            watcher.gameOver();
        }
        return _universe;
    }

    private void printCombatEvents(List<Event> combatEvents)
    {
        for(Event event : combatEvents)
        {
            LandingEvent landing = (LandingEvent) event;
            System.out.println("[" + _turnNumber + "] Player " + landing.getFleetOwner() + " landed " + landing.getSentShipCount() +
            " ships on planet " + landing.getToPlanet() + " and ended up with " + landing.getAfterBattleShipCount() + " ships");
        }
    }

    private void printLaunchEvents(List<Event> playerEvents)
    {
        for (Event event : playerEvents)
        {
            System.out.println("[" + _turnNumber + "] Player " + event.getFleetOwner() + " launched " + event.getSentShipCount() +
            " ships to planet " + event.getToPlanet() + " from planet " + event.getFromPlanet() + ", lands in " + event.getFlightDuration() + " turns");
        }
    }

    private void grow()
    {

        Map<Integer, Planet> plantMap = _universe.getPlanetMap();
        for(Integer planetId : plantMap.keySet())
        {
            Planet oldPlanet = plantMap.get(planetId);
            if(oldPlanet.getOwner() > 0) // neutral don't grow
            {
                Planet updated = oldPlanet;
                switch(oldPlanet.getSize()) {
                    case SMALL:
                        updated = growPlanet(oldPlanet, 1);
                        break;
                    case MEDIUM:
                        updated = growPlanet(oldPlanet, 2);
                        break;
                    default:
                        updated = growPlanet(oldPlanet, 4);
                }
                plantMap.put(updated.getId(), updated);
            }
        }
    }

    private Planet growPlanet(Planet oldPlanet, int increaseAmount)
    {
        int size = oldPlanet.getPopulation() + increaseAmount;
        Planet planet = new Planet(oldPlanet.getX(),
                oldPlanet.getY(),
                oldPlanet.getId(),
                oldPlanet.getOwner(),
                size,
                Size.getSizeForNumber(size));
        return planet;
    }

    private Universe makeUniverseForPlayer(int playerId)
    {
        HashMap<Integer, Planet> planets = new HashMap<Integer, Planet>(_universe.getPlanets().size());

        for(Planet planet : _universe.getPlanets())
        {
            int x = planet.getX();
            int y = planet.getY();
            int id = planet.getId();
            int owner = planet.getOwner();
            int population = planet.getOwner() == playerId ? planet.getPopulation() : -1;
            Size size = Size.getSizeForNumber(planet.getPopulation());
            Planet specialPlanet = new Planet(x, y, id, owner, population, size);
            planets.put(id, specialPlanet);
        }

        return new Universe(Collections.unmodifiableMap(planets));
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

    private List<Fleet> condenseFleets(List<Fleet> fleets)
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
                        0,
                        fleet.getOwner(),
                        fleet.getSize() + condensingFleet.getSize());
                condenser.put(fleet.getOwner(), condensed);
            }
        }

        List<Fleet> fleetList = new ArrayList<Fleet>(condenser.size());
        for(Fleet fleet : condenser.values())
        {
            fleetList.add(fleet);
        }
        return fleetList;
    }

    /**
     * Set the event on any watchers which are present
     * @param event
     */
    private void notifyOfEvent(Event event)
    {
        for (Watcher watcher : _watchers)
        {
            watcher.setEvent(event, _turnNumber);
        }
    }

    /**
     * Breaking stuff!!
     * @param conflictMap
     * @return
     */
    private List<Event> combat(Map<Planet, List<Fleet>> conflictMap)
    {
        List<Event> combatEvents = new ArrayList<Event>(conflictMap.size());

        for (Planet battleGround : conflictMap.keySet())
        {
            List<Fleet> rawFleets = conflictMap.get(battleGround);

            // TODO : Since we condense we lose origins
            List<Fleet> fleetsInvolved = condenseFleets(rawFleets);

            int battleSplit = fleetsInvolved.size(); // size before adding planet

            Fleet planetFleet = new Fleet(battleGround.getId(), battleGround.getId(), 0, battleGround.getOwner(), battleGround.getPopulation());
            fleetsInvolved.add(planetFleet);

            List<Integer> playersInvolved = new ArrayList<Integer>(fleetsInvolved.size());
            for(Fleet fleet : fleetsInvolved) {
                playersInvolved.add(fleet.getOwner());
            }

            Map<Integer, Event> battleEvents = new HashMap<Integer, Event>(fleetsInvolved.size()); // owner, event
            for(Fleet fleet : fleetsInvolved)
            {
                Event landing = new LandingEvent(fleet.get_origin(),
                        fleet.get_destination(),
                        Size.getSizeForNumber(fleet.getSize()),
                        fleet.getSize(),
                        fleet.getSize(),
                        playersInvolved,
                        fleet.getOwner()
                        );
                notifyOfEvent(landing);
                battleEvents.put(fleet.getOwner(), landing);
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
                            // We have to round up so that something gets destroyed.
                            size = size - (int)(Math.ceil(otherFleet.getSize() / (double)battleSplit));
                        }
                    }

                    if(size > 0) {
                        Fleet nextRoundFleet = new Fleet(fleet.get_origin(),
                                fleet.get_destination(),
                                0,
                                fleet.getOwner(),
                                size);
                        nextRoundFleets.add(nextRoundFleet);
                        LandingEvent fleetUpdate = (LandingEvent) battleEvents.get(fleet.getOwner());
                        fleetUpdate.setShipRemainingCount(size);
                    } else {
                        LandingEvent fleetUpdate = (LandingEvent) battleEvents.get(fleet.getOwner());
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
                // System.out.println(everyoneLost.toJson().replace("{turn}", _turnNumber + ""));
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
                    // System.out.println(winningPlanet.toJson().replace("{turn}", _turnNumber + ""));
                } else {
                    Planet everyoneStillLost = makePlanetWithNewOwnerAndSize(battleGround, 0, 0);
                    updatePlanet(everyoneStillLost);
                    // System.out.println(everyoneStillLost.toJson().replace("{turn}", _turnNumber + ""));
                }
            }

            // Add to events
            combatEvents.addAll(battleEvents.values());
        }

        return combatEvents;
    }

    private Planet getPlanet(int id)
    {
        Planet planet =  _universe.getPlanetMap().get(id);
        if (planet == null) {
            System.out.println("Planet with id of " + id + " does not exist");
        }
        return planet;
    }

    private void updatePlanet(Planet planet)
    {
        _universe.getPlanetMap().put(planet.getId(), planet);
    }

    private Planet makePlanetWithNewOwnerAndSize(Planet planet, int owner, int size)
    {
        if(size == 0)
        {
            // become neutral if no one is on it.
            owner = 0;
        }
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
        Planet destination = _universe.getPlanetMap().get(fleet.get_destination());
        return destination.getOwner() == fleet.getOwner();
    }

    private List<Event> getEventsForFleets(List<Fleet> fleets)
    {
        List<Event> launchEvents = new ArrayList<Event>(fleets.size());

        for(Fleet fleet : fleets)
        {
            LaunchEvent event = new LaunchEvent(fleet.get_origin(),
                    fleet.get_destination(),
                    Size.getSizeForNumber(fleet.getSize()),
                    fleet.getOwner(),
                    fleet.getSize(),
                    fleet.getTurnsRemaining());
            notifyOfEvent(event);
            launchEvents.add(event);
            String launchJson = event.toJson();
            //System.out.println(launchJson.replace("{turn}", _turnNumber + ""));
        }
        return launchEvents;
    }

    private List<Fleet> getFleetsForPlayer(Player player, List<Move> moves)
    {
        List<Fleet> playerFleets = new ArrayList<Fleet>(moves.size());

        for(Move move : moves)
        {
            // TODO: Should remove moves that involve duplicate origin planets
            if(moveValidForPlayer(player, move))
            {
                int fleetSize = getFleetSize(move);
                if (fleetSize > 0)
                {
                    Planet origin = _universe.getPlanetMap().get(move.getFromPlanet());
                    Planet destination = _universe.getPlanetMap().get(move.getToPlanet());

                    Fleet fleet = new Fleet(origin.getId(),
                            destination.getId(),
                            Universe.getTimeToTravel(origin, destination),
                            _playerIds.get(player),
                            fleetSize);
                    Planet updatedOrigin = makePlanetWithNewOwnerAndSize(origin, origin.getOwner(), origin.getPopulation() - fleetSize);
                    updatePlanet(updatedOrigin);
                    playerFleets.add(fleet);
                }
            }
        }

        return playerFleets;
    }

    private int getFleetSize(Move move)
    {
        Planet origin = getPlanet(move.getFromPlanet());
        if (origin == null) return 0;

        int shipsOnPlanet = origin.getPopulation();

        if (shipsOnPlanet <= 0) return 0;
        switch (move.getSize()) {
            case NONE:
                return 0;
            case SCOUTING: // .25
                return shipsOnPlanet / 4;
            case RAIDING: // .5
                return shipsOnPlanet / 2;
            case ASSAULT: // .75
                return  (int) (shipsOnPlanet * .75);
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
