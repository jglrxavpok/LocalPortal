package org.jglrxavpok.localportal.client

import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.util.math.MathHelper
import net.minecraftforge.client.event.EntityViewRenderEvent
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.client.registry.ClientRegistry
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.relauncher.Side
import org.jglrxavpok.localportal.LocalPortal
import org.jglrxavpok.localportal.common.*
import org.jglrxavpok.localportal.common.PortalLocator.NoInfos
import org.jglrxavpok.localportal.extensions.angleTo
import org.jglrxavpok.localportal.extensions.toDegrees
import org.jglrxavpok.localportal.extensions.toRadians
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11.*
import org.lwjgl.util.glu.Project
import org.lwjgl.util.vector.Matrix4f
import org.lwjgl.util.vector.Vector3f
import kotlin.math.abs

@Mod.EventBusSubscriber(value = arrayOf(Side.CLIENT), modid = LocalPortal.ModID)
class Proxy: LocalPortalProxy() {

    private val portalRenderRequests = mutableListOf<PortalRenderRequest>()
    private val cameraEntity = EntityCamera()
    private var renderingPortalView = false
    private var nearPlane = 0.05f

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
            renderingPortalView = true
            val transformationMatrix = Matrix4f()
            for((portalIndex, request) in portalRenderRequests.withIndex()) {
                val texID = getTextureOrLoad(portalIndex)

                if(request.infos == NoInfos)
                    continue
                val origin = if(request.isFirstInPair) request.pair.firstPortalOrigin else request.pair.secondPortalOrigin
                val otherOrigin = if(!request.isFirstInPair) request.pair.firstPortalOrigin else request.pair.secondPortalOrigin
                val otherFrame = PortalLocator.getFrameInfosAt(otherOrigin, mc.world)
                if(otherFrame == NoInfos)
                    continue

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
                val otherFacing = otherFrame.frameType.facing
                val renderViewX = prevRenderEntity.posX
                val renderViewY = prevRenderEntity.posY
                val renderViewZ = prevRenderEntity.posZ
                val renderViewPitch = prevRenderEntity.rotationPitch
                val renderViewYaw = prevRenderEntity.rotationYaw
                val infos = request.infos
                val angleDiff = infos.frameType.facing.angleTo(otherFacing)
                val dy = renderViewY - origin.y -.5
                val dx = renderViewX - origin.x -.5
                val dz = renderViewZ - origin.z -.5

                val yaw = prevRenderEntity.rotationYaw + angleDiff + 180f
                val pitch = prevRenderEntity.rotationPitch

                val portalDx = dx
                val portalDy = dy
                val portalDz = dz

                val cos = MathHelper.cos(angleDiff.toRadians().toFloat())
                val sin = MathHelper.sin(angleDiff.toRadians().toFloat())
                val rotatedDx = portalDx * cos + portalDz * sin
                val rotatedDz = -portalDx * sin + portalDz * cos
                cameraEntity.posX = otherOrigin.x+.5-rotatedDx
                cameraEntity.posY = otherOrigin.y.toDouble()+.5f+portalDy+1f
                cameraEntity.posZ = otherOrigin.z+.5-rotatedDz
                cameraEntity.lastTickPosX = cameraEntity.posX
                cameraEntity.lastTickPosY = cameraEntity.posY
                cameraEntity.lastTickPosZ = cameraEntity.posZ
                cameraEntity.rotationPitch = pitch//renderViewPitch
                cameraEntity.rotationYaw = yaw.toFloat()
                cameraEntity.rotationYawHead = cameraEntity.rotationYaw
                cameraEntity.prevRotationPitch = cameraEntity.rotationPitch
                cameraEntity.prevRotationYaw = cameraEntity.rotationYaw
                settings.fovSetting = 70f
                nearPlane = (abs(portalDx * otherFacing.directionVec.x) + abs(portalDz * otherFacing.directionVec.z)).toFloat()

                renderer.renderWorld(1f, time+1)

                nearPlane = 0.05f

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
            renderingPortalView = false
            portalRenderRequests.clear()
        }
    }

    @SubscribeEvent
    fun onCameraSetup(event: EntityViewRenderEvent.CameraSetup) {
        if (renderingPortalView) { // reset projection matrix to change the nearPlane
            GlStateManager.matrixMode(GL_PROJECTION)
            GlStateManager.loadIdentity()
            val mc = Minecraft.getMinecraft()
            val fov = 70f
            val farPlaneDistance = (mc.gameSettings.renderDistanceChunks * 16).toFloat()
            Project.gluPerspective(fov, 1f, nearPlane, farPlaneDistance * MathHelper.SQRT_2)

            GlStateManager.matrixMode(GL_MODELVIEW)
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

        val UpVector = Vector3f(0f, 1f, 0f)
        val RightVector = Vector3f(1f, 0f, 0f)
    }

    override fun requestPortalRender(portalRenderRequest: PortalRenderRequest): Int {
        if(renderingPortalView) {
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