// Slint build script — compiles the single app.slint file into Rust.
use slint_build::CompilerConfiguration;

fn main() {
    let config = CompilerConfiguration::new();
    slint_build::compile_with_config("ui/app.slint", config)
        .expect("Slint build failed");
    println!("cargo:rerun-if-changed=ui/app.slint");
}
