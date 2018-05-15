package org.jglrxavpok.localportal.common

import net.minecraft.entity.Entity
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.network.NetworkManager
import net.minecraft.network.play.server.SPacketUpdateTileEntity
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.ITickable
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.ChunkPos
import net.minecraft.util.math.MathHelper
import net.minecraft.world.World
import net.minecraftforge.common.ForgeChunkManager
import org.jglrxavpok.localportal.LocalPortal
import org.jglrxavpok.localportal.common.PortalLocator.NoInfos
import org.jglrxavpok.localportal.extensions.angleTo
import org.jglrxavpok.localportal.extensions.toRadians

class TileEntityLocalPortal: TileEntity(), ITickable {

    var portalInfos = NoInfos
    private var ticket: ForgeChunkManager.Ticket? = null
    private val originOfOtherPortal = BlockPos.MutableBlockPos(BlockPos.ORIGIN)
    private val lastKnownOrigin = BlockPos.MutableBlockPos(BlockPos.ORIGIN)
    private val prevOtherPos = BlockPos.MutableBlockPos(BlockPos.ORIGIN)

    override fun setWorld(worldIn: World?) {
        super.setWorld(worldIn)
        if(worldIn != null) {
            if(ticket != null)
                ForgeChunkManager.releaseTicket(ticket)
            ticket = ForgeChunkManager.requestTicket(LocalPortal, world, ForgeChunkManager.Type.NORMAL)
            ForgeChunkManager.forceChunk(ticket, ChunkPos(pos))
        }
    }

    override fun update() {

        val prevPortalID = portalInfos.portalID
        prevOtherPos.setPos(originOfOtherPortal)
        val newInfos = readPortalInfos()
        if(portalInfos != NoInfos && newInfos == NoInfos) {
            world.destroyBlock(pos, false)
            return
        }
        portalInfos = newInfos
        if(world.isRemote)
            return
        val pair = PortalLocator.getPortalPair(portalInfos.portalID, world) ?: return

        val origin = getOriginPos()
        originOfOtherPortal.setPos(origin) // reset in case other portal gets destroyed
        if(pair.firstPortalOrigin == origin) {
            if(pair.hasSecond) {
                updateLinkTo(pair.secondPortalOrigin)
            } else {
                updateLinkTo(pos)
            }
        } else {
            updateLinkTo(pair.firstPortalOrigin)
        }
        lastKnownOrigin.setPos(origin.x, origin.y, origin.z)
        origin.release()

        if(portalInfos.portalID != prevPortalID) {
            val state = world.getBlockState(pos)
            world.notifyBlockUpdate(pos, state, state, 3)
            markDirty()
        }
    }

    private fun updateLinkTo(otherOrigin: BlockPos) {
        originOfOtherPortal.setPos(otherOrigin)
    }

    private fun readPortalInfos(): PortalLocator.PortalFrameInfos {
        val origin = getOriginPos()
        val infos = PortalLocator.getFrameInfosAt(origin, world)
        origin.release()
        return infos
    }

    fun getOriginPos(): BlockPos.PooledMutableBlockPos {
        val location = getLocationInFrame()
        val dy = -location.second
        val facing = world.getBlockState(pos).getValue(PortalFacing).rotateY()
        val dx = -facing.directionVec.x*location.first
        val dz = -facing.directionVec.z*location.first
        return BlockPos.PooledMutableBlockPos.retain(pos.add(dx, dy-1, dz))
    }

    private fun getLocationInFrame(): Pair<Int, Int> {
        var y = 0
        while(world.getBlockState(pos.up(y)).block == BlockLocalPortal) {
            y++
        }
        val facing = world.getBlockState(pos).getValue(PortalFacing)
        val localRight = facing.rotateY()
        var x = 0
        while(world.getBlockState(pos.offset(localRight, x)).block == BlockLocalPortal) {
            x++
        }
        val row = (3-y)
        val column = (2-x)
        return Pair(column, row)
    }

    override fun readFromNBT(compound: NBTTagCompound) {
        super.readFromNBT(compound)
        portalInfos = PortalLocator.PortalFrameInfos.fromNBT(compound)
        originOfOtherPortal.setPos(compound.getInteger("otherX"), compound.getInteger("otherY"), compound.getInteger("otherZ"))
    }

    override fun writeToNBT(compound: NBTTagCompound): NBTTagCompound {
        portalInfos.toNBT(compound)
        compound.setInteger("otherX", originOfOtherPortal.x)
        compound.setInteger("otherY", originOfOtherPortal.y)
        compound.setInteger("otherZ", originOfOtherPortal.z)
        return super.writeToNBT(compound)
    }

