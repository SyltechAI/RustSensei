package com.sylvester.rustsensei.data

/**
 * Remembers whether the user agreed to send editor code off-device.
 *
 * Compile and Run tests post the Rust source in the editor to the public Rust
 * Playground. Everything else in RustSensei is local, so that crossing is gated
 * on an explicit opt-in. Narrow interface (rather than the whole
 * [PreferencesManager]) so ViewModels stay unit-testable on the JVM.
 */
interface RemoteComputeConsent {
    fun hasAcceptedRemoteCompile(): Boolean
    fun setAcceptedRemoteCompile(accepted: Boolean)
}
