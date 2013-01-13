package com.linkedin.domination.sample;

import com.linkedin.domination.api.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

/**
 * This is a dumb AI.
 */
public class StupidPlayer implements Player
{
    private int me;
    private Random random;

    public StupidPlayer() {
        me = 0;
        random = new Random();
    }

    @Override
    public void initialize(Integer playerNbr) {
        me = playerNbr;
        System.out.println("Stupid player is player " + me);
    }

    @Override
    public List<Move> makeMove(Universe universe, List<Event> events) {
        List<Planet> myPlanets = getMyPlanets(universe);
        List<Planet> targets = getTargetPlanets(universe);

        List<Move> myMoves = new ArrayList<Move>(myPlanets.size());
        for(Planet planet : myPlanets) {
            if(planet.getSize().equals(Size.LARGE))
            {
                Move attack = new Move(planet, getClosestPlanet(planet, targets), Move.FleetType.RAIDING);
                myMoves.add(attack);
            }
        }

        return myMoves;
    }

    private Planet getClosestPlanet(Planet ideal, Collection<Planet> planets)
    {
        Planet closest = null;
        int nearestDistance = Integer.MAX_VALUE;

        for(Planet planet : planets)
        {
            int distanceFromIdeal = Universe.getTimeToTravel(planet, ideal);
            if (distanceFromIdeal < nearestDistance)
            {
                nearestDistance = distanceFromIdeal;
                closest = planet;
            }
        }

        return closest;
    }

    private List<Planet> getTargetPlanets(Universe universe)
    {
        List<Planet> targetPlanets = new ArrayList<Planet>();
        for(Planet planet : universe.getPlanets())
        {
            if (planet.getOwner() != me)
            {
                targetPlanets.add(planet);
            }
        }
        return targetPlanets;
    }

    private List<Planet> getMyPlanets(Universe universe) {
        List<Planet> myPlanets = new ArrayList<Planet>();
        for(Planet planet : universe.getPlanets())
        {
            if (planet.getOwner() == me)
            {
                myPlanets.add(planet);
            }
        }
        return myPlanets;
    }
}
