package org.jglrxavpok.localportal.common.network

import io.netty.buffer.ByteBuf
import net.minecraftforge.fml.common.network.simpleimpl.IMessage
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext
import org.jglrxavpok.localportal.common.PortalLocator

class C1PortalPairRequest(): IMessage {

    private var portalID = -1

    constructor(portalID: Int): this() {
        this.portalID = portalID
    }

    override fun fromBytes(buf: ByteBuf) {
        portalID = buf.readInt()
    }

    override fun toBytes(buf: ByteBuf) {
        buf.writeInt(portalID)
    }

    object Handler: IMessageHandler<C1PortalPairRequest, S0UpdatePortalPair?> {
        override fun onMessage(message: C1PortalPairRequest, ctx: MessageContext): S0UpdatePortalPair? {
            val world = ctx.serverHandler.player.world
            val pair = PortalLocator.getPortalPair(message.portalID, world) ?: return null
            return S0UpdatePortalPair(pair)
        }
    }
}