package com.linkedin.domination.sample;

import com.linkedin.domination.api.Event;
import com.linkedin.domination.api.Move;
import com.linkedin.domination.api.Player;
import com.linkedin.domination.api.Universe;

import java.util.ArrayList;
import java.util.List;

public class SamplePlayer implements Player
{
    Universe _universe = null;

    public SamplePlayer()
    {
        // Nothing to be done yet.
    }

    @Override
    public void initialize(Universe universe)
    {
        _universe = universe;
    }

    @Override
    public List<Move> makeMove(List<Event> events)
    {
        return new ArrayList();
    }
}
