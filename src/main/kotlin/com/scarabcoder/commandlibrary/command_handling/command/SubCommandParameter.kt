package com.scarabcoder.commandlibrary.command_handling.command

import com.scarabcoder.commandlibrary.command_handling.resolver.SubCommandParameterType
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.ComponentLike
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor

data class SubCommandParameter(
    val name: String,
    val type: SubCommandParameterType,
    val description: String
) {

    fun getUsageComponent(): ComponentLike {
        return Component
            .text()
            .append(Component.text("<").color(NamedTextColor.GRAY))
            .append(Component.text(name).color(NamedTextColor.GRAY))
            .append(Component.text(">").color(NamedTextColor.GRAY))
            .hoverEvent(HoverEvent.showText(
                Component.text()
                    .append(Component.text("${this.type.displayName}").color(NamedTextColor.DARK_GRAY))
                    .append(Component.text(" $description").color(NamedTextColor.WHITE))
            ))
    }

}