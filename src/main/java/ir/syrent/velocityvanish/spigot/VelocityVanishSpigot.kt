package ir.syrent.velocityvanish.spigot

import com.google.gson.JsonObject
import io.papermc.lib.PaperLib
import ir.syrent.velocityvanish.spigot.bridge.BukkitBridge
import ir.syrent.velocityvanish.spigot.bridge.BukkitBridgeManager
import ir.syrent.velocityvanish.spigot.command.vanish.VanishCommand
import ir.syrent.velocityvanish.spigot.core.VanishManager
import ir.syrent.velocityvanish.spigot.hook.DependencyManager
import ir.syrent.velocityvanish.spigot.listener.*
import ir.syrent.velocityvanish.spigot.ruom.RUoMPlugin
import ir.syrent.velocityvanish.spigot.ruom.Ruom
import ir.syrent.velocityvanish.spigot.ruom.adventure.AdventureApi
import ir.syrent.velocityvanish.spigot.ruom.messaging.BukkitMessagingEvent
import ir.syrent.velocityvanish.spigot.storage.Settings
import ir.syrent.velocityvanish.spigot.storage.Settings.bstats
import ir.syrent.velocityvanish.spigot.storage.Settings.velocitySupport
import ir.syrent.velocityvanish.spigot.utils.ServerVersion
import ir.syrent.velocityvanish.spigot.utils.Utils
import ir.syrent.velocityvanish.utils.component
import org.bstats.bukkit.Metrics
import org.bukkit.entity.Player
import xyz.jpenilla.squaremap.api.Squaremap
import xyz.jpenilla.squaremap.api.SquaremapProvider


class VelocityVanishSpigot : RUoMPlugin() {

    var bridgeManager: BukkitBridgeManager? = null
    lateinit var vanishManager: VanishManager
        private set

    val proxyPlayers = mutableMapOf<String, List<String>>()
    val vanishedNames = mutableSetOf<String>()

    override fun onEnable() {
        instance = this
        dataFolder.mkdir()

        initializeInstances()
        resetData(true)
        sendFiglet()
        sendWarningMessages()
        initializeCommands()
        initializeListeners()

        if (velocitySupport) {
            initializePluginChannels()
        }

        if (bstats) {
            enableMetrics()
        }
    }

    private fun sendFiglet() {
        sendConsoleMessage("<dark_purple>__      __  _            _ _      __      __         _     _     ")
        sendConsoleMessage("<dark_purple>\\ \\    / / | |          (_) |     \\ \\    / /        (_)   | |    ")
        sendConsoleMessage("<dark_purple> \\ \\  / /__| | ___   ___ _| |_ _   \\ \\  / /_ _ _ __  _ ___| |__  ")
        sendConsoleMessage("<dark_purple>  \\ \\/ / _ \\ |/ _ \\ / __| | __| | | \\ \\/ / _` | '_ \\| / __| '_ \\ ")
        sendConsoleMessage("<dark_purple>   \\  /  __/ | (_) | (__| | |_| |_| |\\  / (_| | | | | \\__ \\ | | |")
        sendConsoleMessage("<dark_purple>    \\/ \\___|_|\\___/ \\___|_|\\__|\\__, | \\/ \\__,_|_| |_|_|___/_| |_|")
        sendConsoleMessage("<dark_purple>                                __/ |                            ")
        sendConsoleMessage("<dark_purple>                               |___/                             v${Ruom.getServer().pluginManager.getPlugin("VelocityVanish")?.description?.version ?: " Unknown"}")
        sendConsoleMessage(" ")
        sendConsoleMessage("<white>Wiki: <blue><u>https://github.com/Syrent/VelocityVanish/wiki</u></blue>")
        sendConsoleMessage(" ")
    }

    private fun sendWarningMessages() {
        if (!ServerVersion.supports(16)) {
            Ruom.warn("Your running your server on a legacy minecraft version (< 16).")
            Ruom.warn("This plugin is not tested on legacy versions, so it may not work properly.")
            Ruom.warn("Please consider updating your server to 1.16.5 or higher.")
        }

        PaperLib.suggestPaper(this)
        DependencyManager
    }

    private fun resetData(startup: Boolean) {
        try {
            for (player in Ruom.getOnlinePlayers()) {
                if (startup) {
                    Utils.sendReportsActionbar(player)
                }
                vanishManager.unVanish(player)
            }
        } catch (_: Exception) {
            Ruom.warn("Plugin didn't fully complete reset data task on plugin shutdown")
        }
    }

    private fun enableMetrics() {
        val pluginID = 16679
        Metrics(this, pluginID)
    }

    private fun initializeInstances() {
        AdventureApi.initialize()
        vanishManager = VanishManager(this)

        Settings
    }

    private fun initializeCommands() {
        VanishCommand(this)
    }

    private fun initializeListeners() {
        PlayerJoinListener(this)
        PlayerQuitListener(this)
        PlayerInteractListener(this)
        PostVanishListener(this)
        PostUnVanishListener(this)
        PlayerTeleportListener(this)
        PlayerDeathListener(this)
        if (DependencyManager.sayanChatHook.exists) {
            PlayerMentionListener(this)
            if (!velocitySupport) PrivateMessageListener(this)
        }
        if (ServerVersion.supports(12)) TabCompleteListener(this)
        if (ServerVersion.supports(19)) BlockReceiveGameListener(this)
    }

    private fun initializePluginChannels() {
        val bridge = BukkitBridge()
        bridgeManager = BukkitBridgeManager(bridge, this)

        object : BukkitMessagingEvent(bridge) {
            override fun onPluginMessageReceived(player: Player, jsonObject: JsonObject) {
                bridgeManager!!.handleMessage(jsonObject)
            }
        }
    }

    override fun onDisable() {
        Ruom.shutdown()
        resetData(false)
    }

    private fun sendConsoleMessage(message: String) {
        AdventureApi.get().sender(server.consoleSender).sendMessage(message.component())
    }

    companion object {
        lateinit var instance: VelocityVanishSpigot
            private set
    }

}