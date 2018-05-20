package org.jglrxavpok.localportal

import net.minecraft.block.Block
import net.minecraft.util.ResourceLocation
import net.minecraftforge.common.ForgeChunkManager
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.capabilities.CapabilityManager
import net.minecraftforge.common.config.Configuration
import net.minecraftforge.event.RegistryEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.SidedProxy
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper
import net.minecraftforge.fml.common.registry.GameRegistry
import org.apache.logging.log4j.Logger
import org.jglrxavpok.localportal.common.*
import net.minecraft.init.Blocks as MCBlocks
import net.minecraft.init.Items as MCItems
import net.minecraftforge.common.capabilities.CapabilityInject
import net.minecraftforge.event.world.ChunkEvent

@Mod(modLanguageAdapter = "net.shadowfacts.forgelin.KotlinAdapter", modid = LocalPortal.ModID, dependencies = "required-after:forgelin;",
        name = "Local Portal", version = "1.0.0-indev", updateJSON = "https://raw.githubusercontent.com/jglrxavpok/LocalPortal/master/updateCheck.json")
object LocalPortal {
    const val ModID = "localportal"

    lateinit var logger: Logger

    @SidedProxy(clientSide = "org.jglrxavpok.localportal.client.Proxy", serverSide = "org.jglrxavpok.localportal.server.Proxy")
    lateinit var proxy: LocalPortalProxy

    val network = SimpleNetworkWrapper(ModID)
    lateinit var config: Configuration
        private set

    @CapabilityInject(PortalTrackingCapability::class)
    lateinit var PortalTracking: Capability<PortalTrackingCapability>
    val PortalTrackingKey = ResourceLocation(ModID, "portal_tracking")

    @Mod.EventHandler
    fun preInit(event: FMLPreInitializationEvent) {
        logger = event.modLog
        config = Configuration(event.suggestedConfigurationFile)
        LPConfig.backing = config
        LPConfig.loadAll()
        MinecraftForge.EVENT_BUS.register(this)
        MinecraftForge.EVENT_BUS.register(ItemEventHandler)
        proxy.preInit()

        CapabilityManager.INSTANCE.register(PortalTrackingCapability::class.java, PortalTrackingCapability.Storage) { PortalTrackingCapability(-1) }
    }

    @Mod.EventHandler
    fun init(event: FMLInitializationEvent) {
        proxy.init()
        ForgeChunkManager.setForcedChunkLoadingCallback(this, ChunkLoading)
    }

    @SubscribeEvent
    fun registerBlocks(e: RegistryEvent.Register<Block>) {
        e.registry.registerAll(BlockLocalPortal)
        GameRegistry.registerTileEntity(TileEntityLocalPortal::class.java, BlockLocalPortal.registryName.toString())
    }

    @SubscribeEvent
    fun onChunkUnload(event: ChunkEvent.Unload) {
        LocalPortal.logger.info("Unloading chunk at ${event.chunk.pos}")
    }


}