// Finishes the Genshin Impact design system in the rdr2-map Figma file: the dialog and location card
// components, the foundations sheet and the app's screens. It builds on the variables, styles and
// components that are already in the file, and skips whatever it finds already built.

const COMPONENTS_PAGE = 'Components';
const GENSHIN_PAGE = 'Genshin Impact';
const W = 393;
const H = 852;
const STATUS_BAR_H = 54;
const HOME_INSET = 34;
const APP_VERSION = '0.1';
const DARK_COLLECTION = 'GI / Color · Dark';

const LOG = [];
function log(message) {
  LOG.push(message);
  console.log(message);
}

// Tokens and components of the file, by name
let C = {}; // GI / Color
let CD = {}; // GI / Color · Dark, the same tokens for the dark theme
let P = {}; // GI / Palette
let SZ = {}; // GI / Size
let TS = {}; // text styles without the "GI/" prefix
let ES = {}; // effect styles without the "GI/" prefix
let K = {}; // GI components and component sets
let compPage;
let genshinPage;

async function loadTokens() {
  const collections = await figma.variables.getLocalVariableCollectionsAsync();
  const collectionName = {};
  for (const c of collections) collectionName[c.id] = c.name;
  for (const v of await figma.variables.getLocalVariablesAsync()) {
    const name = collectionName[v.variableCollectionId];
    if (name === 'GI / Color') C[v.name] = v;
    else if (name === DARK_COLLECTION) CD[v.name] = v;
    else if (name === 'GI / Palette') P[v.name] = v;
    else if (name === 'GI / Size') SZ[v.name] = v;
  }
  if (!C['text/primary']) throw new Error('The GI variables are not in this file');
  for (const s of await figma.getLocalTextStylesAsync()) {
    if (!s.name.startsWith('GI/')) continue;
    TS[s.name.slice(3)] = s;
    try {
      await figma.loadFontAsync(s.fontName);
    } catch (e) {
      throw new Error(`The font ${s.fontName.family} ${s.fontName.style} of ${s.name} is not available`);
    }
  }
  for (const s of await figma.getLocalEffectStylesAsync()) {
    if (s.name.startsWith('GI/')) ES[s.name.slice(3)] = s;
  }
  await figma.loadFontAsync({ family: 'Inter', style: 'Regular' });
  await figma.loadFontAsync({ family: 'Inter', style: 'Semi Bold' });
}

async function loadComponents() {
  await compPage.loadAsync();
  K = {};
  for (const n of compPage.findAllWithCriteria({ types: ['COMPONENT', 'COMPONENT_SET'] })) {
    if (n.name.startsWith('GI/') && !K[n.name]) K[n.name] = n;
  }
}

// ---------- Helpers ----------

function rgb(hex) {
  const n = parseInt(hex.slice(1), 16);
  return { r: ((n >> 16) & 255) / 255, g: ((n >> 8) & 255) / 255, b: (n & 255) / 255 };
}

function toHex(c) {
  const h = x => Math.round(x * 255).toString(16).padStart(2, '0').toUpperCase();
  return `#${h(c.r)}${h(c.g)}${h(c.b)}`;
}

// The opacity has to be in the paint before the variable is bound, it's dropped otherwise
function solid(key, opacity = 1) {
  const v = C[key] || P[key];
  if (!v) throw new Error(`Unknown color token ${key}`);
  return figma.variables.setBoundVariableForPaint({ type: 'SOLID', color: { r: 0, g: 0, b: 0 }, opacity }, 'color', v);
}

function radius(node, key, fallback) {
  if (!SZ[key] && fallback !== undefined) {
    node.cornerRadius = fallback;
    return;
  }
  for (const corner of ['topLeftRadius', 'topRightRadius', 'bottomLeftRadius', 'bottomRightRadius']) {
    node.setBoundVariable(corner, SZ[key]);
  }
}

// As tall as the row, which hugs its texts
function stretchHeight(node, height) {
  try {
    node.layoutSizingVertical = 'FILL';
  } catch (e) {
    try {
      node.layoutAlign = 'STRETCH';
    } catch (e2) {
      node.resize(node.width, height);
    }
  }
}

function frame(name, w, h) {
  const f = figma.createFrame();
  f.name = name;
  f.resize(w, h);
  f.fills = [];
  f.clipsContent = false;
  return f;
}

function autoLayout(direction, props = {}) {
  const f = figma.createFrame();
  f.layoutMode = direction;
  f.primaryAxisSizingMode = 'AUTO';
  f.counterAxisSizingMode = 'AUTO';
  f.fills = [];
  f.clipsContent = false;
  for (const [k, v] of Object.entries(props)) f[k] = v;
  return f;
}

// A fixed width and a height that hugs the content; resize() resets the sizing modes, so they go after it
function column(name, width, props = {}) {
  const f = autoLayout('VERTICAL', Object.assign({ name }, props));
  f.resize(width, 10);
  f.counterAxisSizingMode = 'FIXED';
  f.primaryAxisSizingMode = 'AUTO';
  return f;
}

function row(name, width, props = {}) {
  const f = autoLayout('HORIZONTAL', Object.assign({ name }, props));
  f.resize(width, 10);
  f.primaryAxisSizingMode = 'FIXED';
  f.counterAxisSizingMode = 'AUTO';
  return f;
}

async function text(chars, style, color, name = 'label') {
  const t = figma.createText();
  await t.setTextStyleIdAsync(TS[style].id);
  t.characters = chars;
  t.fills = [solid(color)];
  t.name = name;
  return t;
}

// Documentation text, outside the design system
function docText(chars, size, color = '#7E8698', weight = 'Regular') {
  const t = figma.createText();
  t.fontName = { family: 'Inter', style: weight };
  t.fontSize = size;
  t.characters = chars;
  t.fills = [{ type: 'SOLID', color: rgb(color) }];
  return t;
}

const SHAPES = ['VECTOR', 'ELLIPSE', 'RECTANGLE', 'BOOLEAN_OPERATION', 'STAR', 'POLYGON'];

// The "cut" of a two-tone icon (the cross of Clear) keeps its own color
function tintVar(node, variable, opacity = 1) {
  const paint = () => figma.variables.setBoundVariableForPaint({ type: 'SOLID', color: { r: 0, g: 0, b: 0 }, opacity }, 'color', variable);
  for (const n of node.findAll(n => SHAPES.includes(n.type) && n.name !== 'cut')) {
    if (Array.isArray(n.fills) && n.fills.length) n.fills = [paint()];
    if (Array.isArray(n.strokes) && n.strokes.length) n.strokes = [paint()];
  }
}

function tint(node, key, opacity = 1) {
  const v = C[key] || P[key];
  if (!v) throw new Error(`Unknown color token ${key}`);
  tintVar(node, v, opacity);
}

function variant(set, props) {
  const found = set.children.find(c => Object.entries(props).every(([k, v]) => c.variantProperties && c.variantProperties[k] === v));
  if (!found) throw new Error(`No variant ${JSON.stringify(props)} in ${set.name}`);
  return found;
}

// Values by the property's name without the "#id" suffix
function setProps(instance, values) {
  const defs = instance.componentProperties;
  const out = {};
  for (const [name, value] of Object.entries(values)) {
    const key = Object.keys(defs).find(k => k === name || k.split('#')[0] === name);
    if (!key) throw new Error(`No property "${name}" on ${instance.name}`);
    out[key] = value;
  }
  instance.setProperties(out);
}

function inst(name, variantProps = {}, values = {}) {
  const node = K[name];
  if (!node) throw new Error(`Missing component ${name}`);
  const component = node.type === 'COMPONENT_SET'
    ? (Object.keys(variantProps).length ? variant(node, variantProps) : node.defaultVariant)
    : node;
  const i = component.createInstance();
  if (Object.keys(values).length) setProps(i, values);
  return i;
}

function icon(name, size, color, opacity = 1) {
  const i = inst(`GI/Icon/${name}`);
  i.resize(size, size);
  if (color) tint(i, color, opacity);
  return i;
}

async function effect(node, name) {
  await node.setEffectStyleIdAsync(ES[name].id);
}

function place(parent, node, x, y) {
  parent.appendChild(node);
  node.x = x;
  node.y = y;
  return node;
}

function boardItems(boardName) {
  const board = compPage.findOne(n => n.type === 'FRAME' && n.name === boardName);
  if (!board) throw new Error(`Missing board ${boardName}`);
  return board.children.find(n => n.name === 'items') || board;
}

function caption(parent, node, label) {
  const c = autoLayout('VERTICAL', { name: label, itemSpacing: 8 });
  parent.appendChild(c);
  c.appendChild(node);
  c.appendChild(docText(label, 11));
  return c;
}

function holder(parent, name) {
  const h = autoLayout('VERTICAL', { name });
  parent.appendChild(h);
  return h;
}

function asSet(components, parent, name, description) {
  const set = figma.combineAsVariants(components, parent);
  set.name = name;
  set.layoutMode = 'HORIZONTAL';
  set.itemSpacing = 40;
  set.paddingTop = set.paddingBottom = set.paddingLeft = set.paddingRight = 28;
  set.primaryAxisSizingMode = 'AUTO';
  set.counterAxisSizingMode = 'AUTO';
  set.fills = [];
  set.strokes = [{ type: 'SOLID', color: rgb('#9747FF') }];
  set.dashPattern = [8, 4];
  set.cornerRadius = 8;
  set.description = description;
  return set;
}

// Highlighted words and links of a marker description, as the app renders its markdown
function richText(t, body, accents = [], links = []) {
  t.characters = body;
  t.setRangeFills(0, body.length, [solid('text/primary')]);
  t.setRangeTextDecoration(0, body.length, 'NONE');
  for (const a of accents) {
    const i = body.indexOf(a);
    if (i >= 0) t.setRangeFills(i, i + a.length, [solid('text/accent')]);
  }
  for (const l of links) {
    const i = body.indexOf(l);
    if (i >= 0) {
      t.setRangeFills(i, i + l.length, [solid('text/link')]);
      t.setRangeTextDecoration(i, i + l.length, 'UNDERLINE');
    }
  }
}

// ---------- Components ----------

async function fixSelectedRows() {
  for (const name of ['GI/Settings Row', 'GI/Radio Row']) {
    if (!K[name]) continue;
    for (const v of K[name].children) v.strokesIncludedInLayout = false;
  }
  log('✓ Selected rows are as tall as the default ones');
}

