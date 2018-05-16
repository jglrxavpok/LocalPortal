package org.jglrxavpok.localportal.server

import org.jglrxavpok.localportal.common.LocalPortalProxy
import org.jglrxavpok.localportal.common.PortalRenderRequest

class Proxy: LocalPortalProxy() {
    override fun requestPortalRender(portalRenderRequest: PortalRenderRequest): Int {
        return 0
    }

}