package com.scarabcoder.commandlibrary.exception

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

class ParameterParseException(
    message: String,
    val formattedMessage: Component = Component.text(message).color(NamedTextColor.RED)
) : Exception(message)