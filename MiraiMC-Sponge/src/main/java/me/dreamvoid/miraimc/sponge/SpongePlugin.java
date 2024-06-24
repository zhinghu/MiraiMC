package me.dreamvoid.miraimc.sponge;

import com.google.inject.Inject;
import me.dreamvoid.miraimc.IMiraiAutoLogin;
import me.dreamvoid.miraimc.IMiraiEvent;
import me.dreamvoid.miraimc.LifeCycle;
import me.dreamvoid.miraimc.Platform;
import me.dreamvoid.miraimc.commands.MiraiCommand;
import me.dreamvoid.miraimc.commands.MiraiMcCommand;
import me.dreamvoid.miraimc.internal.Utils;
import me.dreamvoid.miraimc.internal.config.PluginConfig;
import me.dreamvoid.miraimc.internal.loader.LibraryLoader;
import me.dreamvoid.miraimc.sponge.utils.Metrics;
import me.dreamvoid.miraimc.sponge.utils.SpecialUtils;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.api.Server;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.*;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.util.Ticks;
import org.spongepowered.api.util.metric.MetricsConfigManager;
import org.spongepowered.plugin.PluginContainer;
import org.spongepowered.plugin.builtin.jvm.Plugin;

import java.io.File;
import java.io.IOException;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Plugin(value = "miraimc")
public class SpongePlugin implements Platform {
    private final LifeCycle lifeCycle;
    private PluginConfig platformConfig;
    @SuppressWarnings("SpongeLogging")
    private java.util.logging.Logger SpongeLogger;
    private final LibraryLoader loader;
    private final Metrics.Factory metricsFactory;

    @Inject
    public SpongePlugin(Metrics.Factory factory){
        lifeCycle = new LifeCycle(this);
        lifeCycle.startUp(new SpongeLogger("MiraiMC", LoggerFactory.getLogger("MiraiMC")));
        loader = new LibraryLoader((URLClassLoader) getClass().getClassLoader());

        metricsFactory = factory;
    }

    @Inject
    private Logger logger;

    @Inject
    @ConfigDir(sharedRoot = false)
    private File dataFolder;

    @Inject
    private PluginContainer pluginContainer;

    @Inject
    private MetricsConfigManager metricsConfigManager;

    private MiraiEvent MiraiEvent;
    private MiraiAutoLogin MiraiAutoLogin;

    /**
     * StartingEngineEvent 将在指定的 Engine 启动时触发。此时该引擎的任何内容都尚未初始化，世界将不存在，并且引擎范围的注册表此时尚未准备好。
     */
    @Listener
    public void onLoad(StartingEngineEvent<Server> e) {
        SpongeLogger = new SpongeLogger("MiraiMC", this.getLogger());

        try {
            platformConfig = new SpongeConfig(this);
            lifeCycle.preLoad();

            MiraiAutoLogin = new MiraiAutoLogin(this);
            MiraiEvent = new MiraiEvent(this);
        } catch (Exception ex) {
            Utils.resolveException(ex, SpongeLogger, "加载 MiraiMC 阶段 1 时出现异常！");
        }
    }

    /**
     * StartedEngineEvent 将在指定的 Engine 完成初始化时触发。具体来说，这意味着注册表已被填充，对于服务器引擎来说，世界已被创建。
     */
    @Listener
    public void onEnable(StartedEngineEvent<Server> e) {
        try {
            lifeCycle.postLoad();

            // 监听事件
            if (PluginConfig.General.LogEvents) {
                getLogger().info("Registering events.");
                Sponge.eventManager().registerListeners(this.pluginContainer, new Events());
            }

            // bStats统计
            if (PluginConfig.General.AllowBStats) {
                if (this.metricsConfigManager.collectionState(this.pluginContainer).asBoolean()) {
                    getLogger().info("Initializing bStats metrics.");
                    int pluginId = 12847;
                    metricsFactory.make(pluginId);
                } else {
                    getLogger().warn("你在配置文件中启用了bStats，但是MetricsConfigManager告知MiraiMC不允许收集信息，因此bStats已关闭");
                    getLogger().warn("要启用bStats，请执行命令 /sponge metrics miraimc enable");
                    getLogger().warn("或者在配置文件中禁用bStats隐藏此警告");
                }
            }

            // HTTP API
            if (PluginConfig.General.EnableHttpApi) {
                getLogger().info("Initializing HttpAPI async task.");
                runTaskTimerAsync(new MiraiHttpAPIResolver(this), PluginConfig.HttpApi.MessageFetch.Interval);
            }
        } catch (Exception ex){
            Utils.resolveException(ex, SpongeLogger, "加载 MiraiMC 阶段 2 时出现异常！");
        }
    }

