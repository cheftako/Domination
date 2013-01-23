package com.linkedin.domination.sample;

import com.linkedin.domination.api.Event;
import com.linkedin.domination.api.Move;
import com.linkedin.domination.api.Planet;
import com.linkedin.domination.api.Player;
import com.linkedin.domination.api.Size;
import com.linkedin.domination.api.Universe;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author jkessler
 * The Tin Woodsman - It has no heart
 * Goals:
 *  - Focus on only fighting one opponent at a time
 *  - Keep an idea of the universe state, and try to launch attacks that will knock opponents under size thresholds
 *  - Maintain a 'battle line' of Large & Small planets, and a 'core' of Medium production planets
 */
public class WoodsmanPlayer implements Player {

  // Some useful numbers
  private static final int SMALL_SIZE = 0;
  private static final int MEDIUM_SIZE = 20;
  private static final int LARGE_SIZE = 50;

  private static final int SCOUT_MIN_SIZE = 10;
  private static final int PRODUCTION_MIN_SIZE = LARGE_SIZE;
  private static final int FORTRESS_MIN_SIZE = MEDIUM_SIZE;

  private static final int MARGIN = 5;
  private static final int MAX_DISTANCE = 40;
  private static final int PROD_TARGET_MAX_DISTANCE = 20;
  private static final int BOUNDARY_BONUS = 5;
  private static final int TARGET_BONUS = 3;

  private static final Map<Size, List<Integer>> sizeRanges;
  private static final int MIN = 0;
  private static final int AVG = 1;
  private static final int MAX = 2;

  private static final Map<Move.FleetType, Double> fleetSizes;


  private Integer me;
  private Integer myTarget;

  private int turn = 0;

  private Map<Integer, Stats> universeState = new HashMap<Integer, Stats>();
  private Map<String, Stats> fleetState = new HashMap<String, Stats>();
  private Map<Integer, Role> myPlanets = new HashMap<Integer, Role>();
  private Map<Integer, Integer> distanceFromEnemy = new HashMap<Integer, Integer>();

  static {
    sizeRanges = new HashMap<Size, List<Integer>>(3);
    sizeRanges.put(Size.SMALL, Arrays.asList(0, 10, 19));
    sizeRanges.put(Size.MEDIUM, Arrays.asList(20, 35, 49));
    sizeRanges.put(Size.LARGE, Arrays.asList(49, 75, 100));

    fleetSizes = new HashMap<Move.FleetType, Double>(4);
    fleetSizes.put(Move.FleetType.NONE, 0.0);
    fleetSizes.put(Move.FleetType.SCOUTING, 0.25);
    fleetSizes.put(Move.FleetType.RAIDING, 0.5);
    fleetSizes.put(Move.FleetType.ASSAULT, 0.75);
    fleetSizes.put(Move.FleetType.HORDE, 1.0);

  }

  private Map<Integer, List<Event>> byPlanet = new HashMap<Integer, List<Event>>();
  private Map<Integer, List<Stats>> incoming = new HashMap<Integer, List<Stats>>();
  private Map<Integer, Integer> reinforce = new HashMap<Integer, Integer>();

  private Map<Integer, Integer> recentAttacks = new HashMap<Integer, Integer>();

  private Set<Integer> raidTargets;
  private Map<Integer, Integer> smashTargets;
  private Map<Integer, Integer> enemyAttacks;


  @Override
  public void initialize(Integer playerNbr) {
    me = playerNbr;
    // Assume three players. I hate you, player n+1!
    myTarget = (playerNbr + 1) % 3;

  }

  @Override
  public String getPlayerName() {
      return "Woodsman";
  }

