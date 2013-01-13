package com.linkedin.domination.sample;

import com.linkedin.domination.api.*;

import java.util.ArrayList;
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
    }

    @Override
    public List<Move> makeMove(Universe universe, List<Event> events) {
        List<Planet> myPlanets = getMyPlanets(universe);
        List<Planet> targets = getTargetPlanets(universe);

        List<Move> myMoves = new ArrayList<Move>(myPlanets.size());
        for(Planet planet : myPlanets) {
            if(planet.getSize().equals(Size.LARGE))
            {
                Move attack = new Move(planet, targets.get(random.nextInt(targets.size())), Move.FleetType.RAIDING);
                myMoves.add(attack);
            }
        }

        return myMoves;
    }

    private List<Planet> getTargetPlanets(Universe universe)
    {
        List<Planet> targetPlanets = new ArrayList<Planet>();
        for(Planet planet : universe.getPlanets())
        {
            if (planet.getId() != me)
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
            if (planet.getId() == me)
            {
                myPlanets.add(planet);
            }
        }
        return myPlanets;
    }
}
