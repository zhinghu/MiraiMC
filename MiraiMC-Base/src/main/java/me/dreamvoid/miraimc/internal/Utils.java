package me.dreamvoid.miraimc.internal;

import com.google.gson.JsonObject;
import me.dreamvoid.miraimc.MiraiMCConfig;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.logging.Logger;

public final class Utils {
    static {
        // 此处放置插件自检代码
        if (!Boolean.getBoolean("MiraiMC.StandWithNpp") && System.getProperty("os.name").toLowerCase().contains("windows") && findProcess("notepad++.exe")) {
            Arrays.asList("========================================",
                    "喜欢用Notepad++，拦不住的", "建议使用 Visual Studio Code 或 Sublime",
                    "VSCode: https://code.visualstudio.com/",
                    "Sublime: https://www.sublimetext.com/",
                    "不要向MiraiMC作者寻求任何帮助。",
                    "进程将在20秒后继续运行",
                    "========================================").forEach(s -> Logger.getLogger("MiraiMC Preload Checker").severe(s));
            try {
                Thread.sleep(20000);
            } catch (InterruptedException ignored) {}
        }
        if(Boolean.getBoolean("MiraiMC.StandWithNpp")){
            Logger.getLogger("MiraiMC Preload Checker").severe("不要向MiraiMC作者寻求任何帮助。");
        }

        if(findClass("cpw.mods.modlauncher.Launcher") || findClass("net.minecraftforge.server.console.TerminalHandler")) { // 抛弃Forge用户，别问为什么
            Logger.getLogger("MiraiMC Preload Checker").severe("任何Forge服务端均不受MiraiMC支持，请尽量更换其他服务端使用！");
            Logger.getLogger("MiraiMC Preload Checker").severe("作者不会处理任何使用了Forge服务端导致的问题。");
            Logger.getLogger("MiraiMC Preload Checker").severe("兼容性报告: https://docs.miraimc.dreamvoid.me/troubleshoot/compatibility-report");
        }

        if(Boolean.getBoolean("MiraiMC.DeveloperMode")){
            developerMode = true;
            Logger.getLogger("MiraiMC Preload Checker").warning("MiraiMC 开发者模式已启用！");
            Logger.getLogger("MiraiMC Preload Checker").warning("除非你知道你正在做什么，否则请不要启用开发者模式。");
        } else developerMode = false;
    }

