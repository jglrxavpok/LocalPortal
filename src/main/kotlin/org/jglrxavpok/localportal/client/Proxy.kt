package org.jglrxavpok.localportal.client

import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.client.registry.RenderingRegistry
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.relauncher.Side
import org.jglrxavpok.localportal.LocalPortal
import org.jglrxavpok.localportal.common.LocalPortalProxy

@Mod.EventBusSubscriber(value = arrayOf(Side.CLIENT), modid = LocalPortal.ModID)
class Proxy: LocalPortalProxy() {

    override fun preInit() {
        MinecraftForge.EVENT_BUS.register(this)
        super.preInit()
    }
}