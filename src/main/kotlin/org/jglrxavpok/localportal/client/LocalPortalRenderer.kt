package org.jglrxavpok.localportal.client

import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.init.Blocks
import net.minecraft.util.ResourceLocation
import org.jglrxavpok.localportal.common.PortalLocator
import org.jglrxavpok.localportal.common.PortalLocator.NoInfos
import org.jglrxavpok.localportal.common.TileEntityLocalPortal

object LocalPortalRenderer: TileEntitySpecialRenderer<TileEntityLocalPortal>() {

    private val CHEST_TEXTURE = ResourceLocation("textures/entity/chest/normal.png")
    private val ENDER_CHEST_TEXTURE = ResourceLocation("textures/entity/chest/ender.png")

    override fun render(te: TileEntityLocalPortal, x: Double, y: Double, z: Double, partialTicks: Float, destroyStage: Int, alpha: Float) {
        GlStateManager.pushMatrix()
        GlStateManager.translate(x, y, z+1f)
        val infos = te.portalInfos
        val portalID = infos.portalID
        val pair = PortalLocator.getPortalPair(portalID, world)
        if(infos == NoInfos || (pair == null || !pair.hasSecond)) {
            bindTexture(CHEST_TEXTURE)
            val block = Blocks.CHEST
            Minecraft.getMinecraft().blockRendererDispatcher.renderBlockBrightness(block.defaultState, 100f)
        } else {
            val origin = te.getOriginPos()
            bindTexture(ENDER_CHEST_TEXTURE)
            val block = Blocks.ENDER_CHEST
            Minecraft.getMinecraft().blockRendererDispatcher.renderBlockBrightness(block.defaultState, 100f)
            origin.release()
        }
        GlStateManager.popMatrix()
    }

}