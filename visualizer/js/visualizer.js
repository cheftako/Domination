/*
 * Galactic Domination
 *
 * Written by Colin Miller, Jacob Kessler, Walter Fender and Zoran Simic
 *
 */

var gamejs = require('gamejs');
var $v = require('gamejs/utils/vectors');

// Globals
var director = new Director(20);
var currentGameReplay = null;     // Current game replay shown
var fleetUniqueIdCounter = 0;

// Settings
gamejs.preload(["restart.png", "start.png", "pause.png", "skip-backward.png", "skip-forward.png"]);
var playerColors = { 0: '#bbb', 1: '#f0f', 2: '#0ff', 3: '#ff0' };
var gameFont = new gamejs.font.Font("14px Verdana");
var scoreboardBigFont = new gamejs.font.Font("18px Verdana");
var scoreboardSmallFont = new gamejs.font.Font("14px Verdana");
var debugFont = new gamejs.font.Font("10px Verdana");

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
  self.representation = function(sep) {
    if (!sep) sep = ', ';
    var res = '';
    self.each(function(k, v) { res += k + '=' + v + sep; });
    return res;
  };
  self.remove = function(key) {
    if (self.has(key)) {
      previous = self.items[key];
      self.length--;
      delete self.items[key];
      return previous;
    }
    return undefined;
  };
  self.keys = function() {
    var keys = [];
    for (var k in self.items) keys.push(k);
    return keys;
  };
  self.values = function() {
    var values = [];
    for (var k in self.items) values.push(self.items[k]);
    return values;
  };
  self.each = function(fn) {
    for (var k in self.items) {
      fn(k, self.items[k]);
    }
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

/*
 * Player: Shows player name, color and current number of ships
 */

var PlayerSprite = function(id, rect) {
  var self = this;
  PlayerSprite.superConstructor.apply(self, arguments);
  self.id = id;
  self.rect = new gamejs.Rect(rect);
  self.update = function(msDuration) {
  };
  self.draw = function(surface) {
    if (!currentGameReplay.currentTurn) return;
    player = currentGameReplay.currentTurn.players.get(self.id);
    gamejs.draw.rect(surface, player.color, self.rect, 0);
    gamejs.draw.rect(surface, 'rgba(0,0,0,0.4)', self.rect, 2);
    var nameRender = scoreboardBigFont.render(player.name, '#000');
    surface.blit(nameRender, [self.rect.x + 2, self.rect.y]);
    var shipsRender = scoreboardSmallFont.render(player.ships + ' ships', '#000');
    surface.blit(shipsRender, [self.rect.right - shipsRender.getSize()[0] - 4, self.rect.top + (self.rect.height - shipsRender.getSize()[1]) / 2 ]);
  };
  return self;
};
gamejs.utils.objects.extend(PlayerSprite, gamejs.sprite.Sprite);

/*
 * ScoreBoard: Shows player info, current turn, total turns
 */

var ScoreBoardSprite = function(rect) {
  var self = this;
  ScoreBoardSprite.superConstructor.apply(self, arguments);
  self.rect = new gamejs.Rect(rect);
  self.update = function(msDuration) {
  };
  self.draw = function(surface) {
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
  self.update = function(msDuration) {
  };
  // Draw for current turn
  self.draw = function(surface) {
    //gamejs.draw.rect(surface, '#000', self.rect, 0);
    if (!currentGameReplay.currentTurn) return;
    currentGameReplay.currentTurn.planets.forEach(function(planet) {
      gamejs.draw.circle(surface, playerColors[planet.owner], [planet.x, planet.y], planet.radius, 0);
      gamejs.draw.circle(surface, 'rgba(0, 0, 0, 0.4)', [planet.x, planet.y], planet.radius, 4);
      var textRender = gameFont.render(planet.ships, '#000');
      surface.blit(textRender, [planet.x - textRender.getSize()[0] / 2, planet.y - textRender.getSize()[1] / 2]);
      textRender = debugFont.render(planet.id, '#000');
      surface.blit(textRender, [planet.x - textRender.getSize()[0] / 2, planet.y + textRender.getSize()[1]]);
    });
    currentGameReplay.currentTurn.fleets.forEach(function(fleet) {
    });
  };
  self.projectCoordinates = function(planets) {
    var minx = 10000;
    var miny = 10000;
    var maxx = 0;
    var maxy = 0;
    planets.forEach(function(planet) {
      minx = Math.min(minx, planet.x);
      miny = Math.min(miny, planet.y);
      maxx = Math.max(maxx, planet.x);
      maxy = Math.max(maxy, planet.y);
    });
    var sx = (self.rect.right - self.rect.left) / (maxx - minx);
    var sy = (self.rect.bottom - self.rect.top) / (maxy - miny);
    planets.forEach(function(planet) {
      planet.x = self.rect.left + (planet.x - minx) * sx;
      planet.y = self.rect.top + (planet.y - miny) * sy;
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
  self.id = other.id;
  self.owner = other.owner;
  self.ships = other.ships;
  self.x = other.x;
  self.y = other.y;
  self.size = other.size;
  self.radius = other.radius;
  self.updateSize = function() {
    self.size = planetSize(self.ships);
    self.radius = 12 + self.size * 2;
  };
  self.grow = function() {
    // Grow the ships on each planet, as defined in https://github.com/cheftako/Domination/blob/master/server/src/main/java/com/linkedin/domination/server/Game.java
    self.ships += self.size * 2 || 1;
  };
  self.clone = function() {
    return new PlanetTurnInfo(self);
  };
  return self;
};

var PlayerTurnInfo = function(other) {
  var self = this;
  self.id = other.id;
  self.name = other.name;
  self.color = other.color;
  self.ships = other.ships;
  self.aggregateShips = function(planets, fleets) {
    self.ships = 0;
    planets.forEach(function(planet) { if (planet.owner == self.id) self.ships += planet.ships; });
    fleets.forEach(function(fleet) { if (fleet.player == self.id) self.ships += fleet.ships; });
  };
  self.clone = function() {
    return new PlayerTurnInfo(self);
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
    return 'f' + self.id + ': [' + self.origin + '->' + self.destination + ' p=' + self.player + ', s=' + self.ships + ', tr=' + this.turnsRemaining + ']';
  };
  self.clone = function() {
    return new FleetTurnInfo(self);
  };
  return self;
};

var TurnInfo = function(previous) {
  var self = this;
  self.number = 0;
  self.planets = new HashTable();   // Planet states in this turn
  self.players = new HashTable();   // Player states in this turn
  self.fleets = new HashTable();    // Fleet up in the air during this turn, hashed by a "unique id across fleets across all turns"
  if (previous) {
    previous.planets.forEach(function(planet) { self.planets.set(planet.id, planet.clone()); });
    previous.players.forEach(function(player) { self.players.set(player.id, player.clone()); });
    if (previous.fleets) previous.fleets.forEach(function(fleet) { if (fleet.turnsRemaining) self.fleets.set(fleet.id, fleet.clone()); });
  }
  self.departing = [];              // Fleets taking off on this turn
  self.arriving = [];               // Fleets landing on this turn
  self.toString = function() {
    var res = '[turn ' + self.number + ']: ';
    self.players.forEach(function (player) { res += 'p' + player.id + ':' + player.ships + ' '; });
    res += ', ' + self.departing.length + ' departing';
    res += ', ' + self.arriving.length + ' arriving';
    res += ' p24: [' + self.planets.get(24).owner + ', ' + self.planets.get(24).ships + ']';
    return res + "\n";
  };
  self.next = function() {
    var nextTurn = new TurnInfo(self);
    nextTurn.number = self.number + 1;
    return nextTurn;
  };
  self.addFleet = function(event) {
    fleet = new FleetTurnInfo(event);
    self.departing.push(fleet);
  };
  self.update = function() {
    var planet_id, player_id, fleet_id;
    self.departing.forEach(function(fleet) {
      // Transfer departing
      self.fleets.set(fleet.id, fleet);
      self.planets.get(fleet.origin).ships -= fleet.ships;
    });
    self.fleets.forEach(function(fleet) {
      fleet.turnsRemaining--;
      if (!fleet.turnsRemaining) self.arriving.push(fleet);
    });
    self.arriving.forEach(function(fleet) {
      // Combat
      var planet = self.planets.get(fleet.destination);
      if (fleet.player == planet.owner) {
        planet.ships += fleet.ships;
      } else if (fleet.ships > planet.ships) {
        planet.ships = fleet.ships - planet.ships - 2;
        planet.owner = planet.ships ? fleet.player : 0;
      } else {
        planet.ships -= fleet.ships;
      }
    });
    self.planets.forEach(function(planet) {
      planet.updateSize();
      if (self.number) planet.grow();
    });
    self.players.forEach(function(player) { player.aggregateShips(self.planets, self.fleets); });
  };
  return self;
};

var GameReplay = function() {
  var self = this;
  // Load a JSON game replay
  self.reset = function() {
    self.players = new HashTable();   // Players hashed by id
    self.planets = new HashTable();   // Planets from universe at start of game, hashed by planet id
    self.turns = [];
    self.totalTurns = 0;
    self.currentTurn = null;
    self.message = null;
    self.error = null;
  };
  self.load = function(path, universe) {
    // Load game replay file, 'path' should be either an object or a path (file or http) to a json file
    self.reset();
    try {
      var json = gamejs.http.load(path);
      fleetUniqueIdCounter = 0;
      json.players.forEach(function(player) {
        player.color = playerColors[player.id];
        player.ships = 0;
        self.players.set(player.id, new PlayerTurnInfo(player));
      });
      json.planets.forEach(function(planet) {
        var pinfo = new PlanetTurnInfo(planet);
        pinfo.updateSize();
        self.planets.set(pinfo.id, pinfo);
      });
      universe.projectCoordinates(self.planets);
      self.turns = [];
      // Generate one turn info object per turn, to be able to show game at any turn with calculating stuff
      var previous = new TurnInfo(self);
      self.turns.push(previous);
      json.events.forEach(function(event) {
        event.duration -= 1;
        while (previous.number < event.turn) {
          var ti = previous.next();
          self.turns.push(ti);
          previous.update();
          previous = ti;
        }
        if (event.turn == previous.number) previous.addFleet(event);
      });
      previous.update();
      self.currentTurn = self.turns[0];
      self.totalTurns = previous.number;
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
      self.message = self.currentTurn.toString();
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
  self.bgColor = '#eee';
  self.messageFont = new gamejs.font.Font("14px Verdana");
  self.views = new gamejs.sprite.Group();
  // State
  self.isPlaying = false;
  self.turnSpeed = 200;       // Turn speed in milliseconds (how long to wait to go to next turn when playing)
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
    self.isPlaying = true;
    self.playButton.image = "pause.png";
  };
  self.pause = function() {
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
  };
  self.stepBackward = function() {
    // Step 1 turn backward
    self.lastTurnTick = 0;
    currentGameReplay.moveTurn(-1);
  };
  // Helpers
  self.addButton = function(rect, image, tooltip, onClick) {
    var button = new Button(rect, onClick);
    button.image = image;
    button.bgColor = self.bgColor;
    self.views.add(button);
    return button;
  };
  // Layout
  self.addButton([0, 0, 34, 34], "restart.png", "Restart game from beginning", self.restart);
  self.playButton = self.addButton([35, 0, 36, 34], "start.png" ,"Play/pause game", self.togglePlay);
  self.addButton([0, 35, 34, 34], "skip-backward.png", "Step one turn back", self.stepBackward);
  self.addButton([35, 35, 34, 34], "skip-forward.png", "Step one turn forward", self.stepForward);
  self.scoreBoard = new ScoreBoardSprite([72, 0, 788, 40]);
  self.views.add(self.scoreBoard);
  var x = self.scoreBoard.rect.x;
  var pwidth = 200;
  var pheight = 26;
  for (var player_id in playerColors) {
    if (player_id > 0) {
      self.views.add(new PlayerSprite(player_id, [x, self.scoreBoard.rect.y, pwidth, pheight]));
      x += pwidth;
    }
  }
  self.universe = new UniverseSprite([30, 100, 800, 570]);
  self.views.add(self.universe);
  // Events
  self.handleEvent = function(event) {
    self.views.forEach(function(view) { if (view.handleEvent) view.handleEvent(event); });
  };
  // Update
  self.update = function(msDuration) {
    if (self.isPlaying) {
      self.lastTurnTick += msDuration;
      if (self.lastTurnTick > self.turnSpeed) {
        self.stepForward();
        if (currentGameReplay.currentTurn.number >= currentGameReplay.totalTurns) self.pause();
      }
    }
    self.views.update(msDuration);
  };
  // Draw
  self.draw = function(surface) {
    surface.fill(self.bgColor);
    var sw = surface.getSize()[0];
    var sh = surface.getSize()[1];
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
        if (self.tickDuration) drawCenteredText(mainSurface, -2, -2, self.tickFont, "#bb0000", self.tickDuration);
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
    //} else if (self.mousePressed) {
    //  gamejs.draw.rect(surface, 'rgba(0, 0, 220, 0.3)', self.rect, 0);
    }
  };
  return self;
};
gamejs.utils.objects.extend(Button, gamejs.sprite.Sprite);

/*
 * Helpers
 */

function planetSize(ships) {
  if (ships < 20) return 0;
  if (ships < 50) return 1;
  return 2;
}

function simpleCopy(obj) {
  var res = {};
  for (var key in obj) res[key] = obj[key];
  return res;
}

function centeredPosition(r1, r2) {
  var x0 = (r1.width - r2[0]) / 2 + r1.x;
  var y0 = (r1.height - r2[1]) / 2 + r1.y;
  return [x0, y0];
}

function drawTextAt(surface, x, y, font, color, text) {
  var textRender = font.render(text, color);
  surface.blit(textRender, [x, y]);
}

function drawCenteredText(surface, x, y, font, color, text) {
  var textRender = font.render(text, color);
  if (x < 0) x = surface.getSize()[0] - textRender.getSize()[0] + x;
  if (y < 0) y = surface.getSize()[1] - textRender.getSize()[1] + y;
  surface.blit(textRender, [x, y]);
}

/*
 * Main function
 */

gamejs.ready(function () {
  //director.push(new TitleScene("Galactic Domination"));
  currentGameReplay = new GameReplay();
  var gameScene = new GameScene();
  director.push(gameScene);
  gameScene.load("test-replay.json");
});
