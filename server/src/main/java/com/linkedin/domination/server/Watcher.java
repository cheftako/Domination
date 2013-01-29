package com.linkedin.domination.server;

import com.linkedin.domination.api.Event;
import com.linkedin.domination.api.Move;
import com.linkedin.domination.api.Player;
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

    void setEvent(Event event, int turn);

    void setPlayer(int playerNbr, Player player);

    void gameOver();
}
