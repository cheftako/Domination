/*
 * Galactic Domination
 *
 * Written by Colin Miller, Jacob Kessler, Walter Fender and Zoran Simic
 *
 */

var gamejs = require('gamejs');
var $v = require('gamejs/utils/vectors');

// Globals
var director = new Director(30);
var currentGameReplay = null;     // Current game replay shown
var fleetUniqueIdCounter = 0;
var numberOfAlertsShown = 0;

// Settings
gamejs.preload(["restart.png", "start.png", "pause.png", "skip-backward.png", "skip-forward.png", "switch.png"]);
var canvasWidth = 1000;
var canvasHeight = 800;
var maxPlanetRadius = 30;
var playerColors = { 0: '#bbb', 1: '#f0f', 2: '#0ff', 3: '#ff0' };
var fleetColors = { 1: 'rgba(255, 0, 255, 0.2)', 2: 'rgba(0, 255, 255, 0.2)', 3: 'rgba(255, 255, 0, 0.2)' };
var gameFont = new gamejs.font.Font("14px Verdana");
var playerNameFont = new gamejs.font.Font("18px Verdana");
var fleetFont = new gamejs.font.Font("10px Verdana");

/*
 * PlayerSprite: Shows player name, color and current number of ships
 */

var PlayerSprite = function(id, rect) {
  var self = this;
  PlayerSprite.superConstructor.apply(self, arguments);
  self.id = id;
  self.rect = new gamejs.Rect(rect);
  self.buffer = new gamejs.Surface(self.rect.width, self.rect.height);
  self.lastTurnRendered = null;
  self.player = null;
  self.nameRender = null;
  self.nameX = null;
  self.nameY = null;
  self.update = function(msDuration) {
    if (!currentGameReplay || self.lastTurnRendered == currentGameReplay.currentTurn) return;
    self.lastTurnRendered = currentGameReplay.currentTurn;
    self.buffer.clear();
    if (!currentGameReplay.currentTurn) return;
    if (self.player != currentGameReplay.players.get(self.id)) {
      self.player = currentGameReplay.players.get(self.id);
      if (!self.player) return;
      self.nameRender = playerNameFont.render(self.player.name, '#000');
      self.nameX = 2;
      self.nameY = (self.rect.height - self.nameRender.getSize()[1]) / 2;
    }
    if (!currentGameReplay.currentTurn || !self.nameRender) return;
    player = currentGameReplay.currentTurn.players.get(self.id);
    self.buffer.fill(player.color);
    self.buffer.blit(self.nameRender, [self.nameX, self.nameY]);
    var textRender = gameFont.render(plural(player.ships, ' ship'), '#000');
    self.buffer.blit(textRender, [self.rect.width - textRender.getSize()[0] - 4, -1]);
    textRender = gameFont.render(player.planets + ' pl., ' + player.fleets + ' fl.', '#000');
    self.buffer.blit(textRender, [self.rect.width - textRender.getSize()[0] - 4, 12]);
  };
  self.draw = function(surface) {
    surface.blit(self.buffer, self.rect);
  };
  return self;
};
gamejs.utils.objects.extend(PlayerSprite, gamejs.sprite.Sprite);

/*
 * TurnCounterSprite: Shows current turn number
 */

var TurnCounterSprite = function(rect) {
  var self = this;
  TurnCounterSprite.superConstructor.apply(self, arguments);
  self.rect = new gamejs.Rect(rect);
  self.buffer = new gamejs.Surface(self.rect.width, self.rect.height);
  self.lastTurnRendered = null;
  self.update = function(msDuration) {
    if (!currentGameReplay || self.lastTurnRendered == currentGameReplay.currentTurn) return;
    self.lastTurnRendered = currentGameReplay.currentTurn;
    self.buffer.clear();
    if (!currentGameReplay.currentTurn) return;
    var textRender = gameFont.render(currentGameReplay.currentTurn.number + ' / ' + currentGameReplay.totalTurns + ' turns', '#000');
    self.buffer.blit(textRender, [self.rect.width - textRender.getSize()[0] - 4, - 1]);
    textRender = gameFont.render(plural(currentGameReplay.currentTurn.fleets.length, ' fleet'), '#000');
    self.buffer.blit(textRender, [self.rect.width - textRender.getSize()[0] - 4, 14]);
  };
  self.draw = function(surface) {
    surface.blit(self.buffer, self.rect);
  };
  return self;
};
gamejs.utils.objects.extend(TurnCounterSprite, gamejs.sprite.Sprite);

