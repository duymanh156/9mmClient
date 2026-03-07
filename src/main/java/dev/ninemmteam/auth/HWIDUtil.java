package dev.ninemmteam.auth;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.NetworkInterface;
import java.security.MessageDigest;
import java.util.Enumeration;

public class HWIDUtil {
    
    public static String getHWID() {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            String hwid;
            
            if (os.contains("win")) {
                hwid = getWindowsHWID();
            } else if (os.contains("mac")) {
                hwid = getMacHWID();
            } else {
                hwid = getLinuxHWID();
            }
            
            return hash(hwid);
        } catch (Exception e) {
            return hash("fallback-" + System.getProperty("user.name") + "-" + System.getenv("COMPUTERNAME"));
        }
    }
    
    private static String getWindowsHWID() {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"wmic", "csproduct", "get", "UUID"});
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && !line.equalsIgnoreCase("UUID")) {
                    return line;
                }
            }
        } catch (Exception ignored) {}
        
        return getMACAddress();
    }
    
    private static String getMacHWID() {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"ioreg", "-rd1", "-c", "IOPlatformExpertDevice"});
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("IOPlatformUUID")) {
                    String[] parts = line.split("\"");
                    if (parts.length >= 4) {
                        return parts[3];
                    }
                }
            }
        } catch (Exception ignored) {}
        
        return getMACAddress();
    }
    
    private static String getLinuxHWID() {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"cat", "/etc/machine-id"});
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();
            if (line != null && !line.isEmpty()) {
                return line.trim();
            }
        } catch (Exception ignored) {}
        
        return getMACAddress();
    }
    
    private static String getMACAddress() {
        try {
            Enumeration<NetworkInterface> networks = NetworkInterface.getNetworkInterfaces();
            while (networks.hasMoreElements()) {
                NetworkInterface network = networks.nextElement();
                byte[] mac = network.getHardwareAddress();
                if (mac != null && mac.length > 0 && !network.isLoopback()) {
                    StringBuilder sb = new StringBuilder();
                    for (byte b : mac) {
                        sb.append(String.format("%02X", b));
                    }
                    return sb.toString();
                }
            }
        } catch (Exception ignored) {}
        return "unknown-mac";
    }
    
    private static String hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString().substring(0, 32).toUpperCase();
        } catch (Exception e) {
            return input.hashCode() + "";
        }
    }
}