  @Override
  public List<Move> makeMove(Universe universe, List<Event> events) {
    if (turn == 0) {
      initializeState(universe);
    } else { // update state!
      processTurn(universe, events);
    }
    List<Move> result = new LinkedList<Move>();
    // Alright! state updated (We hope!)

    raidTargets = new HashSet<Integer>();
    smashTargets = new HashMap<Integer, Integer>();
    byPlanet = new HashMap<Integer, List<Event>>();
    incoming = new HashMap<Integer, List<Stats>>();
    reinforce = new HashMap<Integer, Integer>();
    enemyAttacks = new HashMap<Integer, Integer>();
    for (int i = 0; i < 3; i++) {
      enemyAttacks.put(i, 0);
    }

    // Sanity check for orders
    Collection<Planet> planets = universe.getPlanets();
    for (Planet p : planets) {
      if (p.getOwner() == me) {
        if (!myPlanets.containsKey(p.getId())) {
          myPlanets.put(p.getId(), p.getSize().equals(Size.SMALL)? Role.SCOUT : Role.FORTRESS); // And complain
        }
      } else {
        if (myPlanets.containsKey(p.getId())) {
          myPlanets.remove(p.getId()); // and complain
        }

      }
    }

    // For each planet, make it do it's thing
    for (Integer i : myPlanets.keySet()) {
      Move m;
      switch(myPlanets.get(i)) {
        case SCOUT: m = generateScoutTarget(i, universe); break;
        case FORTRESS: m = generateFortressTarget(i, universe); break;
        case PRODUCTION: m = generateProductionTarget(i, universe); break;
        default: myPlanets.put(i, Role.SCOUT); m = generateScoutTarget(i, universe); break;
      }
      if (m != null) {
        result.add(m);
      }

    }
    turn += 1;
    return result;
  }

  private void processTurn(Universe u, List<Event> events) {
    // recent attack ticks
    Set<Integer> keySet = new HashSet<Integer>(recentAttacks.keySet());
    for (Integer i : keySet) {
      int value = recentAttacks.get(i);
      if (value == 0) {
        recentAttacks.remove(i);
      } else {
        recentAttacks.put(i, recentAttacks.get(i) - 1);
      }
    }
    // Sort events by planet
    for (Event e : events) {
      if (e.getEventType().equals(Event.EventType.LAUNCH)) {
        if (!(byPlanet.containsKey(e.getFromPlanet())))
          byPlanet.put(e.getFromPlanet(), new LinkedList<Event>());
        byPlanet.get(e.getFromPlanet()).add(0, e);


      } else {
        // Landing event
        if (!(byPlanet.containsKey(e.getFromPlanet())))
          byPlanet.put(e.getFromPlanet(), new LinkedList<Event>());
        byPlanet.get(e.getFromPlanet()).add(e);
      }
    }
    // process events per-planet, with launch events first
    for (Integer i : byPlanet.keySet()) {
      processPlanetEvents(u, byPlanet.get(i));
    }

    // growth
    for (int i : universeState.keySet()) {
      Stats s = universeState.get(i);
      if (s.getOwner() != 0) {
        int growth = 0;
        switch (s.getCurrentSize()) {
          case SMALL: growth = 1; break;
          case MEDIUM: growth = 2; break;
          case LARGE: growth = 3; break; // 4?
        }
        universeState.put(i, new Stats(s, s.getEstSize() + growth, null, null, null));
      }
    }
    // Stats
    for (Integer i : myPlanets.keySet()) {
      int min_distance = Integer.MAX_VALUE;
      for (Planet p : u.getPlanets()) {
        if (p.getOwner() != me) {
          int distance = u.getTimeToTravel(i, p.getId());
          min_distance = Math.min(min_distance, distance);
        }
      }
      distanceFromEnemy.put(i, min_distance);
    }
    /*
    int maxAttacks = 1;
    for (int i = 0; i < 3; i++) {
      if (enemyAttacks.get(i) > maxAttacks) {
        maxAttacks = enemyAttacks.get(i);
        myTarget = i; // REVENGE!
      }
    }
    */

  }