// The inner gold frame, the faded emblem and the glowing corners of a game panel
function panelChrome(panel, withWatermark) {
  const inset = figma.createRectangle();
  inset.name = 'inner-frame';
  inset.fills = [];
  inset.strokes = [solid('border/ornament', 0.55)];
  inset.strokeWeight = 1;
  inset.cornerRadius = 3;
  panel.appendChild(inset);
  inset.layoutPositioning = 'ABSOLUTE';
  inset.x = 6;
  inset.y = 6;
  inset.resize(panel.width - 12, panel.height - 12);
  inset.constraints = { horizontal: 'STRETCH', vertical: 'STRETCH' };
  if (withWatermark) {
    for (const [name, size, opacity] of [['sparkle', 150, 0.35], ['diamond', 96, 0.3]]) {
      const wm = icon(name, size, 'ornament/star', opacity);
      wm.name = 'watermark';
      panel.appendChild(wm);
      wm.layoutPositioning = 'ABSOLUTE';
      wm.x = (panel.width - size) / 2;
      wm.y = (panel.height - size) / 2;
      wm.constraints = { horizontal: 'CENTER', vertical: 'CENTER' };
    }
  }
  const corners = [['Top Left', 'MIN', 'MIN'], ['Top Right', 'MAX', 'MIN'], ['Bottom Left', 'MIN', 'MAX'], ['Bottom Right', 'MAX', 'MAX']];
  for (const [position, h, v] of corners) {
    const c = inst('GI/Ornament/Corner', { Position: position });
    c.name = `corner ${position.toLowerCase()}`;
    panel.appendChild(c);
    c.layoutPositioning = 'ABSOLUTE';
    c.x = h === 'MIN' ? -10 : panel.width - 14;
    c.y = v === 'MIN' ? -10 : panel.height - 14;
    c.constraints = { horizontal: h, vertical: v };
  }
}

async function panelBase(component, width) {
  component.layoutMode = 'VERTICAL';
  component.resize(width, 300);
  component.counterAxisSizingMode = 'FIXED';
  component.primaryAxisSizingMode = 'AUTO';
  component.fills = [solid('surface/panel')];
  component.strokes = [solid('border/subtle')];
  component.strokeWeight = 1.5;
  component.strokeAlign = 'INSIDE';
  radius(component, 'radius/xs');
  component.clipsContent = false;
  await effect(component, 'Shadow Panel');
}

function optionsContent(name, options, selected, height) {
  const c = figma.createComponent();
  c.name = `GI/Dialog Content/${name}`;
  c.layoutMode = 'VERTICAL';
  c.itemSpacing = 8;
  c.fills = [];
  c.resize(305, height || 100);
  c.counterAxisSizingMode = 'FIXED';
  c.primaryAxisSizingMode = height ? 'FIXED' : 'AUTO';
  c.clipsContent = !!height;
  for (const [label, description] of options) {
    const values = { Label: label };
    if (description) Object.assign(values, { Description: description, 'Show description': true });
    const r = inst('GI/Radio Row', { Selected: String(label === selected) }, values);
    c.appendChild(r);
    r.layoutSizingHorizontal = 'FILL';
  }
  return c;
}

async function buildDialog() {
  if (K['GI/Dialog']) {
    log('• GI/Dialog already exists');
    return;
  }
  const items = boardItems('GI · Panels & Overlays');
  const contents = autoLayout('HORIZONTAL', { name: 'Dialog Contents', itemSpacing: 24 });
  items.appendChild(contents);

  const placeholder = figma.createComponent();
  placeholder.name = 'GI/Dialog Content/Placeholder';
  placeholder.layoutMode = 'VERTICAL';
  placeholder.primaryAxisAlignItems = 'CENTER';
  placeholder.counterAxisAlignItems = 'CENTER';
  placeholder.resize(305, 120);
  placeholder.primaryAxisSizingMode = 'FIXED';
  placeholder.counterAxisSizingMode = 'FIXED';
  placeholder.fills = [];
  placeholder.strokes = [solid('border/subtle')];
  placeholder.dashPattern = [6, 4];
  placeholder.cornerRadius = 8;
  placeholder.appendChild(await text('Swap the Content property', 'Body M', 'text/secondary'));
  contents.appendChild(placeholder);

  const theme = optionsContent('Theme', [['System default'], ['Light'], ['Dark']], 'System default');
  const languages = [['System default', 'English'], ['Deutsch'], ['English'], ['Español'], ['Français'], ['Português'], ['Русский'], ['日本語'], ['简体中文'], ['한국어']];
  const language = optionsContent('Language', languages, 'System default', 420);
  contents.appendChild(theme);
  contents.appendChild(language);

  const dlg = figma.createComponent();
  dlg.name = 'GI/Dialog';
  await panelBase(dlg, 345);
  dlg.paddingTop = 26;
  dlg.paddingBottom = 22;
  dlg.paddingLeft = dlg.paddingRight = 20;
  dlg.itemSpacing = 14;
  dlg.counterAxisAlignItems = 'CENTER';
  const title = await text('Theme', 'Title L', 'text/primary', 'title');
  title.textAlignHorizontal = 'CENTER';
  dlg.appendChild(title);
  title.layoutSizingHorizontal = 'FILL';
  const top = inst('GI/Ornament/Divider');
  top.name = 'divider-top';
  dlg.appendChild(top);
  top.layoutSizingHorizontal = 'FILL';
  const slot = theme.createInstance();
  slot.name = 'content';
  dlg.appendChild(slot);
  slot.layoutSizingHorizontal = 'FILL';
  const bottom = inst('GI/Ornament/Divider');
  bottom.name = 'divider-bottom';
  dlg.appendChild(bottom);
  bottom.layoutSizingHorizontal = 'FILL';
  const actions = autoLayout('HORIZONTAL', { name: 'actions', itemSpacing: 12, primaryAxisAlignItems: 'CENTER' });
  dlg.appendChild(actions);
  actions.layoutSizingHorizontal = 'FILL';
  const cancel = inst('GI/Button', { Style: 'Dark', Size: 'Large', State: 'Default' }, { Label: 'Cancel' });
  cancel.name = 'cancel';
  const confirm = inst('GI/Button', { Style: 'Light', Size: 'Large', State: 'Default' }, { Label: 'Confirm' });
  confirm.name = 'confirm';
  actions.appendChild(cancel);
  actions.appendChild(confirm);
  cancel.layoutSizingHorizontal = 'FILL';
  confirm.layoutSizingHorizontal = 'FILL';
  panelChrome(dlg, true);

  const pTitle = dlg.addComponentProperty('Title', 'TEXT', 'Theme');
  title.componentPropertyReferences = { characters: pTitle };
  const preferred = [theme, language, placeholder].map(c => ({ type: 'COMPONENT', key: c.key }));
  let pContent;
  try {
    pContent = dlg.addComponentProperty('Content', 'INSTANCE_SWAP', theme.id, { preferredValues: preferred });
  } catch (e) {
    pContent = dlg.addComponentProperty('Content', 'INSTANCE_SWAP', theme.id);
  }
  slot.componentPropertyReferences = { mainComponent: pContent };
  const pCancel = dlg.addComponentProperty('Show cancel', 'BOOLEAN', true);
  cancel.componentPropertyReferences = { visible: pCancel };
  const pConfirm = dlg.addComponentProperty('Show confirm', 'BOOLEAN', false);
  confirm.componentPropertyReferences = { visible: pConfirm };
  dlg.description = 'Game dialog: cream panel with glowing gold corners, gold rules and pill actions. Put the body in through the Content property.';
  caption(items, dlg, 'Dialog');
  log('✓ GI/Dialog with the Theme and Language contents');
}

const CARD_TEXT = 'Requirements: complete the Domain puzzle first.\n• Light the three torches near the gate\n• Take the stairs down to the lower floor';

async function buildLocationCard() {
  if (K['GI/Location Card']) {
    log('• GI/Location Card already exists');
    return;
  }
  const items = boardItems('GI · Panels & Overlays');
  const cards = [];
  for (const found of ['false', 'true']) {
    const c = figma.createComponent();
    c.name = `Found=${found}`;
    await panelBase(c, 361);
    c.paddingTop = 18;
    c.paddingBottom = 16;
    c.paddingLeft = c.paddingRight = 18;
    c.itemSpacing = 10;
    const title = await text('Precious Chest', 'Title M', 'text/primary', 'title');
    c.appendChild(title);
    title.layoutSizingHorizontal = 'FILL';
    title.textAutoResize = 'HEIGHT';
    const divider = inst('GI/Ornament/Divider');
    c.appendChild(divider);
    divider.layoutSizingHorizontal = 'FILL';
    const description = await text(CARD_TEXT, 'Body M', 'text/primary', 'description');
    c.appendChild(description);
    description.layoutSizingHorizontal = 'FILL';
    description.textAutoResize = 'HEIGHT';
    description.paragraphSpacing = 6;
    richText(description, CARD_TEXT, ['Requirements:'], ['Domain puzzle']);

    const floors = autoLayout('HORIZONTAL', { name: 'floors', itemSpacing: 8, counterAxisAlignItems: 'CENTER' });
    c.appendChild(floors);
    floors.layoutSizingHorizontal = 'FILL';
    floors.appendChild(inst('GI/Chip', { Selected: 'true' }, { Label: 'B1', 'Show marker': true }));
    floors.appendChild(inst('GI/Chip', { Selected: 'false' }, { Label: 'B2', 'Show marker': false }));
    const spacer = frame('spacer', 1, 1);
    floors.appendChild(spacer);
    spacer.layoutGrow = 1;
    floors.appendChild(inst('GI/Text Button', { Glyph: 'Cancel' }, { Label: 'Exit' }));

    const footer = autoLayout('HORIZONTAL', { name: 'footer', primaryAxisAlignItems: 'MAX' });
    c.appendChild(footer);
    footer.layoutSizingHorizontal = 'FILL';
    const isFound = found === 'true';
    const button = inst('GI/Button', { Style: isFound ? 'Dark' : 'Light', Size: 'Small', State: 'Default' }, { Label: 'Found' });
    button.name = 'found';
    if (isFound) button.findOne(n => n.name === 'glyph').swapComponent(variant(K['GI/Button Glyph'], { Type: 'Check' }));
    footer.appendChild(button);
    panelChrome(c, false);
    cards.push(c);
  }
  const set = asSet(cards, holder(items, 'Location Card'), 'GI/Location Card',
    'Popup of a selected marker: title, description with highlighted words and links, dungeon floors, and the Found button (dark with a check once found).');
  const pTitle = set.addComponentProperty('Title', 'TEXT', 'Precious Chest');
  const pFloors = set.addComponentProperty('Show floors', 'BOOLEAN', true);
  for (const c of set.children) {
    c.findOne(n => n.name === 'title').componentPropertyReferences = { characters: pTitle };
    c.findOne(n => n.name === 'floors').componentPropertyReferences = { visible: pFloors };
  }
  log('✓ GI/Location Card');
}

