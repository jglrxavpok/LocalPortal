package org.jglrxavpok.localportal.common

import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLeashKnot
import net.minecraft.init.Items
import net.minecraft.util.EnumFacing
import net.minecraft.util.text.TextComponentString
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.capabilities.ICapabilityProvider
import net.minecraftforge.event.AttachCapabilitiesEvent
import net.minecraftforge.event.entity.EntityJoinWorldEvent
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.jglrxavpok.localportal.LocalPortal
import org.jglrxavpok.localportal.common.PortalLocator.NoInfos

@Mod.EventBusSubscriber(modid = LocalPortal.ModID)
object ItemEventHandler {

    @SubscribeEvent
    fun onEntityInteract(event: PlayerInteractEvent.RightClickBlock) {
        val player = event.entityPlayer
        if(player.world.isRemote)
            return
        val stack = event.itemStack
        if(stack.item == Items.DIAMOND) {
            val infos = PortalLocator.getFrameInfosAt(event.pos, event.world)
            if(infos != NoInfos) {
                if(PortalLocator.createPortal(infos, event.pos, event.world, player.dimension)) {
                    player.sendStatusMessage(TextComponentString("ok: $infos"), true)
                } else {
                    player.sendStatusMessage(TextComponentString("too many of: $infos"), true)
                }
            } else {
                player.sendStatusMessage(TextComponentString("not ok"), true)
            }
        }
    }

    @SubscribeEvent
    fun onEntityLoaded(event: AttachCapabilitiesEvent<Entity>) {
        val capaInstance = PortalTrackingCapability(-1)
        val provider = object : ICapabilityProvider {
            override fun <T : Any?> getCapability(capability: Capability<T>, facing: EnumFacing?): T? {
                if(capability == LocalPortal.PortalTracking)
                    return capaInstance as T
                return null
            }

            override fun hasCapability(capability: Capability<*>, facing: EnumFacing?): Boolean {
                if(capability == LocalPortal.PortalTracking)
                    return true
                return false
            }

        }
        event.addCapability(LocalPortal.PortalTrackingKey, provider)
    }
}