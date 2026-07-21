// Slint build script — compiles .slint UI files into Rust modules.
use slint_build::CompilerConfiguration;

fn main() {
    let config = CompilerConfiguration::new()
        .with_style("material".into())
        .with_include_paths(vec!["ui".into()]);

    slint_build::compile_with_config("ui/app.slint", config)
        .expect("Slint build failed");
    println!("cargo:rerun-if-changed=ui/app.slint");
    println!("cargo:rerun-if-changed=ui/screens");
    println!("cargo:rerun-if-changed=ui/components");
}
