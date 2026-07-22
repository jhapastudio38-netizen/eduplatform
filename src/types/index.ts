/**
 * Shared types — mirror Prisma enums for client use.
 */

export type Role = "STUDENT" | "TEACHER" | "ADMIN";

export type QuestionType =
  | "SINGLE_CHOICE"
  | "MULTIPLE_CHOICE"
  | "TRUE_FALSE"
  | "ONE_WORD"
  | "SHORT_ANSWER"
  | "LONG_ANSWER"
  | "FILL_BLANK"
  | "MATCHING";

export type Difficulty = "EASY" | "MEDIUM" | "HARD";

export interface Subject {
  id: string;
  name: string;
  slug: string;
  description?: string | null;
  iconUrl?: string | null;
}

export interface Chapter {
  id: string;
  subjectId: string;
  title: string;
  slug: string;
  description?: string | null;
  order: number;
  isPublished: boolean;
  lessons?: Lesson[];
}

export interface Lesson {
  id: string;
  chapterId: string;
  title: string;
  slug: string;
  type: "TEXT" | "VIDEO" | "PDF" | "INTERACTIVE";
  content: string;
  videoUrl?: string | null;
  durationMin: number;
  order: number;
  isPublished: boolean;
}

export interface Question {
  id: string;
  chapterId?: string | null;
  type: QuestionType;
  difficulty: Difficulty;
  stem: string;
  options?: string[] | null;
  imageUrl?: string | null;
  audioUrl?: string | null;
  correctAnswer?: string | null;
  explanation?: string | null;
  tags?: string[] | null;
  aiGenerated: boolean;
}

export interface Test {
  id: string;
  title: string;
  description?: string | null;
  durationMin: number;
  isExam: boolean;
  passScore: number;
  startAt?: string | null;
  endAt?: string | null;
  isPublished: boolean;
  items?: (TestItem & { question: Question })[];
}

export interface TestItem {
  id: string;
  testId: string;
  questionId: string;
  points: number;
  order: number;
}

export interface Submission {
  id: string;
  testId: string;
  userId: string;
  answers: Record<string, unknown>;
  score?: number | null;
  maxScore?: number | null;
  startedAt: string;
  submittedAt?: string | null;
  graded: boolean;
}

export interface LiveClass {
  id: string;
  teacherId: string;
  title: string;
  description?: string | null;
  subject?: string | null;
  scheduledAt: string;
  durationMin: number;
  roomCode: string;
  isLive: boolean;
  endedAt?: string | null;
}

export interface QAQuestion {
  id: string;
  userId: string;
  lessonId?: string | null;
  title: string;
  body: string;
  tags?: string[] | null;
  createdAt: string;
  answers?: QAAnswer[];
}

export interface QAAnswer {
  id: string;
  authorId: string;
  body: string;
  isAccepted: boolean;
  createdAt: string;
}