/*
 * ScoreBoard: Shows player info, current turn, total turns
 */

var ScoreBoardSprite = function(rect, callback) {
  var self = this;
  ScoreBoardSprite.superConstructor.apply(self, arguments);
  self.rect = new gamejs.Rect(rect);
  self.dx = 0;
  self.buffer = new gamejs.Surface(self.rect.width, self.rect.height);
  self.bufferRect = gamejs.Rect(0, 0, self.rect.width, self.rect.height);
  self.lastTurnRendered = null;
  self.mousePressed = false;
  self.hovered = false;
  self.callback = callback;
  self.handleEvent = function(event) {
    if (!currentGameReplay) return;
    if (event.type == gamejs.event.MOUSE_DOWN) {
      self.mousePressed = self.hovered;
    } else if (event.type == gamejs.event.MOUSE_MOTION) {
      self.hovered = self.rect.collidePoint(event.pos);
      if (self.mousePressed && self.hovered) {
        self.callback(Math.round((event.pos[0] - self.rect.left) / self.dx));
      }
    } else if (event.type == gamejs.event.MOUSE_UP) {
      if (self.mousePressed && self.hovered) {
        self.callback(Math.round((event.pos[0] - self.rect.left) / self.dx));
      }
      self.mousePressed = false;
    }
  };
  self.renderTitle = function(name) {
    drawCenteredText(self.buffer, self.bufferRect, playerNameFont, 'rgba(150, 150, 150, 0.5)', name);
  };
  self.shipPlot = function(dy) {
    currentGameReplay.players.forEach(function(player) {
      var lastX = 0;
      var lastY = self.rect.height;
      // Small trick to enlarge ships graph around loosing player's max ships, as otherwise they get dwarfed by winner
      var color = player.color;
      for (var i = 0; i < currentGameReplay.totalTurns; i++) {
        var p = currentGameReplay.turns[i].players.get(player.id);
        var newX = i * self.dx;
        var newY = Math.max(0, self.rect.height - p.ships * dy);
        gamejs.draw.line(self.buffer, color, [lastX, lastY], [newX, newY], 2);
        lastX = newX;
        lastY = newY;
      }
    });
    gamejs.draw.rect(self.buffer, '#000', self.bufferRect, 1);
  };
  self.zoomedShipPlot = function() {
    self.shipPlot(self.rect.height / currentGameReplay.loosingPlayersMaxShips / 1.4);
    self.renderTitle("Ships (zoomed in)");
  };
  self.rawShipPlot = function() {
    self.shipPlot(self.rect.height / currentGameReplay.maxShips);
    self.renderTitle("Ships");
  };
  self.planetStackedArea = function() {
    var prevx = 0;
    currentGameReplay.turns.forEach(function(turn) {
      var y = 0;
      turn.players.forEach(function(player) {
        var h = self.rect.height * player.planets / turn.planets.length;
        var area = new gamejs.Rect(prevx, y, self.dx, h);
        gamejs.draw.rect(self.buffer, playerColors[player.id], area, 0);
        y += h;
      });
      prevx += self.dx;
    });
    gamejs.draw.rect(self.buffer, '#000', self.bufferRect, 1);
    self.renderTitle("% planets");
  };
  self.shipStackedArea = function() {
    var prevx = 0;
    currentGameReplay.turns.forEach(function(turn) {
      var y = 0;
      turn.players.forEach(function(player) {
        var h = self.rect.height * player.ships / turn.ships;
        var area = new gamejs.Rect(prevx, y, self.dx, h);
        gamejs.draw.rect(self.buffer, playerColors[player.id], area, 0);
        y += h;
      });
      prevx += self.dx;
    });
    gamejs.draw.rect(self.buffer, '#000', self.bufferRect, 1);
    self.renderTitle("% ships");
  };
  self.switchRenderers = function() {
    if (self.renderer == self.shipStackedArea) self.renderer = self.planetStackedArea;
    else if (self.renderer == self.planetStackedArea) self.renderer = self.rawShipPlot;
    else if (self.renderer == self.rawShipPlot) self.renderer = self.zoomedShipPlot;
    else self.renderer = self.shipStackedArea;
    self.buffer.fill('#eee');
    if (!currentGameReplay) return;
    self.renderer();
  };
  self.renderer = self.shipStackedArea;
  self.update = function(msDuration) {
    if (!currentGameReplay || self.lastTurnRendered == currentGameReplay.turns[0]) return;
    self.lastTurnRendered = currentGameReplay.turns[0];
    self.buffer.fill('#eee');
    if (!currentGameReplay) return;
    self.dx = self.rect.width / currentGameReplay.totalTurns;
    self.renderer();
  };
  self.draw = function(surface) {
    surface.blit(self.buffer, self.rect);
    if (!currentGameReplay || !currentGameReplay.currentTurn) return;
    var x = self.rect.left + currentGameReplay.currentTurn.number * self.dx;
    gamejs.draw.line(surface, '#f00', [x, self.rect.top], [x, self.rect.bottom], 1);
  };
  return self;
};
gamejs.utils.objects.extend(ScoreBoardSprite, gamejs.sprite.Sprite);

