package org.jglrxavpok.localportal.common.network

import io.netty.buffer.ByteBuf
import net.minecraft.client.Minecraft
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.fml.common.network.ByteBufUtils
import net.minecraftforge.fml.common.network.simpleimpl.IMessage
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import org.jglrxavpok.localportal.common.PortalPair

class S0UpdatePortalPair(): IMessage {

    var portalPair: PortalPair? = null

    constructor(pair: PortalPair): this() {
        this.portalPair = pair
    }

    override fun fromBytes(buf: ByteBuf) {
        val name = ByteBufUtils.readUTF8String(buf)
        val nbt = ByteBufUtils.readTag(buf)!!
        portalPair = PortalPair(name).apply { readFromNBT(nbt) }
    }

    override fun toBytes(buf: ByteBuf) {
        val data = portalPair!!.writeToNBT(NBTTagCompound())
        ByteBufUtils.writeUTF8String(buf, portalPair!!.mapName)
        ByteBufUtils.writeTag(buf, data)
    }

    object Handler: IMessageHandler<S0UpdatePortalPair, IMessage?> {

        @SideOnly(Side.CLIENT)
        override fun onMessage(message: S0UpdatePortalPair, ctx: MessageContext): IMessage? {
            val pair = message.portalPair!!
            Minecraft.getMinecraft().world.setData(pair.mapName, pair)
            return null
        }
    }
}