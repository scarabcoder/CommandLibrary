package com.scarabcoder.commandlibrary.api

import javax.naming.OperationNotSupportedException

abstract class RootCommandSection(name: String) : CommandSection(name) {

    override var parent: CommandSection?
        get() = null
        set(_) {
            throw OperationNotSupportedException("Root sections cannot have a parent!")
        }

}