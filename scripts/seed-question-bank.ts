/**
 * Seed Script — DreamKorea Question Bank
 *
 * 1. Deletes ALL existing tests, test items, and question bank questions
 * 2. Creates a fresh question bank with sample Korean learning questions
 *    across multiple categories (Vocabulary, Grammar, Listening, Reading)
 *    with images, audio, and varied question types
 * 3. Creates 2 sample tests (1 practice, 1 exam) with questions
 *
 * Usage: npx tsx scripts/seed-question-bank.ts
 *   (or run via /api/admin/seed endpoint)
 */
import { PrismaClient } from "@prisma/client";

const db = new PrismaClient();

// Sample image URLs — Korean-themed placeholder images hosted on Unsplash
const IMAGES = {
  hangul: "https://images.unsplash.com/photo-1531973486364-5fa64260d75b?w=400&h=300&fit=crop",
  seoul: "https://images.unsplash.com/photo-1538485399081-7c8ed7144b6c?w=400&h=300&fit=crop",
  food: "https://images.unsplash.com/photo-1553163147-622ab57be1c7?w=400&h=300&fit=crop",
  temple: "https://images.unsplash.com/photo-1548549019-2c6b6822c8b5?w=400&h=300&fit=crop",
  classroom: "https://images.unsplash.com/photo-1503676260728-1c00da094a0b?w=400&h=300&fit=crop",
};

// Sample audio URL — a short Korean pronunciation clip
const AUDIO = {
  greeting: "https://www2.cs.uic.edu/~i101/SoundFiles/gettysburg.wav", // placeholder
  alphabet: "https://www2.cs.uic.edu/~i101/SoundFiles/preamble10.wav",
};

