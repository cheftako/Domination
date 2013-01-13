package com.linkedin.domination.sample;

import com.linkedin.domination.api.Event;
import com.linkedin.domination.api.Move;
import com.linkedin.domination.api.Player;
import com.linkedin.domination.api.Universe;
import sun.jvmstat.perfdata.monitor.PerfIntegerMonitor;

import java.util.ArrayList;
import java.util.List;

public class SamplePlayer implements Player
{
    private Integer _me = null;

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
        return new ArrayList();
    }
}
