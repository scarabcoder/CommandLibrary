package com.scarabcoder.commandlibrary.api

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.HoverEvent
import kotlin.reflect.KClass

interface ObjectCommandSection {

    val objectClass: KClass<*>
    val objectDescription: String
    val objectName: String
    val objectParameterId: String

    val parameterMessageComponent: Component
        get() = Component.text(objectParameterId).hoverEvent(HoverEvent.showText(Component.text(objectDescription)))

}