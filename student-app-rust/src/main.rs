//! EduPlatform Student — native mobile app.
//!
//! Architecture:
//!   - UI: Slint (declarative, native rendering, glassmorphism via custom shaders)
//!   - State: parking_lot::RwLock<AppState> shared across callbacks
//!   - HTTP: reqwest + tokio, all network on a background runtime
//!   - Storage: OS keyring for session token, in-memory cache for content
//!
//! The student app talks to the shared Next.js backend API. Teachers and
//! admins use the web app directly (this binary does NOT support those roles).

mod api;
mod auth;
mod models;
mod state;
mod strings;

use std::rc::Rc;
use std::sync::Arc;

use slint::{ComponentHandle, PhysicalSize};

use crate::state::AppState;

slint::include_modules!();

fn main() -> anyhow::Result<()> {
    env_logger::init();

    // Boot the async runtime that the API client uses.
    let runtime = tokio::runtime::Builder::new_multi_thread()
        .enable_all()
        .build()?;
    let _runtime_guard = runtime.enter();

    // Shared application state.
    let state = Arc::new(AppState::new());
    let state_for_ui = state.clone();

    // Slint main window.
    let app = App::new()?;

    // Try to restore session from keyring (silent — fails gracefully).
    {
        let app_handle = app.as_weak();
        let st = state.clone();
        runtime.spawn(async move {
            if st.restore_session().await.is_ok() {
                // If we have a valid session, jump straight to home.
                slint::invoke_from_event_loop(move || {
                    if let Some(handle) = app_handle.upgrade() {
                        handle.invoke_navigate_home();
                    }
                })
                .ok();
            }
        });
    }

    // ─── Wire UI callbacks ────────────────────────────────────────────────────

    // OTP request — user entered email/phone, tapped "Send code".
    {
        let app_handle = app.as_weak();
        let st = state.clone();
        let api = state.api_client();
        app.on_request_otp(move |contact: SharedString| {
            let app_handle = app_handle.clone();
            let st = st.clone();
            let api = api.clone();
            let contact = contact.to_string();
            runtime.spawn(async move {
                app_handle.upgrade().map(|h| h.set_boot_loading(true));
                match api.request_otp(&contact).await {
                    Ok(_) => {
                        st.set_pending_contact(contact.clone());
                        slint::invoke_from_event_loop(move || {
                            if let Some(h) = app_handle.upgrade() {
                                h.set_boot_loading(false);
                                h.invoke_show_otp_screen();
                            }
                        }).ok();
                    }
                    Err(e) => {
                        let msg = e.to_string();
                        slint::invoke_from_event_loop(move || {
                            if let Some(h) = app_handle.upgrade() {
                                h.set_boot_loading(false);
                                h.set_boot_error(msg.into());
                            }
                        }).ok();
                    }
                }
            });
        });
    }

    // OTP verify — user entered the 6-digit code.
    {
        let app_handle = app.as_weak();
        let st = state.clone();
        let api = state.api_client();
        app.on_verify_otp(move |code: SharedString| {
            let app_handle = app_handle.clone();
            let st = st.clone();
            let api = api.clone();
            let code = code.to_string();
            runtime.spawn(async move {
                app_handle.upgrade().map(|h| h.set_boot_loading(true));
                let contact = st.pending_contact().unwrap_or_default();
                match api.verify_otp(&contact, &code).await {
                    Ok(user) => {
                        st.set_session(user.clone()).await;
                        slint::invoke_from_event_loop(move || {
                            if let Some(h) = app_handle.upgrade() {
                                h.set_boot_loading(false);
                                h.set_boot_error(SharedString::default());
                                h.invoke_navigate_home();
                            }
                        }).ok();
                    }
                    Err(e) => {
                        let msg = e.to_string();
                        slint::invoke_from_event_loop(move || {
                            if let Some(h) = app_handle.upgrade() {
                                h.set_boot_loading(false);
                                h.set_boot_error(msg.into());
                            }
                        }).ok();
                    }
                }
            });
        });
    }

    // Logout.
    {
        let app_handle = app.as_weak();
        let st = state.clone();
        app.on_logout(move || {
            let st = st.clone();
            let app_handle = app_handle.clone();
            runtime.spawn(async move {
                st.clear_session().await;
                slint::invoke_from_event_loop(move || {
                    if let Some(h) = app_handle.upgrade() {
                        h.invoke_navigate_auth();
                    }
                }).ok();
            });
        });
    }

    // Load home data (subjects + stats).
    {
        let app_handle = app.as_weak();
        let st = state.clone();
        let api = state.api_client();
        app.on_load_home(move || {
            let app_handle = app_handle.clone();
            let st = st.clone();
            let api = api.clone();
            runtime.spawn(async move {
                let home = api.get_home_stats().await.ok();
                let subjects = api.get_subjects().await.unwrap_or_default();
                slint::invoke_from_event_loop(move || {
                    if let Some(h) = app_handle.upgrade() {
                        h.set_home_stats(home.into());
                        h.set_subjects(subjects.into());
                    }
                }).ok();
            });
        });
    }

    // Load chapters for a subject.
    {
        let app_handle = app.as_weak();
        let st = state.clone();
        let api = state.api_client();
        app.on_load_chapters(move |subject_id: SharedString| {
            let app_handle = app_handle.clone();
            let st = st.clone();
            let api = api.clone();
            let id = subject_id.to_string();
            runtime.spawn(async move {
                let chapters = api.get_chapters(&id).await.unwrap_or_default();
                st.cache_chapters(id.clone(), chapters.clone());
                slint::invoke_from_event_loop(move || {
                    if let Some(h) = app_handle.upgrade() {
                        h.set_chapters(chapters.into());
                    }
                }).ok();
            });
        });
    }

    // Load lessons for a chapter.
    {
        let app_handle = app.as_weak();
        let st = state.clone();
        let api = state.api_client();
        app.on_load_lessons(move |chapter_id: SharedString| {
            let app_handle = app_handle.clone();
            let st = st.clone();
            let api = api.clone();
            let id = chapter_id.to_string();
            runtime.spawn(async move {
                let lessons = api.get_lessons(&id).await.unwrap_or_default();
                st.cache_lessons(id.clone(), lessons.clone());
                slint::invoke_from_event_loop(move || {
                    if let Some(h) = app_handle.upgrade() {
                        h.set_lessons(lessons.into());
                    }
                }).ok();
            });
        });
    }

    // Load tests.
    {
        let app_handle = app.as_weak();
        let api = state.api_client();
        app.on_load_tests(move || {
            let app_handle = app_handle.clone();
            let api = api.clone();
            runtime.spawn(async move {
                let tests = api.get_tests().await.unwrap_or_default();
                slint::invoke_from_event_loop(move || {
                    if let Some(h) = app_handle.upgrade() {
                        h.set_tests(tests.into());
                    }
                }).ok();
            });
        });
    }

    // Start test (load full test with questions).
    {
        let app_handle = app.as_weak();
        let api = state.api_client();
        app.on_start_test(move |test_id: SharedString| {
            let app_handle = app_handle.clone();
            let api = api.clone();
            let id = test_id.to_string();
            runtime.spawn(async move {
                match api.get_test_detail(&id).await {
                    Ok(detail) => {
                        slint::invoke_from_event_loop(move || {
                            if let Some(h) = app_handle.upgrade() {
                                h.set_active_test(detail.into());
                                h.invoke_show_test_runner();
                            }
                        }).ok();
                    }
                    Err(e) => {
                        let msg = e.to_string();
                        slint::invoke_from_event_loop(move || {
                            if let Some(h) = app_handle.upgrade() {
                                h.set_boot_error(msg.into());
                            }
                        }).ok();
                    }
                }
            });
        });
    }

    // Submit test answers.
    {
        let app_handle = app.as_weak();
        let st = state.clone();
        let api = state.api_client();
        app.on_submit_test(move |test_id: SharedString, answers_json: SharedString| {
            let app_handle = app_handle.clone();
            let api = api.clone();
            let _st = st.clone();
            let id = test_id.to_string();
            let answers = answers_json.to_string();
            runtime.spawn(async move {
                match api.submit_test(&id, &answers).await {
                    Ok(result) => {
                        slint::invoke_from_event_loop(move || {
                            if let Some(h) = app_handle.upgrade() {
                                h.set_test_result(result.into());
                                h.invoke_show_test_result();
                            }
                        }).ok();
                    }
                    Err(e) => {
                        let msg = e.to_string();
                        slint::invoke_from_event_loop(move || {
                            if let Some(h) = app_handle.upgrade() {
                                h.set_boot_error(msg.into());
                            }
                        }).ok();
                    }
                }
            });
        });
    }

    // Load Q&A questions.
    {
        let app_handle = app.as_weak();
        let api = state.api_client();
        app.on_load_qa(move || {
            let app_handle = app_handle.clone();
            let api = api.clone();
            runtime.spawn(async move {
                let qs = api.get_qa_questions().await.unwrap_or_default();
                slint::invoke_from_event_loop(move || {
                    if let Some(h) = app_handle.upgrade() {
                        h.set_qa_questions(qs.into());
                    }
                }).ok();
            });
        });
    }

    // Post a new Q&A question.
    {
        let app_handle = app.as_weak();
        let api = state.api_client();
        app.on_post_question(move |title: SharedString, body: SharedString| {
            let app_handle = app_handle.clone();
            let api = api.clone();
            let title = title.to_string();
            let body = body.to_string();
            runtime.spawn(async move {
                match api.post_qa_question(&title, &body).await {
                    Ok(_) => {
                        slint::invoke_from_event_loop(move || {
                            if let Some(h) = app_handle.upgrade() {
                                h.invoke_close_question_sheet();
                                h.invoke_load_qa();
                            }
                        }).ok();
                    }
                    Err(e) => {
                        let msg = e.to_string();
                        slint::invoke_from_event_loop(move || {
                            if let Some(h) = app_handle.upgrade() {
                                h.set_boot_error(msg.into());
                            }
                        }).ok();
                    }
                }
            });
        });
    }

    // Set window size (mobile dimensions).
    #[cfg(not(target_os = "android"))]
    #[cfg(not(target_os = "ios"))]
    {
        app.window().set_size(PhysicalSize::new(390, 844)); // iPhone 14 logical size
    }

    app.run()?;
    Ok(())
}
