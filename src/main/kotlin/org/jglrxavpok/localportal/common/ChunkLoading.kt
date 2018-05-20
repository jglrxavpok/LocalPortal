package org.jglrxavpok.localportal.common

import net.minecraft.util.math.ChunkPos
import net.minecraft.world.World
import net.minecraftforge.common.ForgeChunkManager
import org.jglrxavpok.localportal.LocalPortal
import java.util.*

object ChunkLoading: ForgeChunkManager.LoadingCallback {

    // Each world has a ticket per portalID
    private val internalTicketMap = WeakHashMap<World, WeakHashMap<Int, ForgeChunkManager.Ticket>>()

    override fun ticketsLoaded(tickets: MutableList<ForgeChunkManager.Ticket>, world: World) {
        val map = WeakHashMap<Int, ForgeChunkManager.Ticket>()
        tickets.forEach {
            val nbtData = it.modData
            val firstX = nbtData.getInteger("firstX")
            val firstZ = nbtData.getInteger("firstZ")
            val secondX = nbtData.getInteger("secondX")
            val secondZ = nbtData.getInteger("secondZ")
            val portalID = nbtData.getInteger("portalID")

            ForgeChunkManager.forceChunk(it, ChunkPos(firstX shr 4, firstZ shr 4))
            ForgeChunkManager.forceChunk(it, ChunkPos(secondX shr 4, secondZ shr 4))

            map[portalID] = it
        }

        internalTicketMap[world] = map
    }

    fun onPortalCreation(portalID: Int, world: World, pair: PortalPair) {
        val ticket = ForgeChunkManager.requestTicket(LocalPortal, world, ForgeChunkManager.Type.NORMAL)
        if(ticket != null) {
            val nbt = ticket.modData
            nbt.setInteger("firstX", pair.firstPortalOrigin.x)
            nbt.setInteger("firstZ", pair.firstPortalOrigin.z)
            nbt.setInteger("secondX", pair.secondPortalOrigin.x)
            nbt.setInteger("secondZ", pair.secondPortalOrigin.z)
            nbt.setInteger("portalID", portalID)

            ForgeChunkManager.forceChunk(ticket, ChunkPos(pair.firstPortalOrigin.x shr 4, pair.firstPortalOrigin.z shr 4))
            ForgeChunkManager.forceChunk(ticket, ChunkPos(pair.secondPortalOrigin.x shr 4, pair.secondPortalOrigin.z shr 4))

            if(world !in internalTicketMap) {
                internalTicketMap[world] = WeakHashMap()
            }
            val map = internalTicketMap[world]!!
            map[portalID] = ticket

            LocalPortal.logger.info("Forcing chunks to stay loaded for portal id $portalID (${ChunkPos(pair.firstPortalOrigin)} & ${ChunkPos(pair.secondPortalOrigin)})")
        } else {
            LocalPortal.logger.error("No more chunk tickets available!")
        }
    }

    fun onPortalRemoval(portalID: Int, world: World, pair: PortalPair) {
        if(world !in internalTicketMap)
            return
        val worldTickets = internalTicketMap[world]!!
        if(portalID !in worldTickets)
            return
        val ticket = worldTickets.remove(portalID)!!
        ForgeChunkManager.releaseTicket(ticket)
    }
}