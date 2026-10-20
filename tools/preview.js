// Maakt preview.html uit Data.kt zodat de stokfiguurtjes in de browser te bekijken zijn.
// Gebruik: node tools/preview.js
const fs = require('fs');
const path = require('path');
const src = fs.readFileSync(path.join(__dirname, '../app/src/main/java/com/fit15/app/Data.kt'), 'utf8');

const defaults = { uaA: -90, faA: -90, uaB: -90, faB: -90, thA: -90, shA: -90, thB: -90, shB: -90, toeA: 0, toeB: 0, lift: 0 };
function parsePose(s) {
  const o = { ...defaults };
  for (const m of s.matchAll(/(\w+)\s*=\s*(-?[\d.]+)f/g)) o[m[1]] = parseFloat(m[2]);
  return o;
}
const ex = [];
const body = src.split('class Week(')[0];
const byVar = {};
const parts = body.split(/val (\w+) = Exercise\(/);
for (let i = 1; i < parts.length; i += 2) {
  const v = parts[i], chunk = parts[i + 1];
  const id = chunk.match(/id = "(\w+)"/)[1];
  const name = chunk.match(/name = "([^"]+)"/)[1];
  const get = (k) => {
    const m = chunk.match(new RegExp('\\b' + k + ' = Pose\\(([\\s\\S]*?)\\)\\s*,'));
    if (m) return parsePose(m[1]);
    const r = chunk.match(new RegExp('\\b' + k + ' = (\\w+)\\.' + k));
    return byVar[r[1]][k];
  };
  const a = get('a'), b = get('b');
  const prop = (chunk.match(/prop = Prop\.(\w+)/) || [0, 'NONE'])[1];
  const front = /front = true/.test(chunk);
  const half = parseInt((chunk.match(/halfPeriodMs = (\d+)/) || [0, 1200])[1]);
  byVar[v] = { a, b };
  ex.push({ id, name, a, b, prop, front, half });
}
fs.writeFileSync(path.join(__dirname, 'preview.html'), `<!doctype html><meta charset=utf-8><body style="font-family:sans-serif;background:#fff">
<div id=g style="display:flex;flex-wrap:wrap;gap:8px"></div>
<script>
const EX=${JSON.stringify(ex)};
const T=14,UA=8,FA=8,TH=10,SH=10,FT=3.5,HR=3.4,HD=5.2,FL=6;
const d=a=>[Math.cos(a*Math.PI/180),Math.sin(a*Math.PI/180)];
const add=(p,v,l)=>[p[0]+v[0]*l,p[1]+v[1]*l];
function solve(p){
  const hip=[p.hx,0],neck=add(hip,d(p.torso),T),head=add(neck,d(p.headDir??p.torso),HD);
  const eA=add(neck,d(p.uaA),UA),hA=add(eA,d(p.faA),FA),eB=add(neck,d(p.uaB),UA),hB=add(eB,d(p.faB),FA);
  const kA=add(hip,d(p.thA),TH),aA=add(kA,d(p.shA),SH),tA=add(aA,d(-p.toeA),FT);
  const kB=add(hip,d(p.thB),TH),aB=add(kB,d(p.shB),SH),tB=add(aB,d(-p.toeB),FT);
  const low=Math.min(...[hip,eA,hA,eB,hB,kA,aA,tA,kB,aB,tB].map(q=>q[1]));
  const u=FL-low+(p.lift||0);
  const S=q=>[q[0],q[1]+u];
  return {head:S(head),neck:S(neck),hip:S(hip),eA:S(eA),hA:S(hA),eB:S(eB),hB:S(hB),kA:S(kA),aA:S(aA),tA:S(tA),kB:S(kB),aB:S(aB),tB:S(tB)};
}
const lerp=(a,b,t)=>a+(b-a)*t;
function lp(a,b,t){const o={};for(const k in a)o[k]=lerp(a[k],b[k]??a[k],t);o.headDir=lerp(a.headDir??a.torso,b.headDir??b.torso,t);return o}
function draw(c,e,t){
  const W=300,H=180,s=3;c.clearRect(0,0,W,H);
  const P=q=>[q[0]*s,(60-q[1])*s];
  const line=(a,b,col,w)=>{c.strokeStyle=col;c.lineWidth=w;c.lineCap='round';c.beginPath();c.moveTo(...P(a));c.lineTo(...P(b));c.stroke()};
  line([4,FL-1.3],[96,FL-1.3],'#aaa',2);
  const hx=e.a.hx;
  if(e.prop=='WALL')line([hx-4.6,FL-1.3],[hx-4.6,56],'#aaa',4);
  if(e.prop=='CHAIR_FEET'){const l=hx-31,r=hx-19,top=FL-1.3+10;line([l,top],[r,top],'#aaa',5);line([l+.5,top],[l+.5,FL-1.3],'#aaa',3);line([r-.5,top],[r-.5,FL-1.3],'#aaa',3)}
  if(e.prop=='CHAIR'){const l=hx-11.5,r=hx-1.5,top=FL-1.3+10;line([l,top],[r,top],'#aaa',5);line([l+.5,top],[l+.5,FL-1.3],'#aaa',3);line([r-.5,top],[r-.5,FL-1.3],'#aaa',3)}
  const k=lp(e.a,e.b,t),S=solve(k),far=e.front?'#222':'#999',w=2.6*s;
  line(S.neck,S.eB,far,w);line(S.eB,S.hB,far,w);line(S.hip,S.kB,far,w);line(S.kB,S.aB,far,w);line(S.aB,S.tB,far,w);
  line(S.hip,S.neck,'#222',w);
  c.strokeStyle='#222';c.lineWidth=w*.85;c.beginPath();c.arc(...P(S.head),HR*s,0,7);c.stroke();
  line(S.neck,S.eA,'#222',w);line(S.eA,S.hA,'#222',w);line(S.hip,S.kA,'#222',w);line(S.kA,S.aA,'#222',w);line(S.aA,S.tA,'#222',w);
}
const g=document.getElementById('g');
for(const e of EX){
  const div=document.createElement('div');div.innerHTML='<b>'+e.name+'</b><br>';
  const cs=[];
  for(const t of [0,1]){const cv=document.createElement('canvas');cv.width=300;cv.height=180;cv.style.border='1px solid #ddd';div.appendChild(cv);draw(cv.getContext('2d'),e,t)}
  g.appendChild(div);
}
</script>`);
console.log(ex.map(e => e.id).join(', '));
