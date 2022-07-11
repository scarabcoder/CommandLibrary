package com.scarabcoder.commandlibrary.command_handling.command

import com.destroystokyo.paper.event.server.AsyncTabCompleteEvent.Completion
import com.scarabcoder.commandlibrary.annotation.Argument
import com.scarabcoder.commandlibrary.annotation.CmdObject
import com.scarabcoder.commandlibrary.api.CommandSection
import com.scarabcoder.commandlibrary.api.ObjectCommandSection
import com.scarabcoder.commandlibrary.command_handling.resolver.CommandSenderResolver
import com.scarabcoder.commandlibrary.exception.CommandSenderResolutionException
import com.scarabcoder.commandlibrary.exception.ParameterParseException
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.ComponentLike
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.CommandSender
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter

data class SubCommand(
    val name: String,
    val parentSection: CommandSection,
    val aliases: List<String>,
    val description: String,
    val parameters: List<SubCommandParameter>,
    internal val function: KFunction<*>,
    internal val commandSenderResolver: CommandSenderResolver
) : ICommandWithUsage, ICommandWithTabComplete {

    private fun getCurrentPath(): List<String> {
        return this.parentSection.getCurrentPath() + this.name
    }

    private fun getCurrentPathComponent(
        cmdObjects: Map<String, Any> = emptyMap(),
        showArgumentTemplates: Boolean = true
    ): ComponentLike {
        return this
            .parentSection
            .getCurrentPathComponent(cmdObjects, showArgumentTemplates)
            .append(
                Component
                    .text(" $name")
                    .color(NamedTextColor.WHITE)
                    .hoverEvent(
                        HoverEvent.showText(
                            Component.text()
                                .append(Component.text(description).color(NamedTextColor.WHITE))
                                .append(
                                    Component.text(" (${this.aliases.joinToString(", ")})").color(NamedTextColor.GRAY)
                                )
                        )
                    )
            )
    }

    override fun doTabComplete(sender: CommandSender, args: List<String>): List<Completion> {
        if (args.isEmpty()) {
            return emptyList()
        }
        val offset = args.size
        if (offset > this.parameters.size) {
            return emptyList()
        }

        val relevantParameter = this.parameters[args.size - 1]

        return relevantParameter.type.tabCompleteHandler(sender)
            .map { Completion.completion(it, Component.text(relevantParameter.description)) }
    }

    override val usageComponent: Component
        get() {
            val comp = Component.text()
                .append(Component.text("/").color(NamedTextColor.GRAY))
                .append(
                    getCurrentPathComponent()
                )
            this.parameters.forEach {
                comp
                    .append(Component.text(" "))
                    .append(it.getUsageComponent())
            }
            return comp.asComponent()
        }

    override fun getUsageComponentWithFilledObjects(
        cmdObjects: Map<String, Any>,
        showArgumentTemplates: Boolean
    ): Component {
        val comp = Component.text()
            .append(Component.text("/").color(NamedTextColor.GRAY))
            .append(
                getCurrentPathComponent(cmdObjects, showArgumentTemplates)
            )
        if (showArgumentTemplates) {
            this.parameters.forEach {
                comp
                    .append(Component.text(" "))
                    .append(it.getUsageComponent())
            }
        }
        return comp.asComponent()
    }

    fun execute(sender: CommandSender, args: List<String>, cmdObjects: Map<String, Any>) {
        var requiredParamsSize = parameters.size
        if (parentSection is ObjectCommandSection) {
            requiredParamsSize += 1 // Extra parameter required for the object reference command section
        }
        if (args.size != parameters.size) {
            sender.sendMessage(
                Component.text("Expected ${parameters.size} parameters, received ${args.size}")
                    .color(NamedTextColor.RED)
            )

            sender.sendMessage(
                Component.text()
                    .append(
                        Component.text(
                            "Usage: "
                        ).color(NamedTextColor.GRAY)
                    )
                    .append(usageComponent)
            )
            return
        }

        val processedSender: Any
        try {
            processedSender = this.commandSenderResolver.resolve(sender)
        } catch (exception: CommandSenderResolutionException) {
            sender.sendMessage(exception.formattedMessage)
            return
        }

        var kParams = function.parameters.toList()
        val processedParameters = mutableMapOf(
            kParams[0] to this.parentSection,
            kParams[1] to processedSender
        )
        kParams = kParams
            .filter { kFunc -> kFunc.annotations.any { it is Argument } }
        val objParams: Map<KParameter, Any> = function.parameters
            .filter { kParam -> kParam.annotations.any { it is CmdObject } }
            .associateWith { kParam -> cmdObjects[(kParam.annotations.find { it is CmdObject } as CmdObject).id]!! }

        for ((index, parameter) in this.parameters.withIndex()) {
            try {
                processedParameters[kParams[index]] = parameter.type.resolve(args[index], sender)
            } catch (exception: ParameterParseException) {
                sender.sendMessage(exception.formattedMessage)
                return
            }
        }

        this.function.callBy(processedParameters + objParams)
    }

}