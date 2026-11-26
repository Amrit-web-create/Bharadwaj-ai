const $=s=>document.querySelector(s),$$=s=>[...document.querySelectorAll(s)];
const I=p=>`<svg viewBox="0 0 24 24">${p}</svg>`;
const IC={copy:I('<rect x="9" y="9" width="11" height="11" rx="2"/><path d="M5 15V6a2 2 0 012-2h9"/>'),share:I('<path d="M12 16V4M7 9l5-5 5 5M5 14v4a2 2 0 002 2h10a2 2 0 002-2v-4"/>'),redo:I('<path d="M20 11a8 8 0 10-2.3 5.7M20 4v7h-7"/>'),up:I('<path d="M12 19V5M5 12l7-7 7 7"/>'),stop:'<svg viewBox="0 0 24 24" style="fill:currentColor;stroke:none"><rect x="6" y="6" width="12" height="12" rx="2.5"/></svg>',x:I('<path d="M6 6l12 12M18 6L6 18"/>'),trash:I('<path d="M4 7h16M10 11v6M14 11v6M6 7l1 13h10l1-13M9 7V4h6v3"/>')};
const E={msgs:$('#msgs'),chat:$('#chat'),input:$('#input'),send:$('#send'),files:$('#files'),hist:$('#hist'),side:$('#side'),scrim:$('#scrim'),search:$('#search'),file:$('#file'),title:$('#title'),pop:$('#pop')};
const MODELS={nara:'Nara · Auto',groq:'Groq',gemini:'Gemini',nvidia:'NVIDIA'};
const G=["What's up today?","How are you doing?","Good to see you.","What's on your mind?","Ready when you are."];
const SK='<div class="sk"><i></i><i></i><i></i></div>';
const TEXT=/\.(txt|md|json|csv|js|ts|jsx|tsx|py|html|css|xml|yml|yaml|java|kt|c|cpp|h|go|rs|php|sql|sh|toml|ini|log|gradle|properties|svg)$/i;
let chats=[],cur=null,pending=[],busy=false,ctrl=null,liveAcc='',LB=[];
try{chats=JSON.parse(localStorage.getItem('bh_chats')||'[]')}catch{}
let provider=localStorage.getItem('bh_prov');if(!MODELS[provider])provider='nara';
let theme=localStorage.getItem('bh_theme')||'system';
const mq=matchMedia('(prefers-color-scheme:dark)');
const applyTheme=()=>{document.documentElement.dataset.theme=theme==='system'?(mq.matches?'dark':'light'):theme;$$('#themeSeg button').forEach(b=>b.classList.toggle('on',b.dataset.t===theme))};
mq.onchange=applyTheme;
const save=()=>{try{localStorage.setItem('bh_chats',JSON.stringify(chats.map(c=>({...c,msgs:c.msgs.map(({role,content,names,t})=>({role,content,names,t}))}))))}catch{}};
const toast=t=>{const e=$('#toast');e.textContent=t;e.classList.add('on');clearTimeout(toast.h);toast.h=setTimeout(()=>e.classList.remove('on'),1600)};
const copyTxt=t=>{(navigator.clipboard?navigator.clipboard.writeText(t):Promise.reject()).then(()=>toast('Copied'),()=>toast('Copy failed'))};
const readAs=(f,m)=>new Promise(r=>{const x=new FileReader();x.onload=()=>r(x.result);m==='text'?x.readAsText(f):x.readAsDataURL(f)});
const dl=(name,blob)=>{const a=document.createElement('a');a.href=URL.createObjectURL(blob);a.download=name;document.body.appendChild(a);a.click();a.remove();setTimeout(()=>URL.revokeObjectURL(a.href),4000)};
let zipP;const loadZip=()=>window.JSZip?Promise.resolve():(zipP=zipP||new Promise((ok,no)=>{const s=document.createElement('script');s.src='https://cdnjs.cloudflare.com/ajax/libs/jszip/3.10.1/jszip.min.js';s.onload=ok;s.onerror=()=>no(new Error('ZIP library load nahi hui (internet check karo)'));document.head.appendChild(s)}));
async function zipFiles(files){try{await loadZip();const z=new JSZip();files.forEach(f=>z.file(f.name,f.code));dl('bharadwaj-project.zip',await z.generateAsync({type:'blob'}))}catch(e){toast(e.message)}}
async function zipText(f){await loadZip();const z=await JSZip.loadAsync(f),names=Object.keys(z.files).filter(n=>!z.files[n].dir);let out=`ZIP contents (${names.length} files):\n`+names.slice(0,200).join('\n'),used=0;for(const n of names){if(used>6e4)break;if(!TEXT.test(n))continue;const t=await z.files[n].async('string');if(t.length>2e4)continue;out+=`\n\n--- ${n} ---\n${t}`;used+=t.length}return out}