  private void processPlanetEvents(Universe u, List<Event> planetEvents) {
    Map<Integer,Planet> planets = u.getPlanetMap();
    for (Event e : planetEvents) {
      if (e.getEventType().equals(Event.EventType.LAUNCH)) {
        // Add fleet
        String key = e.getFromPlanet() + "_" +
                e.getToPlanet() + "_" +
                (turn + u.getTimeToTravel(e.getFromPlanet(), e.getToPlanet()));
        int[] launchFleetSize;
        if (e.getSentShipCount() != -1) { // hey look! we have actual data!
          launchFleetSize = new int[2];
          launchFleetSize[0] = e.getSentShipCount();
          launchFleetSize[1] = universeState.get(e.getFromPlanet()).getEstSize() - e.getSentShipCount();
        } else {
          launchFleetSize = calculateFleetSize( // ignore the case where a planet flees and then is crushed on the same turn
                  universeState.get(e.getFromPlanet()),
                  e.getFleetSize(),
                  planets.get(e.getFromPlanet()).getSize());
        }
        Stats fleet = new Stats(launchFleetSize[0], turn, e.getFleetOwner(), e.getFleetSize());
        fleetState.put(key, fleet);
        Stats planet = universeState.get(e.getFromPlanet());
        universeState.put(e.getFromPlanet(),
                new Stats(planet, launchFleetSize[1], null, null, planets.get(e.getFromPlanet()).getSize()));
        // Construct incoming set
        try {
        if (planets.get(e.getToPlanet()).getOwner() == me && e.getFleetOwner() != me) {
          if (!(incoming.containsKey(e.getToPlanet())))
            incoming.put(e.getToPlanet(), new LinkedList<Stats>());
          incoming.get(e.getToPlanet()).add(fleet);
          enemyAttacks.put(e.getFleetOwner(), enemyAttacks.get(e.getFleetOwner()) + 1);

        }
        } catch (NullPointerException ex) {
          //System.out.println("Null pointer what?");
        }
      } else {
        // Landing event
        // remove fleet
        String key = e.getFromPlanet() + "_" +
                e.getToPlanet() + "_" +
                (turn - u.getTimeToTravel(e.getFromPlanet(), e.getToPlanet()));
        Stats fleet = fleetState.remove(key);
        if (fleet != null) {  // if we weren't tracking the fleet, it was a defense fleet. We'll handle it when we see the other side
          int newPlanetSize;
          int newPlanetOwner = planets.get(e.getToPlanet()).getOwner();
          if (e.getAfterBattleShipCount() != -1) {
            newPlanetSize = e.getAfterBattleShipCount();
          } else {
            newPlanetSize = calculatePlanetSize(universeState.get(e.getToPlanet()), fleet, planets.get(e.getToPlanet()).getSize(), newPlanetOwner);
          }
          if (newPlanetOwner == me) {
            if (myPlanets.containsKey(e.getToPlanet())){} // already tracked
            else {
              // New Conquest
              if (Size.getSizeForNumber(newPlanetSize).equals(Size.SMALL))
                myPlanets.put(e.getToPlanet(), Role.SCOUT);
              else
                myPlanets.put(e.getToPlanet(), Role.FORTRESS);
            }
          } else {
            if (myPlanets.containsKey(e.getToPlanet())) {
              // I lost a planet =(
              myPlanets.remove(e.getToPlanet());
            }
          }
          Stats newPlanet = new Stats(newPlanetSize, turn, newPlanetOwner, planets.get(e.getToPlanet()).getSize());
          universeState.put(e.getToPlanet(), newPlanet);
        }
      }
    }

  }