// ---------- Foundations sheet ----------

async function aliasName(v) {
  const value = v.valuesByMode[Object.keys(v.valuesByMode)[0]];
  if (value && value.type === 'VARIABLE_ALIAS') {
    const target = await figma.variables.getVariableByIdAsync(value.id);
    return target ? target.name : '?';
  }
  return `${toHex(value)} · ${Math.round((value.a === undefined ? 1 : value.a) * 100)}%`;
}

async function hexOf(v) {
  let value = v.valuesByMode[Object.keys(v.valuesByMode)[0]];
  while (value && value.type === 'VARIABLE_ALIAS') {
    const target = await figma.variables.getVariableByIdAsync(value.id);
    value = target.valuesByMode[Object.keys(target.valuesByMode)[0]];
  }
  return toHex(value);
}

// The dark tokens are shown on a dark plate, as they are meant to be seen
async function tokenGrid(parent, title, vars, dark) {
  const pad = dark ? 28 : 0;
  const block = column(title, 1192, { itemSpacing: 20, paddingTop: pad, paddingBottom: pad, paddingLeft: pad, paddingRight: pad });
  if (dark) {
    block.fills = [{ type: 'SOLID', color: rgb('#262B38') }];
    block.cornerRadius = 24;
  }
  parent.appendChild(block);
  block.appendChild(dark ? docText(title, 22, '#ECE5D8', 'Semi Bold') : await text(title, 'Title L', 'text/primary'));
  const grid = row('tokens', 1192 - 2 * pad, { itemSpacing: 16 });
  grid.layoutWrap = 'WRAP';
  grid.counterAxisSpacing = 16;
  block.appendChild(grid);
  const cardWidth = dark ? 272 : 286;
  for (const [name, v] of Object.entries(vars)) {
    const token = row(name, cardWidth, { itemSpacing: 12, counterAxisAlignItems: 'CENTER', paddingTop: 8, paddingBottom: 8, paddingLeft: 8, paddingRight: 8 });
    token.fills = [{ type: 'SOLID', color: rgb(dark ? '#313747' : '#F6F7F9') }];
    token.cornerRadius = 12;
    const chip = figma.createRectangle();
    chip.resize(40, 40);
    chip.cornerRadius = 8;
    chip.fills = [figma.variables.setBoundVariableForPaint({ type: 'SOLID', color: { r: 0, g: 0, b: 0 } }, 'color', v)];
    chip.strokes = [{ type: 'SOLID', color: rgb(dark ? '#FFFFFF' : '#000000'), opacity: dark ? 0.12 : 0.08 }];
    chip.strokeWeight = 1;
    token.appendChild(chip);
    const labels = autoLayout('VERTICAL', { name: 'labels', itemSpacing: 2 });
    labels.appendChild(docText(name, 12, dark ? '#ECE5D8' : '#3B4255', 'Semi Bold'));
    labels.appendChild(docText(`→ ${await aliasName(v)}`, 11, dark ? '#A7AEBC' : '#7E8698'));
    token.appendChild(labels);
    grid.appendChild(token);
  }
}

async function buildFoundations() {
  if (genshinPage.children.some(n => n.type === 'SECTION' && n.name === 'Foundations')) {
    log('• Foundations already exist');
    return;
  }
  const section = figma.createSection();
  section.name = 'Foundations';
  section.x = 0;
  section.y = 0;

  const board = column('Foundations', 1320, { itemSpacing: 56, paddingTop: 64, paddingBottom: 72, paddingLeft: 64, paddingRight: 64 });
  board.fills = [{ type: 'SOLID', color: rgb('#FFFFFF') }];
  board.cornerRadius = 32;
  place(section, board, 80, 80);

  const header = column('Header', 1192, { itemSpacing: 12 });
  board.appendChild(header);
  header.appendChild(await text('Genshin Impact · Foundations', 'Display', 'text/primary'));
  const intro = docText('Colors taken from the game UI: cream panels, slate text, gold ornaments, the yellow ring of Confirm and the blue cross of Cancel. Components are on the Components page, in the "Genshin Impact · Design System" section.', 14);
  header.appendChild(intro);
  intro.layoutSizingHorizontal = 'FILL';
  intro.textAutoResize = 'HEIGHT';

  // Palette
  const palette = column('Palette', 1192, { itemSpacing: 20 });
  board.appendChild(palette);
  palette.appendChild(await text('Palette', 'Title L', 'text/primary'));
  const families = {};
  for (const name of Object.keys(P)) {
    const family = name.includes('/') ? name.split('/')[0] : 'base';
    (families[family] = families[family] || []).push(name);
  }
  for (const [family, names] of Object.entries(families)) {
    const line = autoLayout('HORIZONTAL', { name: family, itemSpacing: 16 });
    palette.appendChild(line);
    for (const name of names) {
      const swatch = column(name, 112, { itemSpacing: 6 });
      const chip = figma.createRectangle();
      chip.name = 'swatch';
      chip.resize(112, 72);
      chip.cornerRadius = 12;
      chip.fills = [solid(name)];
      chip.strokes = [{ type: 'SOLID', color: rgb('#000000'), opacity: 0.08 }];
      chip.strokeWeight = 1;
      swatch.appendChild(chip);
      swatch.appendChild(docText(name, 12, '#3B4255', 'Semi Bold'));
      swatch.appendChild(docText(await hexOf(P[name]), 11));
      line.appendChild(swatch);
    }
  }

  // Semantic colors, light and dark
  await tokenGrid(board, 'Semantic colors', C, false);
  if (Object.keys(CD).length) await tokenGrid(board, 'Semantic colors · Dark', CD, true);

  // Typography
  const type = column('Typography', 1192, { itemSpacing: 18 });
  board.appendChild(type);
  type.appendChild(await text('Typography', 'Title L', 'text/primary'));
  const font = TS['Body M'].fontName;
  const fontNote = docText(`One face for everything, as in the game: HYWenHei (zhcn.ttf). Now rendered with ${font.family} ${font.style} as a stand-in.`, 13);
  type.appendChild(fontNote);
  for (const [name, style] of Object.entries(TS)) {
    const line = row(name, 1192, { itemSpacing: 24, counterAxisAlignItems: 'CENTER' });
    const spec = column('spec', 180, { itemSpacing: 2 });
    spec.appendChild(docText(`GI/${name}`, 12, '#3B4255', 'Semi Bold'));
    spec.appendChild(docText(`${style.fontSize} / ${style.lineHeight.value}`, 11));
    line.appendChild(spec);
    const sample = await text('Teyvat Interactive Map · Карта Тейвата · 提瓦特地图', name, 'text/primary', 'sample');
    line.appendChild(sample);
    type.appendChild(line);
  }

  // Effects
  const effects = column('Effects', 1192, { itemSpacing: 20 });
  board.appendChild(effects);
  effects.appendChild(await text('Effects', 'Title L', 'text/primary'));
  const effectRow = autoLayout('HORIZONTAL', { name: 'samples', itemSpacing: 32, paddingTop: 12, paddingBottom: 12, paddingLeft: 12, paddingRight: 12 });
  effects.appendChild(effectRow);
  for (const name of Object.keys(ES)) {
    const sample = column(name, 200, { itemSpacing: 12 });
    const card = frame('sample', 200, 88);
    card.fills = [solid('surface/panel')];
    card.cornerRadius = 12;
    await effect(card, name);
    sample.appendChild(card);
    sample.appendChild(docText(`GI/${name}`, 12, '#3B4255', 'Semi Bold'));
    effectRow.appendChild(sample);
  }

  // Notes
  const notes = column('Notes', 1192, { itemSpacing: 8 });
  board.appendChild(notes);
  notes.appendChild(await text('Notes', 'Title L', 'text/primary'));
  for (const line of [
    'Font: every GI text style takes its family and style from the font/family and font/style variables (GI / Size). Install zhcn.ttf, restart Figma and run Plugins → Development → Genshin DS Builder → Use installed HYWenHei font to switch everything at once.',
    'Dark theme: the Starter plan allows one mode per variable collection, so the dark tokens are a collection of their own, GI / Color · Dark, with the same names as GI / Color. The dark screens are the same components with their colors rebound to it. On a paid plan it becomes the Dark mode of GI / Color.',
  ]) {
    const t = docText(`• ${line}`, 13, '#4A5366');
    notes.appendChild(t);
    t.layoutSizingHorizontal = 'FILL';
    t.textAutoResize = 'HEIGHT';
  }

  section.resizeWithoutConstraints(board.width + 160, board.height + 160);
  log('✓ Foundations: palette, semantic colors, typography, effects');
}

// ---------- Screens ----------

function statusBar(light) {
  const color = light ? '#FFFFFF' : '#1C202A';
  const f = frame('Status Bar', W, STATUS_BAR_H);
  const time = docText('9:41', 17, color, 'Semi Bold');
  place(f, time, 52, 17);
  const icons = figma.createNodeFromSvg(
    `<svg width="78" height="14" viewBox="0 0 78 14" xmlns="http://www.w3.org/2000/svg"><g fill="${color}">` +
    '<rect x="0" y="9" width="3" height="4" rx="1"/><rect x="5" y="6.5" width="3" height="6.5" rx="1"/>' +
    '<rect x="10" y="4" width="3" height="9" rx="1"/><rect x="15" y="1.5" width="3" height="11.5" rx="1"/>' +
    '<path d="M31 3.2c2.3 0 4.4.9 6 2.4l1.1-1.1A10 10 0 0 0 31 1.6a10 10 0 0 0-7.1 2.9L25 5.6a8.4 8.4 0 0 1 6-2.4Zm0 3.3c1.4 0 2.6.5 3.6 1.4l1.1-1.1a6.7 6.7 0 0 0-9.4 0l1.1 1.1c1-.9 2.2-1.4 3.6-1.4Zm0 3.3c.5 0 1 .2 1.3.5L31 11.6l-1.3-1.3c.3-.3.8-.5 1.3-.5Z"/>' +
    `<rect x="50.5" y="1" width="23" height="12" rx="3.5" fill="none" stroke="${color}" stroke-opacity=".4"/>` +
    '<rect x="52.5" y="3" width="19" height="8" rx="2"/><path d="M75 5v4c.8-.3 1.5-1.1 1.5-2S75.8 5.3 75 5Z" fill-opacity=".4"/></g></svg>'
  );
  icons.name = 'Indicators';
  icons.fills = [];
  place(f, icons, W - 24 - 78, 21);
  return f;
}

