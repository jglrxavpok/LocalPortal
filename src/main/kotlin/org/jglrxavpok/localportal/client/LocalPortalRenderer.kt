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
        GlStateManager.translate(x, y, z+1f)
        val infos = te.portalInfos
        val portalID = infos.portalID
        val pair = PortalLocator.getPortalPair(portalID, world)
        val origin = te.getOriginPos()

        if(!Minecraft.getMinecraft().framebuffer.isStencilEnabled)
            Minecraft.getMinecraft().framebuffer.enableStencil()
        glColorMask(false, false, false, false)

        glDisable(GL_SCISSOR_TEST)
        glClearStencil(0x0)
        glStencilMask(0xFF)
        glClear(GL_STENCIL_BUFFER_BIT)
        glEnable(GL_STENCIL_TEST)

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
        glStencilOp(GL_KEEP, GL_KEEP, GL_REPLACE)
        glStencilMask(0xFF)

        glDepthMask(false)
        val block = Blocks.STONE
        Minecraft.getMinecraft().blockRendererDispatcher.renderBlockBrightness(block.defaultState, 100f)
        glColorMask(true, true, true, true)
        GlStateManager.popMatrix()

        glDepthMask(true)

        glStencilFunc(GL_NEVER, 0, 0xFF)

        glStencilMask(0x0)
        GlStateManager.matrixMode(GL_MODELVIEW)
        glPushMatrix()
        glLoadIdentity()

        GlStateManager.matrixMode(GL_PROJECTION)
        GlStateManager.pushMatrix()
        glLoadIdentity()

        GlStateManager.disableDepth()
        GlStateManager.disableCull()
        glStencilFunc(GL_EQUAL, 1+portalRenderIndex, 0xFF)
        glStencilMask(0x0)

        //if(infos == NoInfos || (pair == null || !pair.hasSecond)) {
            bindTexture(Proxy.Companion.PortalTextureLocations[portalRenderIndex])
            val tessellator = Tessellator.getInstance()
            val bufferbuilder = tessellator.buffer
            bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX)
            bufferbuilder.pos(-1.0, -1.0, 1.0).tex(0.0, 0.0).endVertex()
            bufferbuilder.pos(1.0, -1.0, 1.0).tex(0.0, 1.0).endVertex()
            bufferbuilder.pos(1.0, 1.0, 1.0).tex(1.0, 1.0).endVertex()
            bufferbuilder.pos(-1.0, 1.0, 1.0).tex(1.0, 0.0).endVertex()
            tessellator.draw()
       // } else {
            // render view from portal
        //}

        glDepthMask(true)
        glDisable(GL_STENCIL_TEST)

        GlStateManager.enableCull()
        GlStateManager.enableDepth()
        GlStateManager.popMatrix()

        GlStateManager.matrixMode(GL_MODELVIEW)
        glPopMatrix()
    }

}