  private int[] calculateFleetSize(Stats source, Size fleetSize, Size sourceSize) {
    int estSize = source.getEstSize(); // How large we thought that it was
    int high = estSize + MARGIN;
    int low = estSize - MARGIN;
    if (!(Size.getSizeForNumber(estSize).equals(source.getCurrentSize()))) { // we were wrong about our size estimate
      if (Size.getSizeForNumber(high).equals(source.getCurrentSize())) { // We may have just been low
        int minSize = sizeRanges.get(source.getCurrentSize()).get(MIN);
        low = minSize;
        estSize = minSize + MARGIN;
        high = minSize + (MARGIN * 2);
      } else if (Size.getSizeForNumber(low).equals(source.getCurrentSize())) {
        int maxSize = sizeRanges.get(source.getCurrentSize()).get(MAX);
        high = maxSize;
        estSize = maxSize - MARGIN;
        low = maxSize - (MARGIN * 2);
      } else {
        // We're clearly very wrong about how large it is - go with defaults
        estSize = sizeRanges.get(sourceSize).get(AVG);
        high = estSize + MARGIN;
        low = estSize - MARGIN;
      }
    }

    // some defaults
    int newFleetSize = sizeRanges.get(fleetSize).get(AVG);
    int newPlanetSize = estSize - newFleetSize;

    // Be lazy and hope for a single hit...
    for (Move.FleetType fleetType : fleetSizes.keySet()) {
      // Prefer to be optimistic (assuming smaller fleets)
      int maxFleetSize = (int)(high * fleetSizes.get(fleetType));
      if (Size.getSizeForNumber(maxFleetSize).equals(fleetSize) &&
              Size.getSizeForNumber(high - maxFleetSize).equals(sourceSize)) {
        newFleetSize = maxFleetSize;
        newPlanetSize = high - newFleetSize;
      }
      int minFleetSize = (int)(low * fleetSizes.get(fleetType));
      if (Size.getSizeForNumber(minFleetSize).equals(fleetSize) &&
              Size.getSizeForNumber(low - minFleetSize).equals(sourceSize)) {
        newFleetSize = minFleetSize;
        newPlanetSize = low - newFleetSize;
      }
      int estFleetSize = (int)(estSize * fleetSizes.get(fleetType));
      if (Size.getSizeForNumber(estFleetSize).equals(fleetSize) &&
              Size.getSizeForNumber(estSize - estFleetSize).equals(sourceSize)) {
        newFleetSize = estFleetSize;
        newPlanetSize = estSize - newFleetSize;
      }
    }

    return new int[] {newFleetSize, newPlanetSize};


  }

  private int calculatePlanetSize(Stats planet, Stats fleet, Size planetSize, int currentOwner) {
    int estSize;
    int high;
    int low;
    boolean friendly;
    try
    {
      friendly = planet.getOwner() == fleet.getOwner();
    } catch (NullPointerException e) {
      //What?
      friendly = false;
    }
    if (friendly) {
      estSize = planet.getEstSize() + fleet.getEstSize();
    } else {
      estSize = planet.getEstSize() - fleet.getEstSize();
    }
    if (currentOwner != planet.getOwner()) {
      // the planet was conquered
      if (estSize < 0) {
        // We expected that
        estSize *= -1;
      } else {
        // We didn't - try the extreme
        estSize = ((planet.getEstSize() - MARGIN) - (fleet.getEstSize() + MARGIN)) * -1;
        if (estSize > 0) {
          // that works
        } else {
          estSize = sizeRanges.get(planetSize).get(AVG);
        }
      }
    }
    // final check
    high = estSize + (MARGIN * 2);
    low = estSize - (MARGIN * 2);
    if (Size.getSizeForNumber(estSize).equals(planetSize)) {
      return estSize;
    } else if (Size.getSizeForNumber(high).equals(planetSize)) {
      return high;
    } else if (Size.getSizeForNumber(low).equals(planetSize)) {
      return low;
    } else {
      return sizeRanges.get(planetSize).get(AVG); // We apparently have no idea
    }
  }

  private void initializeState(Universe u) {
    // Some assumptions here
    // - Each player has one planet
    // - Each player starts with the same number of ships
    // - There are three players
    int[] playerPlanets = new int[3];
    int myFleetSize = 0;

    Map<Integer, Planet> planets = u.getPlanetMap();
    for (Integer i : planets.keySet()) {
      Planet p = planets.get(i);
      if (p.getOwner() != 0) {
        playerPlanets[p.getOwner()-1] = i;
      }
      int estSize;
      if (p.getOwner() == me) {
        myFleetSize = p.getPopulation();
        estSize = p.getPopulation();
        myPlanets.put(i, Role.FORTRESS);
      } else {
        estSize = (sizeRanges.get(p.getSize()).get(AVG));
      }
      Stats s = new Stats(estSize, 0, p.getOwner(), p.getSize());
      universeState.put(i, s);
    }

    // Assume starting fleets are equal
    for (int i = 0; i < playerPlanets.length; i++) {
      Stats s = new Stats(universeState.get(i), myFleetSize, null, null, null);
      universeState.put(i, s);
    }

  }