function homeIndicator(light) {
  const bar = figma.createRectangle();
  bar.name = 'Home Indicator';
  bar.resize(134, 5);
  bar.cornerRadius = 3;
  bar.fills = [{ type: 'SOLID', color: rgb(light ? '#FFFFFF' : '#1C202A') }];
  bar.x = (W - 134) / 2;
  bar.y = H - 8 - 5;
  return bar;
}

function scrim(parent) {
  const r = figma.createRectangle();
  r.name = 'Scrim';
  r.resize(W, H);
  r.fills = [solid('bg/scrim')];
  return place(parent, r, 0, 0);
}

// Around Mondstadt and Starfell Lake, in the coordinates of the screen
const MARKERS = [
  ['Teleport Waypoint', 148, 286], ['Teleport Waypoint', 262, 566], ['Teleport Waypoint', 82, 640], ['Teleport Waypoint', 300, 168],
  ['Statue of The Seven', 205, 452],
  ['Anemoculus', 118, 206], ['Anemoculus', 338, 262], ['Anemoculus', 226, 668], ['Anemoculus', 52, 420], ['Anemoculus', 176, 372],
  ['Common Chest', 244, 330], ['Common Chest', 168, 528], ['Common Chest', 306, 492], ['Common Chest', 96, 520],
  ['Precious Chest', 276, 612], ['Seelie', 128, 456], ['Valberry', 290, 404], ['Cecilia', 60, 330],
];
const CLUSTERS = [['12', 108, 132], ['7', 246, 214]];

function mapBase(name, options = {}) {
  const markers = options.markers || MARKERS;
  const clusters = options.clusters || CLUSTERS;
  const f = frame(name, W, H);
  f.clipsContent = true;
  f.fills = [solid('slate/900')];
  place(f, inst('GI/Map Background/Mondstadt'), 0, 0);
  if (options.dim) {
    const dim = figma.createRectangle();
    dim.name = 'Dungeon dim';
    dim.resize(W, H);
    dim.fills = [solid('slate/900', options.dim)];
    place(f, dim, 0, 0);
  }
  const layer = frame('Markers', W, H);
  place(f, layer, 0, 0);
  markers.forEach(([marker, x, y], index) => {
    if (index === options.selected) {
      const halo = inst('GI/Selection Halo');
      place(layer, halo, x - 28, y - 28);
    }
    const m = inst(`GI/Marker Icon/${marker}`);
    m.resize(30, 30);
    place(layer, m, x - 15, y - 15);
  });
  for (const [count, x, y] of clusters) place(layer, inst('GI/Cluster Pin', {}, { Count: count }), x - 20, y - 20);
  const shade = figma.createRectangle();
  shade.name = 'Top shade';
  shade.resize(W, 120);
  shade.fills = [{
    type: 'GRADIENT_LINEAR',
    gradientTransform: [[0, 1, 0], [-1, 0, 1]],
    gradientStops: [
      { position: 0, color: { r: 0.11, g: 0.125, b: 0.165, a: 0.55 } },
      { position: 1, color: { r: 0.11, g: 0.125, b: 0.165, a: 0 } },
    ],
  }];
  place(f, shade, 0, 0);
  place(f, statusBar(true), 0, 0);
  f.appendChild(homeIndicator(true));
  return f;
}

// The search bar at the bottom, the side buttons above its right end, the zoom in the middle (MapControlsLayout)
const BAR_Y = H - HOME_INSET - 16 - 48;

function mapControls(f, options = {}) {
  const right = W - 16 - 48;
  const zoom = inst('GI/Zoom Control', { Limit: 'None' });
  zoom.name = 'Zoom';
  place(f, zoom, right, Math.round((H - 97) / 2));
  const layers = inst('GI/Icon Button', { Size: '48', State: 'Default' }, { Icon: K['GI/Icon/layers'].id });
  layers.name = 'Layers';
  place(f, layers, right, BAR_Y - 2 * (48 + 8));
  const markers = inst('GI/Icon Button', { Size: '48', State: 'Default' }, { Icon: K['GI/Icon/list'].id });
  markers.name = 'Markers';
  place(f, markers, right, BAR_Y - 48 - 8);

  const bar = row('Search Bar', W - 32, { itemSpacing: 8, counterAxisAlignItems: 'CENTER' });
  place(f, bar, 16, BAR_Y);
  const selector = inst('GI/Icon Button', { Size: '48', State: options.selectorState || 'Default' }, { Icon: K['GI/Map Icon/Teyvat Map'].id });
  selector.name = 'Map selector';
  bar.appendChild(selector);
  const field = inst('GI/Search Field', { Context: 'Map', State: options.fieldState || 'Idle' }, { Text: options.fieldText || 'Search in Teyvat Map' });
  bar.appendChild(field);
  field.layoutSizingHorizontal = 'FILL';
  const settings = inst('GI/Icon Button', { Size: '48', State: 'Default' }, { Icon: K['GI/Icon/settings'].id });
  settings.name = 'Settings';
  bar.appendChild(settings);
  return bar;
}

async function mapDefault() {
  const f = mapBase('Map');
  mapControls(f);
  return f;
}

async function mapSearch() {
  const f = mapBase('Map · Search applied', { markers: MARKERS.filter(m => m[0] === 'Anemoculus'), clusters: [] });
  mapControls(f, { fieldState: 'Filled', fieldText: 'Anemoculus' });
  return f;
}

const MAPS = ['Teyvat Map', 'Enkanomiya', 'The Chasm', 'Sea of Bygone Eras', 'Ancient Sacred Mountain', 'Temple of Space', 'Frost Moon'];

async function mapMenu() {
  const f = mapBase('Map · Maps menu');
  mapControls(f, { selectorState: 'Active' });
  const menu = autoLayout('VERTICAL', { name: 'Maps Menu', itemSpacing: 2, paddingTop: 6, paddingBottom: 6, paddingLeft: 6, paddingRight: 6 });
  menu.fills = [solid('surface/menu')];
  menu.strokes = [solid('border/ornament', 0.6)];
  menu.strokeWeight = 1;
  radius(menu, 'radius/md');
  await effect(menu, 'Shadow Panel');
  for (const name of MAPS) {
    menu.appendChild(inst('GI/Menu Item', { Selected: String(name === 'Teyvat Map') }, { Name: name, Icon: K[`GI/Map Icon/${name}`].id }));
  }
  place(f, menu, 16, BAR_Y - 8 - menu.height);
  return f;
}

function locationCard(f, variantProps, values) {
  const card = inst('GI/Location Card', variantProps, values);
  card.name = 'Location';
  place(f, card, 16, H - HOME_INSET - 16 - card.height);
  return card;
}

async function mapMarker() {
  const f = mapBase('Map · Marker', { selected: 10 });
  const card = locationCard(f, { Found: 'false' }, { Title: 'Common Chest', 'Show floors': false });
  const body = 'Requirements: defeat the hilichurls guarding the camp.\n• The chest is behind the wooden tent\n• More chests around Starfell Lake';
  richText(card.findOne(n => n.name === 'description'), body, ['Requirements:'], ['Starfell Lake']);
  card.y = H - HOME_INSET - 16 - card.height;
  return f;
}

async function mapDungeon() {
  const markers = [['Precious Chest', 196, 352], ['Common Chest', 136, 286], ['Exquisite Chest', 252, 430], ['Teleport Waypoint', 300, 300]];
  const f = mapBase('Map · Dungeon marker', { markers, clusters: [], selected: 0, dim: 0.45 });
  locationCard(f, { Found: 'true' }, { Title: 'Precious Chest', 'Show floors': true });
  return f;
}

function categoryRows(parent, list) {
  for (let i = 0; i < list.length; i += 2) {
    const r = row('Categories', W, { itemSpacing: 9, paddingLeft: 16, paddingRight: 16 });
    parent.appendChild(r);
    r.layoutSizingHorizontal = 'FILL';
    for (const [name, count, hidden] of list.slice(i, i + 2)) {
      const c = inst('GI/Category Item', { Hidden: String(!!hidden) }, { Name: name, Count: count, Icon: K[`GI/Marker Icon/${name}`].id });
      r.appendChild(c);
      c.layoutSizingHorizontal = 'FILL';
    }
  }
}

function groupHeader(parent, title, expanded) {
  const g = inst('GI/Group Header', { Expanded: String(expanded) }, { Title: title });
  // The groups of the catalog have no descriptions
  const description = g.findOne(n => n.name === 'description');
  if (description) description.visible = false;
  parent.appendChild(g);
  g.layoutSizingHorizontal = 'FILL';
}

async function mapList() {
  const f = mapBase('Map · Marker list');
  mapControls(f);
  scrim(f);
  const sheet = autoLayout('VERTICAL', { name: 'Marker List', itemSpacing: 10, paddingBottom: HOME_INSET });
  sheet.resize(W, H - 88);
  sheet.primaryAxisSizingMode = 'FIXED';
  sheet.counterAxisSizingMode = 'FIXED';
  sheet.clipsContent = true;
  sheet.fills = [solid('surface/panel')];
  sheet.topLeftRadius = sheet.topRightRadius = 20;
  sheet.strokes = [solid('border/ornament', 0.8)];
  sheet.strokeTopWeight = 1.5;
  sheet.strokeBottomWeight = 0;
  sheet.strokeLeftWeight = 0;
  sheet.strokeRightWeight = 0;
  await effect(sheet, 'Shadow Panel');
  place(f, sheet, 0, 88);

  const handle = inst('GI/Sheet Handle');
  sheet.appendChild(handle);
  handle.layoutSizingHorizontal = 'FILL';
  const search = row('Search', W, { paddingLeft: 16, paddingRight: 16 });
  sheet.appendChild(search);
  search.layoutSizingHorizontal = 'FILL';
  const field = inst('GI/Search Field', { Context: 'Panel', State: 'Idle' }, { Text: 'Search markers' });
  search.appendChild(field);
  field.layoutSizingHorizontal = 'FILL';
  const actions = row('Visibility', W, { itemSpacing: 16, primaryAxisAlignItems: 'CENTER' });
  sheet.appendChild(actions);
  actions.layoutSizingHorizontal = 'FILL';
  actions.appendChild(inst('GI/Text Button', { Glyph: 'Confirm' }, { Label: 'Show all' }));
  actions.appendChild(inst('GI/Text Button', { Glyph: 'Cancel' }, { Label: 'Hide all' }));

  const list = column('Groups', W, { itemSpacing: 8 });
  sheet.appendChild(list);
  list.layoutSizingHorizontal = 'FILL';
  groupHeader(list, 'Featured', true);
  categoryRows(list, [
    ['Teleport Waypoint', '312'], ['Statue of The Seven', '34', true], ['Anemoculus', '65'],
    ['Geoculus', '131'], ['Electroculus', '181'], ['Dendroculus', '271'],
  ]);
  groupHeader(list, 'Chest', true);
  // A category with respawn cycles, in a bordered row with its cycle tabs
  const cycles = row('Cycles', W, { paddingLeft: 16, paddingRight: 16 });
  list.appendChild(cycles);
  cycles.layoutSizingHorizontal = 'FILL';
  const box = autoLayout('HORIZONTAL', { name: 'Common Chest', counterAxisAlignItems: 'CENTER', paddingRight: 6 });
  box.fills = [solid('surface/row')];
  box.strokes = [solid('border/subtle')];
  box.strokeWeight = 1;
  box.strokeAlign = 'INSIDE';
  radius(box, 'radius/sm');
  cycles.appendChild(box);
  box.layoutSizingHorizontal = 'FILL';
  const common = inst('GI/Category Item', { Hidden: 'false' }, { Name: 'Common Chest', Count: '1210', Icon: K['GI/Marker Icon/Common Chest'].id });
  common.fills = [];
  box.appendChild(common);
  common.layoutSizingHorizontal = 'FILL';
  for (const n of ['1', '2', '3']) box.appendChild(inst('GI/Cycle Tab', { Selected: String(n === '1') }, { Label: n }));
  categoryRows(list, [['Exquisite Chest', '703'], ['Precious Chest', '215'], ['Luxurious Chest', '68'], ['Shrine of Depths', '40']]);
  groupHeader(list, 'Puzzle Chest', false);
  groupHeader(list, 'Mineral', false);
  groupHeader(list, 'Local Specialties', false);
  return f;
}