    override fun getUpdateTag(): NBTTagCompound {
        return writeToNBT(super.getUpdateTag())
    }

    override fun onDataPacket(net: NetworkManager, pkt: SPacketUpdateTileEntity) {
        super.onDataPacket(net, pkt)
        val nbt = pkt.nbtCompound
        readFromNBT(nbt)
    }

    override fun getUpdatePacket(): SPacketUpdateTileEntity? {
        return SPacketUpdateTileEntity(pos, -1, updateTag)
    }

    fun teleport(entity: Entity) {
        if(!world.isRemote && portalInfos != NoInfos && originOfOtherPortal != pos) {
            val portalTracking = entity.getCapability(LocalPortal.PortalTracking, null)
            if(portalTracking == null) {
                LocalPortal.logger.debug("Tried to teleport entity $entity with no portal tracking capability")
                return
            }
            if(portalTracking.lastTeleportTick == entity.ticksExisted) {
                return // already teleported this tick
            }
            val intersects = this.pos.x < entity.posX+entity.width && this.pos.x+1.0 > entity.posX
                    && this.pos.y < entity.posY+entity.height && this.pos.y+1.0 > entity.posY
                    && this.pos.z < entity.posZ+entity.width && this.pos.z+1.0 > entity.posZ
            if(!intersects)
                return
            portalTracking.lastTeleportTick = entity.ticksExisted
            val width = entity.entityBoundingBox.maxX-entity.entityBoundingBox.minX
            val depth = entity.entityBoundingBox.maxZ-entity.entityBoundingBox.minZ
            val distanceInFrontOfPortal = maxOf(width, depth)
            val portalFacing = portalInfos.frameType.facing
            val offsetFromThisBlockX = entity.posX - (pos.x + .5f)
            val offsetFromThisBlockY = entity.posY - (pos.y + .5f)
            val offsetFromThisBlockZ = entity.posZ - (pos.z + .5f)

            val offset = getLocationInFrame()
            val otherPortal = PortalLocator.getFrameInfosAt(originOfOtherPortal, world)
            if(otherPortal == NoInfos) {
                LocalPortal.logger.error("Other portal cannot be inexistent at $originOfOtherPortal")
                return
            }
            val facing = otherPortal.frameType.facing
            val perpendicularFacing = facing.rotateY()
            val dx = -offset.first * perpendicularFacing.directionVec.x
            val dz = -offset.first * perpendicularFacing.directionVec.z
            val dy = offset.second
            val correspondingBlock = originOfOtherPortal.add(dx, dy+1, dz)
            val anglef = portalFacing.angleTo(facing).toFloat()
            entity.rotationYaw += anglef + 180f
            entity.rotationYawHead += anglef + 180f
            entity.prevRotationYaw += anglef+180f

            // (a+ib) * (cos+isin) = acos-bsin+i(asin+bcos)
            val anglefrad = (-anglef+180f).toRadians()
            val cos = MathHelper.cos(anglefrad)
            val sin = MathHelper.sin(anglefrad)
            val offsetX = -offsetFromThisBlockX * cos + -offsetFromThisBlockZ * sin
            val offsetZ = - -offsetFromThisBlockX * sin + -offsetFromThisBlockZ * cos
            val velocityX = entity.motionX * cos + entity.motionZ * sin
            val velocityZ =  -entity.motionX * sin + entity.motionZ * cos
            val push = 0.0f
            val verticalPush = 0.0f
            entity.motionX = velocityX + facing.directionVec.x * push
            entity.motionY += verticalPush
            entity.motionZ = velocityZ + facing.directionVec.z * push
            val offsetY = offsetFromThisBlockY
            val x = correspondingBlock.x + .5f +offsetX + distanceInFrontOfPortal * facing.directionVec.x
            val y = correspondingBlock.y + .5f +offsetY
            val z = correspondingBlock.z + .5f +offsetZ + distanceInFrontOfPortal * facing.directionVec.z
            LocalPortal.logger.debug("Sending entity to $x $y $z next to $originOfOtherPortal from $pos")
            entity.setPositionAndUpdate(x, y, z)
        }
    }

    override fun invalidate() {
        ForgeChunkManager.releaseTicket(ticket)
        if(!world.isRemote) {
            if(lastKnownOrigin.x == pos.x && lastKnownOrigin.y+1 == pos.y && lastKnownOrigin.z == pos.z) {
                PortalLocator.breakPortalPair(portalInfos.portalID, lastKnownOrigin, world.provider.dimension, world)
            }
        }
        super.invalidate()
    }
}