  private Move generateScoutTarget(int planetID, Universe universe) {
    Map<Integer, Planet> planets = universe.getPlanetMap();
    Planet source = planets.get(planetID);

    // if we are big, become a fortress
    if (source.getPopulation() > sizeRanges.get(Size.MEDIUM).get(AVG)) {
      myPlanets.put(planetID, Role.FORTRESS);
      return generateFortressTarget(planetID, universe);
    }

    Move.FleetType fleetSize = generateMaxFleetSize(source, (int)(SCOUT_MIN_SIZE * (1.0d/fleetSizes.get(Move.FleetType.SCOUTING))));
    if (fleetSize.equals(Move.FleetType.NONE)) {
      return null;
    }
    int allowableFleetSize = (int)(source.getPopulation() * fleetSizes.get(fleetSize));

    // Find a target
    int currentTarget = -1;
    double bestScore = 30;
    boolean available = false;
    for (Planet p : universe.getPlanets()) {
      if (Universe.getTimeToTravel(source, p) <= MAX_DISTANCE &&
              p.getOwner() != me) {
        available = true; // there was a target, we're just already scouting it
        if (!raidTargets.contains(p.getId()) || p.getOwner() == 0) {
          double score = generateScoutTargetScore(source, p, allowableFleetSize);
          if (score > bestScore) {
            bestScore = score;
            currentTarget = p.getId();
          }
        }
      }
    }
    if (currentTarget != -1) {
      raidTargets.add(currentTarget);
      recentAttacks.put(currentTarget, Universe.getTimeToTravel(source, universe.getPlanetMap().get(currentTarget)));
      return new Move(planetID, currentTarget, fleetSize);
    } else if (available) return null;
    else {
      // no available worlds, become a production world
      myPlanets.put(planetID, Role.PRODUCTION);
      return generateProductionTarget(planetID, universe);
    }

  }

  private Move generateFortressTarget(int planetID, Universe universe) {
    Map<Integer, Planet> planets = universe.getPlanetMap();
    Planet source = planets.get(planetID);

    // If we are small, become a scout
    if (source.getPopulation() < sizeRanges.get(Size.SMALL).get(MAX)) {
      myPlanets.put(planetID, Role.SCOUT);
      return generateScoutTarget(planetID, universe);
    }

    boolean neutralsOnly = false;
    // Make sure we can launch safely
    if (FORTRESS_MIN_SIZE > (source.getPopulation() - fleetSizes.get(Move.FleetType.SCOUTING) * source.getPopulation()))
      neutralsOnly = true;


    Move.FleetType fleetSize = generateMaxFleetSize(source, FORTRESS_MIN_SIZE);
    if (fleetSize.equals(Move.FleetType.NONE)) {
      return null;
    }
    int allowableFleetSize = (int)(source.getPopulation() * fleetSizes.get(fleetSize));

    // Find a target
    int currentTarget = -1;
    double bestScore = -30;
    for (Planet p : universe.getPlanets()) {
      if (Universe.getTimeToTravel(source, p) <= MAX_DISTANCE &&
              p.getOwner() != me) {
        double score = generateFortressTargetScore(source, p, allowableFleetSize, neutralsOnly);
        if (score > bestScore) {
          bestScore = score;
          currentTarget = p.getId();
        }
      }
    }
    if (currentTarget != -1) {
      if (!smashTargets.containsKey(currentTarget))
        smashTargets.put(currentTarget, allowableFleetSize);
      else
        smashTargets.put(currentTarget, smashTargets.get(currentTarget) + allowableFleetSize);
      recentAttacks.put(currentTarget, Universe.getTimeToTravel(source, universe.getPlanetMap().get(currentTarget)));
      return new Move(planetID, currentTarget, fleetSize);
    } else {
      // no available worlds, become a production world
      myPlanets.put(planetID, Role.PRODUCTION);
      return generateProductionTarget(planetID, universe);
    }
  }