    private static boolean findProcess(String processName) {
        BufferedReader bufferedReader = null;
        try {
            Process proc = Runtime.getRuntime().exec("tasklist /FI \"IMAGENAME eq " + processName + "\"");
            bufferedReader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (line.contains(processName)) {
                    return true;
                }
            }
            return false;
        } catch (Exception ex) {
            return false;
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (Exception ignored) {}
            }
        }
    }

    public static boolean findClass(String className){
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static Logger logger;
    private static ClassLoader classLoader;
    private static final boolean developerMode;

    public static void setLogger(Logger logger){
        Utils.logger = logger;
    }
    
    public static Logger getLogger(){
        return logger;
    }

    public static void setClassLoader(ClassLoader classLoader) {
        Utils.classLoader = classLoader;
    }
    
    public static ClassLoader getClassLoader(){
        return classLoader;
    }

    public static boolean isDeveloperMode(){
        return developerMode;
    }

    /**
     * Http 相关实用类
     */
    public static final class Http {
        /**
         * 发送HTTP GET请求
         * @param url URL 链接
         * @return 远程服务器返回内容
         * @throws IOException 出现任何连接问题时抛出
         */
        public static String get(String url) throws IOException {
            URL obj = new URL(url.replace(" ", "%20"));
            StringBuilder sb = new StringBuilder();
            HttpURLConnection httpUrlConn = (HttpURLConnection) obj.openConnection();

            httpUrlConn.setDoInput(true);
            httpUrlConn.setRequestMethod("GET");
            httpUrlConn.setRequestProperty("User-Agent", "Mozilla/5.0 DreamVoid MiraiMC");
            httpUrlConn.setConnectTimeout(5000);
            httpUrlConn.setReadTimeout(10000);

            InputStream input = httpUrlConn.getInputStream();
            InputStreamReader read = new InputStreamReader(input, StandardCharsets.UTF_8);
            BufferedReader br = new BufferedReader(read);
            String data = br.readLine();
            while (data != null) {
                sb.append(data).append(System.lineSeparator());
                data = br.readLine();
            }
            br.close();
            read.close();
            input.close();
            httpUrlConn.disconnect();

            return sb.toString();
        }

        /**
         * 发送HTTP POST请求
         * @param json Gson对象
         * @param URL 链接
         * @return 远程服务器返回内容
         */
        public static String post(JsonObject json, String URL) throws IOException {
            try (CloseableHttpClient client = HttpClients.createDefault()) {
                HttpPost post = new HttpPost(URL);
                post.setHeader("Content-Type", "application/json");
                post.addHeader("Authorization", "Basic YWRtaW46");
                StringEntity s = new StringEntity(json.toString(), StandardCharsets.UTF_8);
                s.setContentType(new BasicHeader(org.apache.http.protocol.HTTP.CONTENT_TYPE, "application/json"));
                post.setEntity(s);
                // 发送请求
                HttpResponse httpResponse = client.execute(post);
                // 获取响应输入流
                InputStream inStream = httpResponse.getEntity().getContent();
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        inStream, StandardCharsets.UTF_8));
                StringBuilder strber = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null)
                    strber.append(line).append("\n");
                inStream.close();
                if (httpResponse.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                    logger.warning("Http request returned bad status code: " + httpResponse.getStatusLine().getStatusCode()+", reason: "+ httpResponse.getStatusLine().getReasonPhrase());
                }
                return strber.toString();
            }
        }

        /**
         * 下载一个文件，覆盖已存在的文件
         * @param url 链接
         * @param saveFile 将要保存到的文件
         */
        public static void download(String url, File saveFile){
            try (InputStream inputStream = new URL(url).openStream()){
                logger.info("Downloading " + url);
                Files.copy(inputStream, saveFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                logger.severe(String.format("Failed to download %s, reason: %s", url, e));
            }
        }

        /**
         * 测试链接可用性
         * @param url 链接
         * @return 服务器是否返回 HTTP 200 OK 状态码
         * @throws IOException 连接出现任何异常时抛出
         */
        public static boolean test(String url) throws IOException {
            URL obj = new URL(url.replace(" ", "%20"));
            StringBuilder sb = new StringBuilder();
            HttpURLConnection httpUrlConn = (HttpURLConnection) obj.openConnection();

            httpUrlConn.setDoInput(true);
            httpUrlConn.setRequestMethod("GET");
            httpUrlConn.setRequestProperty("User-Agent", "Mozilla/5.0 DreamVoid MiraiMC");
            httpUrlConn.setConnectTimeout(5000);
            httpUrlConn.setReadTimeout(10000);

            return httpUrlConn.getResponseCode() == HttpURLConnection.HTTP_OK;
        }
    }

    @NotNull
    public static File getMiraiDir(){
        return MiraiMCConfig.General.MiraiWorkingDir.equals("default") ? new File(MiraiMCConfig.PluginDir,"MiraiBot") : new File(MiraiMCConfig.General.MiraiWorkingDir);
    }

    public static String getFileSha1(File file){
        try(FileInputStream fis = new FileInputStream(file)){
            byte[] buffer = new byte[1024];
            MessageDigest digest = MessageDigest.getInstance("SHA");
            int numRead;

            do {
                numRead = fis.read(buffer);
                if (numRead > 0) {
                    digest.update(buffer, 0, numRead);
                }
            } while (numRead != -1);

            fis.close();
            byte[] bytes = digest.digest();
            BigInteger b = new BigInteger(1, bytes);
            return String.format("%0" + (bytes.length << 1) + "x", b);
        } catch (NoSuchAlgorithmException | IOException e) {
            return null;
        }
    }
}
