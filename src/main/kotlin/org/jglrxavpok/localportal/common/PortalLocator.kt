package org.jglrxavpok.localportal.common

import net.minecraft.block.BlockQuartz
import net.minecraft.init.Blocks
import net.minecraft.item.EnumDyeColor
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import org.jglrxavpok.localportal.LocalPortal
import org.jglrxavpok.localportal.common.network.S0UpdatePortalPair

object PortalLocator {

    const val PairSaveDataID = "${LocalPortal.ModID}_portal_pair"

    fun dataID(portalID: Int) = "${PairSaveDataID}_$portalID"

    fun getPortalPair(portalID: Int, world: World): PortalPair? {
        return world.loadData(PortalPair::class.java, dataID(portalID)) as? PortalPair
    }

    fun getFrameInfosAt(pos: BlockPos, world: World): PortalFrameInfos {
        val frame = getFrameAt(pos, world)
        if(frame == PortalFrame.None)
            return NoInfos
        val ID = when(frame) {
            PortalFrame.FacingWest -> getPortalID(pos, world, EnumFacing.WEST)
            PortalFrame.FacingEast -> getPortalID(pos, world, EnumFacing.EAST)
            PortalFrame.FacingNorth -> getPortalID(pos, world, EnumFacing.NORTH)
            PortalFrame.FacingSouth -> getPortalID(pos, world, EnumFacing.SOUTH)
            else -> error("No frame found")
        }
        return PortalFrameInfos(frame, ID)
    }

    fun getPortalID(origin: BlockPos, world: World, facing: EnumFacing): Int {
        fun at(pos: BlockPos) = world.getBlockState(pos)

        val facingRight = facing.rotateY()
        val facingLeft = facing.rotateYCCW()

        // Corners
        val leftColumnBase = origin.offset(facingLeft, 2)
        val rightColumnBase = origin.offset(facingRight, 2)
        val bottomLeft = leftColumnBase
        val bottomRight = rightColumnBase
        val topLeft = leftColumnBase.up(4)
        val topRight = rightColumnBase.up(4)

        var ID: Int = 0
        for(corner in arrayOf(bottomLeft, bottomRight, topLeft, topRight)) {
            val state = at(corner)
            // look for color
            val color = state.properties
                    .filter { (key, _) -> key.getName() == "color" && key.getValueClass() == EnumDyeColor::class.java }
                    .map { it.value }
                    .first() as EnumDyeColor
            ID = (ID shl 4) or (color.ordinal and 0xF)
        }
        return ID
    }

    fun getFrameAt(pos: BlockPos, world: World)= when {
        testPortal(pos, world, EnumFacing.Axis.X, EnumFacing.AxisDirection.POSITIVE) -> PortalFrame.FacingNorth
        testPortal(pos, world, EnumFacing.Axis.X, EnumFacing.AxisDirection.NEGATIVE) -> PortalFrame.FacingSouth
        testPortal(pos, world, EnumFacing.Axis.Z, EnumFacing.AxisDirection.POSITIVE) -> PortalFrame.FacingWest
        testPortal(pos, world, EnumFacing.Axis.Z, EnumFacing.AxisDirection.NEGATIVE) -> PortalFrame.FacingEast
        else -> PortalFrame.None
    }

    /** ```
        W===W
        |GGG|
        |GGG|
        |GGG|
        W===W
        --^ origin
        ```
     * W: wool or any dyeable block
     * G: gold block BEHIND the portal
     * | and =: Quartz pillars
     * */
    private fun testPortal(origin: BlockPos, world: World, axis: EnumFacing.Axis, perpendicularDirection: EnumFacing.AxisDirection): Boolean {
        val quartzType = if(axis == EnumFacing.Axis.X) BlockQuartz.EnumType.LINES_X else BlockQuartz.EnumType.LINES_Z
        val horizontalPillar = Blocks.QUARTZ_BLOCK.defaultState.withProperty(BlockQuartz.VARIANT, quartzType)
        val verticalPillar = Blocks.QUARTZ_BLOCK.defaultState.withProperty(BlockQuartz.VARIANT, BlockQuartz.EnumType.LINES_Y)
        val goldBlock = Blocks.GOLD_BLOCK.defaultState
        val clay = Blocks.CLAY.defaultState
        val facingRight = EnumFacing.getFacingFromAxis(EnumFacing.AxisDirection.POSITIVE, axis)
        val facingLeft = EnumFacing.getFacingFromAxis(EnumFacing.AxisDirection.NEGATIVE, axis)
        val perpendicularAxis = if(axis == EnumFacing.Axis.X) EnumFacing.Axis.Z else EnumFacing.Axis.X
        val facingBehind = EnumFacing.getFacingFromAxis(perpendicularDirection, perpendicularAxis)

        fun at(pos: BlockPos) = world.getBlockState(pos)

        // bottom pillars
        if(at(origin) != clay) {
            return false
        }
        if(at(origin.offset(facingRight)) != horizontalPillar) {
            return false
        }
        if(at(origin.offset(facingLeft)) != horizontalPillar) {
            return false
        }

        // top pillars
        val topMiddle = origin.up(4)
        if(at(topMiddle) != horizontalPillar) {
            return false
        }
        if(at(topMiddle.offset(facingRight)) != horizontalPillar) {
            return false
        }
        if(at(topMiddle.offset(facingLeft)) != horizontalPillar) {
            return false
        }

        // Columns
        val leftColumnBase = origin.offset(facingLeft, 2)
        val rightColumnBase = origin.offset(facingRight, 2)
        for(columnBase in arrayOf(leftColumnBase, rightColumnBase)) {
            for (i in 1..3) {
                val pos = columnBase.up(i)
                if(at(pos) != verticalPillar) {
                    return false
                }
            }
        }

        // Gold & air(or portal) blocks
        val originBehind = origin.offset(facingBehind).up()
        for(i in -1..1) {
            val bottomBehind = originBehind.offset(facingRight, i)
            val bottom = origin.up().offset(facingRight, i)
            for(j in 0..2) {
                val pos = bottomBehind.up(j)
                if(at(pos) != goldBlock) {
                    return false
                }
                if( ! world.isAirBlock(bottom.up(j)) && at(bottom.up(j)).block != BlockLocalPortal)
                    return false
            }
        }

        // Corners
        val bottomLeft = leftColumnBase
        val bottomRight = rightColumnBase
        val topLeft = leftColumnBase.up(4)
        val topRight = rightColumnBase.up(4)

        for(corner in arrayOf(bottomLeft, bottomRight, topLeft, topRight)) {
            val state = at(corner)
            // look for color
            val hasColor = state.propertyKeys.any { it.getName() == "color" && it.getValueClass() == EnumDyeColor::class.java }
            if(!hasColor) {
                return false
            }
        }

        return true
    }