  private Move generateProductionTarget(int planetID, Universe universe) {
    Map<Integer, Planet> planets = universe.getPlanetMap();
    Planet source = planets.get(planetID);

    // Find a target
    Move.FleetType fleetSize = generateMaxFleetSize(source, PRODUCTION_MIN_SIZE);
    if (fleetSize.equals(Move.FleetType.NONE)) {
      return null;
    }
    int allowableFleetSize = (int)(source.getPopulation() * fleetSizes.get(fleetSize));
    int currentTarget = -1;
    double bestScore = -30;
    for (Planet p : universe.getPlanets()) { // production worlds consider all planets
        double score = generateProductionTargetScore(source, p, allowableFleetSize);
        if (score > bestScore) {
          bestScore = score;
          currentTarget = p.getId();
      }
    }
    if (currentTarget != -1) {
      if (!reinforce.containsKey(currentTarget))
        reinforce.put(currentTarget, 1);
      else
        reinforce.put(currentTarget, reinforce.get(currentTarget) + 1);
      return new Move(planetID, currentTarget, fleetSize);
    } else {
      // no available worlds, become a fortress world /and skip turn to avoid infinite loop/
      myPlanets.put(planetID, Role.FORTRESS);
      return null;
    }
  }

  private double generateScoutTargetScore(Planet source, Planet dest, int fleetSize) {
    // Scout priorities
    // - is it close?
    // - is it near a boundary condition?
    // - has it not been scouted in a while?
    // - is it a preferred target?

    double score = 0;
    int time = Universe.getTimeToTravel(source, dest);
    score -= time;

    Stats target = universeState.get(dest.getId());
    int pop = target.getEstSize();
    if (dest.getOwner() == 0) {
      score += fleetSize - pop;
    }

    boolean boundary = nearBoundary(pop, time);
    if (boundary) score += BOUNDARY_BONUS;

    score += turn - target.getObservedTurn();
    Integer attacked = recentAttacks.get(dest.getId());
    if (attacked != null) score -= attacked;

    if (target.getOwner() == myTarget) score += TARGET_BONUS;
    return score;

  }

  private double generateFortressTargetScore(Planet source, Planet dest, int fleetSize, boolean neutralsOnly) {
    // Fortress priorities
    // - is it close?
    // - is it neutral?
    // - is it near a boundary condition?
    // - is it on the smash list?
    // - will we probably win?
    // - is it the preferred target?


    double score = 0;
    int time = Universe.getTimeToTravel(source, dest);
    score -= time;

    Stats target = universeState.get(dest.getId());
    int pop = target.getEstSize();
    //int effFleetSize = smashTargets.containsKey(dest.getId()) ? smashTargets.get(dest.getId()) + fleetSize : fleetSize;
    score += Math.min(fleetSize - pop, 30);

    if (target.getOwner() == 0) score += TARGET_BONUS * 2;
    else if (neutralsOnly) return Double.NEGATIVE_INFINITY; // don't attack, we're only hunting neutrals;

    boolean boundary = nearBoundary(pop, time);
    if (boundary) score += BOUNDARY_BONUS;

    Integer attacked = recentAttacks.get(dest.getId());
    if (attacked != null) score -= attacked;

    if (smashTargets.containsKey(dest.getId()) && target.getOwner() != 0) score += 3;

    if (target.getOwner() == myTarget) score += TARGET_BONUS;

    return score;

  }

