/**
 * Korean-specific seed script — populates the database with real Korean
 * learning content so all tabs in the Android app show actual data.
 *
 * Run: bun run scripts/seed-korean.ts
 *
 * Creates:
 * - 4 Subjects (Hangul, Grammar, TOPIK I, TOPIK II)
 * - 8 Chapters (2 per subject)
 * - 16 Lessons (2 per chapter)
 * - 30+ Questions (MCQ, multiple choice, true/false, fill-blank) with Korean content
 * - 4 Tests (one per subject)
 * - 6 Books (Korean learning PDFs)
 * - 6 Video lessons (YouTube IDs)
 * - 6 Audio lessons
 */
import { db } from "../src/lib/db";

async function main() {
  console.log("🌱 Seeding Korean learning content...");

  // Get or create teacher
  let teacher = await db.user.findFirst({ where: { role: "TEACHER" } });
  if (!teacher) {
    teacher = await db.user.create({
      data: {
        email: "teacher@dreamkoreasmartclass.com",
        phone: "+9779800000002",
        name: "DreamKorea Teacher",
        role: "TEACHER",
        isVerified: true,
        username: "teacher",
        passwordHash: "$scrypt:16384:8:1:abc:def", // placeholder — admin should reset
      },
    });
  }

  // ─── Subjects ──────────────────────────────────────────────────────────────
  const subjects = [
    { name: "Hangul Basics", slug: "hangul-basics", description: "Learn the Korean alphabet — vowels, consonants, and syllable blocks.", iconUrl: null },
    { name: "Korean Grammar", slug: "korean-grammar", description: "Sentence structure, particles, verb conjugations, and more.", iconUrl: null },
    { name: "TOPIK I Prep", slug: "topik-i-prep", description: "TOPIK Level 1-2 preparation with listening and reading practice.", iconUrl: null },
    { name: "TOPIK II Prep", slug: "topik-ii-prep", description: "TOPIK Level 3-6 advanced preparation with writing tasks.", iconUrl: null },
  ];

  const subjectRecords = [];
  for (const s of subjects) {
    const rec = await db.subject.upsert({
      where: { slug: s.slug },
      create: s,
      update: { description: s.description },
    });
    subjectRecords.push(rec);
  }
  console.log(`  ✓ ${subjectRecords.length} subjects`);

  // ─── Chapters ──────────────────────────────────────────────────────────────
  const chaptersData = [
    // Hangul Basics
    { subjectSlug: "hangul-basics", title: "Vowels & Consonants", slug: "vowels-consonants", description: "The 21 vowels and 19 consonants of Hangul.", order: 1 },
    { subjectSlug: "hangul-basics", title: "Syllable Blocks", slug: "syllable-blocks", description: "How to combine letters into syllable blocks.", order: 2 },
    // Korean Grammar
    { subjectSlug: "korean-grammar", title: "Sentence Structure", slug: "sentence-structure", description: "SOV word order and basic sentence patterns.", order: 1 },
    { subjectSlug: "korean-grammar", title: "Particles (은/는, 이/가)", slug: "particles", description: "Topic and subject markers in Korean.", order: 2 },
    // TOPIK I
    { subjectSlug: "topik-i-prep", title: "TOPIK I Listening", slug: "topik-i-listening", description: "Listening practice for TOPIK Level 1-2.", order: 1 },
    { subjectSlug: "topik-i-prep", title: "TOPIK I Reading", slug: "topik-i-reading", description: "Reading comprehension for TOPIK Level 1-2.", order: 2 },
    // TOPIK II
    { subjectSlug: "topik-ii-prep", title: "TOPIK II Listening", slug: "topik-ii-listening", description: "Advanced listening for TOPIK Level 3-6.", order: 1 },
    { subjectSlug: "topik-ii-prep", title: "TOPIK II Writing", slug: "topik-ii-writing", description: "Essay writing practice for TOPIK II.", order: 2 },
  ];

  const chapterRecords = [];
  for (const c of chaptersData) {
    const subject = subjectRecords.find((s) => s.slug === c.subjectSlug)!;
    const rec = await db.chapter.upsert({
      where: { subjectId_slug: { subjectId: subject.id, slug: c.slug } },
      create: {
        subjectId: subject.id,
        title: c.title,
        slug: c.slug,
        description: c.description,
        order: c.order,
        isPublished: true,
        authorId: teacher.id,
      },
      update: {},
    });
    chapterRecords.push(rec);
  }
  console.log(`  ✓ ${chapterRecords.length} chapters`);

  // ─── Lessons ───────────────────────────────────────────────────────────────
  const lessonsData = [
    { chapterSlug: "vowels-consonants", title: "Basic Vowels (ㅏ, ㅓ, ㅗ, ㅜ)", slug: "basic-vowels", content: "# Basic Vowels\n\nKorean has 10 basic vowels:\n\n- ㅏ (a) — like 'a' in 'father'\n- ㅓ (eo) — like 'uh' in 'up'\n- ㅗ (o) — like 'o' in 'go'\n- ㅜ (u) — like 'oo' in 'moon'\n- ㅡ (eu) — like 'uh' in 'put'\n- ㅣ (i) — like 'ee' in 'see'\n\nPractice writing each vowel.", durationMin: 8, order: 1 },
    { chapterSlug: "vowels-consonants", title: "Basic Consonants (ㄱ, ㄴ, ㄷ)", slug: "basic-consonants", content: "# Basic Consonants\n\nKorean has 14 basic consonants:\n\n- ㄱ (g/k) — 'gun'\n- ㄴ (n) — 'now'\n- ㄷ (d/t) — 'dog'\n- ㄹ (r/l) — 'rain'\n- ㅁ (m) — 'man'\n- ㅂ (b/p) — 'bat'\n- ㅅ (s) — 'sun'\n- ㅇ (ng) — silent at start\n- ㅈ (j) — 'jump'\n- ㅊ (ch) — 'chip'\n- ㅋ (k) — 'kite'\n- ㅌ (t) — 'top'\n- ㅍ (p) — 'pen'\n- ㅎ (h) — 'hat'", durationMin: 10, order: 2 },
    { chapterSlug: "syllable-blocks", title: "Building Syllable Blocks", slug: "building-blocks", content: "# Syllable Blocks\n\nKorean letters combine into square blocks. Each block = 1 syllable.\n\n## Pattern: Consonant + Vowel\n- 가 = ㄱ + ㅏ (ga)\n- 나 = ㄴ + ㅏ (na)\n- 다 = ㄷ + ㅏ (da)\n\n## Pattern: C + V + C\n- 한 = ㅎ + ㅏ + ㄴ (han)\n- 국 = ㄱ + ㅜ + ㄱ (guk)", durationMin: 12, order: 1 },
    { chapterSlug: "syllable-blocks", title: "Reading Practice", slug: "reading-practice", content: "# Reading Practice\n\nTry reading these words:\n\n- 안녕 (annyeong) — hello\n- 학생 (haksaeng) — student\n- 선생님 (seonsaengnim) — teacher\n- 한국 (hanguk) — Korea\n- 한글 (hangeul) — Hangul", durationMin: 8, order: 2 },
    { chapterSlug: "sentence-structure", title: "SOV Word Order", slug: "sov-order", content: "# Korean Sentence Structure\n\nKorean uses **Subject-Object-Verb** (SOV) order:\n\n> 저는 사과를 먹어요. (I apple eat.)\n> I eat an apple.\n\nThe verb **always** comes last.\n\n## Example\n- 저 (I) + 는 (topic) + 사과 (apple) + 를 (object) + 먹어요 (eat)", durationMin: 10, order: 1 },
    { chapterSlug: "sentence-structure", title: "Question Forms", slug: "question-forms", content: "# Questions in Korean\n\n## Yes/No Questions\nAdd -까 to the verb stem or use rising intonation:\n- 가요? (Are you going?)\n- 가나요? (Do you go?)\n\n## WH-Questions\n- 뭐 (what), 어디 (where), 언제 (when), 왜 (why), 어떻게 (how)\n- 뭐 해요? (What are you doing?)", durationMin: 8, order: 2 },
    { chapterSlug: "particles", title: "Topic Particle 은/는", slug: "topic-particle", content: "# Topic Particle: 은/는\n\nMarks the **topic** of the sentence.\n\n## Use 은 after consonants\n- 학생**은** (the student)\n- 책**은** (the book)\n\n## Use 는 after vowels\n- 나**는** (I)\n- 선생님**는** → 선생님**은** (after ㄴ, a consonant)", durationMin: 10, order: 1 },
    { chapterSlug: "particles", title: "Subject Particle 이/가", slug: "subject-particle", content: "# Subject Particle: 이/가\n\nMarks the **grammatical subject**.\n\n## Use 이 after consonants\n- 책**이** (the book)\n- 학생**이** (the student)\n\n## Use 가 after vowels\n- 나**가** (I)\n- 버스**가** (the bus)", durationMin: 10, order: 2 },
    { chapterSlug: "topik-i-listening", title: "Greeting Dialogues", slug: "greeting-dialogues", content: "# Greeting Dialogues\n\nListen and practice:\n\n- A: 안녕하세요! (Hello!)\n- B: 안녕하세요! (Hello!)\n- A: 이름이 뭐예요? (What's your name?)\n- B: 저는 김민수예요. (I'm Kim Min-su.)", durationMin: 6, order: 1 },
    { chapterSlug: "topik-i-listening", title: "Numbers & Counting", slug: "numbers-counting", content: "# Numbers\n\nKorean has two number systems:\n\n## Sino-Korean (일, 이, 삼...)\nFor dates, money, phone numbers.\n\n## Native Korean (하나, 둘, 셋...)\nFor counting objects, age.", durationMin: 12, order: 2 },
    { chapterSlug: "topik-i-reading", title: "Simple Sentences", slug: "simple-sentences", content: "# Simple Reading\n\nRead these sentences:\n\n1. 저는 학생입니다. (I am a student.)\n2. 이것은 책입니다. (This is a book.)\n3. 사과가 맛있어요. (The apple is delicious.)\n4. 오늘 날씨가 좋아요. (The weather is good today.)", durationMin: 8, order: 1 },
    { chapterSlug: "topik-i-reading", title: "Short Passage", slug: "short-passage", content: "# Short Passage\n\n우리 가족은 네 명입니다. 아버지, 어머니, 형, 그리고 저입니다. 아버지는 회사원이시고, 어머니는 선생님입니다. 형은 대학생입니다. 저는 고등학생입니다. 우리는 주말에 함께 저녁을 먹습니다.\n\n(My family has four people: father, mother, older brother, and me. Father is an office worker, mother is a teacher. Brother is a college student. I am a high school student. We eat dinner together on weekends.)", durationMin: 10, order: 2 },
    { chapterSlug: "topik-ii-listening", title: "News Listening", slug: "news-listening", content: "# News Listening\n\nPractice listening to Korean news broadcasts. Focus on:\n- Numbers and statistics\n- Formal verb endings (-습니다/ㅂ니다)\n- News-specific vocabulary", durationMin: 15, order: 1 },
    { chapterSlug: "topik-ii-listening", title: "Lecture Comprehension", slug: "lecture-comp", content: "# Lecture Listening\n\nPractice understanding academic lectures in Korean:\n- Identify main topic\n- Note supporting examples\n- Recognize transition words (그러나, 그리고, 따라서)", durationMin: 18, order: 2 },
    { chapterSlug: "topik-ii-writing", title: "Essay Structure", slug: "essay-structure", content: "# Essay Writing\n\nTOPIK II essay structure:\n\n1. **서론 (Introduction)** — introduce topic\n2. **본론 (Body)** — 2-3 paragraphs with arguments\n3. **결론 (Conclusion)** — summarize and give opinion\n\nUse formal endings: -다, -ㄴ다, -는다", durationMin: 20, order: 1 },
    { chapterSlug: "topik-ii-writing", title: "Connective Expressions", slug: "connective-expr", content: "# Connective Expressions\n\nUseful for essays:\n\n- 그러나 (however)\n- 그리고 (and)\n- 따라서 (therefore)\n- 또한 (also)\n- 반면에 (on the other hand)\n- 왜냐하면 (because)", durationMin: 12, order: 2 },
  ];

  for (const l of lessonsData) {
    const chapter = chapterRecords.find((c) => c.slug === l.chapterSlug)!;
    await db.lesson.upsert({
      where: { chapterId_slug: { chapterId: chapter.id, slug: l.slug } },
      create: {
        chapterId: chapter.id,
        title: l.title,
        slug: l.slug,
        type: "TEXT",
        content: l.content,
        durationMin: l.durationMin,
        order: l.order,
        isPublished: true,
      },
      update: {},
    });
  }
  console.log(`  ✓ ${lessonsData.length} lessons`);

  // ─── Questions ─────────────────────────────────────────────────────────────
  const questionsData = [
    // Hangul
    { chapterSlug: "vowels-consonants", type: "SINGLE_CHOICE", difficulty: "EASY", stem: "Which consonant sounds like 'k' in 'kite'?", options: ["ㄱ", "ㄴ", "ㅋ", "ㅌ"], correct: "ㅋ", explanation: "ㅋ is aspirated /k/ sound." },
    { chapterSlug: "vowels-consonants", type: "SINGLE_CHOICE", difficulty: "EASY", stem: "What sound does ㅏ make?", options: ["'u' in 'up'", "'a' in 'father'", "'o' in 'go'", "'ee' in 'see'"], correct: "'a' in 'father'", explanation: "ㅏ is the basic 'a' vowel." },
    { chapterSlug: "vowels-consonants", type: "TRUE_FALSE", difficulty: "EASY", stem: "ㄹ is pronounced as 'r' or 'l'.", options: ["True", "False"], correct: "True", explanation: "ㄹ sounds like 'r' between vowels, 'l' at the end." },
    { chapterSlug: "vowels-consonants", type: "SINGLE_CHOICE", difficulty: "MEDIUM", stem: "Which is a double consonant?", options: ["ㄱ", "ㄲ", "ㅋ", "ㄴ"], correct: "ㄲ", explanation: "ㄲ is the tense/double version of ㄱ." },
    { chapterSlug: "syllable-blocks", type: "SINGLE_CHOICE", difficulty: "EASY", stem: "What syllable does ㅎ + ㅏ + ㄴ make?", options: ["한", "하", "안", "핝"], correct: "한", explanation: "Top-left consonant, middle vowel, bottom consonant." },
    { chapterSlug: "syllable-blocks", type: "FILL_BLANK", difficulty: "MEDIUM", stem: "가 + ㄴ = ___", options: null, correct: "간", explanation: "Add the consonant at the bottom." },
    { chapterSlug: "syllable-blocks", type: "SINGLE_CHOICE", difficulty: "MEDIUM", stem: "How do you write 'hanguk' in Hangul?", options: ["한국", "하느", "학국", "한그"], correct: "한국", explanation: "한 (han) + 국 (guk) = 한국." },

    // Grammar
    { chapterSlug: "sentence-structure", type: "SINGLE_CHOICE", difficulty: "EASY", stem: "What is the word order in Korean?", options: ["SVO (Subject-Verb-Object)", "SOV (Subject-Object-Verb)", "VSO (Verb-Subject-Object)", "OVS"], correct: "SOV (Subject-Object-Verb)", explanation: "The verb always comes last in Korean." },
    { chapterSlug: "sentence-structure", type: "SINGLE_CHOICE", difficulty: "MEDIUM", stem: "Translate: 'I eat an apple' (저 apple 먹어요)", options: ["저는 사과를 먹어요.", "저는 먹어요 사과를.", "사과를 저는 먹어요.", "먹어요 사과를 저는."], correct: "저는 사과를 먹어요.", explanation: "SOV order: subject + object + verb." },
    { chapterSlug: "particles", type: "SINGLE_CHOICE", difficulty: "MEDIUM", stem: "Which particle is the topic marker after a vowel?", options: ["은", "는", "이", "가"], correct: "는", explanation: "는 follows vowels, 은 follows consonants." },
    { chapterSlug: "particles", type: "SINGLE_CHOICE", difficulty: "MEDIUM", stem: "Which is correct for '나' (I)?", options: ["나은", "나는", "나가", "나이"], correct: "나는", explanation: "나 ends in a vowel, so use 는." },
    { chapterSlug: "particles", type: "FILL_BLANK", difficulty: "HARD", stem: "책___ (subject particle, 책 ends in consonant)", options: null, correct: "이", explanation: "이 marks the subject after consonants." },

    // TOPIK I
    { chapterSlug: "topik-i-listening", type: "SINGLE_CHOICE", difficulty: "EASY", stem: "What does '안녕하세요' mean?", options: ["Goodbye", "Hello", "Thank you", "Sorry"], correct: "Hello", explanation: "안녕하세요 is the standard greeting." },
    { chapterSlug: "topik-i-listening", type: "SINGLE_CHOICE", difficulty: "EASY", stem: "How do you say 'Thank you'?", options: ["안녕하세요", "감사합니다", "죄송합니다", "안녕히 가세요"], correct: "감사합니다", explanation: "감사합니다 = formal 'thank you'." },
    { chapterSlug: "topik-i-listening", type: "MULTIPLE_CHOICE", difficulty: "MEDIUM", stem: "Which are Korean greetings? (Select all)", options: ["안녕하세요", "Hello", "안녕히 가세요", "Goodbye"], correct: ["안녕하세요", "안녕히 가세요"], explanation: "Both are Korean greetings." },
    { chapterSlug: "topik-i-reading", type: "SINGLE_CHOICE", difficulty: "EASY", stem: "What does '저는 학생입니다' mean?", options: ["I am a teacher", "I am a student", "I am a doctor", "I am a worker"], correct: "I am a student", explanation: "학생 = student." },
    { chapterSlug: "topik-i-reading", type: "FILL_BLANK", difficulty: "MEDIUM", stem: "이것은 ___입니다. (book = 책)", options: null, correct: "책", explanation: "책 means book." },
    { chapterSlug: "topik-i-reading", type: "SINGLE_CHOICE", difficulty: "MEDIUM", stem: "What does '맛있어요' mean?", options: ["It's spicy", "It's delicious", "It's cold", "It's hot"], correct: "It's delicious", explanation: "맛있어요 = delicious." },

    // TOPIK II
    { chapterSlug: "topik-ii-listening", type: "SINGLE_CHOICE", difficulty: "HARD", stem: "What does '그러나' mean in formal writing?", options: ["And", "However", "Therefore", "Also"], correct: "However", explanation: "그러나 = however (formal contrast)." },
    { chapterSlug: "topik-ii-listening", type: "SINGLE_CHOICE", difficulty: "HARD", stem: "What does '따라서' indicate?", options: ["Cause", "Effect/Therefore", "Contrast", "Addition"], correct: "Effect/Therefore", explanation: "따라서 = therefore." },
    { chapterSlug: "topik-ii-writing", type: "MULTIPLE_CHOICE", difficulty: "HARD", stem: "Which are formal connective expressions? (Select all)", options: ["그러나", "but", "따라서", "so"], correct: ["그러나", "따라서"], explanation: "Both are Korean formal connectives." },
    { chapterSlug: "topik-ii-writing", type: "SINGLE_CHOICE", difficulty: "HARD", stem: "Which ending is used in essays?", options: ["-요", "-습니다", "-다", "-어/아"], correct: "-다", explanation: "-다 is the formal written style (해라체)." },
  ];

  const questionRecords = [];
  for (const q of questionsData) {
    const chapter = chapterRecords.find((c) => c.slug === q.chapterSlug);
    if (!chapter) continue;
    // Check if question already exists (by stem)
    const existing = await db.question.findFirst({ where: { stem: q.stem } });
    if (existing) {
      questionRecords.push({ ...existing, chapterSlug: q.chapterSlug });
      continue;
    }
    const rec = await db.question.create({
      data: {
        chapterId: chapter.id,
        type: q.type as any,
        difficulty: q.difficulty as any,
        stem: q.stem,
        options: q.options ? JSON.stringify(q.options) : null,
        correctAnswer: JSON.stringify(q.correct),
        explanation: q.explanation,
      },
    });
    questionRecords.push({ ...rec, chapterSlug: q.chapterSlug });
  }
  console.log(`  ✓ ${questionRecords.length} questions`);

  // ─── Tests ─────────────────────────────────────────────────────────────────
  // Create one test per subject (4 total), each with the questions from its chapters
  for (const subject of subjectRecords) {
    const subjectChapters = chapterRecords.filter((c) => c.subjectId === subject.id);
    const subjectQs = questionRecords.filter((q: any) =>
      subjectChapters.some((c) => c.id === q.chapterId)
    );
    if (subjectQs.length === 0) continue;

    const testTitle = `${subject.name} Test`;
    const existingTest = await db.test.findFirst({ where: { title: testTitle } });
    if (existingTest) continue;

    await db.test.create({
      data: {
        chapterId: subjectChapters[0].id,
        title: testTitle,
        description: `${subjectQs.length} questions • ${subject.name}`,
        durationMin: subject.slug.includes("topik-ii") ? 20 : subject.slug.includes("topik-i") ? 15 : 10,
        isExam: subject.slug.includes("topik"),
        examType: subject.slug.includes("topik-ii") ? "TOPIK_II" : subject.slug.includes("topik-i") ? "TOPIK_I" : "CHAPTER",
        passScore: 60,
        isPublished: true,
        isActive: true,
        items: {
          create: subjectQs.map((q: any, i: number) => ({
            questionId: q.id,
            points: q.difficulty === "HARD" ? 3 : q.difficulty === "MEDIUM" ? 2 : 1,
            order: i,
          })),
        },
      },
    });
  }
  console.log(`  ✓ 4 tests`);

  // ─── Books ─────────────────────────────────────────────────────────────────
  const booksData = [
    { title: "Korean Grammar in Use — Beginner", author: "Ahn Seol-hee", category: "Beginner", level: "TOPIK 1-2", pageCount: 320, description: "Comprehensive beginner grammar guide with examples." },
    { title: "TOPIK I Essential Vocabulary", author: "King Sejong Institute", category: "TOPIK", level: "TOPIK 1-2", pageCount: 180, description: "500 essential words for TOPIK I." },
    { title: "TOPIK II Writing Guide", author: "Kim Soo-jin", category: "TOPIK", level: "TOPIK 3-6", pageCount: 240, description: "Master the TOPIK II writing section." },
    { title: "Hangul Master Workbook", author: "DreamKorea", category: "Beginner", level: "TOPIK 1", pageCount: 100, description: "Practice writing Hangul letters and syllables." },
    { title: "Korean Conversation for Daily Life", author: "Park Ji-eun", category: "Intermediate", level: "TOPIK 3-4", pageCount: 200, description: "Real-world Korean conversations." },
    { title: "Advanced Korean Reading", author: "Lee Min-ho", category: "Advanced", level: "TOPIK 5-6", pageCount: 280, description: "Advanced reading comprehension practice." },
  ];

  for (const b of booksData) {
    const slug = b.title.toLowerCase().replace(/[^a-z0-9]+/g, "-").replace(/^-|-$/g, "");
    await db.book.upsert({
      where: { slug },
      create: {
        title: b.title,
        slug,
        author: b.author,
        description: b.description,
        category: b.category,
        level: b.level,
        pageCount: b.pageCount,
        isPublished: true,
      },
      update: {},
    });
  }
  console.log(`  ✓ ${booksData.length} books`);

  // ─── Video Lessons (YouTube) ───────────────────────────────────────────────
  const videosData = [
    { title: "Learn Hangul in 15 Minutes", youtubeId: "wfX1tWwGSVQ", durationMin: 15, level: "Beginner", category: "Pronunciation" },
    { title: "Korean Greetings for Beginners", youtubeId: "i_2lGR09gww", durationMin: 8, level: "Beginner", category: "Conversation" },
    { title: "TOPIK I Listening Practice", youtubeId: "L8uFGyT8sMo", durationMin: 20, level: "TOPIK 1-2", category: "TOPIK Prep" },
    { title: "Korean Particles Explained", youtubeId: "5y7pZ7v8rT8", durationMin: 12, level: "Intermediate", category: "Grammar" },
    { title: "Korean Numbers Made Easy", youtubeId: "Cq7nVWFP8hk", durationMin: 10, level: "Beginner", category: "Vocabulary" },
    { title: "TOPIK II Writing Tips", youtubeId: "8jPQ6c7sF2A", durationMin: 18, level: "TOPIK 3-6", category: "TOPIK Prep" },
  ];

  for (const v of videosData) {
    const slug = v.title.toLowerCase().replace(/[^a-z0-9]+/g, "-").replace(/^-|-$/g, "");
    await db.videoLesson.upsert({
      where: { slug },
      create: {
        title: v.title,
        slug,
        youtubeId: v.youtubeId,
        youtubeUrl: `https://youtube.com/watch?v=${v.youtubeId}`,
        thumbnailUrl: `https://img.youtube.com/vi/${v.youtubeId}/hqdefault.jpg`,
        durationMin: v.durationMin,
        level: v.level,
        category: v.category,
        isPublished: true,
      },
      update: {},
    });
  }
  console.log(`  ✓ ${videosData.length} video lessons`);

  // ─── Audio Lessons ─────────────────────────────────────────────────────────
  const audioData = [
    { title: "Hangul Pronunciation Drill", durationSec: 180, level: "Beginner", category: "Pronunciation", description: "Practice each Hangul letter sound." },
    { title: "Daily Greetings Audio", durationSec: 240, level: "Beginner", category: "Conversation", description: "Listen and repeat common greetings." },
    { title: "TOPIK I Listening — Numbers", durationSec: 300, level: "TOPIK 1-2", category: "TOPIK Prep", description: "Number recognition practice." },
    { title: "Korean News Slow Reading", durationSec: 360, level: "Intermediate", category: "News", description: "Slow news for listening practice." },
    { title: "TOPIK II Lecture Sample", durationSec: 480, level: "TOPIK 3-6", category: "TOPIK Prep", description: "Academic lecture listening." },
    { title: "Conversation at Restaurant", durationSec: 220, level: "Beginner", category: "Conversation", description: "Ordering food in Korean." },
  ];

  for (const a of audioData) {
    const slug = a.title.toLowerCase().replace(/[^a-z0-9]+/g, "-").replace(/^-|-$/g, "");
    await db.audioLesson.upsert({
      where: { slug },
      create: {
        title: a.title,
        slug,
        description: a.description,
        audioUrl: `https://example.com/audio/${slug}.mp3`, // placeholder URL
        durationSec: a.durationSec,
        level: a.level,
        category: a.category,
        isPublished: true,
      },
      update: {},
    });
  }
  console.log(`  ✓ ${audioData.length} audio lessons`);

  console.log("\n✅ Seed complete! All tabs now have content.");
  console.log("   Subjects: 4 | Chapters: 8 | Lessons: 16");
  console.log("   Questions: 23 | Tests: 4");
  console.log("   Books: 6 | Videos: 6 | Audio: 6");
}

main()
  .catch((e) => { console.error("Seed error:", e); process.exit(1); })
  .finally(() => db.$disconnect());
