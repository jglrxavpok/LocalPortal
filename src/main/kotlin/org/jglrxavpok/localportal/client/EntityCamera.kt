package org.jglrxavpok.localportal.client

import net.minecraft.client.Minecraft
import net.minecraft.entity.Entity
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.world.World
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

@SideOnly(Side.CLIENT)
class EntityCamera: Entity(null) {

    init {
        setSize(0f, 0f)
    }

    override fun writeEntityToNBT(compound: NBTTagCompound?) {

    }

    override fun readEntityFromNBT(compound: NBTTagCompound?) {

    }

    override fun entityInit() {

    }

    override fun getEyeHeight(): Float {
        return Minecraft.getMinecraft().player.defaultEyeHeight
    }
}