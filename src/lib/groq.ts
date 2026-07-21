/**
 * Groq API helper.
 * Uses Groq's OpenAI-compatible /chat/completions endpoint.
 * Docs: https://console.groq.com/docs
 *
 * In dev without a key, returns deterministic mock questions so the UI flow
 * can be tested end-to-end.
 */
import { CONFIG } from "@/lib/config";
import type { QuestionType, Difficulty, Question } from "@/types";

interface GenParams {
  topic: string;
  count: number;
  type: QuestionType;
  difficulty: Difficulty;
}

interface GroqMessage {
  role: "system" | "user" | "assistant";
  content: string;
}

export async function generateQuestionsWithGroq(params: GenParams): Promise<Question[]> {
  if (!CONFIG.groq.apiKey) {
    return mockQuestions(params);
  }

  const systemPrompt = `You are an expert exam author. Generate high-quality educational questions.
Return ONLY a JSON array — no prose, no markdown fences. Each item MUST have:
  - stem: string (the question)
  - options: string[] (for choice-type questions; omit otherwise)
  - correctAnswer: string (for choice questions, exactly matches one of the options)
                     OR string (for one-word / fill-blank)
                     OR string[] (for multiple-choice — list of correct options)
  - explanation: string (1-2 sentence explanation)
Question type requested: ${params.type}
Difficulty: ${params.difficulty}
Number of questions: ${params.count}`;

  const userPrompt = `Topic: ${params.topic}

Generate ${params.count} ${params.type.replace(/_/g, " ").toLowerCase()} questions at ${params.difficulty.toLowerCase()} difficulty.
Be specific to the topic. Avoid duplicates. Vary the angle across questions.`;

  const messages: GroqMessage[] = [
    { role: "system", content: systemPrompt },
    { role: "user", content: userPrompt },
  ];

  const res = await fetch(CONFIG.groq.baseUrl, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${CONFIG.groq.apiKey}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      model: CONFIG.groq.model,
      messages,
      temperature: 0.7,
      max_tokens: 4000,
      response_format: { type: "json_object" },
    }),
  });

  if (!res.ok) {
    const text = await res.text();
    throw new Error(`Groq error ${res.status}: ${text.slice(0, 200)}`);
  }

  const data = await res.json();
  const content = data.choices?.[0]?.message?.content || "[]";
  let parsed: unknown;
  try {
    parsed = JSON.parse(content);
  } catch {
    // Groq sometimes wraps arrays inside an object key
    const match = content.match(/\[[\s\S]*\]/);
    parsed = match ? JSON.parse(match[0]) : [];
  }
  const arr = Array.isArray(parsed) ? parsed : (parsed as { questions?: unknown[] }).questions || [];

  return (arr as Record<string, unknown>[]).slice(0, params.count).map((q, i) => ({
    id: `ai-${Date.now()}-${i}`,
    type: params.type,
    difficulty: params.difficulty,
    stem: String(q.stem || q.question || "").trim(),
    options: Array.isArray(q.options) ? (q.options as string[]).map(String) : null,
    correctAnswer: q.correctAnswer !== undefined
      ? (typeof q.correctAnswer === "string" ? q.correctAnswer : JSON.stringify(q.correctAnswer))
      : null,
    explanation: q.explanation ? String(q.explanation) : null,
    tags: [],
    aiGenerated: true,
  })).filter((q) => q.stem);
}

function mockQuestions(params: GenParams): Question[] {
  // Dev fallback when no Groq key is set
  const out: Question[] = [];
  for (let i = 1; i <= params.count; i++) {
    const opts = params.type === "SINGLE_CHOICE" || params.type === "MULTIPLE_CHOICE" || params.type === "TRUE_FALSE"
      ? params.type === "TRUE_FALSE" ? ["True", "False"] : [`Option A`, `Option B`, `Option C`, `Option D`]
      : null;
    out.push({
      id: `mock-${Date.now()}-${i}`,
      type: params.type,
      difficulty: params.difficulty,
      stem: `[MOCK] Question ${i} about "${params.topic}". (Configure GROQ_API_KEY in .env to get real questions.)`,
      options: opts,
      correctAnswer: opts ? opts[0] : "Sample answer",
      explanation: "Mock explanation. Set GROQ_API_KEY to enable real AI generation.",
      tags: [],
      aiGenerated: true,
    });
  }
  return out;
}
