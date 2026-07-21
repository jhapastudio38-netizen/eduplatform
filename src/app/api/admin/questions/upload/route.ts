import { NextResponse, NextRequest } from "next/server";
import { db } from "@/lib/db";
import { getCurrentUser } from "@/lib/session";
import { audit } from "@/lib/audit";
import type { QuestionType, Difficulty } from "@/types";

/**
 * POST /api/admin/questions/upload  (multipart/form-data)
 *   - file: uploaded file (.json | .csv | .txt | .md)
 *   - chapterId: optional
 *
 * The parser auto-detects format by extension and tries multiple
 * shapes. Imported questions are stored without correct answers when
 * the source does not include them — admin can edit afterwards.
 */

interface ParsedQ {
  type: QuestionType;
  difficulty?: Difficulty;
  stem: string;
  options?: string[];
  correctAnswer?: string;
  explanation?: string;
}

const DIFFICULTIES: Difficulty[] = ["EASY", "MEDIUM", "HARD"];
const TYPES: QuestionType[] = [
  "SINGLE_CHOICE", "MULTIPLE_CHOICE", "TRUE_FALSE",
  "ONE_WORD", "SHORT_ANSWER", "LONG_ANSWER", "FILL_BLANK",
];

function inferType(raw: { options?: unknown; answer?: unknown }): QuestionType {
  if (Array.isArray(raw.options)) {
    if (Array.isArray(raw.answer) && raw.answer.length > 1) return "MULTIPLE_CHOICE";
    return "SINGLE_CHOICE";
  }
  if (typeof raw.answer === "string" && raw.answer.split(" ").length <= 2) return "ONE_WORD";
  return "SHORT_ANSWER";
}

function parseJSON(text: string): ParsedQ[] {
  const data = JSON.parse(text);
  const arr = Array.isArray(data) ? data : data.questions || [];
  return arr.map((q: Record<string, unknown>) => {
    const type = (TYPES.includes(q.type as QuestionType) ? q.type : inferType(q)) as QuestionType;
    const difficulty = (DIFFICULTIES.includes(q.difficulty as Difficulty) ? q.difficulty : "MEDIUM") as Difficulty;
    const stem = String(q.stem || q.question || q.text || "").trim();
    const options = Array.isArray(q.options) ? q.options.map(String) : undefined;
    const correctAnswer = q.correctAnswer !== undefined ? JSON.stringify(q.correctAnswer) : q.answer !== undefined ? JSON.stringify(q.answer) : undefined;
    const explanation = q.explanation ? String(q.explanation) : undefined;
    return { type, difficulty, stem, options, correctAnswer, explanation };
  }).filter((q) => q.stem);
}

function parseCSV(text: string): ParsedQ[] {
  // Minimal CSV parser — handles quoted fields
  const lines = text.split(/\r?\n/).filter((l) => l.trim());
  if (lines.length === 0) return [];
  const header = splitCSVLine(lines[0]).map((s) => s.toLowerCase().trim());

  // Find column indices
  const idx = {
    stem: header.findIndex((h) => /question|stem|text/.test(h)),
    type: header.findIndex((h) => h === "type"),
    difficulty: header.findIndex((h) => /diff/.test(h)),
    options: header.findIndex((h) => h === "options"),
    answer: header.findIndex((h) => /answer|correct/.test(h)),
    explanation: header.findIndex((h) => /expl/.test(h)),
  };

  const out: ParsedQ[] = [];
  for (let i = 1; i < lines.length; i++) {
    const cols = splitCSVLine(lines[i]);
    const stem = (idx.stem >= 0 ? cols[idx.stem] : cols[0])?.trim();
    if (!stem) continue;
    const rawType = idx.type >= 0 ? cols[idx.type]?.trim().toUpperCase() : "";
    const type = (TYPES.includes(rawType as QuestionType) ? rawType : "SINGLE_CHOICE") as QuestionType;
    const rawDiff = idx.difficulty >= 0 ? cols[idx.difficulty]?.trim().toUpperCase() : "";
    const difficulty = (DIFFICULTIES.includes(rawDiff as Difficulty) ? rawDiff : "MEDIUM") as Difficulty;
    const options = idx.options >= 0 && cols[idx.options]
      ? cols[idx.options].split("|").map((s) => s.trim()).filter(Boolean)
      : undefined;
    const correctAnswer = idx.answer >= 0 && cols[idx.answer] ? cols[idx.answer].trim() : undefined;
    const explanation = idx.explanation >= 0 ? cols[idx.explanation] : undefined;
    out.push({ type, difficulty, stem, options, correctAnswer, explanation });
  }
  return out;
}

