// Bharadwaj AI - Cloudflare Worker (API keys sirf Worker secrets me, frontend me kabhi nahi)
const SYSTEM = "You are Bharadwaj AI, a helpful, friendly and smart assistant. Reply in the same language the user writes in (Hindi, Hinglish or English). Use clean markdown (tables are fine). When you write code, always use fenced blocks and put the file name after the language, like ```html index.html. For multi-file projects give every file its own block with its path. For web pages prefer one self-contained HTML file unless asked otherwise. If the user shares video frames, treat them as frames sampled from a video (no audio).";

const PROVIDERS = {
  nara:   { url: "https://router.bynara.id/v1/chat/completions", key: "NARA_API_KEY",   model: "auto/bynara" },
  groq:   { url: "https://api.groq.com/openai/v1/chat/completions", key: "GROQ_API_KEY", model: "openai/gpt-oss-120b" },
  nvidia: { url: "https://integrate.api.nvidia.com/v1/chat/completions", key: "NVIDIA_API_KEY", model: "meta/llama-3.3-70b-instruct" },
  gemini: { url: "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions", key: "GEMINI_API_KEY", model: "gemini-2.5-flash" },
};

// Models jo images dekh sakte hain (naam se pehchan). Baaki ke liye images hata kar text bheja jata hai.
const VISION = /scout|maverick|vision|llava|vila|pixtral|-vl|gemma-3|gemini|gpt-4o/i;

const json = (o, s = 200) => new Response(JSON.stringify(o), { status: s, headers: { "Content-Type": "application/json" } });

function flatten(msgs) {
  let dropped = false;
  const out = msgs.map((m, i) => {
    if (!Array.isArray(m.content)) return m;
    const txt = m.content.filter(p => p.type === "text").map(p => p.text).join("\n");
    const n = m.content.filter(p => p.type === "image_url").length;
    if (n && i === msgs.length - 1) dropped = true;
    return { ...m, content: txt + (n ? `\n\n[The user attached ${n} image(s) but this model cannot see images. Tell them to switch the model to Gemini to analyse images.]` : "") };
  });
  return { out, dropped };
}

export default {
  async fetch(req, env) {
    const url = new URL(req.url);
    if (url.pathname === "/api/health") {
      return json(Object.fromEntries(Object.entries(PROVIDERS).map(([k, v]) => [k, env[v.key] ? "key set" : "key MISSING"])));
    }
    if (url.pathname === "/api/chat" && req.method === "POST") {
      let body;
      try { body = await req.json(); } catch { return json({ error: "Bad request" }, 400); }
      const { provider = "nara", messages = [] } = body;
      const p = PROVIDERS[provider];
      if (!p) return json({ error: "Unknown provider" }, 400);
      const key = env[p.key];
      if (!key) return json({ error: `${p.key} Worker ke Variables and Secrets me set nahi hai` }, 500);
      const model = env[provider.toUpperCase() + "_MODEL"] || p.model;
      const hasImg = messages.some(m => Array.isArray(m.content) && m.content.some(x => x.type === "image_url"));
      let msgs = messages, dropped = false;
      if (hasImg && !(provider === "nara" || VISION.test(model))) ({ out: msgs, dropped } = flatten(messages));
      const call = m => fetch(p.url, {
        method: "POST",
        headers: { "Content-Type": "application/json", Authorization: "Bearer " + key },
        body: JSON.stringify({ model, stream: true, messages: [{ role: "system", content: SYSTEM }, ...m] }),
      });
      let r;
      try {
        r = await call(msgs);
        if (!r.ok && r.status === 400 && hasImg && !dropped) { ({ out: msgs, dropped } = flatten(messages)); r = await call(msgs); }
      } catch (e) { return json({ error: "Provider se connect nahi hua: " + e.message }, 502); }
      if (!r.ok) return json({ error: (await r.text()).slice(0, 600) }, r.status);
      const h = { "Content-Type": "text/event-stream", "Cache-Control": "no-cache" };
      if (dropped) h["x-images-dropped"] = "1";
      return new Response(r.body, { headers: h });
    }
    return env.ASSETS.fetch(req);
  },
};
