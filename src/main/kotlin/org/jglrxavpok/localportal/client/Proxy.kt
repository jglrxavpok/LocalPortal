package org.jglrxavpok.localportal.client

import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GLAllocation
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.culling.Frustum
import net.minecraft.client.renderer.texture.TextureMap
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.entity.Entity
import net.minecraft.entity.passive.EntityCow
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
import org.jglrxavpok.localportal.extensions.angleTo
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL11.GL_SCISSOR_TEST
import org.lwjgl.opengl.GL11.glDisable
import org.lwjgl.opengl.GL11.glStencilMask
import java.nio.ByteBuffer

@Mod.EventBusSubscriber(value = arrayOf(Side.CLIENT), modid = LocalPortal.ModID)
class Proxy: LocalPortalProxy() {

    private val portalRenderRequests = mutableListOf<PortalRenderRequest>()
    private val cameraEntity = EntityCamera()
    private var lockRequests = false

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
            val mc = Minecraft.getMinecraft()
            if(mc.world == null)
                return
            cameraEntity.world = mc.world
            val renderer = mc.entityRenderer
            val partialTicks = event.renderTickTime
            val time = System.nanoTime()
            val framebuffer = mc.framebuffer
            lockRequests = true
            for((portalIndex, request) in portalRenderRequests.withIndex()) {
                val texID = getTextureOrLoad(portalIndex)

                val settings = mc.gameSettings
                val fovSave = settings.fovSetting
                val hideGuiSave = settings.hideGUI
                val thirdPersonViewSave = settings.thirdPersonView
                val prevW = framebuffer.framebufferWidth
                val prevH = framebuffer.framebufferHeight
                val prevRenderEntity = mc.renderViewEntity!!
                settings.thirdPersonView = 0
                settings.hideGUI = true
                mc.renderViewEntity = cameraEntity
                mc.displayWidth = PortalFrameBufferWidth
                mc.displayHeight = PortalFrameBufferHeight
                val origin = if(request.isFirstInPair) request.pair.firstPortalOrigin else request.pair.secondPortalOrigin
                val otherOrigin = if(!request.isFirstInPair) request.pair.firstPortalOrigin else request.pair.secondPortalOrigin
                val otherFacing = PortalLocator.getFrameInfosAt(otherOrigin, mc.world).frameType.facing
                cameraEntity.posX = otherOrigin.x+otherFacing.directionVec.x.toDouble()
                cameraEntity.posY = otherOrigin.y+prevRenderEntity.eyeHeight.toDouble()
                cameraEntity.posZ = otherOrigin.z+otherFacing.directionVec.z.toDouble()
                cameraEntity.lastTickPosX = cameraEntity.posX
                cameraEntity.lastTickPosY = cameraEntity.posY
                cameraEntity.lastTickPosZ = cameraEntity.posZ
                val angleDiff = request.infos.frameType.facing.angleTo(otherFacing) + 180f
                cameraEntity.rotationPitch = prevRenderEntity.rotationPitch
                cameraEntity.rotationYaw = prevRenderEntity.rotationYaw + angleDiff.toFloat()
                cameraEntity.rotationYawHead = prevRenderEntity.rotationYawHead + angleDiff.toFloat()
                cameraEntity.prevRotationPitch = cameraEntity.rotationPitch
                cameraEntity.prevRotationYaw = cameraEntity.rotationYaw
                settings.fovSetting = 70f

                renderer.renderWorld(1f, time+1)

                mc.renderViewEntity = prevRenderEntity
                mc.displayWidth = prevW
                mc.displayHeight = prevH
                settings.thirdPersonView = thirdPersonViewSave
                settings.hideGUI = hideGuiSave
                settings.fovSetting = fovSave

                GlStateManager.bindTexture(texID)
                glCopyTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, 0,0, PortalFrameBufferWidth, PortalFrameBufferHeight, 0)
                GlStateManager.bindTexture(0)
            }
            lockRequests = false
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
        val texID = GlStateManager.generateTexture()
        GlStateManager.bindTexture(texID)
        // empty texture
        GlStateManager.glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, PortalFrameBufferWidth, PortalFrameBufferHeight, 0, GL_RGB, GL_UNSIGNED_INT, BufferUtils.createIntBuffer(3 * PortalFrameBufferWidth* PortalFrameBufferHeight))
        GlStateManager.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
        GlStateManager.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
        PortalTextureIDs[portalIndex] = texID
    }

    companion object {
        val PortalTextureLocations = Array(255) { i ->
            LocalPortalRenderer.END_SKY_TEXTURE // TODO
        }

        val PortalTextureIDs = IntArray(255).apply { fill(-1) }

        // TODO: put this in a config file
        val PortalFrameBufferWidth = 1080/2
        val PortalFrameBufferHeight = 1080/2
    }

    override fun requestPortalRender(portalRenderRequest: PortalRenderRequest): Int {
        if(lockRequests) {
            return 0
        }
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