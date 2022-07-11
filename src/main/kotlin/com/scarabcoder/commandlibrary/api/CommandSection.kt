package com.scarabcoder.commandlibrary.api

import com.scarabcoder.commandlibrary.command_handling.command.SubCommand
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.ComponentLike
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor

abstract class CommandSection(val name: String) {

    open val aliases: List<String> = emptyList()
    open val description: String? = null
    internal var children: List<CommandSection> = emptyList()
    internal open var parent: CommandSection? = null

    internal fun getCurrentPath(): List<String> {
        if (this.parent == null) {
            return listOf(name)
        }

        return this.parent!!.getCurrentPath() + name
    }

    internal fun getCurrentPathComponent(objectValues: Map<String, Any> = emptyMap(), showArgumentTemplates: Boolean = true): Component {
        val objectDesc: ComponentLike = if (this is ObjectCommandSection) {
            if (objectValues.containsKey(this.objectParameterId)) {
                Component.text(" " + objectValues[this.objectParameterId].toString()).color(NamedTextColor.GRAY)
            } else if(showArgumentTemplates) {
                Component.text()
                    .append(Component.text(" <").color(NamedTextColor.GRAY))
                    .append(parameterMessageComponent.color(NamedTextColor.GRAY))
                    .append(Component.text(">").color(NamedTextColor.GRAY))
            } else {
                Component.text()
            }
        } else {
            Component.text()
        }

        val currentPathComp = Component
            .text()
            .append(
                Component
                    .text(this.name)
                    .append(objectDesc)
                    .hoverEvent(
                        HoverEvent.showText(
                            Component.text()
                                .append(Component.text(this.description ?: ""))
                                .append(
                                    Component.text(" (" + this.aliases.joinToString(", ") + ")")
                                        .color(NamedTextColor.GRAY)
                                )
                        )
                    )
            ).asComponent()
        if (this.parent == null) {
            return currentPathComp
        }
        return Component.text()
            .append(this.parent!!.getCurrentPathComponent(objectValues))
            .append(Component.text(" "))
            .append(currentPathComp)
            .asComponent()
    }

    fun getUsage(command: SubCommand): Component {
        val component = Component.text()
        if (this is ObjectCommandSection) {
            component.append(
                Component
                    .text()
                    .append(Component.text("<").color(NamedTextColor.GRAY))
                    .append(Component.text(this.objectName).color(NamedTextColor.GOLD))
                    .append(Component.text(">").color(NamedTextColor.GRAY))
                    .hoverEvent(HoverEvent.showText(Component.text(this.objectDescription).color(NamedTextColor.GRAY)))
            )
        }
        command.parameters.forEachIndexed { index, it ->
            if (index != 0) {
                component
                    .append(Component.text(" "))
            }
            component.append(
                Component.text()
                    .append(Component.text("<").color(NamedTextColor.GRAY))
                    .append(
                        Component.text(it.name).color(NamedTextColor.GOLD)
                            .append(Component.text(">").color(NamedTextColor.GRAY))
                            .hoverEvent(
                                HoverEvent.showText(
                                    Component.text(it.description).color(NamedTextColor.GRAY)
                                )
                            )
                    )
            )
        }

        return component.asComponent()
    }

    fun children(vararg children: CommandSection) {
        this.children = children.toList()
        children.forEach { it.parent = this }
    }

}