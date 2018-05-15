package org.jglrxavpok.localportal.common

import net.minecraft.nbt.NBTBase
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumFacing
import net.minecraftforge.common.capabilities.Capability

data class PortalTrackingCapability(var lastTeleportTick: Int) {

    object Storage: Capability.IStorage<PortalTrackingCapability> {
        override fun readNBT(capability: Capability<PortalTrackingCapability>, instance: PortalTrackingCapability, side: EnumFacing?, nbt: NBTBase) {
            nbt as NBTTagCompound
            instance.lastTeleportTick = nbt.getInteger("lastTeleportTick")
        }

        override fun writeNBT(capability: Capability<PortalTrackingCapability>, instance: PortalTrackingCapability, side: EnumFacing): NBTBase? {
            val nbt = NBTTagCompound()
            nbt.setInteger("lastTeleportTick", instance.lastTeleportTick)
            return nbt
        }
    }

}