/* ---------- attachments ---------- */
function thumb(a,cls){
  if(a.type==='image'&&a.url){LB.push(a);return `<img class="${cls}" src="${a.url}" data-lb="${LB.length-1}">`}
  if(a.type==='video'&&a.url){LB.push(a);return `<video class="${cls}" src="${a.url}#t=0.1" muted preload="metadata" data-lb="${LB.length-1}"></video>`}
  return `<span class="fchip"><i>${esc((a.name.split('.').pop()||'file').slice(0,4))}</i><em>${esc(a.name)}</em></span>`;
}
async function toPart(f){
  const ext=(f.name.split('.').pop()||'').toLowerCase();
  if(f.type.startsWith('image/')&&f.size<8e6)return{name:f.name,type:'image',url:await readAs(f)};
  if(f.type.startsWith('video/'))return{name:f.name,type:'video',url:URL.createObjectURL(f),size:f.size};
  if((f.type.startsWith('text/')||TEXT.test(f.name))&&f.size<3e5)return{name:f.name,type:'text',text:await readAs(f,'text')};
  if(ext==='zip'&&f.size<25e6){try{return{name:f.name,type:'text',text:await zipText(f)}}catch{}}
  return{name:f.name,type:'bin',size:f.size};
}
function renderFiles(){
  E.files.innerHTML=pending.map((a,i)=>`<div class="pf">${thumb(a,'pth')}<button class="x" data-r="${i}" aria-label="Remove">${IC.x}</button></div>`).join('');
  setSend();
}
E.files.onclick=e=>{const r=e.target.closest('[data-r]');if(r){pending.splice(+r.dataset.r,1);renderFiles();return}openLB(e.target)};
E.file.onchange=async()=>{for(const f of E.file.files)pending.push(await toPart(f));E.file.value='';renderFiles()};

/* ---------- lightbox ---------- */
function openLB(el){const i=el.dataset&&el.dataset.lb;if(i===undefined)return;const a=LB[+i];$('#lbBody').innerHTML=a.type==='video'?`<video src="${a.url}" controls autoplay playsinline></video>`:`<img src="${a.url}">`;$('#lb').classList.add('on')}
const closeLB=()=>{$('#lb').classList.remove('on');$('#lbBody').innerHTML=''};
$('#lbClose').onclick=closeLB;$('#lb').onclick=e=>{if(e.target.id==='lb')closeLB()};