/*
 * UniverseSprite: Shows all planets for current turn
 * - color on planet identifies ownership
 * - number of ships determines size: small, medium or large
 * - number of ships on planet shown on sprite
 */
var UniverseSprite = function(rect) {
  var self = this;
  UniverseSprite.superConstructor.apply(self, arguments);
  self.rect = new gamejs.Rect(rect);
  self.buffer = new gamejs.Surface(self.rect.width, self.rect.height);
  self.lastTurnRendered = null;
  self.update = function(msDuration) {
    if (!currentGameReplay || self.lastTurnRendered == currentGameReplay.currentTurn) return;
    self.lastTurnRendered = currentGameReplay.currentTurn;
    self.buffer.clear();
    if (!currentGameReplay.currentTurn) return;
    currentGameReplay.currentTurn.planets.forEach(function(planet) {
      gamejs.draw.circle(self.buffer, playerColors[planet.owner], [planet.x, planet.y], planet.radius, 0);
      gamejs.draw.circle(self.buffer, 'rgba(0, 0, 0, 0.4)', [planet.x, planet.y], planet.radius, 4);
      var textRender = gameFont.render(planet.ships, '#000');
      var ts = textRender.getSize();
      var x = planet.x - ts[0] / 2;
      var y = planet.y - ts[1] / 2 - 2;
      self.buffer.blit(textRender, [x, y]);
      y += ts[1];
      textRender = fleetFont.render(planet.id, 'rgba(50, 50, 50, 0.5)');
      ts = textRender.getSize();
      x = planet.x - ts[0] / 2;
      self.buffer.blit(textRender, [x, y]);
    });
    currentGameReplay.currentTurn.fleets.forEach(function(fleet) {
      gamejs.draw.rect(self.buffer, fleetColors[fleet.player], fleet.rect, 0);
      var textRender = fleetFont.render(fleet.ships, '#000');
      self.buffer.blit(textRender, fleet.rect.topleft);
    });
  };
  // Draw for current turn
  self.draw = function(surface) {
    surface.blit(self.buffer, self.rect);
  };
  self.projectCoordinates = function(planets) {
    var minx = 100000;
    var miny = 100000;
    var maxx = 0;
    var maxy = 0;
    planets.forEach(function(planet) {
      minx = Math.min(minx, planet.x);
      miny = Math.min(miny, planet.y);
      maxx = Math.max(maxx, planet.x);
      maxy = Math.max(maxy, planet.y);
    });
    var sx = (self.rect.width - 2 * maxPlanetRadius) / (maxx - minx);
    var sy = (self.rect.height - 2 * maxPlanetRadius) / (maxy - miny);
    planets.forEach(function(planet) {
      planet.x = maxPlanetRadius + (planet.x - minx) * sx;
      planet.y = maxPlanetRadius + (planet.y - miny) * sy;
    });
  };
  return self;
};
gamejs.utils.objects.extend(UniverseSprite, gamejs.sprite.Sprite);

/*
 * Turn info: objects allowing to get a snapshot per turn and quickly show game state on each turn
 */

