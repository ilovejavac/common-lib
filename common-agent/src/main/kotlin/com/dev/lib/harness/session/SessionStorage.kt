package com.dev.lib.harness.session

interface SessionStorage {
    fun getSession(id: String): AgentSession
}