#version 330

in vec2 uv;

out vec4 fragColor;

layout(std140) uniform OverlayData {
  float energyPercent;
  float healthPercent;
  float manaPercent;
  float aspectRatio;
  float is1080p;
};

float opacity = 0.7;
vec4 colorEnergyBase = vec4(0.8, 0, 0.2, opacity);
vec4 colorEnergyBase2 = vec4(0.6, 0, 0.15, opacity);
vec4 colorEnergy = vec4(0, 0.8, 0.55, opacity);
vec4 colorEnergy2 = vec4(0, 0.7, 0.7, opacity);

vec4 colorHpBase = vec4(0.8, 0, 0.2, 0.85);
vec4 colorHp = vec4(0, 0.75, 0.6, 0.85);

vec4 colorManaBase = vec4(0.8, 0, 0.2, opacity);
vec4 colorMana = vec4(0.37, 0.725, 1.0, opacity);

// TODO - config???!+!!+!+!+???!+!
float healthBarSize = 0.3;
float healthBarOutlineSize = .002;
float healthBarThickness = 0.008;
// TODO - fix for 1080p???
float healthBarY = 0.972;
float healthBarY1080 = 0.965;
float healthBarMixThreshold = 0.001;

// TODO - CONFIGGILG?NG?G
float barWidth = 19;
float energyBarOutlineSize = .0012;
// bigger is closer lol
float yOffset = 0.097;

// eyeball it baby
// TODO OOOOOOOOO
float energyWidth = 0.049;

// hehehe
float healthBarStart = 0.5 - healthBarSize / 2;
float energyStart = 0.5 - energyWidth / 2;

float manaBarSize = 0.047;
float manaBarStart = 0.5 - manaBarSize / 2;
float manaBarY = 0.46;
float manaBarThickness = 0.0035;
float manaBarOutlineSize = .0013;

#define PI 3.14159265

float udSegment(in vec2 a, in vec2 b) {
  a.x *= aspectRatio;
  b.x *= aspectRatio;
  vec2 ba = b - a;
  vec2 pa = vec2(uv.x * aspectRatio, uv.y) - a;
  float h = clamp(dot(pa, ba) / dot(ba, ba), 0.0, 1.0);
  return length(pa - h * ba);
}

float sdArc(in vec2 p, in vec2 sca, in vec2 scb, in float ra, in float rb) {
  p.x *= aspectRatio;
  p *= mat2(sca.x, sca.y, -sca.y, sca.x);
  p.x = abs(p.x);
  float k = (scb.y * p.x > scb.x * p.y) ? dot(p.xy, scb) : length(p);
  return sqrt(dot(p, p) + ra * ra - 2.0 * ra * k) - rb;
}

float drawEnergyBar(bool flip) {
  float arc = barWidth;// TODO CHANGE FOR THICKEST HAPPY
  float ta = flip ? radians(-90.0) : radians(90.0);
  float tb = radians(arc);
  float rb = 0.004;// TODO - change for THICKNESS aka also make thick happy ? maybe

  vec2 offset = vec2(0, yOffset);// TODO - change to OFFSET IT LOL
  return sdArc(vec2(-0.5, -0.5) + uv + (flip ? -offset : offset), vec2(sin(ta), cos(ta)), vec2(sin(tb), cos(tb)), 0.12, rb);// TODO change .12 to make TINY or BIG??
}

float drawManaBar() {
  vec2 start = vec2(manaBarStart + manaBarThickness / 2.0, manaBarY);
  return udSegment(start, start + vec2(manaBarSize - manaBarThickness, 0)) - manaBarThickness;
}

float drawHealthBar() {
  float healthBarY = is1080p > 0.5 ? healthBarY1080 : healthBarY;
  vec2 start = vec2(healthBarStart + healthBarThickness / 2.0, healthBarY);
  return udSegment(start, start + vec2(healthBarSize - healthBarThickness, 0)) - healthBarThickness;
}

