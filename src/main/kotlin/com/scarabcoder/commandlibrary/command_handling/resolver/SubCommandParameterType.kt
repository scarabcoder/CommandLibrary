package com.scarabcoder.commandlibrary.command_handling.resolver

import org.bukkit.command.CommandSender
import kotlin.reflect.KClass

class SubCommandParameterType(
    val type: KClass<*>,
    val resolve: (String, CommandSender) -> Any,
    val tabCompleteHandler: (CommandSender) -> List<String> = { _ -> emptyList() },
    val displayName: String = type.simpleName!!
)