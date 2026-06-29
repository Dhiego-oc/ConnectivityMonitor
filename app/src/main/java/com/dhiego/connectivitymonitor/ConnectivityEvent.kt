package com.dhiego.connectivitymonitor

data class ConnectivityEvent(
    val timestamp: Long,
    val type: String,
    val description: String
)