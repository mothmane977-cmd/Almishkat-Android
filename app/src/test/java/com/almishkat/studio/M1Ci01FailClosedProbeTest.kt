package com.almishkat.studio

import org.junit.Assert.fail
import org.junit.Test

/**
 * ADM-M1-213 — DELIBERATE FAILURE for isolated GitHub required-check verification.
 * NEVER merge this probe into main or develop.
 * Production code, real regression assertions, and workflow files remain unchanged.
 */
class M1Ci01FailClosedProbeTest {
    @Test
    fun deliberatelyFailsToProveRequiredDbTestsBlocksMerge() {
        fail("ADM-M1-213 intentional CI failure: isolated test PR; DO NOT MERGE")
    }
}