var PlanetTurnInfo = function(other) {
  var self = this;
  self.turn = other.turn;
  self.id = other.id;
  self.owner = other.owner;
  self.ships = other.ships;
  self.x = other.x;
  self.y = other.y;
  self.size = other.size;
  self.radius = other.radius;
  self.updatePlanet = function() {
    // Grow the ships on each planet, as defined in https://github.com/cheftako/Domination/blob/master/server/src/main/java/com/linkedin/domination/server/Game.java
    if (self.turn && self.owner) {
      self.size = planetSize(self.ships);
      if (self.size) self.ships += self.size * 2;
      else self.ships += 1;
    }
    self.size = planetSize(self.ships);
    self.radius = 22 + self.size * 5;
  };
  self.clone = function() {
    return new PlanetTurnInfo(self);
  };
  return self;
};

var PlayerTurnInfo = function(other) {
  var self = this;
  self.turn = other.turn;
  self.id = other.id;
  self.name = other.name;
  self.color = other.color;
  self.ships = other.ships;
  self.planets = other.planets;
  self.fleets = other.fleets;
  self.aggregateShips = function(planets, fleets) {
    self.ships = 0;
    self.planets = 0;
    self.fleets = 0;
    planets.forEach(function(planet) {
      if (planet.owner == self.id) {
        self.ships += planet.ships;
        self.planets += 1;
      }
    });
    fleets.forEach(function(fleet) {
      if (fleet.player == self.id) {
        self.ships += fleet.ships;
        self.fleets += 1;
      }
    });
  };
  self.clone = function() {
    return new PlayerTurnInfo(self);
  };
  self.toString = function() {
    res = 'p' + self.id + ' s=' + self.ships + ' f=' + self.fleets;
    return res;
  };
  return self;
};

var FleetTurnInfo = function(other) {
  var self = this;
  self.id = other.id;
  self.turn = other.turn;
  self.player = other.player;
  self.ships = other.ships;
  self.origin = other.origin;
  self.destination = other.destination;
  self.duration = other.duration;
  self.turnsRemaining = other.turnsRemaining || other.duration;
  self.toString = function() {
    return 'f' + self.id + ': [' + self.turn + ']' + ' ' + self.origin + '->' + self.destination +
      ' p=' + self.player + ', s=' + self.ships + ', tr=' + this.turnsRemaining;
  };
  self.clone = function() {
    return new FleetTurnInfo(self);
  };
  return self;
};

var TurnInfo = function(other) {
  var self = this;
  self.number = 0;
  self.planets = new HashTable();   // Planet states in this turn
  self.players = new HashTable();   // Player states in this turn
  self.fleets = new HashTable();    // Fleet up in the air during this turn, hashed by a "unique id across fleets across all turns"
  self.ships = 0;                   // Total number of ships in this turn
  if (other) {
    other.planets.forEach(function(planet) { self.planets.set(planet.id, planet.clone()); });
    other.players.forEach(function(player) { self.players.set(player.id, player.clone()); });
    if (other.fleets) other.fleets.forEach(function(fleet) { self.fleets.set(fleet.id, fleet.clone()); });
  }
  self.departing = [];              // Fleets taking off on this turn
  self.landing = [];                // Landing events on this turn
  self.toString = function() {
    var res = '[turn ' + self.number + ']: ';
    self.players.forEach(function (player) { res += player.toString() + ' '; });
    res += ', ' + self.departing.length + ' departing';
    return res + "\n";
  };
  self.next = function() {
    var nextTurn = new TurnInfo(self);
    nextTurn.number = self.number + 1;
    return nextTurn;
  };
  self.consumeEvent = function(event) {
    if (event.duration) {
      fleet = new FleetTurnInfo(event);
      fleet.id = fleetUniqueIdCounter++;
      self.departing.push(fleet);
    } else {
      self.landing.push(event);
    }
  };
  self.updateTurnState = function() {
    self.departing.forEach(function(fleet) {
      // Transfer departing
      self.fleets.set(fleet.id, fleet);
      var planet = self.planets.get(fleet.origin);
      var s0 = planet.ships;
      planet.ships -= fleet.ships;
      if (planet.ships < 0) showAlert(planet.ships + " ships on planet " + planet.id + ' at turn ' + self.number);
      if (planet.ships < 0) planet.ships = 0;   // Try compensate for a bug on producer side
    });
    var arriving = [];
    self.fleets.forEach(function(fleet) {
      fleet.turnsRemaining--;
      if (!fleet.turnsRemaining) {
        arriving.push(fleet);
      } else {
        var origin = self.planets.get(fleet.origin);
        var destination = self.planets.get(fleet.destination);
        var progress = (fleet.duration - fleet.turnsRemaining) / fleet.duration;
        var dir = $v.multiply([destination.x - origin.x, destination.y - origin.y], progress);
        var pos = $v.add([origin.x, origin.y], dir);
        var fsize = 12;
        fleet.rect = new gamejs.Rect([pos[0] - fsize / 2, pos[1] - fsize / 2, fsize, fsize]);
      }
    });
    arriving.forEach(function(fleet) {
      self.fleets.remove(fleet.id);
    });
    self.landing.forEach(function(event) {
      var planet = self.planets.get(event.planet);
      planet.owner = event.player;
      planet.ships = event.ships;
    });
    self.planets.forEach(function(planet) {
      planet.turn = self.number;
      planet.updatePlanet();
    });
    var maxShips = 0;
    self.ships = 0;
    self.players.forEach(function(player) {
      player.turn = self.number;
      player.aggregateShips(self.planets, self.fleets);
      maxShips = Math.max(maxShips, player.ships);
      self.ships += player.ships;
    });
    return maxShips;
  };
  return self;
};

