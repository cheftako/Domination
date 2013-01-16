/*
 * Galactic Domination
 *
 * Written by Colin Miller, Jacob Kessler, Walter Fender and Zoran Simic
 *
 */

var gamejs = require('gamejs');
var $v = require('gamejs/utils/vectors');

var bigFont = new gamejs.font.Font("40px Verdana");
var smallFont = new gamejs.font.Font("14px Times");

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

/*
 * Scene director
 */

function drawText(surface, x, y, font, color, text) {
  var textRender = font.render(text, color);
  if (x < 0) x = surface.getSize()[0] - textRender.getSize()[0] + x;
  if (y < 0) y = surface.getSize()[1] - textRender.getSize()[1] + y;
  surface.blit(textRender, [x, y]);
}

function Director () {
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
        if (self.tickDuration) drawText(mainSurface, -2, -2, smallFont, "#bb0000", self.tickDuration);
      }
    }
  }

  self.push = function(scene) {
    if (activeScene) scenes.push(activeScene);
    activeScene = scene;
  };

  self.pop = function(scene) {
    activeScene = scenes.pop();
  };

  self.replace = function(scene) {
    activeScene = scene;
  };

  self.getScene = function() {
    return activeScene;
  };

  gamejs.time.fpsCallback(tick, self, 20);
  return self;
}

/*
 * UI
 */

var Button = function(rect, onClick) {
  var self = this;
  Button.superConstructor.apply(self, arguments);
  self.text = null;
  self.color = '#000';
  self.onClick = onClick;
  self.rect = new gamejs.Rect(rect);
  self.enabled = true;
  self.hovered = false;
  self.font = new gamejs.font.Font("18px Times");
  self.handleEvent = function(event) {
    self.hovered = self.rect.collidePoint(event.pos);
    if (!self.enabled) return false;
  };
  self.draw = function(surface) {
    if (!self.rect) return;
    if (self.text) {
      textRender = self.font.render(self.text, self.color);
      var tsize = textRender.getSize();
      var x0 = (self.rect.width - tsize[0]) / 2 + self.rect.x;
      var y0 = (self.rect.height - tsize[1]) / 2 + self.rect.y - 1;
      surface.blit(textRender, [x0, y0]);
    }
    if (self.hovered) {
      gamejs.draw.rect(surface, 'rgba(150, 150, 1, 0.4)', self.rect, 0);
      gamejs.draw.rect(surface, 'rgba(0, 0, 0, 0.6)', self.rect, 2);
    } else {
      gamejs.draw.rect(surface, 'rgba(0, 0, 0, 0.9)', self.rect, 2);
    }
    if (!self.enabled) {
      gamejs.draw.rect(surface, 'rgba(200, 200, 200, 0.5)', self.rect, 0);
    }
  };
  return self;
};
gamejs.utils.objects.extend(Button, gamejs.sprite.Sprite);

/*
 * Main game scene
 */

var GameScene = function() {
  var self = this;
  self.scoreboard = new gamejs.sprite.Group();
  self.controls = new gamejs.sprite.Group();
  self.planets = new gamejs.sprite.Group();
  self.ships = new gamejs.sprite.Group();

  return self;
};

/*
 * Simple title scene
 */

function handleGroupEvents(event, group) {
  var handled = false;
  group.forEach(function(sprite) {
    if (sprite.handleEvent && sprite.handleEvent(event)) handled = true;
  });
  return handled;
}

var TitleScene = function(title) {
  var self = this;
  self.buttons = new gamejs.sprite.Group();
  var startButton = new Button([350, 150, 100, 30], function() {});
  startButton.text = "Start";
  self.buttons.add(startButton);
  self.bgColor = null;
  self.fgColor = '#7777ff';
  self.title = title;
  gamejs.display.setCaption(title);
  self.handleEvent = function(event) {
    if (handleGroupEvents(event, self.buttons)) return;
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
 * Main function
 */

gamejs.ready(function () {
  var director = new Director();
  director.push(new TitleScene("Galactic Domination"));
});