async function settingsScreen(name, options = {}) {
  const f = frame(name, W, H);
  f.clipsContent = true;
  f.fills = [solid('bg/screen')];
  place(f, statusBar(!!options.dark), 0, 0);
  const top = inst('GI/Top Bar', {}, { Title: 'Settings' });
  place(f, top, 0, STATUS_BAR_H);

  const content = column('Content', W, { itemSpacing: 6, paddingLeft: 16, paddingRight: 16 });
  place(f, content, 0, STATUS_BAR_H + top.height);
  const section = title => {
    const h = inst('GI/Section Header', {}, { Title: title });
    content.appendChild(h);
    h.layoutSizingHorizontal = 'FILL';
    try { h.paddingTop = 12; } catch (e) { /* keeps the component's padding */ }
  };
  const setting = (type, values, extra = {}) => {
    const r = inst('GI/Settings Row', { Type: type, State: extra.selected ? 'Selected' : 'Default' }, values);
    content.appendChild(r);
    r.layoutSizingHorizontal = 'FILL';
    if (extra.off) r.findOne(n => n.name === 'toggle').setProperties({ State: 'Off' });
    if (extra.noValue) r.findOne(n => n.name === 'value').visible = false;
    return r;
  };
  section('Map');
  setting('Switch', { Title: 'Hide markers while moving', Summary: 'The map moves more smoothly' });
  setting('Switch', { Title: 'Group markers', Summary: 'Nearby markers merge into one when you zoom out' }, { off: true });
  section('Appearance');
  setting('Navigation', { Title: 'Theme', 'Show summary': false, Value: 'System default' }, { selected: options.pressed === 'theme' });
  section('Language');
  setting('Navigation', { Title: 'App language', 'Show summary': false, Value: 'System default (English)' }, { selected: options.pressed === 'language' });
  section('Privacy');
  const hasLink = K['GI/Settings Row'].children.some(v => parseVariant(v.name).Type === 'Link');
  if (hasLink) setting('Link', { Title: 'Privacy policy', 'Show summary': false });
  else setting('Navigation', { Title: 'Privacy policy', 'Show summary': false }, { noValue: true });

  const footer = column('Footer', W, { itemSpacing: 6, counterAxisAlignItems: 'CENTER' });
  const signature = autoLayout('HORIZONTAL', { name: 'Signature', itemSpacing: 5, counterAxisAlignItems: 'CENTER' });
  signature.appendChild(await text('Developed with', 'Body M', 'text/secondary'));
  const heart = figma.createNodeFromSvg('<svg width="14" height="14" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path d="M12 21.3l-1.4-1.3C5.4 15.4 2 12.3 2 8.5 2 5.4 4.4 3 7.5 3c1.7 0 3.4.8 4.5 2.1C13.1 3.8 14.8 3 16.5 3 19.6 3 22 5.4 22 8.5c0 3.8-3.4 6.9-8.6 11.5L12 21.3z" fill="#E5614B"/></svg>');
  heart.name = 'heart';
  heart.fills = [];
  tint(heart, 'red/400');
  signature.appendChild(heart);
  signature.appendChild(await text('by', 'Body M', 'text/secondary'));
  signature.appendChild(await text('@onthecrow', 'Body M', 'text/accent'));
  footer.appendChild(signature);
  const links = autoLayout('HORIZONTAL', { name: 'Links', itemSpacing: 14 });
  for (const network of ['telegram', 'github', 'instagram']) {
    const b = inst('GI/Icon Button', { Size: '40', State: 'Default' }, { Icon: K[`GI/Icon/${network}`].id });
    b.name = network;
    links.appendChild(b);
  }
  footer.appendChild(links);
  footer.appendChild(await text('Feel free to reach me if you have any questions', 'Body S', 'text/secondary'));
  footer.appendChild(await text(`Version ${APP_VERSION}`, 'Body S', 'text/secondary'));
  place(f, footer, 0, H - HOME_INSET - footer.height);
  f.appendChild(homeIndicator(!!options.dark));
  return f;
}

async function settings() {
  return settingsScreen('Settings');
}

function dialog(f, title, content) {
  scrim(f);
  const d = inst('GI/Dialog', {}, { Title: title, Content: K[`GI/Dialog Content/${content}`].id, 'Show cancel': true, 'Show confirm': false });
  place(f, d, (W - d.width) / 2, Math.round((H - d.height) / 2));
  return d;
}

async function settingsTheme() {
  const f = await settingsScreen('Settings · Theme', { pressed: 'theme' });
  dialog(f, 'Theme', 'Theme');
  return f;
}

async function settingsLanguage() {
  const f = await settingsScreen('Settings · Language', { pressed: 'language' });
  dialog(f, 'App language', 'Language');
  return f;
}

async function buildScreens() {
  if (genshinPage.children.some(n => n.type === 'SECTION' && n.name === 'Screens')) {
    log('• Screens already exist');
    return;
  }
  const foundations = genshinPage.children.find(n => n.type === 'SECTION' && n.name === 'Foundations');
  const section = figma.createSection();
  section.name = 'Screens';
  section.x = foundations ? foundations.x + foundations.width + 200 : 0;
  section.y = 0;
  const rows = [
    [mapDefault, mapSearch, mapMenu, mapMarker, mapDungeon, mapList],
    [settings, settingsTheme, settingsLanguage],
  ];
  if (Object.keys(CD).length) rows.push([mapDark, mapListDark, settingsDark, settingsThemeDark]);
  const total = rows.reduce((sum, r) => sum + r.length, 0);
  const gap = 100;
  let built = 0;
  for (let r = 0; r < rows.length; r++) {
    for (let i = 0; i < rows[r].length; i++) {
      const x = 80 + i * (W + gap);
      const y = 140 + r * (H + 200);
      try {
        const screen = await rows[r][i]();
        place(section, screen, x, y);
        place(section, docText(screen.name, 22, '#3B4255', 'Semi Bold'), x, y - 48);
        built++;
      } catch (e) {
        log(`✗ Screen ${rows[r][i].name}: ${e.message}`);
      }
    }
  }
  section.resizeWithoutConstraints(80 + 6 * (W + gap), 140 + rows.length * (H + 200));
  log(`✓ Screens: ${built} of ${total}`);
  // In the free space to the right of the settings screens
  await step('Layout rules', () => buildRules(section, 80 + 3 * (W + gap), 140 + (H + 200) - 48));
}

function removeOwnSections() {
  for (const n of [...genshinPage.children]) {
    if (n.type === 'SECTION' && (n.name === 'Foundations' || n.name === 'Screens')) n.remove();
  }
  log('• Removed the old Foundations and Screens sections');
}

// ---------- Review fixes, round 1 ----------

const LINE = 'stroke="#000" stroke-width="3" stroke-linecap="round" stroke-linejoin="round" fill="none"';
const svg24 = body => `<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24">${body}</svg>`;
const LINE_ICON_NOTE = 'Line icon: 3px rounded strokes, as the cross of Exit and of the switch.';

// Every line icon gets the 3px rounded stroke of the cross; a stroke keeps its weight whatever the size of the instance
const ICON_UPDATES = {
  'check': svg24(`<path d="M5.5 12.5L10 17L18.5 7.5" ${LINE}/>`),
  'chevron-right': svg24(`<path d="M9.5 5.5L16 12L9.5 18.5" ${LINE}/>`),
  'chevron-left': svg24(`<path d="M14.5 5.5L8 12L14.5 18.5" ${LINE}/>`),
  'expand-more': svg24(`<path d="M6 9.5L12 15.5L18 9.5" ${LINE}/>`),
  'add': svg24(`<path d="M12 5.5V18.5M5.5 12H18.5" ${LINE}/>`),
  'remove': svg24(`<path d="M5.5 12H18.5" ${LINE}/>`),
  'arrow-back': svg24(`<path d="M19 12H5.5M11 6L5 12L11 18" ${LINE}/>`),
  'search': svg24(`<circle cx="10.5" cy="10.5" r="6" ${LINE}/><path d="M15.2 15.2L19.5 19.5" ${LINE}/>`),
  'list': svg24(`<circle cx="5" cy="6.5" r="1.9" fill="#000"/><circle cx="5" cy="12" r="1.9" fill="#000"/><circle cx="5" cy="17.5" r="1.9" fill="#000"/><path d="M10 6.5H19.5M10 12H19.5M10 17.5H19.5" ${LINE}/>`),
  'filter': svg24(`<path d="M4.5 7H19.5M7.5 12H16.5M10.5 17H13.5" ${LINE}/>`),
  'layers': svg24(`<path d="M12 4.5L19.5 9.2L12 13.9L4.5 9.2Z" fill="#000" stroke="#000" stroke-width="2" stroke-linejoin="round"/><path d="M4.5 14.2L12 18.9L19.5 14.2" ${LINE}/>`),
  'clear': svg24('<circle cx="12" cy="12" r="9.5" fill="#000"/><path d="M9 9L15 15M15 9L9 15" stroke="#FFF" stroke-width="3" stroke-linecap="round" fill="none"/>'),
};
// The game's cross: four thick arms ending in arrowheads, the round join softens the tips
const CLOSE_ICON = svg24('<path d="M6.13 6.13L10.23 6.56L9.21 7.58L16.42 14.79L17.44 13.77L17.87 17.87L13.77 17.44L14.79 16.42L7.58 9.21L6.56 10.23ZM6.13 17.87L6.56 13.77L7.58 14.79L14.79 7.58L13.77 6.56L17.87 6.13L17.44 10.23L16.42 9.21L9.21 16.42L10.23 17.44Z" fill="#000" stroke="#000" stroke-width="1.1" stroke-linejoin="round"/>');
const CLOSE_NOTE = 'The game\'s cross: four thick arms ending in arrowheads, for closing screens.';

