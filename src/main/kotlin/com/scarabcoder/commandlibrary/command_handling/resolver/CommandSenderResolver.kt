package com.scarabcoder.commandlibrary.command_handling.resolver

import org.bukkit.command.CommandSender
import kotlin.reflect.KClass

class CommandSenderResolver(val commandSenderClass: KClass<*>, val resolve: (CommandSender) -> Any) {
}