  private double generateProductionTargetScore(Planet source, Planet dest, int allowableFleetSize) {
    // Production Score priorities
    // If this is an enemy or neutral planet, and it is within MAX_RANGE
    //  - score = -distance + 100 (we want to swarm it dead)
    // Otherwise
    //  - is it not a production world?
    //  - is it close?
    //  - is it on the 'incoming' list?
    //  - if it is, has it not been reinforced much?


    double score = 0;

    if (dest.getOwner() != me) {
      if (Universe.getTimeToTravel(source, dest) <= PROD_TARGET_MAX_DISTANCE)
        return (Universe.getTimeToTravel(source, dest) * -1) + 100;
      else return Double.NEGATIVE_INFINITY;
    }
    else {
      if (Role.PRODUCTION.equals(myPlanets.get(dest.getId())))
        return Double.NEGATIVE_INFINITY;
      score -= Universe.getTimeToTravel(source, dest);
      Integer distToEnemy = distanceFromEnemy.get(dest.getId());
      if (distToEnemy != null) {
        score -= distToEnemy;
      } else {
        score -= MAX_DISTANCE;
      }
      if (incoming.containsKey(dest.getId()))
        score += 3;
      if (reinforce.containsKey(dest.getId())) {
        score -= reinforce.get(dest.getId());
      }
    }

    return score;

  }

  private boolean nearBoundary(int population, int timeToTravel) {
    int growth = plusGrowth(population, timeToTravel);

    return (!Size.getSizeForNumber(population + growth).equals(Size.getSizeForNumber(population + growth + MARGIN))) ||
            (!Size.getSizeForNumber(population + growth).equals(Size.getSizeForNumber(population + growth - MARGIN)));
  }

  private int plusGrowth(int population, int timeToTravel) {
    int growth = 0;
    for (int i = 0; i < timeToTravel; i++) {
      Size current = Size.getSizeForNumber(population + growth);
      switch (current) {
        case SMALL: growth += 1; break;
        case MEDIUM: growth += 2; break;
        case LARGE: growth += 3; break;
        default: growth += 1;
      }
    }
    return growth;
  }

  private Move.FleetType generateMaxFleetSize(Planet source, int population) {
    if (population == 0) return Move.FleetType.HORDE;
    int currentPop = source.getPopulation();
    if (currentPop - (currentPop * fleetSizes.get(Move.FleetType.ASSAULT)) >= population) return Move.FleetType.ASSAULT;
    else if (currentPop - (currentPop * fleetSizes.get(Move.FleetType.RAIDING)) >= population) return Move.FleetType.RAIDING;
    else if (currentPop - (currentPop * fleetSizes.get(Move.FleetType.SCOUTING)) >= population) return Move.FleetType.SCOUTING;
    else return Move.FleetType.NONE; // Oops...

  }
}

/*
 * Classes for maintaining universe state. They are probably overkill
 */
enum Role {
  SCOUT,
  PRODUCTION,
  FORTRESS
}

class Stats {

  Size currentSize;
  int estimatedSize;
  int observedTurn;
  int owner;

  // standard constructor
  public Stats(int estimatedSize, int observedTurn, int owner, Size currentSize) {
    this.estimatedSize = estimatedSize;
    this.observedTurn = observedTurn;
    this.currentSize = currentSize;
    this.owner = owner;
  }
  // Copy Constructor
  public Stats(Stats base, Integer estimatedSize, Integer observedTurn, Integer owner, Size currentSize) {
    // Sometimes I wish that Java had optional arguments...
    this.estimatedSize = estimatedSize == null ? base.getEstSize() : estimatedSize;
    this.observedTurn = observedTurn == null ? base.getObservedTurn() : observedTurn;
    this.currentSize = currentSize == null ? base.getCurrentSize() : currentSize;
    this.owner = owner == null ? base.getOwner() : owner;
  }

  public String toString() {
    return "Size " + currentSize + ", estimated pop " + estimatedSize + " owned by " + owner + ", last observed turn " + observedTurn;
  }

  public int getEstSize() {
    return estimatedSize;
  }

  public int getObservedTurn() {
    return observedTurn;
  }

  public Size getCurrentSize() {
    return currentSize;
  }

  public int getOwner() {
    return owner;
  }
}
