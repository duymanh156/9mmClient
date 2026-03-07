package dev.ninemmteam.auth;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class AuthClient {
    private static final String SERVER_HOST = "150.138.84.70";
    private static final int SERVER_PORT = 80;
    
    private static final Path AUTH_FILE = Paths.get(
        System.getProperty("user.home"), 
        ".fentanyl_auth"
    );
    
    private static String cachedKey;
    private static String cachedHWID;
    private static boolean authenticated = false;
    
    public static AuthResult verify(String key) {
        String hwid = HWIDUtil.getHWID();
        return verify(key, hwid);
    }
    
    public static AuthResult verify(String key, String hwid) {
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            
            socket.setSoTimeout(10000);
            
            out.println("VERIFY:" + key + ":" + hwid);
            
            String response = in.readLine();
            if (response == null) {
                return new AuthResult(false, "服务器无响应");
            }
            
            String[] parts = response.split(":");
            if (parts.length < 2) {
                return new AuthResult(false, "无效的服务器响应");
            }
            
            boolean success = "SUCCESS".equals(parts[0]);
            String message = parts.length > 1 ? parts[1] : (success ? "验证成功" : "验证失败");
            
            if (success) {
                authenticated = true;
                cachedKey = key;
                cachedHWID = hwid;
                saveAuth(key, hwid);
            }
            
            return new AuthResult(success, message);
        } catch (Exception e) {
            return new AuthResult(false, "连接服务器失败: " + e.getMessage());
        }
    }
    
    public static AuthResult checkLocalAuth() {
        if (!Files.exists(AUTH_FILE)) {
            return new AuthResult(false, "未找到本地验证信息");
        }
        
        try {
            String content = new String(Files.readAllBytes(AUTH_FILE), StandardCharsets.UTF_8);
            String[] parts = content.split("\n");
            if (parts.length < 2) {
                return new AuthResult(false, "本地验证信息损坏");
            }
            
            String savedKey = parts[0].trim();
            String savedHWID = parts[1].trim();
            String currentHWID = HWIDUtil.getHWID();
            
            if (!savedHWID.equals(currentHWID)) {
                return new AuthResult(false, "HWID已变更，请重新验证");
            }
            
            AuthResult result = verify(savedKey, savedHWID);
            if (result.success()) {
                authenticated = true;
                cachedKey = savedKey;
                cachedHWID = savedHWID;
            }
            
            return result;
        } catch (Exception e) {
            return new AuthResult(false, "读取本地验证信息失败");
        }
    }
    
    private static void saveAuth(String key, String hwid) {
        try {
            String content = key + "\n" + hwid;
            Files.write(AUTH_FILE, content.getBytes(StandardCharsets.UTF_8));
            AUTH_FILE.toFile().setReadable(true, true);
            AUTH_FILE.toFile().setWritable(true, true);
        } catch (Exception ignored) {}
    }
    
    public static boolean isAuthenticated() {
        return authenticated;
    }
    
    public static String getCachedKey() {
        return cachedKey;
    }
    
    public static String getCachedHWID() {
        return cachedHWID;
    }
    
    public static void clearAuth() {
        authenticated = false;
        cachedKey = null;
        cachedHWID = null;
        try {
            Files.deleteIfExists(AUTH_FILE);
        } catch (Exception ignored) {}
    }
    
    public static String getServerInfo() {
        return SERVER_HOST + ":" + SERVER_PORT;
    }
    
    public static record AuthResult(boolean success, String message) {}
}
