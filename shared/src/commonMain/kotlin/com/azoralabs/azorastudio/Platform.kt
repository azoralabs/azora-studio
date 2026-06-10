package com.azoralabs.azorastudio

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform