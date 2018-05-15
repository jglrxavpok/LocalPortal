package org.jglrxavpok.localportal.common

import net.minecraftforge.fml.relauncher.Side
import org.jglrxavpok.localportal.LocalPortal
import org.jglrxavpok.localportal.common.network.S0UpdatePortalPair

open class LocalPortalProxy {
    open fun init() {
        LocalPortal.network.registerMessage(S0UpdatePortalPair.Handler, S0UpdatePortalPair::class.java, 0, Side.CLIENT)
    }

    open fun preInit() {
    }

}