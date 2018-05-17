package org.jglrxavpok.localportal.client

import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.init.Blocks
import net.minecraft.util.ResourceLocation
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

        val portalRenderIndex =
            if(infos == NoInfos || (pair == null || !pair.hasSecond)) {
                glStencilFunc(GL_ALWAYS, 1, 0xFF)
                0
            } else {
                val request = PortalRenderRequest(infos, pair, pair.firstPortalOrigin == origin)
                val portalIndex = LocalPortal.proxy.requestPortalRender(request)
                glStencilFunc(GL_ALWAYS, 1+portalIndex, 0xFF)
                portalIndex
            }

        origin.release()
        glBindTexture(GL_TEXTURE_2D, Proxy.Companion.PortalTextureIDs[portalRenderIndex])
        GlStateManager.disableCull()
        GlStateManager.disableLighting()
        GlStateManager.disableBlend()
        val tessellator = Tessellator.getInstance()
        val buffer = tessellator.buffer
        val location = te.getLocationInFrame()
        val minU = (-location.first + 1.0) / 3.0
        val minV = (location.second) / 3.0
        val maxU = minU + (1.0/3.0)
        val maxV = minV + (1.0/3.0)
        buffer.begin(GL_QUADS, DefaultVertexFormats.POSITION_TEX)
        buffer.pos(0.0, 0.0, 0.0).tex(maxU, minV).endVertex()
        buffer.pos(1.0, 0.0, 0.0).tex(minU, minV).endVertex()
        buffer.pos(1.0, 1.0, 0.0).tex(minU, maxV).endVertex()
        buffer.pos(0.0, 1.0, 0.0).tex(maxU, maxV).endVertex()

        buffer.pos(0.0, 0.0, 1.0).tex(maxU, minV).endVertex()
        buffer.pos(1.0, 0.0, 1.0).tex(minU, minV).endVertex()
        buffer.pos(1.0, 1.0, 1.0).tex(minU, maxV).endVertex()
        buffer.pos(0.0, 1.0, 1.0).tex(maxU, maxV).endVertex()

        buffer.pos(0.0, 0.0, 0.0).tex(maxU, minV).endVertex()
        buffer.pos(0.0, 0.0, 1.0).tex(minU, minV).endVertex()
        buffer.pos(0.0, 1.0, 1.0).tex(minU, maxV).endVertex()
        buffer.pos(0.0, 1.0, 0.0).tex(maxU, maxV).endVertex()

        buffer.pos(1.0, 0.0, 0.0).tex(maxU, minV).endVertex()
        buffer.pos(1.0, 0.0, 1.0).tex(minU, minV).endVertex()
        buffer.pos(1.0, 1.0, 1.0).tex(minU, maxV).endVertex()
        buffer.pos(1.0, 1.0, 0.0).tex(maxU, maxV).endVertex()
        tessellator.draw()

        GlStateManager.popMatrix()
    }

}