vec4 drawArc(float len, bool filled, bool colorGreen) {
  vec4 col = vec4(0.);
  vec4 outline = vec4(vec3(0.), colorEnergyBase.a);
  vec4 inner = filled && colorGreen ? colorEnergy : colorEnergyBase;
  vec4 inner2 = filled && colorGreen ? colorEnergy2 : colorEnergyBase2;

  // pretty???
  vec4 final = len > 0.1 ? inner : mix(inner2, inner, min(1.0, -len * 200.0));
  col = mix(col, final, 1. - smoothstep(0., energyBarOutlineSize, len + energyBarOutlineSize / 3));
  if (!filled) {
    col = mix(col, outline, 1. - smoothstep(0., energyBarOutlineSize, abs(len)));
  }
  return col;
}

vec4 drawHealthArc(float len, bool filled) {
  vec4 col = vec4(0.);
  vec4 outline = vec4(vec3(0.), colorHpBase.a);
  vec4 inner;

  // End of the filled healthbar
  float filledEnd = healthBarStart + healthBarSize * healthPercent;
  if (filled && uv.x <= filledEnd) {
    inner = colorHp;
  } else if (filled && uv.x >= filledEnd) {
    float mixxy = uv.x - filledEnd;
    if (mixxy < healthBarMixThreshold) {
      inner = mix(colorHp, colorHpBase, (1.0 / healthBarMixThreshold) * mixxy);
    } else {
      inner = colorHpBase;
    }
  } else {
    inner = colorHpBase;
  }

  col = mix(col, inner, 1. - smoothstep(0., healthBarOutlineSize, len + healthBarOutlineSize / 3));
  if (!filled) {
    col = mix(col, outline, 1. - smoothstep(0., healthBarOutlineSize, abs(len)));
  }
  return col;
}

vec4 drawManaArc(float len, bool filled) {
  vec4 col = vec4(0.);
  vec4 outline = vec4(vec3(0.), colorManaBase.a);
  vec4 inner;

  // End of the filled manabar
  float filledEnd = manaBarStart + manaBarSize * manaPercent;
  if (filled && uv.x <= filledEnd) {
    inner = colorMana;
  } else if (filled && uv.x >= filledEnd) {
    float mixxy = uv.x - filledEnd;
    if (mixxy < healthBarMixThreshold) {
      inner = mix(colorMana, colorManaBase, (1.0 / healthBarMixThreshold) * mixxy);
    } else {
      inner = colorManaBase;
    }
  } else {
    inner = colorManaBase;
  }

  col = mix(col, inner, 1. - smoothstep(0., manaBarOutlineSize, len + manaBarOutlineSize / 3));
  if (!filled) {
    col = mix(col, outline, 1. - smoothstep(0., manaBarOutlineSize, abs(len)));
  }
  return col;
}

void main() {
  vec4 top = drawArc(drawEnergyBar(false), false, false);
  float energyFill = (uv.x - energyStart) / energyWidth;
  bool topPercent = energyFill < min(1., energyPercent * 2);
  vec4 topFilled = drawArc(drawEnergyBar(false), true, topPercent);
  vec4 colT = mix(top, topFilled, topFilled.a);

  vec4 btm = drawArc(drawEnergyBar(true), false, false);
  bool btmPercent = energyFill < min(1., max(0., (energyPercent - .5) * 2.));
  vec4 btmFilled = drawArc(drawEnergyBar(true), true, btmPercent);
  vec4 colB = mix(btm, btmFilled, btmFilled.a);

  // Mana bar
  vec4 mana = drawManaArc(drawManaBar(), false);
  float mPerc = min(1.0, manaPercent);
  vec4 manaBlue = drawManaArc(drawManaBar(), true);
  vec4 colM = mix(mana, manaBlue, mPerc > 0.0 ? manaBlue.a : 0.0);

  vec4 health = drawHealthArc(drawHealthBar(), false);
  vec4 healthGreen = drawHealthArc(drawHealthBar(), true);
  vec4 colH = mix(health, healthGreen, healthGreen.a);

  if (colT.a > 0) {
    fragColor = colT;
    fragColor.a *= .85;
  } else if (colB.a > 0) {
    fragColor = colB;
    fragColor.a *= .85;
  } else if (colM.a > 0.0) {
    fragColor = colM;
    fragColor.a *= 0.85;
  }
  else if (colH.a > 0) {
    fragColor = colH;
    fragColor.a *= .95;
  }
}

