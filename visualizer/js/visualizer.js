/*
 * Galactic Domination
 *
 * Written by Colin Miller, Jacob Kessler, Walter Fender and Zoran Simic
 *
 */

var gamejs = require('gamejs');
var $v = require('gamejs/utils/vectors');

gamejs.preload(["restart.png", "start.png", "pause.png", "skip-backward.png", "skip-forward.png"]);

var bigFont = new gamejs.font.Font("40px Verdana");
var smallFont = new gamejs.font.Font("14px Times");

var director = new Director(20);

/*
 * Player: Shows player name, color and current number of ships
 */

var Player = function(id, name, color, avatar) {
  var self = this;
  Planet.superConstructor.apply(self, arguments);
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
gamejs.utils.objects.extend(Player, gamejs.sprite.Sprite);

/*
 * Planet: Shows player ownership (by color) and has one of 3 sizes: small, medium or large
 * Shows number of ships it currently holds
 */
var Planet = function(id, x, y, owner, ships) {
  var self = this;
  Planet.superConstructor.apply(self, arguments);
  self.id = id;
  self.x = x;
  self.y = y;
  self.owner = owner;
  self.ships = ships;
  self.rect = new gamejs.Rect(rect);

  self.update = function(msDuration) {
  };

  self.draw = function(surface) {
  };

  return self;
};
gamejs.utils.objects.extend(Planet, gamejs.sprite.Sprite);

var GameReplay = function() {
  var self = this;
  self.planets = [];
  self.players = [];
  self.events = [];
  var load = function(json) {
    self.planets = json['planets'];
    self.players = json['players'];
    self.events = json['events'];
  };
};

/*
 * Main game scene
 */

var GameScene = function() {
  var self = this;
  // Settings
  self.bgColor = '#eee';
  // Components
  self.scoreboard = new gamejs.sprite.Group();
  self.buttons = new gamejs.sprite.Group();
  self.planets = new gamejs.sprite.Group();
  self.ships = new gamejs.sprite.Group();
  // State
  self.game = null;         // Game replay object, of type GameReplay
  self.isPlaying = false;
  self.currentTurn = 0;
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
    // Load game replay file, 'path' should be either an object or a path (file or http) to a json file
  };
  self.restart = function() {
    // Restart game from beginning
  };
  self.togglePlay = function() {
    // Toggle play/pause
    self.isPlaying = !self.isPlaying;
    if (self.isPlaying) {
      self.playButton.image = "pause.png";
    } else {
      self.playButton.image = "start.png";
    }
  };
  self.stepForward = function() {
    // Step 1 turn forward
  };
  self.stepBackward = function() {
    // Step 1 turn backward
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
    self.scoreboard.update(msDuration);
    self.buttons.update(msDuration);
    self.planets.update(msDuration);
    self.ships.update(msDuration);
  };
  // Draw
  self.draw = function(surface) {
    surface.fill(self.bgColor);
    self.scoreboard.draw(surface);
    self.buttons.draw(surface);
    self.planets.draw(surface);
    self.ships.draw(surface);
  };
  return self;
};

/*
 * Simple title scene
 */

var TitleScene = function(title) {
  var self = this;
  self.buttons = new gamejs.sprite.Group();
  var startButton = new Button([350, 150, 100, 30], function() { director.push(new GameScene()); });
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
    var textRender = bigFont.render(self.title, self.fgColor);
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
        if (self.tickDuration) drawCenteredText(mainSurface, -2, -2, smallFont, "#bb0000", self.tickDuration);
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

function centeredPosition(r1, r2) {
  var x0 = (r1.width - r2[0]) / 2 + r1.x;
  var y0 = (r1.height - r2[1]) / 2 + r1.y;
  return [x0, y0];
}

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
    } else if (event.type === gamejs.event.MOUSE_MOTION) {
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
    if (self.enabled === false || !self.onClick) {
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
  director.push(new TitleScene("Galactic Domination"));
});
