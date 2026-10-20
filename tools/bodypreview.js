// Maakt body.html uit BodyDraw.kt zodat de spierkaart in de browser te bekijken is.
const fs = require('fs');
const path = require('path');
const s = fs.readFileSync(path.join(__dirname, '../app/src/main/java/com/fit15/app/BodyDraw.kt'), 'utf8');
const sil = [...s.match(/private val SIL = pth\(([\s\S]*?)\n\)/)[1].matchAll(/"([^"]+)"/g)].map((m) => m[1]).join('');
const delt = s.match(/DELT = "([^"]+)"/)[1];
const grab = (name) => {
  const block = s.match(new RegExp('private val ' + name + ' = listOf\\(([\\s\\S]*?)\\n\\)'))[1];
  return [...block.matchAll(/Part\(Muscle\.(\w+), (DELT|"[^"]+")\)/g)].map((m) => [m[1], m[2] === 'DELT' ? delt : m[2].slice(1, -1)]);
};
const cols = {
  CHEST: '#e5392b', CORE: '#f2b33d', QUADS: '#4da3e0', CALVES: '#f2b33d', SHOULDERS: '#e5392b',
  UPPER_BACK: '#e5392b', LOWER_BACK: '#4da3e0', GLUTES: '#f2b33d', HAMSTRINGS: '#e5392b', TRICEPS: '#4da3e0',
};
const view = (parts) =>
  '<svg width="300" height="624" viewBox="0 0 100 208" style="background:#fff;border:1px solid #ccc">' +
  [0, 1].map((m) =>
    '<g transform="' + (m ? 'translate(100,0) scale(-1,1)' : '') + '"><path d="' + sil + '" fill="#ddd" stroke="#999" stroke-width="0.7"/>' +
    parts.map((p) => '<path d="' + p[1] + '" fill="' + cols[p[0]] + '" stroke="#fff" stroke-width="0.5"/>').join('') + '</g>').join('') +
  '<ellipse cx="50" cy="12.5" rx="8.3" ry="10.2" fill="#ddd"/></svg>';
fs.writeFileSync(path.join(__dirname, 'body.html'), '<body style="display:flex;gap:20px">' + view(grab('FRONT')) + view(grab('BACK')));
console.log('ok');
