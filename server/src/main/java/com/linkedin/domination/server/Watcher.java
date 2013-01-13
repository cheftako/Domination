package com.linkedin.domination.server;

import com.linkedin.domination.api.Event;
import com.linkedin.domination.api.Move;
import com.linkedin.domination.api.Universe;

import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: wfender
 * Date: 1/12/13
 * Time: 5:44 PM
 * To change this template use File | Settings | File Templates.
 */
public interface Watcher
{
    void setUniverse(Universe universe);

    void turn(Map<Integer, List<Move>> moves, List<Event> events);
}
