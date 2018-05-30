package org.jglrxavpok.localportal.common

import net.minecraftforge.fml.relauncher.Side
import org.jglrxavpok.localportal.LocalPortal
import org.jglrxavpok.localportal.common.network.C1PortalPairRequest
import org.jglrxavpok.localportal.common.network.S0UpdatePortalPair

abstract class LocalPortalProxy {
    open fun init() {
        LocalPortal.network.registerMessage(S0UpdatePortalPair.Handler, S0UpdatePortalPair::class.java, 0, Side.CLIENT)
        LocalPortal.network.registerMessage(C1PortalPairRequest.Handler, C1PortalPairRequest::class.java, 1, Side.SERVER)
    }

    open fun preInit() {
    }

    abstract fun requestPortalRender(portalRenderRequest: PortalRenderRequest): Int

}

data class PortalRenderRequest(val infos: PortalLocator.PortalFrameInfos, val pair: PortalPair, val isFirstInPair: Boolean)
object EmptyPortalPair : PortalPair("local_portal:empty_portal_pair")
val EmptyPortalRequest = PortalRenderRequest(PortalLocator.NoInfos, EmptyPortalPair, true)
