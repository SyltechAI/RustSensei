package com.sylvester.rustsensei.testdoubles

import com.sylvester.rustsensei.data.RemoteComputeConsent

/** In-memory consent store. Defaults to accepted so existing tests exercise the
 *  compile path directly; flip it to drive the not-yet-consented case. */
class FakeRemoteComputeConsent(
    private var accepted: Boolean = true
) : RemoteComputeConsent {
    override fun hasAcceptedRemoteCompile(): Boolean = accepted
    override fun setAcceptedRemoteCompile(accepted: Boolean) {
        this.accepted = accepted
    }
}
