// Bharadwaj AI - Cloudflare Worker (API keys yahin Worker secrets me rehte hain, frontend me kabhi nahi)
const SYSTEM = "You are Bharadwaj AI, a helpful, friendly and smart assistant. Reply in the same language the user writes in (Hindi, Hinglish or English). Use clean markdown.";

const PROVIDERS = {
  nara:   { url: "https://router.bynara.id/v1/chat/completions", key: "NARA_API_KEY",   model: "auto/bynara" },
  groq:   { url: "https://api.groq.com/openai/v1/chat/completions", key: "GROQ_API_KEY", model: "openai/gpt-oss-120b" },
  gemini: { url: "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions", key: "GEMINI_API_KEY", model: "gemini-2.5-flash" },
};

const json = (o, s = 200) => new Response(JSON.stringify(o), { status: s, headers: { "Content-Type": "application/json" } });

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
      const model = env[provider.toUpperCase() + "_MODEL"] || p.model; // optional override
      let r;
      try { r = await fetch(p.url, {
        method: "POST",
        headers: { "Content-Type": "application/json", Authorization: "Bearer " + key },
        body: JSON.stringify({ model, stream: true, messages: [{ role: "system", content: SYSTEM }, ...messages] }),
      }); } catch (e) { return json({ error: "Provider se connect nahi hua: " + e.message }, 502); }
      if (!r.ok) return json({ error: (await r.text()).slice(0, 600) }, r.status);
      return new Response(r.body, { headers: { "Content-Type": "text/event-stream", "Cache-Control": "no-cache" } });
    }
    return env.ASSETS.fetch(req);
  },
};