/* ---------- messages ---------- */
function addMsg(role,content,atts=[],i){
  const d=document.createElement('div');d.className='m '+(role==='user'?'u':'a');if(i!==undefined)d.dataset.i=i;
  if(role==='user'){d.innerHTML=`<div>${atts.length?`<div class="atts">${atts.map(a=>thumb(a,'th')).join('')}</div>`:''}<div class="bub"></div></div>`;const b=d.querySelector('.bub');b.textContent=content;if(!content)b.remove()}
  else d.innerHTML='<div class="bub"></div><div class="acts"></div>';
  E.msgs.appendChild(d);return d;
}
function finish(d,last){
  const arts=d.querySelectorAll('.art');
  d.querySelector('.acts').innerHTML=`<button data-a="copy" aria-label="Copy">${IC.copy}</button><button data-a="share" aria-label="Share">${IC.share}</button>${last?`<button data-a="regen" aria-label="Try again">${IC.redo}</button>`:''}${arts.length>1?'<button class="zipbar" data-a="zip">Download all (.zip)</button>':''}${last?'<p class="disc">Bharadwaj AI is AI and can make mistakes. Please double-check responses.</p>':''}`;
}
function pin(el){const c=E.chat;c.scrollTo({top:el.getBoundingClientRect().top-c.getBoundingClientRect().top+c.scrollTop-14,behavior:'smooth'})}
function hero(){E.msgs.innerHTML=`<div class="hero"><h1>${G[Math.floor(Math.random()*G.length)]}</h1><p>Ask anything, or attach files and images.</p><div class="chips">${['Explain a concept simply','Help me write an email','Build a small web page','Plan my week'].map(t=>`<button>${t}</button>`).join('')}</div></div>`}
function render(){
  E.msgs.innerHTML='';E.title.textContent=cur?cur.title:'Bharadwaj AI';
  if(!cur||!cur.msgs.length)hero();
  else{cur.msgs.forEach((m,i)=>{const d=addMsg(m.role,m.content,m.role==='user'?(m.names||[]).map(n=>({name:n,type:'bin'})):[],i);if(m.role==='assistant'){d.querySelector('.bub').innerHTML=md(m.content);finish(d,i===cur.msgs.length-1)}});
    E.chat.scrollTop=Math.max(0,E.chat.scrollHeight-E.chat.clientHeight-Math.round(innerHeight*.4)+30)}
  renderHist();
}
function renderHist(){
  const q=E.search.value.toLowerCase();
  const l=chats.filter(c=>!q||c.title.toLowerCase().includes(q)||c.msgs.some(m=>m.content.toLowerCase().includes(q))).sort((a,b)=>b.ts-a.ts);
  E.hist.innerHTML=l.length?'':'<div class="empty-h">No chats found</div>';
  l.forEach(c=>{const d=document.createElement('div');d.className='h'+(cur&&cur.id===c.id?' on':'');d.innerHTML=`<span>${esc(c.title)}</span><button aria-label="Delete">${IC.trash}</button>`;
    d.onclick=()=>{if(busy)return;cur=c;render();closeSide()};
    d.querySelector('button').onclick=e=>{e.stopPropagation();if(busy)return;chats=chats.filter(x=>x!==c);if(cur===c)cur=null;save();render()};
    E.hist.appendChild(d)});
}
const openSide=()=>{E.side.classList.add('open');E.scrim.classList.add('on')};
const closeSide=()=>{E.side.classList.remove('open');E.scrim.classList.remove('on')};
function newChat(){if(busy)return;cur=null;pending=[];renderFiles();render();closeSide();E.input.focus()}
function setSend(){E.send.innerHTML=busy?IC.stop:IC.up;E.send.disabled=!busy&&!(E.input.value.trim()||pending.length)}
function sync(){E.input.style.height='auto';E.input.style.height=Math.min(E.input.scrollHeight,180)+'px';setSend()}

/* ---------- chat engine ---------- */
function buildApi(){
  return cur.msgs.map(m=>{
    if(m.role!=='user'||!m.parts?.length)return{role:m.role,content:m.content+((m.names?.length&&!m.parts)?`\n[Attached earlier: ${m.names.join(', ')}]`:'')};
    let t=m.content;const c=[];
    m.parts.forEach(p=>{if(p.type==='image')c.push({type:'image_url',image_url:{url:p.url}});else if(p.type==='text')t+=`\n\n--- File: ${p.name} ---\n${p.text}`;else t+=`\n\n[Attached ${p.type} file: ${p.name}${p.size?` (${Math.round(p.size/1024)} KB)`:''} - cannot be read by the model]`});
    return{role:'user',content:c.length?[{type:'text',text:t||'Describe this.'},...c]:t};
  });
}
let raf=0,pend=null;
const paint=(b,a)=>{pend=[b,a];if(!raf)raf=requestAnimationFrame(()=>{raf=0;pend[0].innerHTML=md(pend[1])+'<span class="cur"></span>'})};
async function generate(){
  busy=true;setSend();ctrl=new AbortController();
  $$('.disc,[data-a=regen]').forEach(x=>x.remove());
  const d=addMsg('assistant'),b=d.querySelector('.bub');b.innerHTML=SK;
  let acc='',err=null;liveAcc='';
  try{
    const r=await fetch('/api/chat',{method:'POST',headers:{'Content-Type':'application/json'},signal:ctrl.signal,body:JSON.stringify({provider,messages:buildApi()})});
    if(!r.ok){let e='Error '+r.status;try{e=(await r.json()).error||e}catch{}throw new Error(e)}
    if((r.headers.get('content-type')||'').includes('json')){const j=await r.json();acc=j.choices?.[0]?.message?.content||'';if(!acc)throw new Error(JSON.stringify(j).slice(0,300))}
    else{
      const rd=r.body.getReader(),dec=new TextDecoder();let buf='';
      for(;;){const{done,value}=await rd.read();if(done)break;
        buf+=dec.decode(value,{stream:true});const lines=buf.split('\n');buf=lines.pop();
        for(const l of lines){if(!l.startsWith('data:'))continue;const x=l.slice(5).trim();if(x==='[DONE]')continue;
          let j;try{j=JSON.parse(x)}catch{continue}
          if(j.error)throw new Error(j.error.message||JSON.stringify(j.error));
          const t=j.choices?.[0]?.delta?.content;if(t){acc+=t;liveAcc=acc;paint(b,acc)}}}
      if(!acc)throw new Error('Empty response from model');
    }
  }catch(e){if(e.name!=='AbortError')err=navigator.onLine?e.message:"You're offline. Check your connection."}
  cancelAnimationFrame(raf);raf=0;
  if(err){b.innerHTML=(acc?md(acc):'')+`<div class="errbox"><span>${esc(err)}</span><button class="retry">${IC.redo} Retry</button></div>`;d.dataset.err=1}
  else if(acc){b.innerHTML=md(acc);cur.msgs.push({role:'assistant',content:acc,t:Date.now()});d.dataset.i=cur.msgs.length-1;finish(d,true)}
  else{b.innerHTML='<span class="mute">Stopped.</span>'}
  busy=false;ctrl=null;liveAcc='';save();renderHist();setSend();
}
async function sendMsg(){
  const text=E.input.value.trim();if(busy||!(text||pending.length))return;
  if(!cur){cur={id:Date.now()+'',title:(text||pending[0].name).slice(0,40),ts:Date.now(),msgs:[]};chats.push(cur);E.title.textContent=cur.title}
  const att=pending;pending=[];renderFiles();E.input.value='';sync();
  if(!cur.msgs.length)E.msgs.innerHTML='';
  const um={role:'user',content:text,names:att.map(a=>a.name),parts:att,t:Date.now()};
  cur.msgs.push(um);cur.ts=Date.now();
  const ud=addMsg('user',text,att,cur.msgs.length-1);
  pin(ud);generate();
}
E.send.onclick=()=>busy?ctrl&&ctrl.abort():sendMsg();
E.input.oninput=sync;
E.input.onkeydown=e=>{if(e.key==='Enter'&&!e.shiftKey&&matchMedia('(min-width:900px)').matches){e.preventDefault();sendMsg()}};

