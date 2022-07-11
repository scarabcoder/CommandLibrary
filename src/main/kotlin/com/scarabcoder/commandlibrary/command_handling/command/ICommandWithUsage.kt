package com.scarabcoder.commandlibrary.command_handling.command

import net.kyori.adventure.text.Component

interface ICommandWithUsage {

    val usageComponent: Component

    fun getUsageComponentWithFilledObjects(cmdObjects: Map<String, Any>, showArgumentTemplates: Boolean = true): Component

}