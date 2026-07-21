//! Library crate entry point for Android (cdylib).
//!
//! On Android, Slint's `android-activity` backend calls `android_main`
//! through this library. The `bin` crate (main.rs) is used on desktop.
//!
//! On desktop targets, this file compiles to nothing useful — the bin
//! crate is what runs.

#[cfg(target_os = "android")]
slint::include_modules!();

#[cfg(target_os = "android")]
#[no_mangle]
pub fn android_main(app: slint::android::AndroidApp) {
    use slint::ComponentHandle;

    // Initialize the Slint Android backend with the AndroidApp handle.
    slint::android::init(app.clone());

    // Boot the same UI as desktop
    let ui = App::new().expect("Failed to launch Slint UI");
    ui.run().expect("Slint event loop exited with error");
}
