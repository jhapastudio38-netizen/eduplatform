//! Data models that mirror the backend API.
//!
//! These types are Serde-deserialised from JSON responses. Slint-facing
//! types (in `models.rs` for the UI bridge) are converted from these.

use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct User {
    pub id: String,
    pub name: Option<String>,
    pub email: String,
    pub phone: Option<String>,
    pub role: String,
    #[serde(default)]
    pub avatar_url: Option<String>,
}

#[derive(Debug, Deserialize)]
pub struct VerifyOtpResponse {
    pub token: String,
    pub user: User,
}

#[derive(Debug, Deserialize)]
pub struct MeResponse {
    pub user: Option<User>,
}

#[derive(Debug, Clone, Deserialize)]
pub struct HomeStats {
    #[serde(default)]
    pub lessons_completed: i32,
    #[serde(default)]
    pub tests_taken: i32,
    #[serde(default)]
    pub qa_asked: i32,
    #[serde(default)]
    pub streak: i32,
    #[serde(default)]
    pub recent_activity: Vec<RecentActivity>,
}

#[derive(Debug, Clone, Deserialize)]
pub struct RecentActivity {
    pub id: String,
    pub label: String,
    pub ts: String,
}

#[derive(Debug, Deserialize)]
pub struct SubjectsResponse {
    pub subjects: Vec<Subject>,
}

#[derive(Debug, Clone, Deserialize)]
pub struct Subject {
    pub id: String,
    pub name: String,
    pub slug: String,
    pub description: Option<String>,
    pub icon_url: Option<String>,
}

#[derive(Debug, Deserialize)]
pub struct ChaptersResponse {
    pub chapters: Vec<Chapter>,
}

#[derive(Debug, Clone, Deserialize)]
pub struct Chapter {
    pub id: String,
    pub subject_id: String,
    pub title: String,
    pub slug: String,
    pub description: Option<String>,
    pub order: i32,
    pub is_published: bool,
}

#[derive(Debug, Deserialize)]
pub struct LessonsResponse {
    pub lessons: Vec<Lesson>,
}

#[derive(Debug, Clone, Deserialize)]
pub struct Lesson {
    pub id: String,
    pub chapter_id: String,
    pub title: String,
    pub slug: String,
    #[serde(rename = "type")]
    pub lesson_type: String,
    pub duration_min: i32,
    pub order: i32,
    pub is_published: bool,
    pub video_url: Option<String>,
}

#[derive(Debug, Deserialize)]
pub struct TestsResponse {
    pub tests: Vec<Test>,
}

#[derive(Debug, Clone, Deserialize)]
pub struct Test {
    pub id: String,
    pub title: String,
    pub description: Option<String>,
    pub duration_min: i32,
    pub is_exam: bool,
    pub pass_score: i32,
    pub is_published: bool,
    #[serde(default)]
    pub question_count: i32,
}

#[derive(Debug, Deserialize)]
pub struct TestDetailResponse {
    pub test: TestDetail,
}

#[derive(Debug, Clone, Deserialize)]
pub struct TestDetail {
    pub id: String,
    pub title: String,
    pub description: Option<String>,
    pub duration_min: i32,
    pub is_exam: bool,
    pub pass_score: i32,
    pub items: Vec<TestItem>,
}

#[derive(Debug, Clone, Deserialize)]
pub struct TestItem {
    pub id: String,
    pub test_id: String,
    pub question_id: String,
    pub points: i32,
    pub order: i32,
    pub question: Question,
}

#[derive(Debug, Clone, Deserialize)]
pub struct Question {
    pub id: String,
    #[serde(rename = "type")]
    pub question_type: String,
    pub difficulty: String,
    pub stem: String,
    pub options: Option<Vec<String>>,
}

#[derive(Debug, Deserialize)]
pub struct TestResult {
    pub score: i32,
    pub max_score: i32,
    pub graded: bool,
    #[serde(default)]
    pub submission_id: String,
}

#[derive(Debug, Deserialize)]
pub struct QAResponse {
    pub questions: Vec<QAQuestion>,
}

#[derive(Debug, Clone, Deserialize)]
pub struct QAQuestion {
    pub id: String,
    pub title: String,
    pub body: String,
    #[serde(default)]
    pub tags: Vec<String>,
    pub created_at: String,
    #[serde(default)]
    pub answers_count: i32,
}
