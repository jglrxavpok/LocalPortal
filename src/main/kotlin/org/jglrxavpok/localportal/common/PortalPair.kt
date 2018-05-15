package org.jglrxavpok.localportal.common

import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.math.BlockPos
import net.minecraft.world.storage.WorldSavedData

class PortalPair(name: String): WorldSavedData(name) {

    val firstPortalOrigin: BlockPos.MutableBlockPos = BlockPos.MutableBlockPos(BlockPos.ORIGIN)
    val secondPortalOrigin: BlockPos.MutableBlockPos = BlockPos.MutableBlockPos(BlockPos.ORIGIN)
    var hasSecond = false
    var isClean = false

    override fun writeToNBT(compound: NBTTagCompound): NBTTagCompound {
        compound.setInteger("firstX", firstPortalOrigin.x)
        compound.setInteger("firstY", firstPortalOrigin.y)
        compound.setInteger("firstZ", firstPortalOrigin.z)
        compound.setInteger("secondX", secondPortalOrigin.x)
        compound.setInteger("secondY", secondPortalOrigin.y)
        compound.setInteger("secondZ", secondPortalOrigin.z)

        compound.setBoolean("hasSecond", hasSecond)
        compound.setBoolean("isClean", isClean)
        return compound
    }

    override fun readFromNBT(compound: NBTTagCompound) {
        firstPortalOrigin.setPos(compound.getInteger("firstX"), compound.getInteger("firstY"), compound.getInteger("firstZ"))
        secondPortalOrigin.setPos(compound.getInteger("secondX"), compound.getInteger("secondY"), compound.getInteger("secondZ"))

        hasSecond = compound.getBoolean("hasSecond")
        isClean = compound.getBoolean("isClean")
    }
}