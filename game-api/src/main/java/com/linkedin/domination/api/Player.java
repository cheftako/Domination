package com.linkedin.domination.api;

import java.util.List;

public interface Player
{
  public void initialize(Integer playerNbr);

  public List<Move> makeMove(Universe universe, List<Event> events);

  public String getPlayerName();
}