var GameReplay = function() {
  var self = this;
  // Load a JSON game replay
  self.reset = function() {
    self.players = new HashTable();   // Players hashed by id
    self.planets = new HashTable();   // Planets from universe at start of game, hashed by planet id
    self.maxShips = 0;
    self.turns = [];
    self.totalTurns = 0;
    self.currentTurn = null;
    self.winningPlayer = null;
    self.loosingPlayersMaxShips = 0;
    self.message = null;
    self.error = null;
  };
  self.load = function(path, universe) {
    // Load game replay file, 'path' should be either an object or a path (file or http) to a json file
    self.reset();
    try {
      numberOfAlertsShown = 0;
      var json = gamejs.http.load(path);
      self.maxShips = 0;
      fleetUniqueIdCounter = 1;
      json.players.forEach(function(player) {
        player.turn = 0;
        player.color = playerColors[player.id];
        player.ships = 0;
        player.planets = 0;
        player.fleets = 0;
        self.players.set(player.id, new PlayerTurnInfo(player));
      });
      json.planets.forEach(function(planet) {
        planet.turn = 0;
        var pinfo = new PlanetTurnInfo(planet);
        pinfo.updatePlanet();
        self.planets.set(pinfo.id, pinfo);
      });
      universe.projectCoordinates(self.planets);
      self.turns = [];
      // Generate one turn info object per turn, to be able to show game at any turn without recalculating stuff all the time
      var currentInfo = new TurnInfo(self);
      self.turns.push(currentInfo);
      json.events.forEach(function(event) {
        event.turn++;
        while (currentInfo.number < event.turn) {
          self.maxShips = Math.max(self.maxShips, currentInfo.updateTurnState());
          currentInfo = currentInfo.next();
          self.turns.push(currentInfo);
        }
        if (event.turn == currentInfo.number) currentInfo.consumeEvent(event);
      });
      self.maxShips = Math.max(self.maxShips, currentInfo.updateTurnState());
      // Add one last turn to show correct final player ship counts
      currentInfo = currentInfo.next();
      self.turns.push(currentInfo);
      self.maxShips = Math.max(self.maxShips, currentInfo.updateTurnState());
      self.currentTurn = self.turns[0];
      self.totalTurns = currentInfo.number;
      currentInfo.players.forEach(function(player) {
        if (!self.winningPlayer || self.winningPlayer.ships < player.ships) self.winningPlayer = player;
      });
      self.turns.forEach(function(turnInfo) {
        turnInfo.players.forEach(function(player) {
          if (player.id != self.winningPlayer.id) self.loosingPlayersMaxShips = Math.max(self.loosingPlayersMaxShips, player.ships);
        });
      });
    } catch (err) {
      self.reset();
      self.error = "Can't load game replay:\n" + err;
    }
  };
  self.moveTurn = function(step) {
    if (self.currentTurn) self.setTurn(self.currentTurn.number + step);
  };
  self.setTurn = function(turn) {
    var number = turn;
    if (number < 0) number = 0;
    if (number > self.totalTurns) number = self.totalTurns;
    if (self.turns.length) {
      self.currentTurn = self.turns[number];
      //self.message = self.currentTurn.toString();
    } else {
      self.currentTurn = null;
    }
  };
  self.reset();
  return self;
};

/*
 * Main game scene
 */

