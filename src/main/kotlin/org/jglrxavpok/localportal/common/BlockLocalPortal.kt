package org.jglrxavpok.localportal.common

import net.minecraft.block.Block
import net.minecraft.block.material.Material
import net.minecraft.block.properties.PropertyEnum
import net.minecraft.block.state.BlockFaceShape
import net.minecraft.block.state.BlockStateContainer
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.Entity
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.BlockRenderLayer
import net.minecraft.util.EnumBlockRenderType
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World
import org.jglrxavpok.localportal.LocalPortal

val PortalFacing = PropertyEnum.create("facing", EnumFacing::class.java)

object BlockLocalPortal: Block(Material.PORTAL) {

    // TODO
    init {
        setRegistryName(LocalPortal.ModID, "local_portal")
        unlocalizedName = "local_portal"
        defaultState = blockState.baseState.withProperty(PortalFacing, EnumFacing.NORTH)
    }

    override fun createBlockState(): BlockStateContainer {
        return BlockStateContainer(this, PortalFacing)
    }

    override fun hasTileEntity(state: IBlockState) = true
    override fun isCollidable() = false

    override fun onEntityCollidedWithBlock(worldIn: World, pos: BlockPos, state: IBlockState, entityIn: Entity) {
        if (!entityIn.isRiding && !entityIn.isBeingRidden && entityIn.isNonBoss) {
            val tilePortal = worldIn.getTileEntity(pos) as TileEntityLocalPortal
            tilePortal.teleport(entityIn)
        }
    }

    override fun createTileEntity(world: World, state: IBlockState): TileEntity {
        return TileEntityLocalPortal()
    }

    override fun getStateFromMeta(meta: Int): IBlockState {
        return defaultState.withProperty(PortalFacing, EnumFacing.values()[meta])
    }

    override fun getMetaFromState(state: IBlockState): Int {
        return state.getValue(PortalFacing).ordinal
    }

    override fun getCollisionBoundingBox(blockState: IBlockState?, worldIn: IBlockAccess?, pos: BlockPos?): AxisAlignedBB? {
        return NULL_AABB
    }

    override fun isOpaqueCube(state: IBlockState): Boolean {
        return false
    }

    override fun isFullCube(state: IBlockState): Boolean {
        return false
    }

    override fun getRenderType(state: IBlockState): EnumBlockRenderType {
        return EnumBlockRenderType.ENTITYBLOCK_ANIMATED
    }

    override fun getBlockFaceShape(worldIn: IBlockAccess, state: IBlockState, pos: BlockPos, face: EnumFacing): BlockFaceShape {
        return BlockFaceShape.UNDEFINED
    }
}