/* ---------- artifact viewer ---------- */
let V={files:[],i:0,tab:'code'};
const canPrev=f=>/^(html|htm|svg)$/.test(f.lang);
const rx=n=>n.replace(/[.*+?^${}()|[\]\\]/g,'\\$&');
function srcdoc(f){
  if(f.lang==='svg')return `<body style="margin:0;display:grid;place-items:center;min-height:100vh">${f.code}</body>`;
  let h=f.code;
  V.files.forEach(o=>{if(o===f)return;const n=rx(o.name.split('/').pop());
    if(/css$/i.test(o.name))h=h.replace(new RegExp(`<link[^>]*href=["'](?:\\./)?(?:[\\w./-]*/)?${n}["'][^>]*>`,'i'),()=>`<style>${o.code}</style>`);
    if(/js$/i.test(o.name))h=h.replace(new RegExp(`<script[^>]*src=["'](?:\\./)?(?:[\\w./-]*/)?${n}["'][^>]*>\\s*</script>`,'i'),()=>`<script>${o.code}<\/script>`)});
  return h;
}
function openArt(files,i){V={files:nameAll(files),i,tab:null};V.tab=canPrev(V.files[i])?'preview':'code';$('#viewer').classList.add('on');drawV()}
function drawV(){
  const f=V.files[V.i],pv=canPrev(f),body=$('#vBody');
  $('#vTitle').textContent=f.name;$('#vSub').textContent=(f.lang||'code').toUpperCase()+(f.done===false?' · writing…':'');
  $('#tPrev').style.display=pv?'':'none';$('#tPrev').classList.toggle('on',V.tab==='preview'&&pv);$('#tCode').classList.toggle('on',!(V.tab==='preview'&&pv));
  $('#vFiles').innerHTML=V.files.length>1?V.files.map((x,k)=>`<button class="${k===V.i?'on':''}" data-k="${k}">${esc(x.name)}</button>`).join(''):'';
  $('#vZip').style.display=V.files.length>1?'':'none';
  if(V.tab==='preview'&&pv){body.innerHTML='<iframe sandbox="allow-scripts allow-forms allow-modals allow-popups"></iframe>';body.firstChild.srcdoc=srcdoc(f)}
  else{body.innerHTML='<pre class="vc"><code></code></pre>';body.querySelector('code').textContent=f.code}
}
$('#vClose').onclick=()=>{$('#viewer').classList.remove('on');$('#vBody').innerHTML='';$('#vMenu').classList.remove('on')};
$('#tPrev').onclick=()=>{V.tab='preview';drawV()};$('#tCode').onclick=()=>{V.tab='code';drawV()};
$('#vFiles').onclick=e=>{const k=e.target.dataset.k;if(k===undefined)return;V.i=+k;V.tab=canPrev(V.files[V.i])?'preview':'code';drawV()};
$('#vDots').onclick=e=>{e.stopPropagation();$('#vMenu').classList.toggle('on')};
$('#vMenu').onclick=e=>{const a=e.target.dataset.v;if(!a)return;$('#vMenu').classList.remove('on');const f=V.files[V.i];
  if(a==='copy')copyTxt(f.code);
  else if(a==='dl')dl(f.name,new Blob([f.code],{type:'text/plain'}));
  else if(a==='zip')zipFiles(V.files);
  else if(a==='open'){const h=canPrev(f)?srcdoc(f):`<pre style="white-space:pre-wrap;font:14px monospace">${esc(f.code)}</pre>`;window.open(URL.createObjectURL(new Blob([h],{type:'text/html'})),'_blank')}};

