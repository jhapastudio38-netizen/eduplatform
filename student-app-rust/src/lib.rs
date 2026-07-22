//! Library crate entry point for Android (cdylib).
//!
//! On Android, Slint's `android-activity` backend calls `android_main`
//! through this library. The `bin` crate (main.rs) is used on desktop.

// Re-export everything from main so we share the same Slint module + state code.
// We can't `mod main` because main.rs has a `fn main()`, so we duplicate the
// essential wiring here. For v0.2 this should be refactored into a shared module.

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
use tokio::runtime::Handle;

use crate::state::AppState;

slint::include_modules!();

/// Android entry point — called by android-activity via JNI.
///
/// Slint 1.7's android-activity backend: the AndroidApp is passed to
/// slint::android::init_app() BEFORE creating the UI component, so the
/// backend knows which window to render into.
#[cfg(target_os = "android")]
#[no_mangle]
pub fn android_main(app: slint::android::AndroidApp) {
    use std::sync::Arc;

    // Initialize Slint's Android backend with the AndroidApp handle.
    // This MUST happen before any Slint component is created.
    slint::android::init(app);

    // Boot the tokio runtime for async API calls
    let runtime = tokio::runtime::Builder::new_multi_thread()
        .enable_all()
        .build()
        .expect("Failed to boot tokio runtime");
    let _guard = runtime.enter();
    let handle: Handle = runtime.handle().clone();

    // Initialize app state (loads session token from keyring if present)
    let state = Arc::new(AppState::new());
    let ui = App::new().expect("Failed to launch Slint UI");

    // Restore session silently — jump to signed-in step if token is valid
    {
        let ui_handle = ui.as_weak();
        let st = state.clone();
        handle.spawn(async move {
            if st.restore_session().await.is_ok() {
                let _ = slint::invoke_from_event_loop(move || {
                    if let Some(h) = ui_handle.upgrade() {
                        h.set_step(2);
                    }
                });
            }
        });
    }

    // Stub callbacks not used in v0.1
    ui.on_load_home(|| {});
    ui.on_load_tests(|| {});
    ui.on_load_qa(|| {});
    ui.on_start_test(|_| {});
    ui.on_submit_test(|_, _| {});
    ui.on_post_question(|_, _| {});
    ui.on_load_chapters(|_| {});
    ui.on_load_lessons(|_| {});
    ui.on_navigate_home(|| {});
    ui.on_navigate_auth(|| {});
    ui.on_show_otp_screen(|| {});
    ui.on_show_test_runner(|| {});
    ui.on_show_test_result(|| {});
    ui.on_close_question_sheet(|| {});

    // Request OTP
    {
        let ui_handle = ui.as_weak();
        let st = state.clone();
        let api = state.api_client();
        let h = handle.clone();
        ui.on_request_otp(move |contact: slint::SharedString| {
            let ui_handle = ui_handle.clone();
            let st = st.clone();
            let api = api.clone();
            let contact = contact.to_string();
            h.spawn(async move {
                if let Some(h) = ui_handle.upgrade() { h.set_boot_loading(true); }
                match api.request_otp(&contact).await {
                    Ok(_) => {
                        st.set_pending_contact(contact.clone());
                        let _ = slint::invoke_from_event_loop(move || {
                            if let Some(h) = ui_handle.upgrade() {
                                h.set_boot_loading(false);
                                h.set_boot_error(slint::SharedString::default());
                            }
                        });
                    }
                    Err(e) => {
                        let msg = e.to_string();
                        let _ = slint::invoke_from_event_loop(move || {
                            if let Some(h) = ui_handle.upgrade() {
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
        let ui_handle = ui.as_weak();
        let st = state.clone();
        let api = state.api_client();
        let h = handle.clone();
        ui.on_verify_otp(move |code: slint::SharedString| {
            let ui_handle = ui_handle.clone();
            let st = st.clone();
            let api = api.clone();
            let code = code.to_string();
            h.spawn(async move {
                if let Some(h) = ui_handle.upgrade() { h.set_boot_loading(true); }
                let contact = st.pending_contact().unwrap_or_default();
                match api.verify_otp(&contact, &code).await {
                    Ok(user) => {
                        st.set_session(user.clone()).await;
                        let name = user.name.unwrap_or_else(|| "Student".into());
                        let _ = slint::invoke_from_event_loop(move || {
                            if let Some(h) = ui_handle.upgrade() {
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
                            if let Some(h) = ui_handle.upgrade() {
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
        let ui_handle = ui.as_weak();
        let st = state.clone();
        let h = handle.clone();
        ui.on_logout(move || {
            let st = st.clone();
            let ui_handle = ui_handle.clone();
            h.spawn(async move {
                st.clear_session().await;
                let _ = slint::invoke_from_event_loop(move || {
                    if let Some(h) = ui_handle.upgrade() {
                        h.set_step(0);
                    }
                });
            });
        });
    }

    ui.run().expect("Slint event loop exited with error");
}
