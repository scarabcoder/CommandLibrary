package com.scarabcoder.commandlibrary.annotation

annotation class Command(
    val description: String = "",
    val permission: String = "",
    val noPermRequired: Boolean = false,
    val aliases: Array<String> = []
)