/* ---------- message clicks ---------- */
E.msgs.onclick=e=>{
  const t=e.target;
  if(t.classList.contains('cp')){copyTxt(t.nextSibling.textContent);return}
  const art=t.closest('.art');
  if(art){const d=art.closest('.m'),m=cur&&cur.msgs[+d.dataset.i];openArt(fences(m?m.content:liveAcc),+art.dataset.i);return}
  if(t.dataset.lb!==undefined){openLB(t);return}
  const ch=t.closest('.chips button');if(ch){E.input.value=ch.textContent;sync();sendMsg();return}
  const rt=t.closest('.retry');if(rt){if(busy)return;rt.closest('.m').remove();generate();return}
  const ab=t.closest('[data-a]');if(!ab)return;
  const d=ab.closest('.m'),m=cur.msgs[+d.dataset.i],a=ab.dataset.a;
  if(a==='copy')copyTxt(m.content);
  else if(a==='share'){navigator.share?navigator.share({text:m.content}).catch(()=>{}):copyTxt(m.content)}
  else if(a==='zip')zipFiles(nameAll(fences(m.content)));
  else if(a==='regen'){if(busy)return;cur.msgs.pop();d.remove();generate()}
};

/* ---------- long-press menu ---------- */
const menu=document.createElement('div');menu.className='ctx';document.body.appendChild(menu);
const hideMenu=()=>menu.classList.remove('on');
function showMenu(el,x,y){
  const i=+el.dataset.i,m=cur&&cur.msgs[i];if(!m||busy)return;
  menu.innerHTML=`<div class="ct">${m.t?new Date(m.t).toLocaleTimeString([],{hour:'2-digit',minute:'2-digit'}):'Message'}</div><button data-a="copy">Copy message</button><button data-a="sel">Select text</button>${m.role==='user'?'<button data-a="edit">Edit</button>':''}<button data-a="del" class="dng">Delete</button>`;
  menu.classList.add('on');menu.dataset.i=i;menu._el=el;
  const w=menu.offsetWidth,h=menu.offsetHeight;
  menu.style.left=Math.max(10,Math.min(x,innerWidth-w-10))+'px';menu.style.top=Math.max(10,Math.min(y,innerHeight-h-10))+'px';
}
menu.onclick=e=>{
  const a=e.target.dataset.a;if(!a)return;
  const i=+menu.dataset.i,m=cur.msgs[i],el=menu._el;hideMenu();
  if(a==='copy')copyTxt(m.content);
  else if(a==='sel'){el.classList.add('sel');const r=document.createRange();r.selectNodeContents(el.querySelector('.bub'));const g=getSelection();g.removeAllRanges();g.addRange(r)}
  else if(a==='edit'){cur.msgs=cur.msgs.slice(0,i);E.input.value=m.content;save();render();sync();E.input.focus()}
  else if(a==='del'){cur.msgs.splice(i,m.role==='user'&&cur.msgs[i+1]&&cur.msgs[i+1].role==='assistant'?2:1);if(!cur.msgs.length){chats=chats.filter(c=>c!==cur);cur=null}save();render()}
};
let lp;
E.msgs.addEventListener('touchstart',e=>{const el=e.target.closest('.m');if(!el||el.dataset.i===undefined||e.target.closest('button,.th'))return;const t=e.touches[0];lp=setTimeout(()=>showMenu(el,t.clientX,t.clientY),450)},{passive:true});
['touchend','touchmove','touchcancel'].forEach(n=>E.msgs.addEventListener(n,()=>clearTimeout(lp),{passive:true}));
E.msgs.addEventListener('contextmenu',e=>{const el=e.target.closest('.m');if(el&&el.dataset.i!==undefined){e.preventDefault();showMenu(el,e.clientX,e.clientY)}});
document.addEventListener('click',e=>{
  if(!menu.contains(e.target))hideMenu();
  if(!e.target.closest('.m.sel'))$$('.m.sel').forEach(x=>x.classList.remove('sel'));
  if(!e.target.closest('#vMenu,#vDots'))$('#vMenu').classList.remove('on');
  if(!e.target.closest('#pop,#modelBtn'))E.pop.classList.remove('on');
});

