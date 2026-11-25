const UP='<svg viewBox="0 0 24 24"><path d="M12 19V5M5 12l7-7 7 7"/></svg>',ST='<svg viewBox="0 0 24 24" style="fill:currentColor;stroke:none"><rect x="6" y="6" width="12" height="12" rx="2.5"/></svg>';
const $=s=>document.querySelector(s);
const E={msgs:$('#msgs'),chat:$('#chat'),input:$('#input'),send:$('#send'),files:$('#files'),hist:$('#hist'),side:$('#side'),scrim:$('#scrim'),prov:$('#provider'),search:$('#search'),file:$('#file')};
let chats=[],cur=null,pending=[],busy=false,ctrl=null;
try{chats=JSON.parse(localStorage.getItem('bh_chats')||'[]')}catch{}
E.prov.value=localStorage.getItem('bh_prov')||'nara';
const save=()=>{try{localStorage.setItem('bh_chats',JSON.stringify(chats.map(c=>({...c,msgs:c.msgs.map(({role,content,names})=>({role,content,names}))}))))}catch{}};
const esc=s=>s.replace(/[&<>]/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;'}[c]));
function md(s){
  const blocks=[];
  s=esc(s).replace(/```(\w*)\n?([\s\S]*?)(```|$)/g,(_,l,c)=>{blocks.push(`<pre><button class="cp">Copy</button><code>${c.replace(/\n$/,'')}</code></pre>`);return `\u0000${blocks.length-1}\u0000`});
  s=s.replace(/`([^`\n]+)`/g,'<code>$1</code>').replace(/\*\*([^*\n]+)\*\*/g,'<b>$1</b>').replace(/(^|\W)\*([^*\n]+)\*/g,'$1<i>$2</i>');
  const out=[];let list=null;
  for(const ln of s.split('\n')){
    let m;
    if(m=ln.match(/^\s*([-*]|\d+\.)\s+(.*)/)){const t=/\d/.test(m[1])?'ol':'ul';if(list!==t){if(list)out.push(`</${list}>`);out.push(`<${t}>`);list=t}out.push(`<li>${m[2]}</li>`);continue}
    if(list){out.push(`</${list}>`);list=null}
    if(m=ln.match(/^#{1,3}\s+(.*)/))out.push(`<h3>${m[1]}</h3>`);else if(ln.trim())out.push(`<p>${ln}</p>`);
  }
  if(list)out.push(`</${list}>`);
  return out.join('').replace(/\u0000(\d+)\u0000/g,(_,i)=>blocks[i]).replace(/<p>(<pre)/g,'$1').replace(/<\/pre><\/p>/g,'</pre>');
}
const tagHTML=(n,img)=>`<span class="tag">${img?`<img src="${img}">`:''}${esc(n)}</span>`;
function addMsg(role,html,names=[],imgs={}){
  const d=document.createElement('div');d.className='m '+(role==='user'?'u':'a');
  const tags=names.length?`<div class="tags">${names.map(n=>tagHTML(n,imgs[n])).join('')}</div>`:'';
  d.innerHTML=role==='user'?`<div>${tags}<div class="bub"></div></div>`:`<div class="bub"></div>`;
  const b=d.querySelector('.bub');
  if(role==='user'){b.textContent=html;if(!html)b.remove()}else b.innerHTML=html;
  E.msgs.appendChild(d);scroll();return b;
}
const scroll=()=>E.chat.scrollTo({top:E.chat.scrollHeight,behavior:'smooth'});
function hero(){
  E.msgs.innerHTML=`<div class="hero"><h1>How can I help you today?</h1><p>Ask anything, or attach files and images.</p><div class="chips">${['Explain a concept simply','Help me write an email','Review my code','Plan my week'].map(t=>`<button>${t}</button>`).join('')}</div></div>`;
}
function render(){
  E.msgs.innerHTML='';
  if(!cur||!cur.msgs.length)hero();
  else cur.msgs.forEach(m=>addMsg(m.role,m.role==='user'?m.content:md(m.content),m.names||[]));
  E.chat.scrollTop=E.chat.scrollHeight;renderHist();
}
function renderHist(){
  const q=E.search.value.toLowerCase();
  const l=chats.filter(c=>!q||c.title.toLowerCase().includes(q)||c.msgs.some(m=>m.content.toLowerCase().includes(q))).sort((a,b)=>b.ts-a.ts);
  E.hist.innerHTML=l.length?'':'<div class="empty-h">No chats found</div>';
  l.forEach(c=>{const d=document.createElement('div');d.className='h'+(cur&&cur.id===c.id?' on':'');d.innerHTML=`<span>${esc(c.title)}</span><button aria-label="Delete"><svg viewBox="0 0 24 24"><path d="M4 7h16M10 11v6M14 11v6M6 7l1 13h10l1-13M9 7V4h6v3"/></svg></button>`;
    d.onclick=()=>{cur=c;render();closeSide()};
    d.querySelector('button').onclick=e=>{e.stopPropagation();chats=chats.filter(x=>x!==c);if(cur===c)cur=null;save();render()};
    E.hist.appendChild(d)});
}
const openSide=()=>{E.side.classList.add('open');E.scrim.classList.add('on')};
const closeSide=()=>{E.side.classList.remove('open');E.scrim.classList.remove('on')};
function newChat(){if(busy)return;cur=null;pending=[];renderFiles();render();closeSide();E.input.focus()}
function sync(){E.input.style.height='auto';E.input.style.height=Math.min(E.input.scrollHeight,160)+'px';E.send.disabled=!busy&&!(E.input.value.trim()||pending.length)}
function renderFiles(){E.files.innerHTML='';pending.forEach((f,i)=>{const t=document.createElement('span');t.className='tag';t.innerHTML=`${f.img?`<img src="${f.img}">`:''}${esc(f.name)} <button>✕</button>`;t.querySelector('button').onclick=()=>{pending.splice(i,1);renderFiles();sync()};E.files.appendChild(t)});sync()}
const TEXT=/\.(txt|md|json|csv|js|ts|jsx|tsx|py|html|css|xml|yml|yaml|java|kt|c|cpp|h|go|rs|php|sql|sh|toml|ini|log|gradle|properties)$/i;
const readAs=(f,m)=>new Promise(r=>{const x=new FileReader();x.onload=()=>r(x.result);m==='text'?x.readAsText(f):x.readAsDataURL(f)});
E.file.onchange=async()=>{
  for(const f of E.file.files){
    if(f.type.startsWith('image/')&&f.size<6e6)pending.push({name:f.name,type:'image',img:await readAs(f)});
    else if((f.type.startsWith('text/')||TEXT.test(f.name))&&f.size<3e5)pending.push({name:f.name,type:'text',text:await readAs(f,'text')});
    else pending.push({name:f.name,type:'bin',size:f.size});
  }
  E.file.value='';renderFiles();
};
async function sendMsg(){
  const text=E.input.value.trim();if(busy||!(text||pending.length))return;
  if(!cur){cur={id:Date.now()+'',title:(text||pending[0].name).slice(0,40),ts:Date.now(),msgs:[]};chats.push(cur)}
  const att=pending;pending=[];renderFiles();E.input.value='';
  const imgs={};att.forEach(a=>{if(a.img)imgs[a.name]=a.img});
  if(!cur.msgs.length)E.msgs.innerHTML='';
  const um={role:'user',content:text,names:att.map(a=>a.name),parts:att};
  cur.msgs.push(um);cur.ts=Date.now();addMsg('user',text,um.names,imgs);
  const b=addMsg('assistant','<div class="sk"><i></i><i></i><i></i></div>');
  busy=true;E.send.innerHTML=ST;;E.send.disabled=false;sync();E.send.disabled=false;
  const api=cur.msgs.map(m=>{
    if(m.role!=='user'||!m.parts?.length)return{role:m.role,content:m.content+((m.names?.length&&!m.parts)?`\n[Attached earlier: ${m.names.join(', ')}]`:'')};
    let t=m.content;const c=[];
    m.parts.forEach(p=>{if(p.type==='image')c.push({type:'image_url',image_url:{url:p.img}});else if(p.type==='text')t+=`\n\n--- File: ${p.name} ---\n${p.text}`;else t+=`\n\n[Attached file: ${p.name} (${Math.round(p.size/1024)} KB) - binary, content cannot be read]`});
    return{role:'user',content:c.length?[{type:'text',text:t||'Describe this.'},...c]:t};
  });
  let acc='';ctrl=new AbortController();
  try{
    const r=await fetch('/api/chat',{method:'POST',headers:{'Content-Type':'application/json'},signal:ctrl.signal,body:JSON.stringify({provider:E.prov.value,messages:api})});
    if(!r.ok){let e='Error '+r.status;try{e=(await r.json()).error||e}catch{}throw new Error(e)}
    const rd=r.body.getReader(),dec=new TextDecoder();let buf='';
    for(;;){
      const{done,value}=await rd.read();if(done)break;
      buf+=dec.decode(value,{stream:true});const lines=buf.split('\n');buf=lines.pop();
      for(const l of lines){if(!l.startsWith('data:'))continue;const d=l.slice(5).trim();if(d==='[DONE]')continue;
        try{const t=JSON.parse(d).choices?.[0]?.delta?.content;if(t){acc+=t;b.innerHTML=md(acc)+'<span class="cur"></span>';scroll()}}catch{}}
    }
    if(!acc)throw new Error('Empty response');
  }catch(e){if(e.name==='AbortError'){if(!acc)b.innerHTML='<span class="err">Stopped.</span>'}else b.innerHTML=md(acc)+`<p class="err">⚠ ${esc(e.message)}</p>`}
  if(acc){b.innerHTML=md(acc);cur.msgs.push({role:'assistant',content:acc})}
  busy=false;ctrl=null;E.send.innerHTML=UP;save();renderHist();sync();
}
E.send.onclick=()=>busy?ctrl&&ctrl.abort():sendMsg();
E.input.oninput=sync;
E.input.onkeydown=e=>{if(e.key==='Enter'&&!e.shiftKey&&matchMedia('(min-width:900px)').matches){e.preventDefault();sendMsg()}};
E.msgs.onclick=e=>{
  if(e.target.classList.contains('cp')){navigator.clipboard.writeText(e.target.nextSibling.textContent);e.target.textContent='Copied';setTimeout(()=>e.target.textContent='Copy',1200)}
  else if(e.target.closest('.chips button')){E.input.value=e.target.textContent;sync();sendMsg()}
};
E.prov.onchange=()=>localStorage.setItem('bh_prov',E.prov.value);
E.search.oninput=renderHist;
$('#menu').onclick=openSide;$('#closeSide').onclick=closeSide;E.scrim.onclick=closeSide;
$('#newChat').onclick=newChat;$('#newTop').onclick=newChat;
E.send.innerHTML=UP;render();sync();
