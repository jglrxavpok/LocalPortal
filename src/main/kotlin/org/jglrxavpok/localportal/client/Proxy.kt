package org.jglrxavpok.localportal.client

import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GLAllocation
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.culling.Frustum
import net.minecraft.client.renderer.texture.TextureMap
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.util.BlockRenderLayer
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.client.registry.ClientRegistry
import net.minecraftforge.fml.client.registry.RenderingRegistry
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.relauncher.Side
import org.jglrxavpok.localportal.LocalPortal
import org.jglrxavpok.localportal.common.*
import org.jglrxavpok.localportal.common.PortalLocator.NoInfos
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL11.GL_SCISSOR_TEST
import org.lwjgl.opengl.GL11.glDisable
import org.lwjgl.opengl.GL11.glStencilMask
import java.nio.ByteBuffer


@Mod.EventBusSubscriber(value = arrayOf(Side.CLIENT), modid = LocalPortal.ModID)
class Proxy: LocalPortalProxy() {

    private val portalRenderRequests = mutableListOf<PortalRenderRequest>()

    override fun preInit() {
        MinecraftForge.EVENT_BUS.register(this)
        super.preInit()
    }

    override fun init() {
        super.init()
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityLocalPortal::class.java, LocalPortalRenderer)
    }

    @SubscribeEvent
    fun preRenderWorld(event: TickEvent.RenderTickEvent) {
        if(event.phase == TickEvent.Phase.START) {
            // TODO: update/fill textures
            for((portalIndex, request) in portalRenderRequests.withIndex()) {
                val texID = getTextureOrLoad(portalIndex)

            }
            portalRenderRequests.clear()
        }
    }

    private fun getTextureOrLoad(portalIndex: Int): Int {
        val id = PortalTextureIDs[portalIndex]
        if(id == -1) {
            generatePortalTexture(portalIndex)
            return PortalTextureIDs[portalIndex]
        }
        return id
    }

    private fun generatePortalTexture(portalIndex: Int) {
        val texID = glGenTextures()
        glBindTexture(GL_TEXTURE_2D, texID)
        // empty texture
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, PortalFrameBufferWidth, PortalFrameBufferHeight, 0, GL_RGBA, GL_UNSIGNED_INT, null as? ByteBuffer?)
        PortalTextureIDs[portalIndex] = texID
    }

    companion object {
        val PortalTextureLocations = Array(255) { i ->
            LocalPortalRenderer.END_SKY_TEXTURE // TODO
        }

        val PortalTextureIDs = IntArray(255).apply { fill(-1) }

        // TODO: put this in a config file
        val PortalFrameBufferWidth = 1920/2
        val PortalFrameBufferHeight = 1080/2
    }

    override fun requestPortalRender(portalRenderRequest: PortalRenderRequest): Int {
        if(portalRenderRequest == EmptyPortalRequest) {
            return 0
        }
        val same = portalRenderRequests.indexOfFirst { it == portalRenderRequest }
        if(same == -1) {
            val index = portalRenderRequests.size
            portalRenderRequests.add(portalRenderRequest)
            return index
        }
        return same
    }
}