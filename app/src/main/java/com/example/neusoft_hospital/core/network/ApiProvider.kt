package com.example.neusoft_hospital.core.network

/**
 * Central toggle for switching the app between local-mock mode and the real
 * Spring Boot backend.
 *
 * `useMock = true`  → repositories receive *Mock* implementations (no network).
 * `useMock = false` → repositories receive Retrofit-backed implementations.
 *
 * Flip [useMock] in a debug-only menu (or via a settings file) to switch modes
 * without rebuilding from a different source set.
 */
object ApiProvider {
    /** Master switch. When false, Retrofit clients are wired and real HTTP traffic flows. */
    var useMock: Boolean = true

    /** Simulated network latency when running against mocks. */
    const val MOCK_DELAY_MS = 600L

    /**
     * Real backend base URL. Trailing slash required by Retrofit.
     * Override via gradle property `-PbackendBaseUrl=...` or by editing here for local dev.
     */
    var backendBaseUrl: String = "http://172.22.6.51:8090/"

    /** Convenience for the existing field that some modules still read. */
    val baseUrl: String
        get() = if (useMock) "" else backendBaseUrl
}