    fun createPortal(infos: PortalFrameInfos, pos: BlockPos, world: World, dimensionID: Int): Boolean {
        val pair = getPortalPair(infos.portalID, world)
        if(pair == null) {
            val newPair = PortalPair(dataID(infos.portalID)).apply { firstPortalOrigin.setPos(pos) }
            updatePortalPair(newPair, world, dimensionID)
        } else {
            if(pair.isClean) { // portals with given ID existed but were destroyed
                pair.firstPortalOrigin.setPos(pos)
                pair.hasSecond = false
                pair.isClean = false
            } else {
                if(pair.hasSecond)
                    return false
                if(pair.firstPortalOrigin == pos)
                    return false
                pair.secondPortalOrigin.setPos(pos)
                pair.hasSecond = true
            }
            updatePortalPair(pair, world, dimensionID)
            ChunkLoading.onPortalCreation(infos.portalID, world, pair)
        }

        if(world.isRemote)
            return true
        // place blocks
        val portalFacing = infos.frameType.facing
        val facing = infos.frameType.facing.rotateY()
        val direction = facing.directionVec
        for(i in -1..1) {
            for(j in 1..3) {
                val dx = i * direction.x
                val dz = i * direction.z
                world.setBlockState(pos.add(dx, j, dz), BlockLocalPortal.defaultState.withProperty(PortalFacing, portalFacing), 3)
            }
        }

        return true
    }

    private fun updatePortalPair(pair: PortalPair, world: World, dimensionID: Int) {
        pair.markDirty()
        world.setData(pair.mapName, pair)
        if(!world.isRemote) {
            LocalPortal.network.sendToDimension(S0UpdatePortalPair(pair), dimensionID)
            //LocalPortal.network.sendToAll(S0UpdatePortalPair(pair))
        }
    }

    fun breakPortalPair(portalID: Int, originToRemove: BlockPos, dimensionID: Int, world: World) {
        val pair = getPortalPair(portalID, world) ?: return // if null, nothing to do
        if(pair.hasSecond) { // fully linked portals
            ChunkLoading.onPortalRemoval(portalID, world, pair) // call before changing portal origins
        }
        if(pair.firstPortalOrigin == originToRemove) {
            if(pair.hasSecond) {
                pair.firstPortalOrigin.setPos(pair.secondPortalOrigin)
            } else {
                pair.firstPortalOrigin.setPos(0,0,0)
                pair.isClean = true
            }
        }
        pair.secondPortalOrigin.setPos(0,0,0)
        pair.hasSecond = false
        updatePortalPair(pair, world, dimensionID)
    }

    data class PortalFrameInfos(val frameType: PortalFrame, val portalID: Int) {
        fun toNBT(compound: NBTTagCompound) {
            compound.setInteger("frameFacing", frameType.ordinal)
            compound.setInteger("portalID", portalID)
        }

        companion object {
            fun fromNBT(compound: NBTTagCompound): PortalFrameInfos {
                val frame = PortalFrame.values()[compound.getInteger("frameFacing")]
                val id = compound.getInteger("portalID")
                return PortalFrameInfos(frame, id)
            }
        }
    }

    val NoInfos = PortalFrameInfos(PortalFrame.None, 0)

    enum class PortalFrame(val facing: EnumFacing) {
        None(EnumFacing.UP),
        FacingNorth(EnumFacing.NORTH),
        FacingSouth(EnumFacing.SOUTH),
        FacingEast(EnumFacing.EAST),
        FacingWest(EnumFacing.WEST)
    }
}