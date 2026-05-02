package com.kapcode.open.macropad.kmps

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

expect fun openFolder(path: String)

expect fun pickDirectory(onResult: (String?) -> Unit)