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
var playerColors = { 0: '#bbb', 1: '#0f0', 2: '#00f', 3: '#ff0' };
var gameFont = new gamejs.font.Font("14px Verdana");

/*
 * Player: Shows player name, color and current number of ships
 */

var PlayerSprite = function(id, name, color, avatar) {
  var self = this;
  PlayerSprite.superConstructor.apply(self, arguments);
  self.id = id;
  self.name = name;
  self.color = color;
  self.avatar = avatar;
  self.update = function(msDuration) {
  };
  self.draw = function(surface) {
  };

  return self;
};
gamejs.utils.objects.extend(PlayerSprite, gamejs.sprite.Sprite);

/*
 * PlanetSprite: Sprite showing a planet for current turn
 * - color identifies ownership
 * - number of ships determines size: small, medium or large
 * - number of ships on planet shown on sprite
 */
var PlanetSprite = function(planet) {
  var self = this;
  PlanetSprite.superConstructor.apply(self, arguments);
  // State that does not change across turns
  self.planet = planet;
  self.id = planet.id;
  self.x = planet.x;
  self.y = planet.y;
  // Changes at each turn
  self.owner = planet.owner;
  self.ships = planet.ships;
  self.radius = -1;
  self.rect = new gamejs.Rect();
  // Update for current turn
  self.update = function(msDuration) {
    self.owner = currentGameReplay.planetOwner(self.planet);
    self.ships = currentGameReplay.planetShips(self.planet);
    var newRadius = 12 + planetSize(self.ships) * 5;
    if (self.radius != newRadius) {
      self.radius = newRadius;
      self.rect.topLeft = [self.x - self.radius / 2, self.y - self.radius / 2];
      self.rect.bottomRight = [self.x + self.radius / 2, self.y + self.radius / 2];
    }
  };
  // Draw for current turn
  self.draw = function(surface) {
    gamejs.draw.circle(surface, playerColors[self.owner], [x, y], self.radius, 0);
    gamejs.draw.circle(surface, 'rgba(0,0,0,0.4)', [x, y], self.radius, 2);
    var textRender = gameFont.render(self.ships, '#000');
    surface.blit(textRender, [self.x - textRender.getSize()[0] / 2, self.y - textRender.getSize()[1] / 2]);
  };
  return self;
};
gamejs.utils.objects.extend(PlanetSprite, gamejs.sprite.Sprite);

var FleetInfo = function(event) {
  var self = this;
  fleetUniqueIdCounter += 1;
  self.id = fleetUniqueIdCounter;   // Unique id, used to match FleetInfo with FleetSprite
  self.owner = event.player;        // Event comes from JSON
  self.ships = event.ships;
  self.origin = event.from;
  self.destination = event.to;
  self.startTurn = event.turn;
  self.duration = 5;
  self.turnsRemaining = 5;
  self.toString = function() {
    return 'f' + self.id + ': [' + self.origin + '->' + self.destination + ' o=' + self.owner + ', s=' + self.ships + ', tr=' + this.turnsRemaining + ']';
  };
  return self;
};

var TurnInfo = function(previous) {
  var self = this;
  self.previous = previous;
  self.number = previous ? previous.number + 1 : 0;
  self.planetOwner = {};    // Owner of planet, for this turn (hashed by planet id)
  self.planetShips = {};    // Ships on each planet, for this turn (hashed by planet id)
  self.playerShips = {};    // Total ships per player, for this turn (hashed by player id)
  self.fleets = {};         // Fleet up in the air during this turn, hashed by a "unique id across fleets across all turns"
  self.departing = [];      // Fleets taking off on this turn
  self.arriving = [];       // Fleets landing on this turn
  self.toString = function() {
    var res = '[turn ' + self.number + ']: ';
    for (var player_id in self.playerShips) res += 'p' + player_id + ':' + self.playerShips[player_id] + " ";
    res += ', ' + self.departing.length + ' departing';
    res += ', ' + self.arriving.length + ' arriving';
    res += ' p24: [' + self.planetOwner[24] + ', ' + self.planetShips[24] + ']';
    return res + "\n";
  };
  self.addFleet = function(event) {
    fleet = new FleetInfo(event);
    self.departing.push(fleet);
  };
  self.update = function() {
    var planet_id, player_id, fleet_id;
    self.fleets = {};
    self.arriving = [];
    if (self.previous) {
      // Copy previous
      self.planetOwner = simpleCopy(previous.planetOwner);
      self.planetShips = simpleCopy(previous.planetShips);
      self.playerShips = simpleCopy(previous.playerShips);
    }
    self.departing.forEach(function(fleet) {
      // Transfer departing
      self.fleets[fleet.id] = fleet;
      self.planetShips[fleet.origin] -= fleet.ships;
    });
    if (self.previous) {
      for (fleet_id in self.previous.fleets) {
        // Move fleet
        fleet = simpleCopy(self.previous.fleets[fleet_id]);
        if (fleet.turnsRemaining > 0) {
          fleet.turnsRemaining -= 1;
          self.fleets[fleet_id] = fleet;
        } else {
          self.arriving.push(fleet);
        }
      }
      self.arriving.forEach(function(fleet) {
        // Combat
        if (fleet.owner == self.planetOwner[fleet.destination]) {
          self.planetShips[fleet.destination] = self.previous.planetShips[fleet.destination] + fleet.ships;
        } else if (fleet.ships > self.previous.planetShips[fleet.destination]) {
          self.planetShips[fleet.destination] = fleet.ships - self.previous.planetShips[fleet.destination] - 2;
          self.planetOwner[fleet.destination] = self.previous.planetShips[fleet.destination] ? fleet.owner : 0;
        } else {
          self.planetShips[fleet.destination] = self.previous.planetShips[fleet.destination] - fleet.ships;
        }
      });
      for (planet_id in self.planetOwner) {
        // Grow the ships on each planet, as defined in https://github.com/cheftako/Domination/blob/master/server/src/main/java/com/linkedin/domination/server/Game.java
        if (self.planetOwner[planet_id] > 0) {
          var ps = planetSize(self.planetShips[planet_id]);
          if (ps === 0) self.planetShips[planet_id] += 1;
          else if (ps === 1) self.planetShips[planet_id] += 2;
          else self.planetShips[planet_id] += 4;
        }
      }
    }
    for (player_id in self.playerShips) {
      // Calculate player ship counts
      self.playerShips[player_id] = 0;
      for (planet_id in self.planetOwner) {
        if (self.planetOwner[planet_id] == player_id) self.playerShips[player_id] += self.planetShips[planet_id];
      }
      for (fleet_id in self.fleets) {
        fleet = self.fleets[fleet_id];
        if (fleet.owner == player_id) self.playerShips[player_id] += fleet.ships;
      }
    }
  };
  return self;
};

var GameReplay = function() {
  var self = this;
  // Load a JSON game replay
  self.reset = function() {
    self.players = {};      // Players hashed by id
    self.planets = {};      // Planets from universe at start of game, hashed by planet id
    self.events = [];
    self.confirmations = [];
    self.turns = [];
    self.totalTurns = 0;
    self.currentTurn = 0;
    self.message = null;
    self.error = null;
  };
  self.load = function(path) {
    // Load game replay file, 'path' should be either an object or a path (file or http) to a json file
    self.reset();
    try {
      var json = gamejs.http.load(path);
      fleetUniqueIdCounter = 0;
      json.players.forEach(function(player) { self.players[player.id] = player; } );
      json.planets.forEach(function(planet) { self.planets[planet.id] = planet; } );
      self.events = json.events;
      self.confirmations = json.confirmations;
      self.turns = [];
      // Generate one turn info object per turn, to be able to show game at any turn with calculating stuff
      var previous = new TurnInfo(null);
      for (var planet_id in self.planets) {
        planet = self.planets[planet_id];
        previous.planetOwner[planet_id] = planet.owner;
        previous.planetShips[planet_id] = planet.population;
      }
      for (var player_id in self.players) {
        previous.playerShips[player_id] = 0;
      }
      self.turns.push(previous);
      self.events.forEach(function(event) {
        while (previous.number < event.turn) {
          var ti = new TurnInfo(previous);
          self.turns.push(ti);
          previous.update();
          previous = ti;
        }
        if (event.turn == previous.number) previous.addFleet(event);
      });
      previous.update();
      self.totalTurns = previous.number;
    } catch (err) {
      self.reset();
      self.error = "Can't load game replay:\n" + err;
    }
  };
  // Ships on a planet a current turn
  self.planetShips = function(planet) {
    return self.turns[self.currentTurn].planetShips[planet.id];
  };
  // Ships on a planet a current turn
  self.planetOwner = function(planet) {
    return self.turns[self.currentTurn].planetOwner[planet.id];
  };
  self.moveTurn = function(step) {
    self.setTurn(self.currentTurn + step);
  };
  self.setTurn = function(turn) {
    self.currentTurn = turn;
    if (self.currentTurn < 0) self.currentTurn = 0;
    if (self.currentTurn > self.totalTurns) self.currentTurn = self.totalTurns;
    self.message = self.turns[self.currentTurn].toString();
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
  // Components
  self.scoreboard = new gamejs.sprite.Group();
  self.buttons = new gamejs.sprite.Group();
  self.planets = new gamejs.sprite.Group();
  self.ships = new gamejs.sprite.Group();
  // State
  self.isPlaying = false;
  self.turnSpeed = 200;       // Turn speed in milliseconds (how long to wait to go to next turn when playing)
  self.lastTurnTick = 0;      // Ticks since last turn played
  // Helpers
  self.addButton = function(rect, image, tooltip, onClick) {
    var button = new Button(rect, onClick);
    button.image = image;
    button.bgColor = self.bgColor;
    self.buttons.add(button);
    return button;
  };
  // Game control functions
  self.load = function(path) {
    self.pause();
    currentGameReplay.load(path);
    if (!currentGameReplay.error) self.play();
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
  // Buttons
  self.addButton([0, 0, 34, 34], "restart.png", "Restart game from beginning", self.restart);
  self.playButton = self.addButton([35, 0, 36, 34], "start.png" ,"Play/pause game", self.togglePlay);
  self.addButton([0, 35, 34, 34], "skip-backward.png", "Step one turn back", self.stepBackward);
  self.addButton([35, 35, 34, 34], "skip-forward.png", "Step one turn forward", self.stepForward);
  // Events
  self.handleEvent = function(event) {
    self.buttons.forEach(function(btn) { btn.handleEvent(event); });
  };
  // Update
  self.update = function(msDuration) {
    if (self.isPlaying) {
      self.lastTurnTick += msDuration;
      if (self.lastTurnTick > self.turnSpeed) {
        self.stepForward();
      }
    }
    self.scoreboard.update(msDuration);
    self.buttons.update(msDuration);
    self.planets.update(msDuration);
    self.ships.update(msDuration);
  };
  // Draw
  self.draw = function(surface) {
    surface.fill(self.bgColor);
    var sw = surface.getSize()[0];
    var sh = surface.getSize()[1];
    self.scoreboard.draw(surface);
    self.buttons.draw(surface);
    self.planets.draw(surface);
    self.ships.draw(surface);
    if (currentGameReplay) {
      if (currentGameReplay.error) {
        drawTextAt(surface, 2, 80, self.messageFont, '#f00', currentGameReplay.error);
      }
      if (currentGameReplay.message) {
        lines = currentGameReplay.message.split('\n');
        var y = 100;
        for (var i = 0; i < Math.min(lines.length, 50);  i++) {
          if (lines[i].length > 0) {
            drawTextAt(surface, 2, y, self.messageFont, '#00f', lines[i]);
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
