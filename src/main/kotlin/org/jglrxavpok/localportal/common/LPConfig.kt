package org.jglrxavpok.localportal.common

import net.minecraft.init.Items as MCItems
import net.minecraftforge.common.config.Configuration

object LPConfig {

    internal lateinit var backing: Configuration
    val renderOthersideOfPortal: Boolean
        get() = backing.getBoolean("renderOthersideOfPortal", "LocalPortal", true, "Should portal render the world as seen from the other side?")

    fun loadAll() {
        backing.load()

        // load them by calling them
        renderOthersideOfPortal

        // allows for defaults to be saved on first load
        backing.save()
    }


}