    /**
     * 触发 GameStartingServerEvent 时，服务器初始化和世界载入都已经完成，你应该在这时注册插件命令。
     */
    @Listener
    public void onRegisterCommand(RegisterCommandEvent<Command.Parameterized> e) {
        getLogger().info("Registering commands.");

        final Parameter.Key<String> argsKey = Parameter.key("args", String.class);

        e.register(this.pluginContainer, Command.builder()
                .shortDescription(Component.text("MiraiMC Bot Command."))
                .permission("miraimc.command.mirai")
                .executor(context1 -> {
                    String[] args1 = context1.requireOne(argsKey).split(" ");
                    new MiraiCommand().onCommand(SpecialUtils.getSender(context1), args1);
                    return CommandResult.success();
                })
                .build(), "mirai");
        e.register(this.pluginContainer, Command.builder()
                .shortDescription(Component.text("MiraiMC Plugin Command."))
                .permission("miraimc.command.miraimc")
                .executor(context1 -> {
                    String[] args1 = context1.requireOne(argsKey).split(" ");
                    new MiraiMcCommand().onCommand(SpecialUtils.getSender(context1), args1);
                    return CommandResult.success();
                })
                .build(), "miraimc");
        e.register(this.pluginContainer, Command.builder()
                .shortDescription(Component.text("MiraiMC LoginVerify Command."))
                .permission("miraimc.command.miraiverify")
                .executor(context -> {
                    String[] args = context.requireOne(argsKey).split(" ");
                    new MiraiMcCommand().onCommand(SpecialUtils.getSender(context), args);
                    return CommandResult.success();
                })
                .build(), "miraiverify");
    }

    /**
     * 当引擎被告知关闭并且即将关闭它负责的所有内容时，StoppingEngineEvent 将触发。如果游戏异常终止，可能不会触发。
     */
    @Listener
    public void onServerStopping(StoppingEngineEvent<Server> event){
        lifeCycle.unload();
    }

    /**
     * RefreshGameEvent 可以响应于用户请求刷新所有配置而被激发。插件应该侦听此事件并重新加载其配置作为响应。
     */
    @Listener
    public void onRefresh(RefreshGameEvent event){
        try {
            platformConfig.loadConfig();
        } catch (IOException e) {
            Utils.resolveException(e, SpongeLogger, "重新加载配置时出现异常！");
        }
    }

    public Logger getLogger() {
        return logger;
    }

    public File getDataFolder() {
        return dataFolder;
    }

    public PluginContainer getPluginContainer() {
        return pluginContainer;
    }

    @Override
    public String getPlayerName(UUID uuid) {
        return Sponge.server().player(uuid).map(ServerPlayer::name).orElse(null);
    }

    @Override
    public UUID getPlayerUUID(String name) {
        return Sponge.server().player(name).map(ServerPlayer::uniqueId).orElse(null);
    }

    @Override
    public void runTaskAsync(Runnable task) {
        Sponge.asyncScheduler().submit(Task.builder()
                .plugin(this.pluginContainer)
                .execute(task)
                .build());
    }

    @Override
    public void runTaskLaterAsync(Runnable task, long delay) {
        Sponge.asyncScheduler().submit(Task.builder()
                .plugin(this.pluginContainer)
                .delay(Ticks.of(delay))
                .execute(task)
                .build());
    }

    private final HashMap<Integer, Task> tasks = new HashMap<>();

    @Override
    public int runTaskTimerAsync(Runnable task, long period) {
        return Sponge.asyncScheduler().submit(Task.builder()
                .plugin(this.pluginContainer)
                .interval(Ticks.of(period))
                .execute(task)
                .build()).hashCode();
    }

    @Override
    public void cancelTask(int taskId) {
        tasks.get(taskId);
    }

    @Override
    public String getPluginName() {
        return "MiraiMC";
    }

    @Override
    public String getPluginVersion() {
        return getPluginContainer().metadata().version().getQualifier();
    }

    @Override
    public List<String> getAuthors() {
        return Collections.singletonList("DreamVoid");
    }

    @Override
    public java.util.logging.Logger getPluginLogger() {
        return SpongeLogger;
    }

    @Override
    public ClassLoader getPluginClassLoader() {
        return getClass().getClassLoader();
    }

    @Override
    public IMiraiAutoLogin getAutoLogin() {
        return MiraiAutoLogin;
    }

    @Override
    public IMiraiEvent getMiraiEvent() {
        return MiraiEvent;
    }

    @Override
    public PluginConfig getPluginConfig() {
        return platformConfig;
    }

    @Override
    public LibraryLoader getLibraryLoader() {
        return loader;
    }
}