function readTint(node) {
  for (const n of node.findAll(n => SHAPES.includes(n.type) && n.name !== 'cut')) {
    const paints = [].concat(Array.isArray(n.fills) ? n.fills : [], Array.isArray(n.strokes) ? n.strokes : []);
    for (const p of paints) {
      if (p.boundVariables && p.boundVariables.color) return { id: p.boundVariables.color.id, opacity: p.opacity === undefined ? 1 : p.opacity };
    }
  }
  return null;
}

// The stroke-only shape of a two-tone icon is its cut, in the color of the field under it
function fillGlyph(component, svg, twoTone) {
  const g = figma.createNodeFromSvg(svg);
  g.name = 'glyph';
  g.fills = [];
  component.appendChild(g);
  g.resize(24, 24);
  g.x = 0;
  g.y = 0;
  g.constraints = { horizontal: 'SCALE', vertical: 'SCALE' };
  for (const n of g.findAll(n => SHAPES.includes(n.type))) {
    n.constraints = { horizontal: 'SCALE', vertical: 'SCALE' };
    const hasFills = Array.isArray(n.fills) && n.fills.length > 0;
    const hasStrokes = Array.isArray(n.strokes) && n.strokes.length > 0;
    if (twoTone && hasStrokes && !hasFills) {
      n.name = 'cut';
      n.strokes = [solid('input/bg-active')];
      continue;
    }
    if (hasFills) n.fills = [solid('icon/primary')];
    if (hasStrokes) n.strokes = [solid('icon/primary')];
  }
}

// Redraws the icon inside the same component, then puts back the color each instance had
async function replaceIcon(name, svg, note = LINE_ICON_NOTE) {
  const component = K[`GI/Icon/${name}`];
  if (!component || component.description === note) return null;
  const instances = await component.getInstancesAsync();
  const tints = instances.map(i => [i, readTint(i)]);
  for (const child of [...component.children]) child.remove();
  fillGlyph(component, svg, name === 'clear');
  component.description = note;
  let recolored = 0;
  for (const [i, t] of tints) {
    if (!t || i.removed) continue;
    const v = await figma.variables.getVariableByIdAsync(t.id);
    if (!v) continue;
    try {
      tintVar(i, v, t.opacity);
      recolored++;
    } catch (e) {
      // An instance that can't be overridden keeps the default color
    }
  }
  return recolored;
}

async function fixIcons() {
  const done = [];
  for (const [name, svg] of Object.entries(ICON_UPDATES)) {
    const recolored = await replaceIcon(name, svg);
    if (recolored !== null) done.push(`${name} (${recolored})`);
  }
  if (!K['GI/Icon/close']) {
    const c = figma.createComponent();
    c.name = 'GI/Icon/close';
    c.resize(24, 24);
    c.fills = [];
    c.clipsContent = false;
    fillGlyph(c, CLOSE_ICON, false);
    c.description = CLOSE_NOTE;
    caption(boardItems('GI · Icons'), c, 'close');
    K['GI/Icon/close'] = c;
    done.push('close (new)');
  }
  log(`✓ Icons redrawn, instances recolored: ${done.join(', ') || 'nothing to do'}`);
}

async function ensureRadiusXl() {
  if (SZ['radius/xl']) return;
  const collection = await figma.variables.getVariableCollectionByIdAsync(SZ['radius/pill'].variableCollectionId);
  const v = figma.variables.createVariable('radius/xl', collection, 'FLOAT');
  v.setValueForMode(collection.modes[0].modeId, 32);
  v.scopes = ['CORNER_RADIUS'];
  v.setVariableCodeSyntax('ANDROID', 'GiTheme.dimens.radiusXl');
  SZ['radius/xl'] = v;
  log('✓ radius/xl = 32: rows keep the radius when they grow');
}

// Both marks as large, with the same 3px rounded line
function fixToggle() {
  for (const v of K['GI/Toggle'].children) {
    const on = v.variantProperties.State === 'On';
    const old = v.findOne(n => n.name === 'mark');
    if (old && Math.round(old.width) === 22) continue;
    if (old) old.remove();
    const mark = icon(on ? 'check' : 'x-mark', 22, on ? 'control/mark-on' : 'control/mark-off');
    mark.name = 'mark';
    place(v, mark, on ? 37 : 5, 5);
  }
  log('✓ Toggle: the check and the cross are the same size and weight');
}

function fixDialog() {
  const dialog = K['GI/Dialog'];
  if (!dialog) return;
  for (const n of dialog.findAll(n => n.name === 'watermark')) n.remove();
  log('✓ Dialog: no emblem over the options');
}

function fixBackButton() {
  const set = K['GI/Back Button'];
  for (const v of set.children) {
    if (v.findOne(n => n.name === 'close')) continue;
    for (const child of [...v.children]) child.remove();
    const pressed = v.variantProperties.State === 'Pressed';
    const rim = figma.createEllipse();
    rim.name = 'rim';
    rim.resize(44, 44);
    rim.fills = [solid('border/highlight', 0.55)];
    v.appendChild(rim);
    const face = figma.createEllipse();
    face.name = 'face';
    face.resize(36, 36);
    face.fills = [solid(pressed ? 'control/on' : 'button/light-bg')];
    face.strokes = [solid('border/ornament', 0.5)];
    face.strokeWeight = 1;
    face.strokeAlign = 'INSIDE';
    place(v, face, 4, 4);
    const cross = icon('close', 22, 'button/light-text');
    cross.name = 'close';
    place(v, cross, 11, 11);
  }
  set.description = 'Closes a screen: the game\'s round cross, a cream disc in a light rim.';
  log('✓ Back button: the game\'s round cross');
}

// The texts end and the trailing part starts clear of the big star, which sits where the darker end begins
async function fixSettingsRows() {
  const set = K['GI/Settings Row'];
  // The new rows have no padding of their own, their parts carry it; reading the properties of an
  // already rebuilt set is skipped, it may hold a duplicate until addLinkRows repairs it
  if (set.children.every(v => v.paddingLeft === 0)) {
    log('• Settings rows already rebuilt');
    return;
  }
  const defs = set.componentPropertyDefinitions;
  const key = name => Object.keys(defs).find(k => k.split('#')[0] === name);
  const kTitle = key('Title');
  const kSummary = key('Summary');
  const kShowSummary = key('Show summary');
  const kValue = key('Value');
  for (const v of set.children) {
    if (v.paddingLeft === 0) continue;
    const type = v.variantProperties.Type;
    const selected = v.variantProperties.State === 'Selected';
    for (const child of [...v.children]) child.remove();
    v.layoutMode = 'HORIZONTAL';
    v.itemSpacing = 0;
    v.paddingLeft = v.paddingRight = v.paddingTop = v.paddingBottom = 0;
    v.counterAxisAlignItems = 'CENTER';
    v.primaryAxisSizingMode = 'FIXED';
    v.counterAxisSizingMode = 'AUTO';
    v.minHeight = 64;
    v.itemReverseZIndex = true;
    v.clipsContent = true;
    radius(v, 'radius/xl', 32);

    const texts = autoLayout('VERTICAL', { name: 'texts', itemSpacing: 2, paddingLeft: 24, paddingRight: 4, paddingTop: 12, paddingBottom: 12 });
    v.appendChild(texts);
    texts.layoutSizingHorizontal = 'FILL';
    const title = await text('Hide markers while moving', 'Body L', 'text/primary', 'title');
    texts.appendChild(title);
    title.layoutSizingHorizontal = 'FILL';
    title.textAutoResize = 'HEIGHT';
    title.maxLines = 2;
    title.textTruncation = 'ENDING';
    const summary = await text('The map moves more smoothly', 'Body S', 'text/secondary', 'summary');
    texts.appendChild(summary);
    summary.layoutSizingHorizontal = 'FILL';
    summary.textAutoResize = 'HEIGHT';
    summary.maxLines = 3;
    summary.textTruncation = 'ENDING';

    const trailColor = selected ? 'surface/row' : 'surface/row-trail';
    const zone = frame('star-zone', 56, 64);
    v.appendChild(zone);
    stretchHeight(zone, 64);
    const trail = figma.createRectangle();
    trail.name = 'trail';
    trail.resize(28, 64);
    trail.fills = [solid(trailColor)];
    place(zone, trail, 28, 0);
    trail.constraints = { horizontal: 'MAX', vertical: 'STRETCH' };
    const stars = inst('GI/Ornament/Star Cluster');
    stars.name = 'stars';
    place(zone, stars, 28 - 74, 0);
    stars.constraints = { horizontal: 'CENTER', vertical: 'CENTER' };

    const trailing = autoLayout('HORIZONTAL', { name: 'trailing', itemSpacing: 4, paddingLeft: 6, paddingRight: 14, counterAxisAlignItems: 'CENTER' });
    trailing.fills = [solid(trailColor)];
    v.appendChild(trailing);
    stretchHeight(trailing, 64);
    let value = null;
    if (type === 'Switch') {
      const toggle = inst('GI/Toggle', { State: 'On' });
      toggle.name = 'toggle';
      trailing.appendChild(toggle);
      toggle.isExposedInstance = true;
    } else {
      value = await text('System default', 'Body M', 'text/accent', 'value');
      trailing.appendChild(value);
      value.textAlignHorizontal = 'RIGHT';
      value.maxLines = 3;
      value.textTruncation = 'ENDING';
      try {
        value.maxWidth = 120;
      } catch (e) {
        value.resize(120, value.height);
        value.textAutoResize = 'HEIGHT';
      }
      trailing.appendChild(icon('chevron-right', 20, 'icon/secondary'));
    }
    title.componentPropertyReferences = { characters: kTitle };
    summary.componentPropertyReferences = { characters: kSummary, visible: kShowSummary };
    if (value) value.componentPropertyReferences = { characters: kValue };
  }
  for (const v of K['GI/Radio Row'].children) radius(v, 'radius/xl', 32);
  log('✓ Settings rows: content clear of the big star, the radius stays 32');
}

