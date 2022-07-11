package com.scarabcoder.commandlibrary.command_handling

import com.destroystokyo.paper.event.server.AsyncTabCompleteEvent
import com.scarabcoder.commandlibrary.api.CommandRegistry
import com.scarabcoder.commandlibrary.api.ObjectCommandSection
import com.scarabcoder.commandlibrary.api.RootCommandSection
import com.scarabcoder.commandlibrary.command_handling.command.ICommandWithUsage
import com.scarabcoder.commandlibrary.command_handling.command.ResolvedCommandSection
import com.scarabcoder.commandlibrary.exception.ParameterParseException
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin
import kotlin.math.ceil
import kotlin.math.min

class CommandHandler(private val section: RootCommandSection, plugin: JavaPlugin) : Command(section.name), Listener {

    private val resolvedCommandSection = ResolvedCommandSection(section, plugin)

    init {
        Bukkit.getServer().pluginManager.registerEvents(this, plugin)
    }

    override fun getDescription(): String {
        return section.description ?: ""
    }

    override fun execute(sender: CommandSender, commandLabel: String, args: Array<out String>): Boolean {
        handleChild(sender, resolvedCommandSection, args.toList(), mutableMapOf())

        return true
    }

    private fun handleChild(
        sender: CommandSender,
        resolvedSection: ResolvedCommandSection,
        args: List<String>,
        commandSectionObjects: MutableMap<String, Any>
    ) {
        val mutableArgs = args.toMutableList()

        if (resolvedSection.section is ObjectCommandSection) {
            val objSection = resolvedSection.section as ObjectCommandSection
            if (mutableArgs.size == 0) {
                sender.sendMessage(
                    Component
                        .text()
                        .append(Component.text("Missing required parameter "))
                        .append(resolvedSection.section.parameterMessageComponent.color(NamedTextColor.GOLD))
                        .append(Component.text("!"))
                        .color(NamedTextColor.RED)
                )
                sender.sendMessage(
                    Component
                        .text()
                        .append(Component.text("Usage: /").color(NamedTextColor.GRAY))
                        .append(resolvedSection.section.getCurrentPathComponent().color(NamedTextColor.WHITE))
                        .append(Component.text(" ...").color(NamedTextColor.GRAY))
                )
                return
            }

            val parameterType = CommandRegistry.parameterTypes.find { it.type == objSection.objectClass }
            val parameter: Any

            try {
                parameter = parameterType!!.resolve(mutableArgs[0], sender)
            } catch (exception: ParameterParseException) {
                sender.sendMessage(exception.formattedMessage)
                return
            }

            commandSectionObjects[objSection.objectParameterId] = parameter
            mutableArgs.removeAt(0)
        }

        if (mutableArgs.isEmpty()) {
            showHelp(sender, resolvedSection, 0, commandSectionObjects)
            return
        }

        if (mutableArgs[0] == "help") {
            val page = if (mutableArgs.size > 1) {
                mutableArgs[1].toIntOrNull()?.minus(1) ?: 0
            } else {
                0
            }

            showHelp(sender, resolvedSection, page, commandSectionObjects)
            return
        }

        val subCommand =
            resolvedSection.subCommands.find { it.name == mutableArgs[0] || it.aliases.contains(mutableArgs[0]) }
        if (subCommand !== null) {
            subCommand.execute(sender, mutableArgs.subList(1, mutableArgs.size), commandSectionObjects.toMap())
            return
        }

        val childSection = resolvedSection.resolvedChildren.find {
            it.section.name == mutableArgs[0] || it.section.aliases.contains(mutableArgs[0])
        }

        if (childSection == null) {
            showHelp(sender, resolvedSection, 0, commandSectionObjects)
            return
        }

        mutableArgs.removeAt(0)
        handleChild(sender, childSection, mutableArgs.toList(), commandSectionObjects.toMutableMap())
    }

    private fun showHelp(
        commandSender: CommandSender,
        section: ResolvedCommandSection,
        page: Int,
        cmdObjects: Map<String, Any>
    ) {
        val maxElements = section.subCommands.size + section.resolvedChildren.size
        val perPage = 10
        val maxPages = ceil(maxElements.toDouble() / perPage.toDouble()).toInt()
        val actualPage = if (page >= maxPages || page < 0) maxPages - 1 else page

        val emptyPageArrowComp = Component.text("    ").color(NamedTextColor.GRAY)

        val helpCmdBase =
            PlainTextComponentSerializer.plainText()
                .serialize(section.getUsageComponentWithFilledObjects(cmdObjects)) + " help "
        val nextSpace = "    "

        val previousPageComp = if (actualPage == 0)
            emptyPageArrowComp.append(Component.text(nextSpace))
        else
            Component.text("[prev]").color(NamedTextColor.DARK_AQUA)
                .decorate(TextDecoration.ITALIC)
                .decorate(TextDecoration.UNDERLINED)
                .hoverEvent(HoverEvent.showText(Component.text("Previous page")))
                .clickEvent(ClickEvent.runCommand(helpCmdBase + (page)))
        val nextPageComp = if (actualPage == maxPages - 1)
            Component.text(nextSpace).append(emptyPageArrowComp)
        else
            Component.text("[next]").color(NamedTextColor.DARK_AQUA)
                .decorate(TextDecoration.ITALIC)
                .decorate(TextDecoration.UNDERLINED)
                .hoverEvent(HoverEvent.showText(Component.text("Next page")))
                .clickEvent(ClickEvent.runCommand(helpCmdBase + (page + 2)))

        commandSender.sendMessage(
            Component.text()
                .append(previousPageComp)
                .append(
                    Component.text(" ${section.section.name} page ${actualPage + 1} of $maxPages ")
                        .color(NamedTextColor.GOLD)
                )
                .append(nextPageComp)
        )

        val sectionChildren: List<ICommandWithUsage> =
            (section.resolvedChildren + section.subCommands).subList(
                actualPage * perPage,
                min(actualPage * perPage + perPage, maxElements)
            )

        for (child in sectionChildren) {
            var usageComponent = Component
                .text()
                .append(
                    Component.text(": :")
                        .color(NamedTextColor.DARK_AQUA)
                        .decorate(TextDecoration.UNDERLINED)
                        .hoverEvent(HoverEvent.showText(Component.text("Click to autofill command in chat bar")))
                        .clickEvent(
                            ClickEvent.suggestCommand(
                                PlainTextComponentSerializer
                                    .plainText()
                                    .serialize(child.getUsageComponentWithFilledObjects(cmdObjects, false))
                                    .plus(" ")
                            )
                        )
                )
                .append(Component.text(" "))
                .append(
                    child.getUsageComponentWithFilledObjects(cmdObjects)
                )
            if (child is ResolvedCommandSection) {
                usageComponent = usageComponent.append(Component.text(" ...").color(NamedTextColor.GRAY))
            }
            commandSender.sendMessage(
                usageComponent
            )
        }
    }

    @EventHandler
    fun onTabComplete(event: AsyncTabCompleteEvent) {
        if (!event.isCommand) {
            return
        }
        val cmdArgs = event.buffer.split(" ")
        val cmd = cmdArgs[0].substring(1)
        if (cmd != this.section.name && !this.section.aliases.contains(cmd)) {
            return
        }

        event.isHandled = true
        val args = cmdArgs.subList(1, cmdArgs.size).toMutableList()

        event.completions(this.resolvedCommandSection.doTabComplete(event.sender, args))
    }

}