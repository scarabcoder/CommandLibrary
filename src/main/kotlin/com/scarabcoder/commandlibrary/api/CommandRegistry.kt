package com.scarabcoder.commandlibrary.api

import com.scarabcoder.commandlibrary.command_handling.CommandHandler
import com.scarabcoder.commandlibrary.command_handling.resolver.CommandSenderResolver
import com.scarabcoder.commandlibrary.command_handling.resolver.SubCommandParameterType
import com.scarabcoder.commandlibrary.exception.CommandSenderResolutionException
import com.scarabcoder.commandlibrary.exception.ParameterParseException
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

object CommandRegistry {

    internal val commandSenders: MutableList<CommandSenderResolver> = mutableListOf()
    internal val parameterTypes: MutableList<SubCommandParameterType> = mutableListOf()

    init {
        registerCommandSender(CommandSenderResolver(Player::class) {
            if (it !is Player) throw CommandSenderResolutionException("This is a player-only command!")
            it
        })

        registerCommandSender(CommandSenderResolver(CommandSender::class) {
            it
        })

        registerParameterType(
            SubCommandParameterType(
                String::class,
                { it, _ -> it }
            )
        )

        registerParameterType(
            SubCommandParameterType(
                World::class,
                { worldName, _ -> Bukkit.getWorld(worldName)!! },
                { _ -> Bukkit.getWorlds().map { it.name } }
            )
        )

        registerParameterType(
            SubCommandParameterType(
                Int::class,
                { it, _ -> it.toIntOrNull() ?: throw ParameterParseException("Invalid value, expected a number") }
            )
        )

        registerParameterType(
            SubCommandParameterType(
                Player::class,
                { it, _ -> Bukkit.getPlayer(it) ?: throw ParameterParseException("Player '$it' not found!") },
                { _ -> Bukkit.getOnlinePlayers().map { it.name } }
            )
        )
    }

    fun registerCommand(commandSection: RootCommandSection, plugin: JavaPlugin) {
        Bukkit.getServer().commandMap.register(plugin.name, CommandHandler(commandSection, plugin))
    }

    fun registerParameterType(parameterType: SubCommandParameterType) {
        parameterTypes.add(parameterType)
    }

    fun registerCommandSender(commandSenderResolver: CommandSenderResolver) {
        commandSenders.add(commandSenderResolver)
    }

}