var GameScene = function() {
  var self = this;
  // Settings
  self.bgColor = '#fff';
  self.messageFont = new gamejs.font.Font("14px Verdana");
  self.views = new gamejs.sprite.Group();
  // State
  self.isPlaying = false;
  self.turnSpeed = 100;       // Turn speed in milliseconds (how long to wait to go to next turn when playing)
  self.lastTurnTick = 0;      // Ticks since last turn played
  // Game control functions
  self.load = function(path) {
    self.pause();
    currentGameReplay.load(path, self.universe);
    if (!currentGameReplay.error) {
      self.play();
    }
  };
  self.restart = function() {
    // Restart game from beginning
    currentGameReplay.setTurn(0);
    self.play();
  };
  self.play = function() {
    if (self.isPlaying) return;
    self.isPlaying = true;
    self.playButton.image = "pause.png";
  };
  self.pause = function() {
    if (!self.isPlaying) return;
    self.isPlaying = false;
    self.playButton.image = "start.png";
  };
  self.togglePlay = function() {      // Toggle play/pause
    if (self.isPlaying) self.pause();
    else self.play();
  };
  self.stepForward = function() {
    // Step 1 turn forward
    self.lastTurnTick = 0;
    currentGameReplay.moveTurn(1);
    self.pause();
  };
  self.stepBackward = function() {
    // Step 1 turn backward
    self.lastTurnTick = 0;
    currentGameReplay.moveTurn(-1);
    self.pause();
  };
  self.jumpToTurn = function(turn) {
    self.lastTurnTick = self.turnSpeed + 1;
    currentGameReplay.setTurn(turn);
    self.pause();
  };
  // Helpers
  self.addButton = function(rect, image, tooltip, onClick) {
    var button = new Button(rect, onClick);
    button.image = image;
    button.bgColor = self.bgColor;
    self.views.add(button);
    return button;
  };
  self.switchStats = function() {
    self.scoreBoard.switchRenderers();
  };
  // Layout
  var buttonSize = 32;
  self.addButton([0, 0, buttonSize, buttonSize], "restart.png", "Restart game from beginning", self.restart);
  self.playButton = self.addButton([buttonSize, 0, buttonSize, buttonSize], "start.png" ,"Play/pause game", self.togglePlay);
  self.addButton([0, buttonSize + 8, buttonSize, buttonSize], "skip-backward.png", "Step one turn back", self.stepBackward);
  self.addButton([buttonSize, buttonSize + 8, buttonSize, buttonSize], "skip-forward.png", "Step one turn forward", self.stepForward);
  self.turnCounterSprite = new TurnCounterSprite([canvasWidth - buttonSize * 4, 0, buttonSize * 4, buttonSize]);
  self.views.add(self.turnCounterSprite);
  var x = self.playButton.rect.right + 2;
  var pwidth = (self.turnCounterSprite.rect.left - x) / 3;
  for (var player_id in playerColors) {
    if (player_id > 0) {
      self.views.add(new PlayerSprite(player_id, [x, 0, pwidth, buttonSize]));
      x += pwidth;
    }
  }
  self.switchButton = self.addButton([canvasWidth - 16, buttonSize + 2, 16, 40], "switch.png", "Switch stats", self.switchStats);
  self.scoreBoard = new ScoreBoardSprite(
    [
      self.playButton.rect.right + 2,
      self.switchButton.rect.top,
      self.switchButton.rect.left - self.playButton.rect.right,
      self.switchButton.rect.height
    ],
    self.jumpToTurn
  );
  self.views.add(self.scoreBoard);
  self.universe = new UniverseSprite([0, self.scoreBoard.rect.bottom, canvasWidth, canvasHeight - self.scoreBoard.rect.bottom]);
  self.views.add(self.universe);
  // Events
  self.handleEvent = function(event) {
    self.views.forEach(function(view) { if (view.handleEvent) view.handleEvent(event); });
  };
  // Update
  self.update = function(msDuration) {
    if (self.isPlaying && currentGameReplay && currentGameReplay.currentTurn) {
      self.lastTurnTick += msDuration;
      if (self.lastTurnTick > self.turnSpeed) {
        self.lastTurnTick = 0;
        currentGameReplay.moveTurn(1);
        if (currentGameReplay.currentTurn.number >= currentGameReplay.totalTurns) self.pause();
      }
    }
    self.views.update(msDuration);
  };
  // Draw
  self.draw = function(surface) {
    surface.fill(self.bgColor);
    self.views.draw(surface);
    if (currentGameReplay) {
      if (currentGameReplay.error) {
        drawTextAt(surface, 2, 80, self.messageFont, '#f00', currentGameReplay.error);
      }
      if (currentGameReplay.message) {
        lines = currentGameReplay.message.split('\n');
        var y = 40;
        for (var i = 0; i < Math.min(lines.length, 50);  i++) {
          if (lines[i].length > 0) {
            drawTextAt(surface, 74, y, self.messageFont, '#00f', lines[i]);
          }
          y += 20;
        }
      }
    }
  };
  return self;
};