async function seed() {
  console.log("🧹 Cleaning up old data...");

  // Delete all test items, tests, and question bank questions
  await db.testItem.deleteMany({});
  await db.test.deleteMany({});
  await db.question.deleteMany({});
  console.log("   Deleted all tests, test items, and questions");

  console.log("📚 Creating question bank questions...");

  const questions = [
    // ─── Vocabulary ─────────────────────────────────────────────────────────
    {
      type: "SINGLE_CHOICE" as const,
      difficulty: "EASY" as const,
      stem: "What does '안녕하세요' (annyeonghaseyo) mean in English?",
      options: JSON.stringify(["Goodbye", "Hello", "Thank you", "Sorry"]),
      correctAnswer: JSON.stringify("Hello"),
      explanation: "'안녕하세요' is the standard polite greeting in Korean, meaning 'Hello'.",
      category: "Vocabulary",
      imageUrl: null,
      audioUrl: null,
    },
    {
      type: "SINGLE_CHOICE" as const,
      difficulty: "EASY" as const,
      stem: "Which word means 'water' in Korean?",
      options: JSON.stringify(["불 (bul)", "물 (mul)", "공기 (gonggi)", "흙 (heuk)"]),
      correctAnswer: JSON.stringify("물 (mul)"),
      explanation: "'물' (mul) means 'water'. '불' is fire, '공기' is air, '흙' is soil.",
      category: "Vocabulary",
      imageUrl: null,
      audioUrl: null,
    },
    {
      type: "SINGLE_CHOICE" as const,
      difficulty: "MEDIUM" as const,
      stem: "Identify the Korean food shown in the image:",
      options: JSON.stringify(["김치 (Kimchi)", "비빔밥 (Bibimbap)", "떡볶이 (Tteokbokki)", "불고기 (Bulgogi)"]),
      correctAnswer: JSON.stringify("비빔밥 (Bibimbap)"),
      explanation: "This is Bibimbap, a mixed rice dish with vegetables, meat, and gochujang.",
      category: "Vocabulary",
      imageUrl: IMAGES.food,
      audioUrl: null,
    },
    {
      type: "FILL_BLANK" as const,
      difficulty: "MEDIUM" as const,
      stem: "Complete: '사과 한 ___ 주세요' (Please give me one ___ of apple)",
      options: JSON.stringify(["개 (gae)", "권 (gwon)", "병 (byeong)", "장 (jang)"]),
      correctAnswer: JSON.stringify("개 (gae)"),
      explanation: "'개' is the counter for general objects. '권' is for books, '병' for bottles, '장' for flat objects.",
      category: "Vocabulary",
      imageUrl: null,
      audioUrl: null,
    },
    {
      type: "TRUE_FALSE" as const,
      difficulty: "EASY" as const,
      stem: "'선생님' (seonsaengnim) means 'student'.",
      options: JSON.stringify(["True", "False"]),
      correctAnswer: JSON.stringify("False"),
      explanation: "'선생님' means 'teacher'. '학생' (haksaeng) means 'student'.",
      category: "Vocabulary",
      imageUrl: null,
      audioUrl: null,
    },

    // ─── Grammar ────────────────────────────────────────────────────────────
    {
      type: "SINGLE_CHOICE" as const,
      difficulty: "MEDIUM" as const,
      stem: "Which particle marks the topic of a sentence?",
      options: JSON.stringify(["은/는 (eun/neun)", "이/가 (i/ga)", "을/를 (eul/reul)", "에 (e)"]),
      correctAnswer: JSON.stringify("은/는 (eun/neun)"),
      explanation: "은/는 is the topic marker. 이/가 is the subject marker. 을/를 is the object marker. 에 is the location/time marker.",
      category: "Grammar",
      imageUrl: null,
      audioUrl: null,
    },
    {
      type: "SINGLE_CHOICE" as const,
      difficulty: "HARD" as const,
      stem: "Choose the correct polite formal ending for 'I eat':",
      options: JSON.stringify(["먹어 (meogeo)", "먹어요 (meogeoyo)", "먹습니다 (meokseumnida)", "먹자 (meokja)"]),
      correctAnswer: JSON.stringify("먹습니다 (meokseumnida)"),
      explanation: "'먹습니다' is the formal polite (합쇼체) form. '먹어요' is informal polite. '먹어' is casual. '먹자' is let's eat.",
      category: "Grammar",
      imageUrl: null,
      audioUrl: null,
    },
    {
      type: "FILL_BLANK" as const,
      difficulty: "MEDIUM" as const,
      stem: "Complete: '저 ___ 학생입니다' (I ___ am a student)",
      options: JSON.stringify(["는 (neun)", "이 (i)", "을 (eul)", "의 (ui)"]),
      correctAnswer: JSON.stringify("는 (neun)"),
      explanation: "Since '저' ends in a vowel, use '는' as the topic marker.",
      category: "Grammar",
      imageUrl: null,
      audioUrl: null,
    },
    {
      type: "SINGLE_CHOICE" as const,
      difficulty: "MEDIUM" as const,
      stem: "Which form is used to say 'cannot do' something?",
      options: JSON.stringify(["-을 수 있다", "-을 수 없다", "-고 싶다", "-아/어야 하다"]),
      correctAnswer: JSON.stringify("-을 수 없다"),
      explanation: "'-을 수 없다' means 'cannot do'. '-을 수 있다' means 'can do'. '-고 싶다' is 'want to'. '-아/어야 하다' is 'must do'.",
      category: "Grammar",
      imageUrl: null,
      audioUrl: null,
    },

    // ─── Listening ──────────────────────────────────────────────────────────
    {
      type: "SINGLE_CHOICE" as const,
      difficulty: "EASY" as const,
      stem: "Listen to the audio. What greeting is being said?",
      options: JSON.stringify(["안녕히 가세요 (Goodbye - to person leaving)", "안녕히 계세요 (Goodbye - to person staying)", "안녕하세요 (Hello)", "반갑습니다 (Nice to meet you)"]),
      correctAnswer: JSON.stringify("안녕하세요 (Hello)"),
      explanation: "The audio plays the standard greeting '안녕하세요' (Hello).",
      category: "Listening",
      imageUrl: null,
      audioUrl: AUDIO.greeting,
      audioLoop: 2,
      audioLoopDelay: 2,
    },
    {
      type: "SINGLE_CHOICE" as const,
      difficulty: "MEDIUM" as const,
      stem: "Listen and identify what is being said:",
      options: JSON.stringify(["A phone number", "A time", "A price", "A date"]),
      correctAnswer: JSON.stringify("A phone number"),
      explanation: "The audio contains a sequence of numbers being read — typical of a phone number.",
      category: "Listening",
      imageUrl: null,
      audioUrl: AUDIO.alphabet,
      audioLoop: 1,
      audioLoopDelay: 3,
    },

    // ─── Reading ────────────────────────────────────────────────────────────
    {
      type: "SINGLE_CHOICE" as const,
      difficulty: "EASY" as const,
      stem: "Read: '저는 학생입니다.' What does this mean?",
      options: JSON.stringify(["I am a teacher.", "I am a student.", "I am a doctor.", "I am a chef."]),
      correctAnswer: JSON.stringify("I am a student."),
      explanation: "저는 (I - topic) 학생 (student) 입니다 (am). So 'I am a student.'",
      category: "Reading",
      imageUrl: null,
      audioUrl: null,
    },
    {
      type: "SINGLE_CHOICE" as const,
      difficulty: "MEDIUM" as const,
      stem: "Read the sign in the image. What does it say?",
      options: JSON.stringify(["입장 금지 (No entry)", "조심 (Caution)", "출구 (Exit)", "화장실 (Restroom)"]),
      correctAnswer: JSON.stringify("출구 (Exit)"),
      explanation: "This is a standard exit sign seen in Korean buildings. '출구' means 'Exit'.",
      category: "Reading",
      imageUrl: IMAGES.seoul,
      audioUrl: null,
    },
    {
      type: "SINGLE_CHOICE" as const,
      difficulty: "HARD" as const,
      stem: "Read: '오늘은 날씨가 맑습니다.' What is the weather like?",
      options: JSON.stringify(["Rainy", "Snowy", "Sunny/Clear", "Cloudy"]),
      correctAnswer: JSON.stringify("Sunny/Clear"),
      explanation: "'맑다' means 'clear/sunny'. So the sentence says 'Today the weather is clear.'",
      category: "Reading",
      imageUrl: null,
      audioUrl: null,
    },

    // ─── Hangul (Alphabet) ──────────────────────────────────────────────────
    {
      type: "SINGLE_CHOICE" as const,
      difficulty: "EASY" as const,
      stem: "Which consonant is 'ㄱ'?",
      options: JSON.stringify(["ㄴ (n)", "ㄱ (g/k)", "ㄷ (d)", "ㅁ (m)"]),
      correctAnswer: JSON.stringify("ㄱ (g/k)"),
      explanation: "'ㄱ' is pronounced as 'g' or 'k' depending on position.",
      category: "Hangul",
      imageUrl: IMAGES.hangul,
      audioUrl: null,
    },
    {
      type: "SINGLE_CHOICE" as const,
      difficulty: "EASY" as const,
      stem: "What sound does 'ㅗ' make?",
      options: JSON.stringify(["a (ah)", "o (oh)", "u (oo)", "eo (uh)"]),
      correctAnswer: JSON.stringify("o (oh)"),
      explanation: "'ㅗ' is the vowel 'o' as in 'boat'.",
      category: "Hangul",
      imageUrl: null,
      audioUrl: null,
    },
    {
      type: "FILL_BLANK" as const,
      difficulty: "EASY" as const,
      stem: "Combine ㅎ + ㅏ + ㄴ to form a syllable. What is the result?",
      options: JSON.stringify(["하", "한", "핫", "항"]),
      correctAnswer: JSON.stringify("한"),
      explanation: "ㅎ (initial) + ㅏ (medial) + ㄴ (final) = 한 (han).",
      category: "Hangul",
      imageUrl: null,
      audioUrl: null,
    },

    // ─── Culture ────────────────────────────────────────────────────────────
    {
      type: "SINGLE_CHOICE" as const,
      difficulty: "MEDIUM" as const,
      stem: "What is the traditional Korean dress called?",
      options: JSON.stringify(["Kimono", "Hanbok", "Cheongsam", "Sari"]),
      correctAnswer: JSON.stringify("Hanbok"),
      explanation: "The hanbok is the traditional Korean clothing, characterized by vibrant colors and flowing lines.",
      category: "Culture",
      imageUrl: IMAGES.temple,
      audioUrl: null,
    },
    {
      type: "TRUE_FALSE" as const,
      difficulty: "EASY" as const,
      stem: "In Korean culture, it is polite to bow when greeting elders.",
      options: JSON.stringify(["True", "False"]),
      correctAnswer: JSON.stringify("True"),
      explanation: "Bowing is an important part of Korean etiquette, especially when greeting someone older or of higher status.",
      category: "Culture",
      imageUrl: null,
      audioUrl: null,
    },
  ];

  // Create all question bank questions
  for (const q of questions) {
    await db.question.create({
      data: {
        type: q.type,
        difficulty: q.difficulty,
        stem: q.stem,
        options: q.options,
        correctAnswer: q.correctAnswer,
        explanation: q.explanation,
        imageUrl: q.imageUrl,
        audioUrl: q.audioUrl,
        audioLoop: q.audioLoop || 0,
        audioLoopDelay: q.audioLoopDelay || 0,
        inQuestionBank: true,
        category: q.category,
      },
    });
  }
  console.log(`   Created ${questions.length} question bank questions across ${new Set(questions.map(q => q.category)).size} categories`);

  console.log("\n📝 Creating sample tests...");

  // Create 1 practice test with 3 questions
  const practiceTest = await db.test.create({
    data: {
      title: "Beginner Korean Practice",
      description: "A quick practice test covering basic vocabulary and grammar.",
      durationMin: 15,
      isExam: false,
      examType: "REGULAR",
      passScore: 60,
      isActive: true,
      isPublished: true,
    },
  });

  // Reuse 3 questions from the bank for the practice test
  const practiceQs = await db.question.findMany({
    where: { inQuestionBank: true, category: { in: ["Vocabulary", "Grammar", "Hangul"] } },
    take: 3,
  });
  for (let i = 0; i < practiceQs.length; i++) {
    await db.testItem.create({
      data: {
        testId: practiceTest.id,
        questionId: practiceQs[i].id,
        points: 1,
        order: i + 1,
      },
    });
  }
  console.log(`   Created practice test "${practiceTest.title}" with ${practiceQs.length} questions`);

  // Create 1 formal exam with 5 questions
  const examTest = await db.test.create({
    data: {
      title: "TOPIK I Mock Exam",
      description: "A formal mock exam simulating TOPIK I level questions.",
      durationMin: 30,
      isExam: true,
      examType: "TOPIK_I",
      passScore: 50,
      isActive: true,
      isPublished: true,
    },
  });

  const examQs = await db.question.findMany({
    where: { inQuestionBank: true },
    take: 5,
  });
  for (let i = 0; i < examQs.length; i++) {
    await db.testItem.create({
      data: {
        testId: examTest.id,
        questionId: examQs[i].id,
        points: 2,
        order: i + 1,
      },
    });
  }
  console.log(`   Created exam "${examTest.title}" with ${examQs.length} questions`);

  console.log("\n✅ Seed complete!");
  console.log(`   - ${questions.length} question bank questions (Vocabulary, Grammar, Listening, Reading, Hangul, Culture)`);
  console.log(`   - 1 practice test: "${practiceTest.title}"`);
  console.log(`   - 1 formal exam: "${examTest.title}"`);
}

seed()
  .catch((e) => {
    console.error("Seed failed:", e);
    process.exit(1);
  })
  .finally(async () => {
    await db.$disconnect();
  });
