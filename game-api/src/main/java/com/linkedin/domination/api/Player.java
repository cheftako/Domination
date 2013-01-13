package com.linkedin.domination.api;

import java.util.List;

interface Player
{
  void initialize(Universe start);

  List<Move> makeMove(List<Event> events);
}