/*
 * Simple title scene
 */

var TitleScene = function(title) {
  var self = this;
  self.titleFont = new gamejs.font.Font("40px Verdana");
  self.buttons = new gamejs.sprite.Group();
  var startButton = new Button([350, 150, 100, 30], function() {
    var gameScene = new GameScene();
    director.push(gameScene);
    gameScene.load("test-replay.json");

  });
  startButton.text = "Start";
  self.buttons.add(startButton);
  self.bgColor = null;
  self.fgColor = '#7777ff';
  self.title = title;
  gamejs.display.setCaption(title);
  self.handleEvent = function(event) {
    self.buttons.forEach(function(btn) { btn.handleEvent(event); });
  };
  self.draw = function(surface) {
    surface.fill(self.bgColor || '#fff');
    var textRender = self.titleFont.render(self.title, self.fgColor);
    var tw = textRender.getSize()[0];
    var th = textRender.getSize()[1];
    var x0 = (surface.getSize()[0] - tw) / 2;
    gamejs.draw.line(surface, '#ff0000', [x0,th+4], [x0+tw,th+4], 2);    // surface, color, startPos, endPos, width
    surface.blit(textRender, [x0, 0]);
    self.buttons.draw(surface);
  };
  return self;
};

/*
 * Scene director
 */

function Director (fps) {
  var self = this;
  var scenes = [];
  var activeScene = null;
  self.tickFont = new gamejs.font.Font("12px Times");
  self.showTickDuration = true;
  self.cumulatedTickDuration = 0;
  self.tickCount = 0;
  self.tickDuration = null;
  // game loop, msDuration = time since last tick() call
  function tick(msDuration) {
    if (activeScene === null) return;
    if (activeScene.handleEvent) gamejs.event.get().forEach(activeScene.handleEvent);
    else gamejs.event.get();   // throw all events away
    if (activeScene.update) activeScene.update(msDuration);
    if (activeScene.draw) {
      var mainSurface = gamejs.display.getSurface();
      activeScene.draw(mainSurface);
      if (self.showTickDuration) {
        self.cumulatedTickDuration += msDuration;
        self.tickCount += 1;
        if (self.cumulatedTickDuration > 1000) {
          self.tickDuration = Math.round(self.cumulatedTickDuration / self.tickCount) + ' ms';
          self.cumulatedTickDuration = 0;
          self.tickCount = 0;
        }
        if (self.tickDuration) drawTextAt(mainSurface, -2, -2, self.tickFont, "#bb0000", self.tickDuration);
      }
    }
  }
  self.push = function(scene) {
    if (activeScene) scenes.push(activeScene);
    activeScene = scene;
  };
  self.pop = function() {
    activeScene = scenes.pop();
    return activeScene;
  };
  self.replace = function(scene) {
    activeScene = scene;
  };
  gamejs.time.fpsCallback(tick, self, fps);
  return self;
}

/*
 * UI
 */

