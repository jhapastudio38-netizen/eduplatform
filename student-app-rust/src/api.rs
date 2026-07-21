//! HTTP client for the EduPlatform backend.
//!
//! All requests are async. The client stores the session token in-memory and
//! uses the OS keyring for persistent storage (see `auth.rs`).

use anyhow::{anyhow, Result};
use reqwest::{Client, Method};
use serde::{de::DeserializeOwned, Serialize};
use std::time::Duration;

use crate::models::*;

const BASE_URL: &str = "https://api.dreamkoreansmartclass.com";
const STORAGE_KEY: &str = "eduplatform-student-token";

#[derive(Clone)]
pub struct ApiClient {
    http: Client,
    base_url: String,
    token: std::sync::Arc<parking_lot::RwLock<Option<String>>>,
}

impl ApiClient {
    pub fn new(base_url: impl Into<String>) -> Self {
        let http = Client::builder()
            .timeout(Duration::from_secs(15))
            .user_agent("EduPlatform-Student/0.1 (Rust/Slint)")
            .build()
            .expect("Failed to build HTTP client");

        Self {
            http,
            base_url: base_url.into(),
            token: std::sync::Arc::new(parking_lot::RwLock::new(None)),
        }
    }

    pub fn set_token(&self, token: Option<String>) {
        *self.token.write() = token;
    }

    pub fn has_token(&self) -> bool {
        self.token.read().is_some()
    }

    // ─── Auth ─────────────────────────────────────────────────────────────────

    pub async fn request_otp(&self, contact: &str) -> Result<()> {
        self.post("/api/auth/request-otp")
            .json(&serde_json::json!({ "contact": contact }))
            .send_empty()
            .await
    }

    pub async fn verify_otp(&self, contact: &str, code: &str) -> Result<User> {
        let resp: VerifyOtpResponse = self
            .post("/api/auth/verify-otp")
            .json(&serde_json::json!({
                "contact": contact,
                "code": code,
                "role": "STUDENT",
            }))
            .send_json()
            .await?;
        self.set_token(Some(resp.token.clone()));
        // Persist token to OS keyring.
        if let Err(e) = keyring::Entry::new("eduplatform", STORAGE_KEY)
            .and_then(|e| e.set_password(&resp.token))
        {
            log::warn!("Failed to persist token to keyring: {e}");
        }
        Ok(resp.user)
    }

    pub async fn logout(&self) -> Result<()> {
        let _ = self.post("/api/auth/logout").send_empty().await;
        self.set_token(None);
        let _ = keyring::Entry::new("eduplatform", STORAGE_KEY).map(|e| e.delete_credential());
        Ok(())
    }

    pub async fn get_me(&self) -> Result<User> {
        let resp: MeResponse = self.get("/api/auth/me").send_json().await?;
        resp.user.ok_or_else(|| anyhow!("No active session"))
    }

    // ─── Student endpoints ────────────────────────────────────────────────────

    pub async fn get_home_stats(&self) -> Result<HomeStats> {
        self.get("/api/student/home").send_json().await
    }

    pub async fn get_subjects(&self) -> Result<Vec<Subject>> {
        let resp: SubjectsResponse = self.get("/api/student/subjects").send_json().await?;
        Ok(resp.subjects)
    }

    pub async fn get_chapters(&self, subject_id: &str) -> Result<Vec<Chapter>> {
        let resp: ChaptersResponse = self
            .get(&format!("/api/student/subjects/{subject_id}/chapters"))
            .send_json()
            .await?;
        Ok(resp.chapters)
    }

    pub async fn get_lessons(&self, chapter_id: &str) -> Result<Vec<Lesson>> {
        let resp: LessonsResponse = self
            .get(&format!("/api/student/chapters/{chapter_id}/lessons"))
            .send_json()
            .await?;
        Ok(resp.lessons)
    }

    pub async fn get_tests(&self) -> Result<Vec<Test>> {
        let resp: TestsResponse = self.get("/api/student/tests").send_json().await?;
        Ok(resp.tests)
    }

    pub async fn get_test_detail(&self, test_id: &str) -> Result<TestDetail> {
        let resp: TestDetailResponse = self
            .get(&format!("/api/student/tests/{test_id}"))
            .send_json()
            .await?;
        Ok(resp.test)
    }

    pub async fn submit_test(&self, test_id: &str, answers_json: &str) -> Result<TestResult> {
        let answers: serde_json::Value = serde_json::from_str(answers_json).unwrap_or_default();
        self.post(&format!("/api/student/tests/{test_id}/submit"))
            .json(&serde_json::json!({ "answers": answers }))
            .send_json()
            .await
    }

    pub async fn get_qa_questions(&self) -> Result<Vec<QAQuestion>> {
        let resp: QAResponse = self.get("/api/student/qa").send_json().await?;
        Ok(resp.questions)
    }

    pub async fn post_qa_question(&self, title: &str, body: &str) -> Result<()> {
        self.post("/api/student/qa")
            .json(&serde_json::json!({ "title": title, "body": body, "tags": [] }))
            .send_empty()
            .await
    }

    // ─── Internal helpers ───────────────────────────────────────────────────

    fn get(&self, path: &str) -> RequestBuilder<'_> {
        RequestBuilder {
            client: self,
            method: Method::GET,
            path: path.to_string(),
            body: None,
        }
    }

    fn post(&self, path: &str) -> RequestBuilder<'_> {
        RequestBuilder {
            client: self,
            method: Method::POST,
            path: path.to_string(),
            body: None,
        }
    }
}

// Helper builder for fluent request construction.
struct RequestBuilder<'a> {
    client: &'a ApiClient,
    method: Method,
    path: String,
    body: Option<serde_json::Value>,
}

impl<'a> RequestBuilder<'a> {
    fn json(mut self, value: &serde_json::Value) -> Self {
        self.body = Some(value.clone());
        self
    }

    async fn send_empty(self) -> Result<()> {
        let resp = self.send_raw().await?;
        if !resp.status().is_success() {
            let status = resp.status();
            let text = resp.text().await.unwrap_or_default();
            return Err(anyhow!("HTTP {status}: {text}"));
        }
        Ok(())
    }

    async fn send_json<T: DeserializeOwned>(self) -> Result<T> {
        let resp = self.send_raw().await?;
        if !resp.status().is_success() {
            let status = resp.status();
            let text = resp.text().await.unwrap_or_default();
            return Err(anyhow!("HTTP {status}: {text}"));
        }
        let body = resp.json::<T>().await?;
        Ok(body)
    }

    async fn send(self) -> Result<reqwest::Response> {
        self.send_raw().await
    }

    async fn send_raw(self) -> Result<reqwest::Response> {
        let url = format!("{}{}", self.client.base_url, self.path);
        let mut req = self.client.http.request(self.method, &url);
        if let Some(token) = self.client.token.read().as_ref() {
            req = req.bearer_auth(token);
        }
        if let Some(body) = self.body {
            req = req.json(&body);
        }
        let resp = req.send().await?;
        Ok(resp)
    }
}
