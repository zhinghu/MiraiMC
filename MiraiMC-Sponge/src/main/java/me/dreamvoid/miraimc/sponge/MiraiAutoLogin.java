package me.dreamvoid.miraimc.sponge;

import me.dreamvoid.miraimc.interfaces.IMiraiAutoLogin;
import me.dreamvoid.miraimc.api.MiraiBot;
import me.dreamvoid.miraimc.internal.Utils;
import me.dreamvoid.miraimc.sponge.utils.AutoLoginObject;
import net.mamoe.mirai.utils.BotConfiguration;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.CustomClassLoaderConstructor;
import org.yaml.snakeyaml.nodes.Tag;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class MiraiAutoLogin implements IMiraiAutoLogin {

    public MiraiAutoLogin(SpongePlugin plugin) {
        this.plugin = plugin;
        logger = new SpongeLogger("MiraiMC-AutoLogin", plugin.getLogger());
        logger.setParent(Utils.getLogger());
    }

    private final SpongePlugin plugin;
    @SuppressWarnings("SpongeLogging")
    private final Logger logger;
    private static File AutoLoginFile;

    @Override
    public void loadFile() {
        // 建立文件夹
        File ConfigDir = new File(Utils.getMiraiDir(), "config");
        File ConsoleDir = new File(ConfigDir, "Console");
        if(!ConsoleDir.exists() &&!ConsoleDir.mkdirs()) throw new RuntimeException("Failed to create folder " + ConsoleDir.getPath());

        // 建立自动登录文件
        AutoLoginFile = new File(ConsoleDir, "AutoLogin.yml");
        if(!AutoLoginFile.exists()) {
            try {
                if(!AutoLoginFile.createNewFile()){ throw new RuntimeException("Failed to create folder " + AutoLoginFile.getPath()); }
                String defaultText = "accounts: "+ System.lineSeparator();
                File writeName = AutoLoginFile;
                try (FileWriter writer = new FileWriter(writeName);
                     BufferedWriter out = new BufferedWriter(writer)
                ) {
                    out.write(defaultText);
                    out.flush();
                }
            } catch (IOException e) {
                Utils.resolveException(e, logger, "创建自动登录文件时出现异常！");
            }
        }
    }

    @Override
    public List<Map<?, ?>> loadAutoLoginList() throws IOException {
        List<Map<?,?>> list = new ArrayList<>();
        loadAutoLoginListSponge().forEach(accounts -> {
            Map<String, Long> map = new HashMap<>();
            map.put("account", accounts.getAccount());
            list.add(map);
        });
        return list;
    }

    public List<AutoLoginObject.Accounts> loadAutoLoginListSponge() throws IOException {
        Yaml yaml = new Yaml(new CustomClassLoaderConstructor(MiraiAutoLogin.class.getClassLoader()));
        InputStream inputStream = Files.newInputStream(AutoLoginFile.toPath());
        AutoLoginObject data = yaml.loadAs(inputStream, AutoLoginObject.class);
        if(data.getAccounts() == null){
            data.setAccounts(new ArrayList<>());
        }
        return data.getAccounts();
    }

    @Override
    public void startAutoLogin() {
        Runnable thread = () -> {
            try {
                logger.info("Starting auto login task.");
                for(AutoLoginObject.Accounts accounts : loadAutoLoginListSponge()){
                    AutoLoginObject.Password password = accounts.getPassword();
                    AutoLoginObject.Configuration configuration = accounts.getConfiguration();
                    long Account = accounts.getAccount();
                    if(Account != 123456){
                        try {
                            String Password = password.getValue();
                            BotConfiguration.MiraiProtocol Protocol = BotConfiguration.MiraiProtocol.valueOf(configuration.getProtocol().toUpperCase());
                            logger.info("Auto login bot account: " + Account + " Protocol: " + Protocol.name());
                            MiraiBot.doBotLogin(Account, Password, Protocol);
                        } catch (IllegalArgumentException ex){
                            logger.warning("读取自动登录文件时发现未知的协议类型，请修改: " + configuration.getProtocol());
                        }
                    }
                }
            } catch (IOException e) {
                logger.warning("登录机器人时出现异常，原因: " + e);
            }
        };
        plugin.runTaskAsync(thread);
    }

    @Override
    public boolean addAutoLoginBot(long Account, String Password, String Protocol){
        try {
            // 获取现有的机器人列表
            Yaml yaml = new Yaml(new CustomClassLoaderConstructor(MiraiAutoLogin.class.getClassLoader()));
            InputStream inputStream = Files.newInputStream(AutoLoginFile.toPath());
            AutoLoginObject data = yaml.loadAs(inputStream, AutoLoginObject.class);
            if(data.getAccounts() == null){
                data.setAccounts(new ArrayList<>());
            }

            // 新建用于添加进去的Map
            AutoLoginObject.Accounts account = new AutoLoginObject.Accounts();

            // account 节点
            account.setAccount(Account);

            // password 节点
            AutoLoginObject.Password password = new AutoLoginObject.Password();
            password.setKind("PLAIN");
            password.setValue(Password);
            account.setPassword(password);

            // configuration 节点
            AutoLoginObject.Configuration configuration = new AutoLoginObject.Configuration();
            configuration.setDevice("device.json");
            configuration.setProtocol(Protocol.toUpperCase());
            account.setConfiguration(configuration);

            // 添加
            List<AutoLoginObject.Accounts> accounts = data.getAccounts();
            accounts.add(account);
            data.setAccounts(accounts);
            Yaml yaml1 = new Yaml(new CustomClassLoaderConstructor(MiraiAutoLogin.class.getClassLoader()));

            File writeName = AutoLoginFile;
            try (FileWriter writer = new FileWriter(writeName);
                 BufferedWriter out = new BufferedWriter(writer)
            ) {
                out.write(yaml1.dumpAs(data, Tag.MAP, DumperOptions.FlowStyle.BLOCK));
                out.flush();
            }
        } catch (IOException e) {
            Utils.resolveException(e, logger, "保存自动登录文件时出现异常！");
            return false;
        }
        return true;
    }

    @Override
    public boolean deleteAutoLoginBot(long Account){
        try {
            // 获取现有的机器人列表
            Yaml yaml = new Yaml(new CustomClassLoaderConstructor(MiraiAutoLogin.class.getClassLoader()));
            InputStream inputStream = Files.newInputStream(AutoLoginFile.toPath());
            AutoLoginObject data = yaml.loadAs(inputStream, AutoLoginObject.class);
            if(data.getAccounts() == null){
                data.setAccounts(new ArrayList<>());
            }

            for (AutoLoginObject.Accounts bots : data.getAccounts()) {
                if (bots.getAccount() == Account) {
                    data.getAccounts().remove(bots);
                    break;
                }
            }

            Yaml yaml1 = new Yaml(new CustomClassLoaderConstructor(MiraiAutoLogin.class.getClassLoader()));

            File writeName = AutoLoginFile;
            try (FileWriter writer = new FileWriter(writeName);
                 BufferedWriter out = new BufferedWriter(writer)
            ) {
                out.write(yaml1.dumpAs(data, Tag.MAP, DumperOptions.FlowStyle.BLOCK));
                out.flush();
            }
        } catch (IOException e) {
            Utils.resolveException(e, logger, "保存自动登录文件时出现异常！");
            return false;
        }
        return true;
    }
}
