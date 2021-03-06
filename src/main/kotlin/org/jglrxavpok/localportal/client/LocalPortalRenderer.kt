package org.jglrxavpok.localportal.client

import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.OpenGlHelper
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.util.ResourceLocation
import net.minecraft.util.math.Vec3i
import org.jglrxavpok.localportal.LocalPortal
import org.jglrxavpok.localportal.common.PortalLocator
import org.jglrxavpok.localportal.common.PortalLocator.NoInfos
import org.jglrxavpok.localportal.common.PortalRenderRequest
import org.jglrxavpok.localportal.common.TileEntityLocalPortal
import org.lwjgl.opengl.GL11.*

object LocalPortalRenderer: TileEntitySpecialRenderer<TileEntityLocalPortal>() {

    val END_SKY_TEXTURE = ResourceLocation("textures/environment/end_sky.png")

    override fun render(te: TileEntityLocalPortal, x: Double, y: Double, z: Double, partialTicks: Float, destroyStage: Int, alpha: Float) {
        GlStateManager.pushMatrix()
        GlStateManager.translate(x, y, z)
        val infos = te.portalInfos
        val portalID = infos.portalID
        val pair = PortalLocator.getPortalPair(portalID, world)
        val origin = te.getOriginPos()

        var dirVec: Vec3i
        glDisable(GL_SCISSOR_TEST)
        glClearStencil(0xFF)
        glStencilMask(0xFF)
        glClear(GL_STENCIL_BUFFER_BIT)
        glEnable(GL_STENCIL_TEST)
        val portalRenderIndex =
            if(infos == NoInfos || (pair == null || !pair.hasSecond)) {
                glStencilFunc(GL_ALWAYS, 1, 0xFF)
                dirVec = Vec3i.NULL_VECTOR
                0
            } else {
                val request = PortalRenderRequest(infos, pair, pair.firstPortalOrigin == origin)
                val portalIndex = LocalPortal.proxy.requestPortalRender(request)
                glStencilFunc(GL_ALWAYS, 1+portalIndex, 0xFF)
                dirVec = infos.frameType.facing.directionVec
                portalIndex
            }
        glStencilOp(GL_KEEP, GL_KEEP, GL_REPLACE)

        origin.release()
        GlStateManager.color(1f, 1f, 1f, 1f)

        GlStateManager.disableLighting()
        GlStateManager.disableBlend()
        val tessellator = Tessellator.getInstance()
        val buffer = tessellator.buffer

        GlStateManager.colorMask(false, false, false, false)

        buffer.begin(GL_QUADS, DefaultVertexFormats.POSITION)
        if(dirVec.z < 0) {
            buffer.pos(0.0, 1.0, 0.0).endVertex()
            buffer.pos(1.0, 1.0, 0.0).endVertex()
            buffer.pos(1.0, 0.0, 0.0).endVertex()
            buffer.pos(0.0, 0.0, 0.0).endVertex()
        }

        if(dirVec.z > 0) {
            buffer.pos(0.0, 0.0, 1.0).endVertex()
            buffer.pos(1.0, 0.0, 1.0).endVertex()
            buffer.pos(1.0, 1.0, 1.0).endVertex()
            buffer.pos(0.0, 1.0, 1.0).endVertex()
        }

        if(dirVec.x < 0) {
            buffer.pos(0.0, 0.0, 0.0).endVertex()
            buffer.pos(0.0, 0.0, 1.0).endVertex()
            buffer.pos(0.0, 1.0, 1.0).endVertex()
            buffer.pos(0.0, 1.0, 0.0).endVertex()
        }

        if(dirVec.x > 0) {
            buffer.pos(1.0, 1.0, 0.0).endVertex()
            buffer.pos(1.0, 1.0, 1.0).endVertex()
            buffer.pos(1.0, 0.0, 1.0).endVertex()
            buffer.pos(1.0, 0.0, 0.0).endVertex()
        }

        tessellator.draw()
        GlStateManager.colorMask(true, true, true, true)
        GlStateManager.disableDepth()
        GlStateManager.disableCull()

        GlStateManager.matrixMode(GL_MODELVIEW)
        GlStateManager.pushMatrix()
        GlStateManager.loadIdentity()

        GlStateManager.matrixMode(GL_PROJECTION)
        GlStateManager.pushMatrix()
        GlStateManager.loadIdentity()

        glEnable(GL_STENCIL_TEST)
        glStencilFunc(GL_EQUAL, 1+portalRenderIndex, 0xFF)
        glStencilMask(0x0)

        val lightmapX = OpenGlHelper.lastBrightnessX
        val lightmapY = OpenGlHelper.lastBrightnessY
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f)
        GlStateManager.bindTexture(Proxy.Companion.PortalTextureIDs[portalRenderIndex])
        buffer.begin(GL_QUADS, DefaultVertexFormats.POSITION_TEX)
        buffer.pos(-1.0, -1.0, 1.0).tex(0.0, 0.0).endVertex()
        buffer.pos(1.0, -1.0, 1.0).tex(1.0, 0.0).endVertex()
        buffer.pos(1.0, 1.0, 1.0).tex(1.0, 1.0).endVertex()
        buffer.pos(-1.0, 1.0, 1.0).tex(0.0, 1.0).endVertex()
        tessellator.draw()
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lightmapX, lightmapY)
        GlStateManager.enableDepth()
        GlStateManager.enableCull()
        glDisable(GL_STENCIL_TEST)

        GlStateManager.popMatrix()
        GlStateManager.matrixMode(GL_MODELVIEW)
        GlStateManager.popMatrix()

        GlStateManager.enableLighting()
        GlStateManager.enableBlend()
        GlStateManager.popMatrix()
    }

}
