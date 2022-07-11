package com.scarabcoder.commandlibrary.command_handling.command

import com.destroystokyo.paper.event.server.AsyncTabCompleteEvent.Completion
import org.bukkit.command.CommandSender

interface ICommandWithTabComplete {

    fun doTabComplete(sender: CommandSender, args: List<String>): List<Completion>
}