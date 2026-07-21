//! Cross-thread application state.
//!
//! AppState is wrapped in Arc and shared between the UI thread (Slint) and
//! the async Tokio runtime. All access uses parking_lot locks for simplicity.

use parking_lot::RwLock;
use std::collections::HashMap;
use std::sync::Arc;

use crate::api::ApiClient;
use crate::models::*;

pub struct AppState {
    pub base_url: String,
    pub api: ApiClient,
    pub user: RwLock<Option<User>>,
    pub pending_contact: RwLock<Option<String>>,
    /// Cache chapters per subject_id to avoid re-fetching.
    pub chapters_cache: RwLock<HashMap<String, Vec<Chapter>>>,
    /// Cache lessons per chapter_id.
    pub lessons_cache: RwLock<HashMap<String, Vec<Lesson>>>,
}

impl AppState {
    pub fn new() -> Self {
        let base_url =
            std::env::var("EDUPLATFORM_API_URL").unwrap_or_else(|_| "https://api.eduplatform.app".into());
        let api = ApiClient::new(base_url.clone());
        Self {
            base_url,
            api,
            user: RwLock::new(None),
            pending_contact: RwLock::new(None),
            chapters_cache: RwLock::new(HashMap::new()),
            lessons_cache: RwLock::new(HashMap::new()),
        }
    }

    pub fn api_client(&self) -> ApiClient {
        self.api.clone()
    }

    pub fn set_pending_contact(&self, contact: String) {
        *self.pending_contact.write() = Some(contact);
    }

    pub fn pending_contact(&self) -> Option<String> {
        self.pending_contact.read().clone()
    }

    pub async fn set_session(&self, user: User) {
        *self.user.write() = Some(user);
    }

    pub async fn clear_session(&self) {
        let _ = self.api.logout().await;
        *self.user.write() = None;
    }

    /// Restore a previously persisted session from the OS keyring.
    pub async fn restore_session(&self) -> anyhow::Result<()> {
        let token = keyring::Entry::new("eduplatform", "eduplatform-student-token")
            .and_then(|e| e.get_password())
            .map_err(|e| anyhow::anyhow!("No stored session: {e}"))?;
        self.api.set_token(Some(token));
        let user = self.api.get_me().await?;
        *self.user.write() = Some(user);
        Ok(())
    }

    pub fn cache_chapters(&self, subject_id: String, chapters: Vec<Chapter>) {
        self.chapters_cache.write().insert(subject_id, chapters);
    }

    pub fn cache_lessons(&self, chapter_id: String, lessons: Vec<Lesson>) {
        self.lessons_cache.write().insert(chapter_id, lessons);
    }
}

// AppState is shared across threads via Arc.
// The internal locks make &self safe to use from multiple threads.
unsafe impl Send for AppState {}
unsafe impl Sync for AppState {}