/* ---------- model pill, mic, sheets, misc ---------- */
const setModel=()=>{$('#modelName').textContent=MODELS[provider];localStorage.setItem('bh_prov',provider)};
$('#modelBtn').onclick=e=>{e.stopPropagation();E.pop.innerHTML=Object.entries(MODELS).map(([k,v])=>`<button data-p="${k}" class="${k===provider?'on':''}">${v}<span>${k===provider?'✓':''}</span></button>`).join('');E.pop.classList.toggle('on')};
E.pop.onclick=e=>{const b=e.target.closest('[data-p]');if(!b)return;provider=b.dataset.p;setModel();E.pop.classList.remove('on')};
const SR=window.SpeechRecognition||window.webkitSpeechRecognition;let rec=null,base='';
if(!SR)$('#mic').style.display='none';
$('#mic').onclick=()=>{
  if(rec){rec.stop();return}
  rec=new SR();rec.lang=navigator.language||'en-IN';rec.interimResults=true;rec.continuous=true;base=E.input.value?E.input.value+' ':'';
  rec.onresult=e=>{let t='';for(const r of e.results)t+=r[0].transcript;E.input.value=base+t;sync()};
  rec.onend=()=>{rec=null;$('#mic').classList.remove('on')};
  rec.onerror=ev=>{if(ev.error==='not-allowed')toast('Microphone permission do')};
  $('#mic').classList.add('on');rec.start();
};
const sheet=id=>$(id).classList.add('on');
$$('.sheet').forEach(s=>{s.onclick=e=>{if(e.target===s||e.target.closest('[data-close]'))s.classList.remove('on')}});
$('#openSet').onclick=()=>{closeSide();sheet('#setSheet')};
$('#themeSeg').onclick=e=>{const t=e.target.dataset.t;if(!t)return;theme=t;localStorage.setItem('bh_theme',t);applyTheme()};
$('#clearAll').onclick=()=>{if(busy)return;if(confirm('Delete all chats?')){chats=[];cur=null;save();render();$('#setSheet').classList.remove('on')}};
$('#openArts').onclick=()=>{
  closeSide();const items=[];
  chats.slice().sort((a,b)=>b.ts-a.ts).forEach(c=>c.msgs.forEach(m=>{if(m.role!=='assistant')return;const f=fences(m.content);f.forEach((x,i)=>{if(/^(html|htm|svg|jsx|tsx)$/.test(x.lang)||x.name||x.code.split('\n').length>=10)items.push({c,f,i})})}));
  $('#artList').innerHTML=items.length?items.map((it,k)=>{const x=nameAll(it.f)[it.i];return `<button class="ai-row" data-k="${k}"><b>${esc(x.name)}</b><small>${esc(it.c.title)}</small></button>`}).join(''):'<p class="sfoot">No artifacts yet. Ask me to write some code or a web page.</p>';
  $('#artList').onclick=e=>{const b=e.target.closest('[data-k]');if(!b)return;const it=items[+b.dataset.k];$('#artSheet').classList.remove('on');openArt(it.f,it.i)};
  sheet('#artSheet');
};
const modal=$('#modal');
$('#newTop').onclick=()=>{if(busy)return;if(cur&&cur.msgs.length)modal.classList.add('on');else newChat()};
$('#stay').onclick=()=>modal.classList.remove('on');
$('#go').onclick=()=>{modal.classList.remove('on');newChat()};
modal.onclick=e=>{if(e.target===modal)modal.classList.remove('on')};
$('#menu').onclick=openSide;$('#closeSide').onclick=closeSide;E.scrim.onclick=closeSide;$('#newChat').onclick=newChat;
E.search.oninput=renderHist;
const net=()=>$('#off').classList.toggle('on',!navigator.onLine);addEventListener('online',net);addEventListener('offline',net);
applyTheme();setModel();net();render();sync();