var Button = function(rect, onClick) {
  var self = this;
  Button.superConstructor.apply(self, arguments);
  self.text = null;
  self.image = null;
  self.textColor = '#000';
  self.bgColor = '#fff';
  self.font = new gamejs.font.Font("20px Times");
  self.onClick = onClick;
  self.rect = new gamejs.Rect(rect);
  self.enabled = true;
  self.hovered = false;
  self.mousePressed = false;
  self.handleEvent = function(event) {
    if (event.type == gamejs.event.MOUSE_DOWN) {
      self.mousePressed = self.hovered;
    } else if (event.type == gamejs.event.MOUSE_MOTION) {
      self.hovered = self.rect.collidePoint(event.pos);
    } else if (event.type == gamejs.event.MOUSE_UP) {
      if (self.mousePressed && self.hovered && self.enabled && self.onClick) {
        self.onClick();
      }
      self.mousePressed = false;
    }
  };
  self.update = function(msDuration) {
  };
  self.draw = function(surface) {
    gamejs.draw.rect(surface, self.bgColor, self.rect, 0);
    if (self.image) {
      var img = gamejs.image.load(self.image);
      surface.blit(img, centeredPosition(self.rect, img.getSize()));
    }
    if (self.text) {
      gamejs.draw.rect(surface, self.textColor, self.rect, 1);
      textRender = self.font.render(self.text, self.textColor);
      surface.blit(textRender, centeredPosition(self.rect, textRender.getSize()));
    }
    if (!self.enabled || !self.onClick) {
      gamejs.draw.rect(surface, 'rgba(200, 200, 200, 0.2)', self.rect, 0);
    } else if (self.hovered) {
      if (self.mousePressed) {
        gamejs.draw.rect(surface, 'rgba(0, 0, 220, 0.2)', self.rect, 0);
      } else {
        gamejs.draw.rect(surface, 'rgba(220, 220, 0, 0.2)', self.rect, 0);
      }
    }
  };
  return self;
};
gamejs.utils.objects.extend(Button, gamejs.sprite.Sprite);

/*
 * Helpers
 */

function showAlert(msg) {
  if (numberOfAlertsShown < 4) alert(msg);
  numberOfAlertsShown++;
}

/*
 * HashTable: unfortunately, JavaScript lacks one...
 */

var HashTable = function(obj) {
  var self = this;
  self.length = 0;
  self.items = {};
  for (var p in obj) {
    if (obj.hasOwnProperty(p)) {
      self.items[p] = obj[p];
      self.length++;
    }
  }
  self.set = function(key, value) {
    var previous;
    if (self.has(key)) previous = self.items[key];
    else self.length++;
    self.items[key] = value;
    return previous;
  };
  self.get = function(key) {
    return self.has(key) ? self.items[key] : undefined;
  };
  self.has = function(key) {
    return self.items.hasOwnProperty(key);
  };
  self.remove = function(key) {
    if (self.has(key)) {
      var previous = self.items[key];
      self.length--;
      delete self.items[key];
      return previous;
    }
    return undefined;
  };
  self.forEach = function(fn) {
    for (var k in self.items) fn(self.items[k]);
  };
  self.clear = function() {
      self.items = {};
      self.length = 0;
  };
  return self;
};

function plural(count, text) {
  if (count === 1) return count + text;
  return count + text + 's';
}

function planetSize(ships) {
  if (ships < 20) return 0;
  if (ships < 50) return 1;
  return 2;
}

function centeredPosition(r1, r2) {
  var x0 = r1.left + (r1.width - r2[0]) / 2;
  var y0 = r1.top + (r1.height - r2[1]) / 2;
  return [x0, y0];
}

function drawTextAt(surface, x, y, font, color, text) {
  var textRender = font.render(text, color);
  if (x < 0) x = surface.getSize()[0] - textRender.getSize()[0] + x;
  if (y < 0) y = surface.getSize()[1] - textRender.getSize()[1] + y;
  surface.blit(textRender, [x, y]);
}

function drawCenteredText(surface, rect, font, color, text) {
  var textRender = font.render(text, color);
  surface.blit(textRender, centeredPosition(rect, textRender.getSize()));
}

/*
 * Main function
 */

gamejs.ready(function () {
  canvasWidth = gamejs.display.getSurface().getSize()[0];
  canvasHeight = gamejs.display.getSurface().getSize()[1];
  // read URL parameters and store them in the parameters object
  var parameters = window.location.href;
  var path;
  var i;
  if ((i = parameters.indexOf('?')) !== -1) {
    parameters = parameters.substr(i + 1).split('#')[0].split('&');
    for (i = 0; i < parameters.length; i++) {
      var equalPos = parameters[i].indexOf('=');
      var key = parameters[i].substr(0, equalPos);
      var value = parameters[i].substr(equalPos + 1);
      if (key == 'game') {
        path = value;
      }
    }
  }
  if (!path) path = 'replay/test-replay.json';
  //director.push(new TitleScene("Galactic Domination"));
  currentGameReplay = new GameReplay();
  var gameScene = new GameScene();
  director.push(gameScene);
  gameScene.load(path);
});
