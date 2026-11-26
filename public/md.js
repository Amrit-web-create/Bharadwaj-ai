const esc=s=>s.replace(/[&<>]/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;'}[c]));
const FEXT={javascript:'js',typescript:'ts',python:'py',kotlin:'kt',bash:'sh',shell:'sh',markdown:'md',yaml:'yml'};
const FENCE=/```([^\n`]*)\n([\s\S]*?)(```|$)/g;
function parseInfo(info){
  const t=info.trim().split(/[\s:]+/).filter(Boolean);let lang=(t[0]||'').toLowerCase(),name=t.slice(1).join(' ');
  if(!name&&t[0]&&/\.\w{1,6}$/.test(t[0])){name=t[0];lang=t[0].split('.').pop().toLowerCase()}
  return{lang,name};
}
function fences(text){
  const o=[],re=new RegExp(FENCE.source,'g');let m;
  while(m=re.exec(text)){const{lang,name}=parseInfo(m[1]);o.push({lang,name,code:m[2].replace(/\n$/,''),done:m[3]==='```'});if(!m[0])re.lastIndex++}
  return o;
}
function nameAll(f){return f.map((x,k)=>({...x,name:x.name||`file${k+1}.${FEXT[x.lang]||x.lang||'txt'}`}))}
const CODE_SVG='<svg viewBox="0 0 24 24"><path d="M8 8l-4 4 4 4M16 8l4 4-4 4M14 5l-4 14"/></svg>';
function md(s){
  const c=(s.match(/```/g)||[]).length;
  if(c%2){const j=s.lastIndexOf('```');if(s.indexOf('\n',j)<0)s=s.slice(0,j)}
  const blocks=[];let n=0;
  s=esc(s).replace(new RegExp(FENCE.source,'g'),(_,info,cd,end)=>{
    const{lang,name}=parseInfo(info),code=cd.replace(/\n$/,''),i=n++,lines=code.split('\n').length;
    const art=/^(html|htm|svg|jsx|tsx)$/.test(lang)||name||lines>=10;
    blocks.push(art
      ?`<button class="art" data-i="${i}"><span class="ai">${CODE_SVG}</span><span class="at"><b>${name||(lang?lang.toUpperCase()+' code':'Code')}</b><small>${end?`${lang?lang.toUpperCase()+' · ':''}${lines} lines · Tap to open`:'Writing…'}</small></span></button>`
      :`<pre><button class="cp">Copy</button><code>${code}</code></pre>`);
    return `\u0000${blocks.length-1}\u0000`;
  });
  s=s.replace(/`([^`\n]+)`/g,'<code>$1</code>').replace(/\*\*([^*\n]+)\*\*/g,'<b>$1</b>').replace(/(^|\W)\*([^*\n]+)\*/g,'$1<i>$2</i>');
  const L=s.split('\n'),out=[];let list=null;
  const cells=r=>r.trim().replace(/^\||\|$/g,'').split('|').map(x=>x.trim());
  for(let k=0;k<L.length;k++){
    const ln=L[k];let m;
    if(/^\s*\|.*\|\s*$/.test(ln)&&k+1<L.length&&/^\s*\|?\s*:?-{2,}[\s|:-]*$/.test(L[k+1])){
      if(list){out.push(`</${list}>`);list=null}
      const rows=[];while(k<L.length&&/^\s*\|.*\|\s*$/.test(L[k])){rows.push(L[k]);k++}k--;
      out.push(`<div class="tw"><table><thead><tr>${cells(rows[0]).map(x=>`<th>${x}</th>`).join('')}</tr></thead><tbody>${rows.slice(2).map(r=>`<tr>${cells(r).map(x=>`<td>${x}</td>`).join('')}</tr>`).join('')}</tbody></table></div>`);
      continue;
    }
    if(m=ln.match(/^\s*([-*]|\d+\.)\s+(.*)/)){const t=/\d/.test(m[1])?'ol':'ul';if(list!==t){if(list)out.push(`</${list}>`);out.push(`<${t}>`);list=t}out.push(`<li>${m[2]}</li>`);continue}
    if(list){out.push(`</${list}>`);list=null}
    if(/^\s*-{3,}\s*$/.test(ln))out.push('<hr>');
    else if(m=ln.match(/^#{1,3}\s+(.*)/))out.push(`<h3>${m[1]}</h3>`);
    else if(/^\u0000\d+\u0000$/.test(ln.trim()))out.push(ln.trim());
    else if(ln.trim())out.push(`<p>${ln}</p>`);
  }
  if(list)out.push(`</${list}>`);
  return out.join('').replace(/\u0000(\d+)\u0000/g,(_,i)=>blocks[i]);
}
if(typeof module!=='undefined')module.exports={md,fences,nameAll};
