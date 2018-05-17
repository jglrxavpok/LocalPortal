package org.jglrxavpok.localportal.client

import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GLAllocation
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.init.Blocks
import net.minecraft.util.ResourceLocation
import net.minecraft.util.math.Vec3i
import org.jglrxavpok.localportal.LocalPortal
import org.jglrxavpok.localportal.common.PortalLocator
import org.jglrxavpok.localportal.common.PortalLocator.NoInfos
import org.jglrxavpok.localportal.common.PortalRenderRequest
import org.jglrxavpok.localportal.common.TileEntityLocalPortal
import org.jglrxavpok.localportal.extensions.times
import org.jglrxavpok.localportal.extensions.transform
import org.lwjgl.opengl.GL11.*
import org.lwjgl.util.vector.Matrix4f
import org.lwjgl.util.vector.Vector3f
import kotlin.math.abs

object LocalPortalRenderer: TileEntitySpecialRenderer<TileEntityLocalPortal>() {

    val END_SKY_TEXTURE = ResourceLocation("textures/environment/end_sky.png")
    val ModelviewBuffer = GLAllocation.createDirectFloatBuffer(16)
    val ProjectionBuffer = GLAllocation.createDirectFloatBuffer(16)
    private val projectionMatrix = Matrix4f()
    private val modelviewMatrix = Matrix4f()

    override fun render(te: TileEntityLocalPortal, x: Double, y: Double, z: Double, partialTicks: Float, destroyStage: Int, alpha: Float) {
        GlStateManager.pushMatrix()
        GlStateManager.translate(x, y, z)
        val infos = te.portalInfos
        val portalID = infos.portalID
        val pair = PortalLocator.getPortalPair(portalID, world)
        val origin = te.getOriginPos()

        var dirVec: Vec3i
        val portalRenderIndex =
            if(infos == NoInfos || (pair == null || !pair.hasSecond)) {
                glStencilFunc(GL_ALWAYS, 1, 0xFF)
                dirVec = Vec3i.NULL_VECTOR
                0
            } else {
                val request = PortalRenderRequest(infos, pair, pair.firstPortalOrigin == origin)
                val portalIndex = LocalPortal.proxy.requestPortalRender(request)
                glStencilFunc(GL_ALWAYS, 1+portalIndex, 0xFF)
                dirVec = infos.frameType.facing.rotateY().directionVec
                portalIndex
            }

        origin.release()
        GlStateManager.color(1f, 1f, 1f, 1f)
        GlStateManager.bindTexture(Proxy.Companion.PortalTextureIDs[portalRenderIndex])
        GlStateManager.disableLighting()
        GlStateManager.disableBlend()
        val tessellator = Tessellator.getInstance()
        val buffer = tessellator.buffer
        val location = te.getLocationInFrame()
        val horizontalDirection = dirVec.x+dirVec.z // only one of them is not nil
        val lowerU = 0f//(-horizontalDirection*location.first + 1.0) / 3.0
        val upperU = 1f//lowerU + (1.0/3.0)

        // http://tomhulton.blogspot.fr/2015/08/portal-rendering-with-offscreen-render.html
        val lowerV = 0f//(location.second) / 3.0
        val upperV = 1f//lowerV + (1.0/3.0)

        val dU = abs(upperU - lowerU)
        val dV = abs(upperV - lowerV)

        //val minUInverted = (horizontalDirection*location.first + 1.0) / 3.0
        //val maxUInverted = minUInverted + (1.0/3.0)
        GlStateManager.getFloat(2983, ModelviewBuffer)
        GlStateManager.getFloat(2982, ProjectionBuffer)
        buffer.begin(GL_QUADS, DefaultVertexFormats.POSITION_TEX)

        modelviewMatrix.loadTranspose(ModelviewBuffer)
        projectionMatrix.loadTranspose(ProjectionBuffer)

        ModelviewBuffer.rewind()
        ProjectionBuffer.rewind()

        val mvpMatrix = projectionMatrix * modelviewMatrix

        // // Perspective divide (Translate to NDC - (-1, 1))
        //    float2 uv = i.pos_frag.xy / i.pos_frag.w;
        //    // Map -1, 1 range to 0, 1 tex coord range
        //    uv = (uv + float2(1.0)) * 0.5;
        val posMinFrag = mvpMatrix.transform(0f, 0f, 0f, 1f)
        val posMaxFrag = mvpMatrix.transform(1f, 1f, 1f, 1f)
        val maxU = (posMinFrag.x / posMinFrag.w + 1.0) * 0.5 * dU + lowerU
        val minU = (posMaxFrag.x / posMaxFrag.w + 1.0) * 0.5 * dV + lowerV

        val minV = (posMinFrag.y / posMinFrag.w + 1.0) * 0.5 * dU + lowerU
        val maxV = (posMaxFrag.y / posMaxFrag.w + 1.0) * 0.5 * dV + lowerV

        // TODO
        val minUInverted = minU
        val maxUInverted = maxU
        if(dirVec.x != 0) {
            buffer.pos(0.0, 1.0, 0.0).tex(maxU, maxV).endVertex()
            buffer.pos(1.0, 1.0, 0.0).tex(minU, maxV).endVertex()
            buffer.pos(1.0, 0.0, 0.0).tex(minU, minV).endVertex()
            buffer.pos(0.0, 0.0, 0.0).tex(maxU, minV).endVertex()

            buffer.pos(0.0, 0.0, 1.0).tex(minUInverted, minV).endVertex()
            buffer.pos(1.0, 0.0, 1.0).tex(maxUInverted, minV).endVertex()
            buffer.pos(1.0, 1.0, 1.0).tex(maxUInverted, maxV).endVertex()
            buffer.pos(0.0, 1.0, 1.0).tex(minUInverted, maxV).endVertex()
        }

        if(dirVec.z != 0) {
            buffer.pos(0.0, 0.0, 0.0).tex(minUInverted, minV).endVertex()
            buffer.pos(0.0, 0.0, 1.0).tex(maxUInverted, minV).endVertex()
            buffer.pos(0.0, 1.0, 1.0).tex(maxUInverted, maxV).endVertex()
            buffer.pos(0.0, 1.0, 0.0).tex(minUInverted, maxV).endVertex()

            buffer.pos(1.0, 1.0, 0.0).tex(maxU, maxV).endVertex()
            buffer.pos(1.0, 1.0, 1.0).tex(minU, maxV).endVertex()
            buffer.pos(1.0, 0.0, 1.0).tex(minU, minV).endVertex()
            buffer.pos(1.0, 0.0, 0.0).tex(maxU, minV).endVertex()
        }

        tessellator.draw()
        GlStateManager.enableLighting()
        GlStateManager.enableBlend()
        GlStateManager.popMatrix()
    }

}