// ---------- Review fixes, round 2 ----------

// Truncation goes on before the line limit, Figma drops the limit otherwise
function limitLines(t, lines, hugWidth) {
  if (!hugWidth) t.textAutoResize = 'HEIGHT';
  t.textTruncation = 'ENDING';
  t.maxLines = lines;
}

// The big star gets a token of its own, so it can be dimmed in the dark theme apart from the small ones
async function ensureStarTokens() {
  const palette = await figma.variables.getVariableCollectionByIdAsync(P['gold/300'].variableCollectionId);
  for (const [name, hex] of [['gold/700', '#5E5139'], ['gold/800', '#4D4436']]) {
    const v = P[name] || figma.variables.createVariable(name, palette, 'COLOR');
    v.setValueForMode(palette.modes[0].modeId, rgb(hex));
    v.scopes = [];
    P[name] = v;
  }
  if (!C['ornament/star-accent']) {
    const colors = await figma.variables.getVariableCollectionByIdAsync(C['ornament/star'].variableCollectionId);
    const v = figma.variables.createVariable('ornament/star-accent', colors, 'COLOR');
    v.setValueForMode(colors.modes[0].modeId, figma.variables.createVariableAlias(P['gold/300']));
    v.scopes = ['SHAPE_FILL'];
    v.setVariableCodeSyntax('ANDROID', 'GiTheme.colors.ornamentStarAccent');
    C['ornament/star-accent'] = v;
  }
  const cluster = K['GI/Ornament/Star Cluster'];
  const stars = cluster.children.filter(n => n.type === 'INSTANCE');
  if (stars.length) tint(stars.reduce((a, b) => (b.width > a.width ? b : a)), 'ornament/star-accent');
  log('✓ Stars: ornament/star-accent for the big one, gold/700 and gold/800 for the dark theme');
}

// Reads "Type=Link, State=Default" from the name: variantProperties throws while the set has errors
function parseVariant(name) {
  const props = {};
  for (const part of name.split(',')) {
    const [k, v] = part.split('=').map(s => s.trim());
    if (k && v !== undefined) props[k] = v;
  }
  return props;
}

// A row that only leads somewhere (Privacy policy) has neither the darker end nor the stars.
// A clone is named like its original until renamed, which breaks the set, so it's renamed first thing;
// a copy left with a duplicate name by an earlier run becomes the Link row
function addLinkRows() {
  const set = K['GI/Settings Row'];
  const seen = new Set();
  const leftovers = [];
  for (const v of set.children) {
    if (seen.has(v.name)) leftovers.push(v);
    else seen.add(v.name);
  }
  const variants = set.children.map(v => parseVariant(v.name));
  let made = 0;
  for (const state of ['Default', 'Selected']) {
    if (variants.some(p => p.Type === 'Link' && p.State === state)) continue;
    let link = leftovers.find(v => parseVariant(v.name).State === state);
    if (link) {
      leftovers.splice(leftovers.indexOf(link), 1);
      link.name = `Type=Link, State=${state}`;
    } else {
      const nav = set.children.find(v => {
        const p = parseVariant(v.name);
        return p.Type === 'Navigation' && p.State === state;
      });
      if (!nav) continue;
      link = nav.clone();
      link.name = `Type=Link, State=${state}`;
      if (link.parent !== set) set.appendChild(link);
    }
    for (const name of ['star-zone', 'value']) {
      const n = link.findOne(x => x.name === name);
      if (n) n.remove();
    }
    const trailing = link.findOne(x => x.name === 'trailing');
    if (trailing) trailing.fills = [];
    made++;
  }
  for (const extra of leftovers) extra.remove();
  log(made ? '✓ Settings Row: Type=Link, a plain row with a chevron' : '• Settings Row: Type=Link already exists');
}

function fixTextLimits() {
  for (const v of K['GI/Settings Row'].children) {
    const title = v.findOne(n => n.name === 'title');
    if (title) limitLines(title, 2);
    const summary = v.findOne(n => n.name === 'summary');
    if (summary) limitLines(summary, 3);
    const value = v.findOne(n => n.name === 'value');
    if (value) limitLines(value, 2, true);
  }
  // Two lines of the name fit in the same 48 with less padding
  for (const v of K['GI/Category Item'].children) {
    v.paddingTop = v.paddingBottom = 4;
    v.counterAxisAlignItems = 'CENTER';
    v.resize(v.width, 48);
    v.counterAxisSizingMode = 'FIXED';
    const name = v.findOne(n => n.name === 'name');
    if (name) limitLines(name, 2);
  }
  log('✓ Lines: title 2, summary 3, value 2, marker name 2 in the same 48');
}

// The cross stays dark on its cream disc in both themes, the rim turns gold in the dark one
function fixCloseColors() {
  for (const v of K['GI/Back Button'].children) {
    const rim = v.findOne(n => n.name === 'rim');
    if (rim) rim.fills = [solid('border/highlight', 0.55)];
    const face = v.findOne(n => n.name === 'face');
    if (face) face.strokes = [solid('border/ornament', 0.5)];
    const cross = v.findOne(n => n.name === 'close');
    if (cross) tint(cross, 'button/light-text');
  }
  log('✓ Back button: the cross is button/light-text, visible in the dark theme');
}

const ASSET_PREFIXES = ['GI/Marker Icon/', 'GI/Map Icon/', 'GI/Map Background/'];

async function isImageAsset(node) {
  for (let p = node; p && p.type !== 'PAGE'; p = p.parent) {
    if (p.type === 'COMPONENT' && ASSET_PREFIXES.some(x => p.name.startsWith(x))) return true;
    if (p.type === 'INSTANCE') {
      const main = await p.getMainComponentAsync();
      if (main && ASSET_PREFIXES.some(x => main.name.startsWith(x))) return true;
    }
  }
  return false;
}

// Raster is only where the app shows images from its CDN: marker and map icons, the map itself
async function auditRaster() {
  const roots = [compPage.findOne(n => n.type === 'SECTION' && n.name === 'Genshin Impact · Design System'), genshinPage];
  let assets = 0;
  const stray = [];
  for (const root of roots) {
    if (!root) continue;
    const images = root.findAll(n => 'fills' in n && Array.isArray(n.fills) && n.fills.some(p => p.type === 'IMAGE'));
    for (const n of images) {
      if (await isImageAsset(n)) assets++;
      else stray.push(n.name);
    }
  }
  log(stray.length
    ? `✗ Raster outside the image assets: ${stray.slice(0, 20).join(', ')}`
    : `✓ Raster only in the CDN images (${assets} layers: marker and map icons, the map); icons, ornaments and controls are vectors`);
}

// ---------- Review fixes, round 3 ----------

async function fixCloseIcon() {
  const recolored = await replaceIcon('close', CLOSE_ICON, CLOSE_NOTE);
  log(recolored === null ? '• Close icon already redrawn' : `✓ Close icon: arms with arrowhead tips (${recolored} instances recolored)`);
}

// The Link rows were cloned while the set was broken and lost the links of their texts to the properties
function relinkLinkRows() {
  const set = K['GI/Settings Row'];
  const defs = set.componentPropertyDefinitions;
  const key = name => Object.keys(defs).find(k => k.split('#')[0] === name);
  for (const v of set.children) {
    if (parseVariant(v.name).Type !== 'Link') continue;
    const title = v.findOne(n => n.name === 'title');
    if (title) title.componentPropertyReferences = { characters: key('Title') };
    const summary = v.findOne(n => n.name === 'summary');
    if (summary) summary.componentPropertyReferences = { characters: key('Summary'), visible: key('Show summary') };
  }
  log('✓ Settings Row Link: title and summary follow the properties');
}

const LAYOUT_RULES = [
  ['Settings rows', [
    'Title: 2 lines at most, then an ellipsis. Summary: 3 lines. Value on the right: 2 lines, 120 px wide at most.',
    'Nothing overlaps the big star: the texts end before its 56 px zone, the switch or the value start after it. The darker end of the row begins at the centre of the star.',
    'The height hugs the texts, 64 at least; the corner radius stays 32 (radius/xl) however tall the row gets.',
    'A row that only opens something (Privacy policy) is Type=Link: no stars, no darker end, just the chevron.',
    'Selected is lighter, with a 2 px white outline and a glow (gold in the dark theme), and exactly as tall as the default row.',
  ]],
  ['Marker list', [
    'Category name: 2 lines at most inside a fixed 48 px item (4 px vertical padding), the count on the right.',
    'A hidden category is at 50% opacity and struck through.',
    'Two columns 9 px apart, 16 px from the edges; a category with respawn cycles takes the whole width with its 1 / 2 / 3 tabs.',
  ]],
  ['Map', [
    'Controls keep 16 px from the screen edges and the system bars and 8 px between each other: the search bar at the bottom, the side buttons above its right end, the zoom in the vertical middle unless it would overlap them.',
    'The marker popup takes the place of the controls, 16 px above the home indicator.',
    'The map selector stays cream in the dark theme: the map icons are drawn dark on light.',
  ]],
  ['Icons', [
    'Line icons: 3 px stroke with round caps and joins; the weight stays the same whatever the size of the icon.',
    'Content on a cream button or disc uses button/light-text, which is dark in both themes.',
    'Everything is vector. Raster only for the app\'s CDN images: marker and map icons, the map tiles.',
  ]],
  ['Color and type', [
    'Only GI / Color tokens on the screens; the dark theme is GI / Color · Dark, token for token with the same names.',
    'Cream buttons stay cream in the dark theme, as in the game.',
    'One face, HYWenHei, as in the game: emphasis by color (text/accent), links in text/link and underlined, never by weight.',
  ]],
  ['Dialogs', [
    'Cream panel with glowing gold corners and gold rules; nothing is drawn over the options.',
    'A choice dialog has Cancel only: a tap on an option picks it and closes the dialog.',
  ]],
];

