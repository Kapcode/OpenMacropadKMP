package com.kapcode.open.macropad.kmps

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform