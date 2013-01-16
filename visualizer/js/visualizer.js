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
var gameArena = new gamejs.sprite.Group();

/*
 * Player: Shows player name, color and current number of ships
 */

var Player = function(id, name, color, avatar) {
  Planet.superConstructor.apply(this, arguments);
  this.id = id;
  this.name = name;
  this.color = color;
  this.avatar = avatar;

  this.update = function(msDuration) {
  };

  this.draw = function(surface) {
  };

  return this;
};
gamejs.utils.objects.extend(Player, gamejs.sprite.Sprite);

/*
 * Planet: Shows player ownership (by color) and has one of 3 sizes: small, medium or large
 * Shows number of ships it currently holds
 */
var Planet = function(id, x, y, owner, ships) {
  Planet.superConstructor.apply(this, arguments);
  this.id = id;
  this.x = x;
  this.y = y;
  this.owner = owner;
  this.ships = ships;
  this.rect = new gamejs.Rect(rect);

  this.update = function(msDuration) {
  };

  this.draw = function(surface) {
  };

  return this;
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
  var scenes = [];
  var activeScene = null;

  this.showFps = true;

  // game loop, msDuration = time since last tick() call
  function tick(msDuration) {
    if (activeScene === null) return;
    if (activeScene.handleEvent) gamejs.event.get().forEach(activeScene.handleEvent);
    else gamejs.event.get();   // throw all events away
    if (activeScene.update) activeScene.update(msDuration);
    if (activeScene.draw) {
      var mainSurface = gamejs.display.getSurface();
      activeScene.draw(mainSurface);
      if (this.showFps) drawText(mainSurface, -2, -2, smallFont, "#bb0000", Math.round(1000 / msDuration));
    }
  }

  this.push = function(scene) {
    if (activeScene) scenes.push(activeScene);
    activeScene = scene;
  };

  this.pop = function(scene) {
    activeScene = scenes.pop();
  };

  this.replace = function(scene) {
    activeScene = scene;
  };

  this.getScene = function() {
    return activeScene;
  };

  gamejs.time.fpsCallback(tick, this, 20);
  return this;
}

/*
 * Main game scene
 */

var GameScene = function() {
};

/*
 * Simple title scene
 */

var TitleScene = function(title) {
  this.bgColor = null;
  this.fgColor = '#7777ff';
  this.title = title;
  gamejs.display.setCaption(title);
  this.draw = function(surface) {
    surface.fill(this.bgColor || '#fff');
    var textRender = bigFont.render(this.title, this.fgColor);
    var tw = textRender.getSize()[0];
    var th = textRender.getSize()[1];
    var sw = surface.getSize()[0];
    var sh = surface.getSize()[1];
    x0 = (sw - tw) / 2;
    y0 = (sh - th) / 2;
    gamejs.draw.line(surface, '#ff0000', [x0,th+4], [x0+tw,th+4], 2);    // surface, color, startPos, endPos, width
    surface.blit(textRender, [x0, 0]);
  };
};

/*
 * Main function
 */

gamejs.ready(function () {
  var director = new Director();
  director.push(new TitleScene("Galactic Domination"));
});
