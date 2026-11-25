# Bharadwaj AI

## Deploy (GitHub -> Cloudflare)
1. Is poore folder ko GitHub repo me push karo.
2. Cloudflare -> Workers & Pages -> Create -> Import a repository -> repo select karo.
   Build command khali, Deploy command: `npx wrangler deploy`
3. Worker -> Settings -> Variables and Secrets -> Add (type: Secret):
   - NARA_API_KEY  (NaraRouter key, sk-nry-...)
   - GROQ_API_KEY  (GroqCloud key, gsk_...)
   - GEMINI_API_KEY (Google AI Studio key)
   Optional (Text): NARA_MODEL, GROQ_MODEL, GEMINI_MODEL se model name badal sakte ho.
4. Deploy ke baad upar dropdown se Nara / Groq / Gemini choose karo.

API keys sirf Worker me rehti hain, frontend me kabhi nahi.
