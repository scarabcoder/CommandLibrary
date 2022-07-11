package com.scarabcoder.commandlibrary.command_handling.command

import com.destroystokyo.paper.event.server.AsyncTabCompleteEvent
import com.scarabcoder.commandlibrary.annotation.Argument
import com.scarabcoder.commandlibrary.api.CommandRegistry
import com.scarabcoder.commandlibrary.api.CommandSection
import com.scarabcoder.commandlibrary.api.ObjectCommandSection
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.CommandSender
import org.bukkit.plugin.java.JavaPlugin
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.functions
import kotlin.reflect.full.hasAnnotation

internal class ResolvedCommandSection(val section: CommandSection, private val plugin: JavaPlugin) :
    ICommandWithUsage,
    ICommandWithTabComplete {

    val subCommands: List<SubCommand>
    val resolvedChildren: List<ResolvedCommandSection>

    override val usageComponent: Component
        get() = Component
            .text()
            .append(Component.text("/").color(NamedTextColor.GRAY))
            .append(section.getCurrentPathComponent().color(NamedTextColor.WHITE))
            .append(Component.text(" ...").color(NamedTextColor.GRAY))
            .asComponent()

    init {
        this.subCommands = findSubCommands(section::class)
        this.resolvedChildren = section.children.map { ResolvedCommandSection(it, plugin) }
    }

    override fun doTabComplete(sender: CommandSender, args: List<String>): List<AsyncTabCompleteEvent.Completion> {
        val mutableArgs = args.toMutableList()
        if (this.section is ObjectCommandSection) {
            if (mutableArgs.isEmpty()) {
                return emptyList()
            }
            if (mutableArgs.size == 1) {
                val parameterType = CommandRegistry.parameterTypes.find { it.type === this.section.objectClass }!!
                return parameterType.tabCompleteHandler(sender).map {
                    AsyncTabCompleteEvent.Completion.completion(
                        it,
                        Component.text(this.section.objectDescription)
                    )
                }
            }
            mutableArgs.removeAt(0)
        }

        if (mutableArgs.size < 2) {
            return (this.subCommands.map {
                AsyncTabCompleteEvent.Completion.completion(it.name, Component.text(it.description))
            } + this.resolvedChildren.map {
                AsyncTabCompleteEvent.Completion.completion(
                    it.section.name,
                    Component.text(it.section.description ?: "")
                )
            })
        }

        val nextArg = mutableArgs[0]
        val subCommand = this.subCommands.find { it.name == nextArg || it.aliases.contains(nextArg) }
        if (subCommand !== null) {
            mutableArgs.removeAt(0)
            return subCommand.doTabComplete(sender, mutableArgs)
        }
        val commandSection =
            this.resolvedChildren.find { it.section.name == nextArg || it.section.aliases.contains(nextArg) }
        if (commandSection !== null) {
            mutableArgs.removeAt(0)
            return commandSection.doTabComplete(sender, mutableArgs)
        }
        return emptyList()
    }

    override fun getUsageComponentWithFilledObjects(
        cmdObjects: Map<String, Any>,
        showArgumentTemplates: Boolean
    ): Component {
        return Component
            .text()
            .append(Component.text("/").color(NamedTextColor.GRAY))
            .append(section.getCurrentPathComponent(cmdObjects, showArgumentTemplates).color(NamedTextColor.WHITE))
            .asComponent()
    }

    private fun findSubCommands(clazz: KClass<out CommandSection>): List<SubCommand> {
        val subCommandFunctions = clazz.functions.filter { func ->
            func.hasAnnotation<com.scarabcoder.commandlibrary.annotation.Command>()
        }

        return subCommandFunctions.mapNotNull { subCommandFunctionToSubCommand(it) }
    }

    private fun subCommandFunctionToSubCommand(func: KFunction<*>): SubCommand? {
        val commandAnnotation =
            func.annotations.find { it is com.scarabcoder.commandlibrary.annotation.Command } as com.scarabcoder.commandlibrary.annotation.Command

        if (func.parameters.size < 2) {
            plugin.logger.warning("The sub command function ${func.name} in section ${section::class.simpleName} is missing a command sender parameter (sub command not registered)")
            return null
        }

        val commandSenderClass = func.parameters[1].type.classifier as KClass<*>
        val commandSenderResolver =
            CommandRegistry.commandSenders.find { it.commandSenderClass == commandSenderClass }
        if (commandSenderResolver == null) {
            plugin.logger.warning("The sub command function ${func.name} in section ${section::class.simpleName} has an invalid command sender parameter, no handler found for ${commandSenderClass.simpleName}")
            return null
        }

        val parameters = findSubCommandParameters(func) ?: return null

        return SubCommand(
            func.name,
            section,
            commandAnnotation.aliases.toList(),
            commandAnnotation.description,
            parameters,
            func,
            commandSenderResolver
        )
    }

    private fun findSubCommandParameters(func: KFunction<*>): List<SubCommandParameter>? {
        val funcParameters = func.parameters.toMutableList()
        funcParameters.removeAt(0) // Remove the instance
        funcParameters.removeAt(0) // Remove command sender

        val parameters = funcParameters
            .filter { kParam -> kParam.annotations.any { it is Argument } }
            .map { funcParameterToSubCommandParameter(it, func) }
        if (parameters.any { it == null }) {
            return null
        }

        return parameters.filterNotNull()
    }

    private fun funcParameterToSubCommandParameter(
        parameter: KParameter,
        func: KFunction<*>
    ): SubCommandParameter? {
        val paramAnnotation = parameter.annotations.find { it.annotationClass == Argument::class } as? Argument

        val paramType = CommandRegistry.parameterTypes.find { it.type == parameter.type.classifier as KClass<*> }
        if (paramType == null) {
            plugin.logger.warning("No parameter type handler registered for type ${parameter.type.classifier} in function $func (sub command not registered)")
            return null
        }

        return SubCommandParameter(
            paramAnnotation?.name ?: parameter.name!!,
            paramType,
            paramAnnotation?.description ?: ""
        )
    }

}