function splitCSVLine(line: string): string[] {
  const out: string[] = [];
  let cur = "";
  let inQuotes = false;
  for (let i = 0; i < line.length; i++) {
    const c = line[i];
    if (c === '"') {
      if (inQuotes && line[i + 1] === '"') { cur += '"'; i++; }
      else inQuotes = !inQuotes;
    } else if (c === "," && !inQuotes) {
      out.push(cur); cur = "";
    } else cur += c;
  }
  out.push(cur);
  return out;
}

/**
 * TXT / MD parser — heuristics:
 *   - Lines starting with "Q:" or "Question N:" begin a new question
 *   - Lines starting with "A)" "B)" etc. are options
 *   - Line "Answer:" or "Ans:" defines the answer
 *   - "Type:" and "Difficulty:" set metadata
 *   - "Explanation:" sets explanation
 * If no "Q:" markers found, treat each non-empty line as a one-word / short-answer stem.
 */
function parseText(text: string): ParsedQ[] {
  const lines = text.split(/\r?\n/);
  const out: ParsedQ[] = [];
  let cur: Partial<ParsedQ> & { options?: string[] } = {};

  function flush() {
    if (cur.stem) {
      out.push({
        type: cur.type || (cur.options?.length ? "SINGLE_CHOICE" : "ONE_WORD"),
        difficulty: cur.difficulty || "MEDIUM",
        stem: cur.stem,
        options: cur.options,
        correctAnswer: cur.correctAnswer,
        explanation: cur.explanation,
      });
    }
    cur = {};
  }

  for (const raw of lines) {
    const line = raw.trim();
    if (!line) continue;
    const lower = line.toLowerCase();

    if (/^(q[:.)]|question\s+\d+[:.)]?)/i.test(line)) {
      flush();
      cur.stem = line.replace(/^(q[:.)]|question\s+\d+[:.)]?)\s*/i, "");
    } else if (/^[a-j]\)\s/.test(lower)) {
      cur.options = cur.options || [];
      cur.options.push(line.replace(/^[a-j]\)\s*/i, ""));
    } else if (lower.startsWith("answer:") || lower.startsWith("ans:")) {
      cur.correctAnswer = line.replace(/^(answer|ans):\s*/i, "").trim();
    } else if (lower.startsWith("type:")) {
      const t = line.replace(/^type:\s*/i, "").trim().toUpperCase().replace(/ /g, "_");
      if (TYPES.includes(t as QuestionType)) cur.type = t as QuestionType;
    } else if (lower.startsWith("difficulty:")) {
      const d = line.replace(/^difficulty:\s*/i, "").trim().toUpperCase();
      if (DIFFICULTIES.includes(d as Difficulty)) cur.difficulty = d as Difficulty;
    } else if (lower.startsWith("explanation:")) {
      cur.explanation = line.replace(/^explanation:\s*/i, "");
    } else if (!cur.stem) {
      // standalone line → treat as one-word / short-answer stem
      cur.stem = line;
      flush();
    }
  }
  flush();
  return out;
}

export async function POST(req: NextRequest) {
  const user = await getCurrentUser();
  if (!user || user.role !== "ADMIN") return NextResponse.json({ error: "Forbidden" }, { status: 403 });

  const form = await req.formData();
  const file = form.get("file");
  const chapterId = (form.get("chapterId") as string) || null;

  if (!(file instanceof File)) {
    return NextResponse.json({ error: "No file uploaded" }, { status: 400 });
  }
  if (file.size > 5 * 1024 * 1024) {
    return NextResponse.json({ error: "File too large (max 5 MB)" }, { status: 413 });
  }

  const text = await file.text();
  const ext = file.name.toLowerCase().split(".").pop() || "";

  let parsed: ParsedQ[] = [];
  try {
    if (ext === "json") parsed = parseJSON(text);
    else if (ext === "csv") parsed = parseCSV(text);
    else parsed = parseText(text);
  } catch (e) {
    return NextResponse.json(
      { error: `Parse error: ${e instanceof Error ? e.message : "invalid format"}` },
      { status: 422 },
    );
  }

  if (parsed.length === 0) {
    return NextResponse.json({ error: "No questions detected in file" }, { status: 422 });
  }

  // Bulk insert (transactional)
  const created = await db.$transaction(
    parsed.map((q) =>
      db.question.create({
        data: {
          chapterId,
          type: q.type,
          difficulty: q.difficulty || "MEDIUM",
          stem: q.stem,
          options: q.options ? JSON.stringify(q.options) : null,
          correctAnswer: q.correctAnswer || null,
          explanation: q.explanation || null,
          sourceFile: file.name,
        },
      }),
    ),
  );

  await audit({
    actorId: user.id,
    action: "upload_questions",
    entity: "Question",
    metadata: { file: file.name, count: created.length, chapterId },
  });

  return NextResponse.json({ imported: created.length });
}
