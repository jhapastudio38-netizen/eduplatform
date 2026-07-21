//! EduPlatform Student — minimal v0.1 native app.

#![allow(dead_code)]
#![allow(unused_imports)]
#![allow(unused_variables)]

mod api;
mod auth;
mod models;
mod state;
mod strings;

use std::sync::Arc;

use slint::ComponentHandle;

use crate::state::AppState;

slint::include_modules!();

fn main() -> anyhow::Result<()> {
    env_logger::init();

    let runtime = tokio::runtime::Builder::new_multi_thread()
        .enable_all()
        .build()?;
    let handle = runtime.handle().clone();
    let _runtime_guard = runtime.enter();

    let state = Arc::new(AppState::new());
    let app = App::new()?;

    // Restore session silently
    {
        let app_handle = app.as_weak();
        let st = state.clone();
        handle.spawn(async move {
            if st.restore_session().await.is_ok() {
                let _ = slint::invoke_from_event_loop(move || {
                    if let Some(h) = app_handle.upgrade() {
                        h.set_step(2);
                    }
                });
            }
        });
    }

    // Stub callbacks not used in v0.1 (Slint requires them to be set)
    app.on_load_home(|| {});
    app.on_load_tests(|| {});
    app.on_load_qa(|| {});
    app.on_start_test(|_| {});
    app.on_submit_test(|_, _| {});
    app.on_post_question(|_, _| {});
    app.on_load_chapters(|_| {});
    app.on_load_lessons(|_| {});
    app.on_navigate_home(|| {});
    app.on_navigate_auth(|| {});
    app.on_show_otp_screen(|| {});
    app.on_show_test_runner(|| {});
    app.on_show_test_result(|| {});
    app.on_close_question_sheet(|| {});

    // Request OTP
    {
        let app_handle = app.as_weak();
        let st = state.clone();
        let api = state.api_client();
        app.on_request_otp(move |contact: slint::SharedString| {
            let app_handle = app_handle.clone();
            let st = st.clone();
            let api = api.clone();
            let contact = contact.to_string();
            handle.spawn(async move {
                if let Some(h) = app_handle.upgrade() { h.set_boot_loading(true); }
                match api.request_otp(&contact).await {
                    Ok(_) => {
                        st.set_pending_contact(contact.clone());
                        let _ = slint::invoke_from_event_loop(move || {
                            if let Some(h) = app_handle.upgrade() {
                                h.set_boot_loading(false);
                                h.set_boot_error(slint::SharedString::default());
                            }
                        });
                    }
                    Err(e) => {
                        let msg = e.to_string();
                        let _ = slint::invoke_from_event_loop(move || {
                            if let Some(h) = app_handle.upgrade() {
                                h.set_boot_loading(false);
                                h.set_boot_error(msg.into());
                            }
                        });
                    }
                }
            });
        });
    }

    // Verify OTP
    {
        let app_handle = app.as_weak();
        let st = state.clone();
        let api = state.api_client();
        app.on_verify_otp(move |code: slint::SharedString| {
            let app_handle = app_handle.clone();
            let st = st.clone();
            let api = api.clone();
            let code = code.to_string();
            handle.spawn(async move {
                if let Some(h) = app_handle.upgrade() { h.set_boot_loading(true); }
                let contact = st.pending_contact().unwrap_or_default();
                match api.verify_otp(&contact, &code).await {
                    Ok(user) => {
                        st.set_session(user.clone()).await;
                        let name = user.name.unwrap_or_else(|| "Student".into());
                        let _ = slint::invoke_from_event_loop(move || {
                            if let Some(h) = app_handle.upgrade() {
                                h.set_boot_loading(false);
                                h.set_boot_error(slint::SharedString::default());
                                h.set_user_name(name.into());
                                h.set_step(2);
                            }
                        });
                    }
                    Err(e) => {
                        let msg = e.to_string();
                        let _ = slint::invoke_from_event_loop(move || {
                            if let Some(h) = app_handle.upgrade() {
                                h.set_boot_loading(false);
                                h.set_boot_error(msg.into());
                            }
                        });
                    }
                }
            });
        });
    }

    // Logout
    {
        let app_handle = app.as_weak();
        let st = state.clone();
        app.on_logout(move || {
            let st = st.clone();
            let app_handle = app_handle.clone();
            handle.spawn(async move {
                st.clear_session().await;
                let _ = slint::invoke_from_event_loop(move || {
                    if let Some(h) = app_handle.upgrade() {
                        h.set_step(0);
                    }
                });
            });
        });
    }

    app.run()?;
    Ok(())
}
