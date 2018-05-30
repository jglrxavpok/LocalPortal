package org.jglrxavpok.localportal.client

import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.WorldClient
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
            val mc = Minecraft.getMinecraft()
            val framebuffer = mc.framebuffer
            if(!framebuffer.isStencilEnabled)
                framebuffer.enableStencil()
            if(mc.world == null)
                return
            if(!mc.inGameHasFocus)
                return
            val renderer = mc.entityRenderer
            val partialTicks = event.renderTickTime
            val time = System.nanoTime()
            renderingPortalView = true

            for((portalIndex, request) in portalRenderRequests.withIndex()) {
                val texID = getTextureOrLoad(portalIndex)

                if(request.infos == NoInfos)
                    continue
                val origin = if(request.isFirstInPair) request.pair.firstPortalOrigin else request.pair.secondPortalOrigin
                val otherOrigin = if(!request.isFirstInPair) request.pair.firstPortalOrigin else request.pair.secondPortalOrigin

                var otherFrame = PortalLocator.getFrameInfosAt(otherOrigin, mc.world)
                if(otherFrame == NoInfos) {
                    val pair = PortalLocator.getPortalPair(request.infos.portalID, mc.world) ?: continue
                    otherFrame = if(request.isFirstInPair) pair.secondFrameInfos else pair.firstFrameInfos
                    if(otherFrame == NoInfos)
                        continue
                }

                val cameraEntity = EntityCamera()
                cameraEntity.world = mc.world

                val settings = mc.gameSettings
                val renderDistanceSave = settings.renderDistanceChunks
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

                val yaw = renderViewYaw + angleDiff + 180f
                val pitch = renderViewPitch

                val portalDx = dx
                val portalDy = dy
                val portalDz = dz

                val cos = MathHelper.cos(-angleDiff.toRadians().toFloat())
                val sin = MathHelper.sin(-angleDiff.toRadians().toFloat())
                val rotatedDx = portalDx * cos + portalDz * sin
                val rotatedDz = -portalDx * sin + portalDz * cos
                cameraEntity.posX = otherOrigin.x+.5-rotatedDx + otherFacing.directionVec.x
                cameraEntity.posY = otherOrigin.y.toDouble()+.5f+portalDy + prevRenderEntity.eyeHeight
                cameraEntity.posZ = otherOrigin.z+.5-rotatedDz + otherFacing.directionVec.z

                cameraEntity.chunkCoordX = cameraEntity.position.x shr 4
                cameraEntity.chunkCoordY = cameraEntity.position.y shr 4
                cameraEntity.chunkCoordZ = cameraEntity.position.z shr 4
                cameraEntity.lastTickPosX = cameraEntity.posX
                cameraEntity.lastTickPosY = cameraEntity.posY
                cameraEntity.lastTickPosZ = cameraEntity.posZ
                cameraEntity.rotationPitch = pitch//renderViewPitch
                cameraEntity.rotationYaw = yaw.toFloat()
                cameraEntity.rotationYawHead = cameraEntity.rotationYaw
                cameraEntity.prevRotationPitch = cameraEntity.rotationPitch
                cameraEntity.prevRotationYaw = cameraEntity.rotationYaw

          //      settings.fovSetting = 70f
                val dist = Math.sqrt(portalDx*portalDx + portalDz*portalDz)
                nearPlane = dist.toFloat()-0.05f

                // TODO: loadEntityShader is called by setRenderViewEntity -> allows for special effects on the output!
                renderer.renderWorld(1f, System.nanoTime() + (1000000000L / Math.max(30, mc.gameSettings.limitFramerate)))

                nearPlane = 0.05f

                mc.renderViewEntity = prevRenderEntity
                mc.displayWidth = prevW
                mc.displayHeight = prevH
                settings.thirdPersonView = thirdPersonViewSave
                settings.hideGUI = hideGuiSave
                settings.fovSetting = fovSave
                settings.renderDistanceChunks = renderDistanceSave

                GlStateManager.bindTexture(texID)
                glCopyTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, 0,0, PortalFrameBufferWidth, PortalFrameBufferHeight, 0)
                GlStateManager.bindTexture(0)
            }
            renderingPortalView = false
            portalRenderRequests.clear()
        }
    }

    @SubscribeEvent
    fun onFogSetup(event: EntityViewRenderEvent.FogDensity) { // this is hacky
        setupProjectionMatrix()
    }

    private fun setupProjectionMatrix() {
        if (renderingPortalView) { // reset projection matrix to change the nearPlane
            val prevMatrixMode = glGetInteger(GL_MATRIX_MODE)
            GlStateManager.matrixMode(GL_PROJECTION)
            GlStateManager.loadIdentity()
            val mc = Minecraft.getMinecraft()
            val fov = mc.gameSettings.fovSetting
            val farPlaneDistance = (mc.gameSettings.renderDistanceChunks * 16).toFloat()
            val originalAspect = mc.framebuffer.framebufferWidth.toFloat()/mc.framebuffer.framebufferHeight
            Project.gluPerspective(fov, originalAspect, nearPlane, nearPlane+farPlaneDistance * MathHelper.SQRT_2)

            GlStateManager.matrixMode(prevMatrixMode)
        }
    }

    @SubscribeEvent
    fun onCameraSetup(event: EntityViewRenderEvent.CameraSetup) {
        setupProjectionMatrix()
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
        if(portalRenderRequest == EmptyPortalRequest) {
            return 0
        }
        val same = portalRenderRequests.indexOfFirst { it == portalRenderRequest }
        if(same == -1) {
            if(renderingPortalView) {
                return 0
            }
            val index = portalRenderRequests.size
            portalRenderRequests.add(portalRenderRequest)
            return index
        }
        return same
    }

}