// The rules the screens follow, next to them
async function buildRules(section, x, y) {
  const card = column('Layout rules', 1340, { itemSpacing: 28, paddingTop: 44, paddingBottom: 48, paddingLeft: 48, paddingRight: 48 });
  card.fills = [{ type: 'SOLID', color: rgb('#FFFFFF') }];
  card.cornerRadius = 32;
  place(section, card, x, y);
  card.appendChild(await text('Layout rules', 'Title L', 'text/primary'));
  const columns = autoLayout('HORIZONTAL', { name: 'columns', itemSpacing: 48 });
  card.appendChild(columns);
  const sides = [column('left', 598, { itemSpacing: 28 }), column('right', 598, { itemSpacing: 28 })];
  for (const side of sides) columns.appendChild(side);
  // Longer groups on the left, shorter ones on the right
  const placement = { 'Settings rows': 0, 'Map': 0, 'Dialogs': 0, 'Marker list': 1, 'Icons': 1, 'Color and type': 1 };
  for (const [title, rules] of LAYOUT_RULES) {
    const group = column(title, 598, { itemSpacing: 8 });
    sides[placement[title] || 0].appendChild(group);
    group.appendChild(docText(title, 16, '#3B4255', 'Semi Bold'));
    for (const rule of rules) {
      const t = docText(`•  ${rule}`, 13, '#4A5366');
      group.appendChild(t);
      t.layoutSizingHorizontal = 'FILL';
      t.textAutoResize = 'HEIGHT';
    }
  }
  log('✓ Layout rules next to the screens');
}

// ---------- Dark theme ----------

// The game's night menus: slate panels a step lighter than the screen, cream text, gold rules and stars,
// cream buttons kept as in the game; rows sink into a darker end like the light ones
const DARK_TOKENS = {
  'bg/screen': 'slate/800',
  'surface/panel': 'slate/700',
  'surface/raised': 'slate/500',
  'surface/row': 'slate/600',
  'surface/row-trail': 'slate/700',
  'surface/row-active': 'slate/500',
  'surface/menu': 'slate/900',
  'surface/menu-active': 'slate/600',
  'input/bg-active': 'slate/500',
  'input/bg-idle': 'slate/800',
  'input/text': 'paper/100',
  'input/placeholder': 'slate/200',
  'text/primary': 'paper/200',
  'text/secondary': 'slate/200',
  'text/accent': 'gold/300',
  'text/link': 'gold/200',
  'text/on-dark': 'paper/200',
  'text/on-dark-accent': 'gold/300',
  'text/danger': 'red/400',
  'icon/primary': 'paper/200',
  'icon/secondary': 'slate/200',
  'icon/accent': 'gold/300',
  'icon/on-dark': 'paper/200',
  'border/subtle': 'slate/500',
  'border/ornament': 'gold/400',
  'border/highlight': 'gold/200',
  'ornament/star': 'gold/800',
  'ornament/star-accent': 'gold/700',
  'control/on': 'gold/300',
  'control/off': 'slate/900',
  'control/knob': 'paper/100',
  'control/mark-on': 'slate/600',
  'control/mark-off': 'gold/400',
  'button/light-bg': 'paper/200',
  'button/light-text': 'slate/600',
  'button/dark-bg': 'slate/500',
  'button/dark-text': 'paper/200',
  'button/glyph-bg': 'slate/700',
  'button/glyph-confirm': 'amber/400',
  'button/glyph-cancel': 'azure/400',
  'map-control/bg': 'slate/600',
  'map-control/icon': 'paper/200',
  'map-control/active-bg': 'gold/300',
  'map-control/active-icon': 'slate/700',
  'status/success': 'jade/400',
  'bg/scrim': ['#0E1016', 0.65],
  'input/bg-map': ['#0E1016', 0.5],
  'input/border-map': ['#FFFFFF', 0.3],
  'input/placeholder-map': ['#FFFFFF', 0.7],
};

async function ensureDarkColors() {
  const collections = await figma.variables.getLocalVariableCollectionsAsync();
  let collection = collections.find(c => c.name === DARK_COLLECTION);
  if (!collection) {
    collection = figma.variables.createVariableCollection(DARK_COLLECTION);
    collection.renameMode(collection.modes[0].modeId, 'Dark');
  }
  const mode = collection.modes[0].modeId;
  const camel = s => s.replace(/[/-](\w)/g, (_, c) => c.toUpperCase());
  for (const [name, value] of Object.entries(DARK_TOKENS)) {
    const v = CD[name] || figma.variables.createVariable(name, collection, 'COLOR');
    if (Array.isArray(value)) v.setValueForMode(mode, Object.assign(rgb(value[0]), { a: value[1] }));
    else v.setValueForMode(mode, figma.variables.createVariableAlias(P[value]));
    if (C[name]) v.scopes = C[name].scopes;
    v.setVariableCodeSyntax('ANDROID', `GiTheme.darkColors.${camel(name)}`);
    CD[name] = v;
  }
  log(`✓ ${DARK_COLLECTION}: ${Object.keys(DARK_TOKENS).length} tokens`);
}

// Rebinds every GI / Color paint of a screen, instances included, to its dark twin
function applyDark(root) {
  const twin = {};
  for (const [name, v] of Object.entries(C)) if (CD[name]) twin[v.id] = CD[name];
  const darken = paints => {
    let changed = false;
    const next = paints.map(p => {
      const id = p.boundVariables && p.boundVariables.color && p.boundVariables.color.id;
      if (!id || !twin[id]) return p;
      changed = true;
      return figma.variables.setBoundVariableForPaint(p, 'color', twin[id]);
    });
    return changed ? next : null;
  };
  for (const n of [root, ...root.findAll(() => true)]) {
    for (const prop of ['fills', 'strokes']) {
      if (!(prop in n) || !Array.isArray(n[prop]) || !n[prop].length) continue;
      const next = darken(n[prop]);
      if (next) {
        try { n[prop] = next; } catch (e) { /* a layer that can't be overridden keeps its color */ }
      }
    }
    if (n.type === 'TEXT' && n.fills === figma.mixed) {
      for (const segment of n.getStyledTextSegments(['fills'])) {
        const next = darken(segment.fills);
        if (next) {
          try { n.setRangeFills(segment.start, segment.end, next); } catch (e) { /* as above */ }
        }
      }
    }
  }
}

// The map icons are drawn dark on light, so the selector stays cream on the dark map
function creamSelector(f) {
  const selector = f.findOne(n => n.name === 'Map selector');
  if (selector) selector.fills = [figma.variables.setBoundVariableForPaint({ type: 'SOLID', color: { r: 0, g: 0, b: 0 } }, 'color', CD['button/light-bg'])];
}

async function mapDark() {
  const f = await mapDefault();
  f.name = 'Map · Dark';
  applyDark(f);
  creamSelector(f);
  return f;
}

async function mapListDark() {
  const f = await mapList();
  f.name = 'Map · Marker list · Dark';
  applyDark(f);
  creamSelector(f);
  return f;
}

async function settingsDark() {
  const f = await settingsScreen('Settings · Dark', { dark: true });
  applyDark(f);
  return f;
}

async function settingsThemeDark() {
  const f = await settingsScreen('Settings · Theme · Dark', { dark: true, pressed: 'theme' });
  const d = dialog(f, 'Theme', 'Theme');
  // Dark is the picked option on this one
  const options = d.findAll(n => n.type === 'INSTANCE' && Object.keys(n.componentProperties).some(k => k.startsWith('Label')) && 'Selected' in n.componentProperties);
  for (const option of options) {
    if (option.removed) continue;
    const label = option.findOne(n => n.name === 'label');
    option.setProperties({ Selected: String(!!label && label.characters === 'Dark') });
  }
  applyDark(f);
  return f;
}

// ---------- Font ----------

async function useHYWenHei() {
  const fonts = await figma.listAvailableFontsAsync();
  const candidates = fonts.filter(f => /HYWenHei|汉仪文黑/i.test(f.fontName.family));
  if (!candidates.length) {
    log('✗ HYWenHei is not installed. Install zhcn.ttf (double-click it → Install), restart Figma and run this command again.');
    return;
  }
  const pick = candidates.find(f => /85|Heavy/i.test(f.fontName.style)) || candidates[0];
  await figma.loadFontAsync(pick.fontName);
  const family = SZ['font/family'];
  const style = SZ['font/style'];
  const collection = await figma.variables.getVariableCollectionByIdAsync(family.variableCollectionId);
  const mode = collection.modes[0].modeId;
  family.setValueForMode(mode, pick.fontName.family);
  style.setValueForMode(mode, pick.fontName.style);
  log(`✓ The GI text styles now use ${pick.fontName.family} ${pick.fontName.style}`);
  log(`Found: ${candidates.map(f => `${f.fontName.family} ${f.fontName.style}`).join(', ')}`);
}

// ---------- Run ----------

async function step(name, fn) {
  try {
    await fn();
  } catch (e) {
    log(`✗ ${name}: ${e && e.message ? e.message : e}`);
  }
}

async function main() {
  compPage = figma.root.children.find(p => p.name === COMPONENTS_PAGE);
  if (!compPage) throw new Error('There is no Components page');
  genshinPage = figma.root.children.find(p => p.name === GENSHIN_PAGE);
  if (!genshinPage) {
    genshinPage = figma.createPage();
    genshinPage.name = GENSHIN_PAGE;
  }
  await loadTokens();
  if (figma.command === 'font') {
    await useHYWenHei();
    return;
  }
  await loadComponents();
  await figma.setCurrentPageAsync(compPage);
  await step('Selected rows', fixSelectedRows);
  await step('Dialog', buildDialog);
  await step('Location card', buildLocationCard);
  if (figma.command === 'fixes') {
    // Instances on every page get their colors back after an icon is redrawn
    await figma.loadAllPagesAsync();
    await step('Radius', ensureRadiusXl);
    await step('Icons', fixIcons);
    await step('Toggle', fixToggle);
    await step('Dialog emblem', fixDialog);
    await step('Back button', fixBackButton);
    await step('Settings rows', fixSettingsRows);
    await step('Stars', ensureStarTokens);
    await step('Link rows', addLinkRows);
    await step('Link texts', relinkLinkRows);
    await step('Line limits', fixTextLimits);
    await step('Close icon', fixCloseIcon);
    await step('Back button colors', fixCloseColors);
    await step('Dark colors', ensureDarkColors);
  }
  await loadComponents();
  await figma.setCurrentPageAsync(genshinPage);
  await genshinPage.loadAsync();
  if (figma.command === 'rebuild' || figma.command === 'fixes') removeOwnSections();
  await step('Foundations', buildFoundations);
  await step('Screens', buildScreens);
  if (figma.command === 'fixes') await step('Raster audit', auditRaster);
  log('Done. Copy this log if something failed.');
}

function showLog() {
  const escape = s => s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
  figma.showUI(
    `<style>body{font:12px/1.5 Inter,system-ui,sans-serif;margin:12px;color:#3B4255}pre{white-space:pre-wrap;margin:0;user-select:text}</style><pre>${escape(LOG.join('\n'))}</pre>`,
    { width: 460, height: 360, title: 'Genshin DS Builder' }
  );
}

main().then(showLog, e => {
  log(`✗ ${e && e.message ? e.message : e}`);
  showLog();
});
