package com.kapcode.open.macropad.kmp

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform