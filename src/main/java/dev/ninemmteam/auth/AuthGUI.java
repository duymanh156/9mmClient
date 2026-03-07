package dev.ninemmteam.auth;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class AuthGUI {
    private static final Color BG_COLOR = new Color(30, 30, 35);
    private static final Color PANEL_COLOR = new Color(45, 45, 50);
    private static final Color ACCENT_COLOR = new Color(100, 149, 237);
    private static final Color TEXT_COLOR = new Color(220, 220, 220);
    private static final Color ERROR_COLOR = new Color(255, 100, 100);
    private static final Color SUCCESS_COLOR = new Color(100, 255, 100);
    
    private final CountDownLatch latch = new CountDownLatch(1);
    private final AtomicBoolean authenticated = new AtomicBoolean(false);
    
    private JFrame frame;
    private JTextField keyField;
    private JLabel statusLabel;
    private JButton loginButton;
    private JLabel hwidLabel;
    
    public boolean showAndWait() {
        SwingUtilities.invokeLater(this::createAndShowGUI);
        
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        return authenticated.get();
    }
    
    private void createAndShowGUI() {
        frame = new JFrame("fent@nyl - 登录验证");
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setSize(400, 320);
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        frame.setUndecorated(true);
        
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(BG_COLOR);
        mainPanel.setBorder(BorderFactory.createLineBorder(ACCENT_COLOR, 2));
        
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(PANEL_COLOR);
        titlePanel.setPreferredSize(new Dimension(400, 50));
        
        JLabel titleLabel = new JLabel("  fent@nyl Client", JLabel.LEFT);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(ACCENT_COLOR);
        titlePanel.add(titleLabel, BorderLayout.WEST);
        
        JButton closeButton = new JButton("×");
        closeButton.setFont(new Font("Segoe UI", Font.BOLD, 20));
        closeButton.setForeground(TEXT_COLOR);
        closeButton.setBackground(PANEL_COLOR);
        closeButton.setBorderPainted(false);
        closeButton.setFocusPainted(false);
        closeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        closeButton.addActionListener(e -> exit());
        closeButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                closeButton.setBackground(ERROR_COLOR);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                closeButton.setBackground(PANEL_COLOR);
            }
        });
        titlePanel.add(closeButton, BorderLayout.EAST);
        
        mainPanel.add(titlePanel, BorderLayout.NORTH);
        
        JPanel centerPanel = new JPanel();
        centerPanel.setBackground(BG_COLOR);
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
        
        JLabel keyLabel = new JLabel("密钥 (Key)");
        keyLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        keyLabel.setForeground(TEXT_COLOR);
        keyLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        centerPanel.add(keyLabel);
        centerPanel.add(Box.createVerticalStrut(8));
        
        keyField = new JTextField();
        keyField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        keyField.setBackground(PANEL_COLOR);
        keyField.setForeground(TEXT_COLOR);
        keyField.setCaretColor(TEXT_COLOR);
        keyField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(60, 60, 65), 1),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        keyField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        keyField.setAlignmentX(Component.LEFT_ALIGNMENT);
        centerPanel.add(keyField);
        
        centerPanel.add(Box.createVerticalStrut(15));
        
        JPanel hwidPanel = new JPanel(new BorderLayout());
        hwidPanel.setBackground(BG_COLOR);
        hwidPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        hwidPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        
        JLabel hwidTitleLabel = new JLabel("HWID (已自动获取)");
        hwidTitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        hwidTitleLabel.setForeground(new Color(150, 150, 150));
        hwidPanel.add(hwidTitleLabel, BorderLayout.NORTH);
        
        String hwid = HWIDUtil.getHWID();
        hwidLabel = new JLabel(hwid);
        hwidLabel.setFont(new Font("Consolas", Font.PLAIN, 12));
        hwidLabel.setForeground(new Color(180, 180, 180));
        hwidPanel.add(hwidLabel, BorderLayout.CENTER);
        
        JButton copyHwidButton = new JButton("复制");
        copyHwidButton.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        copyHwidButton.setBackground(PANEL_COLOR);
        copyHwidButton.setForeground(TEXT_COLOR);
        copyHwidButton.setBorderPainted(false);
        copyHwidButton.setFocusPainted(false);
        copyHwidButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        copyHwidButton.addActionListener(e -> {
            StringSelection selection = new StringSelection(hwid);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
            showStatus("HWID已复制到剪贴板", false);
        });
        hwidPanel.add(copyHwidButton, BorderLayout.EAST);
        
        centerPanel.add(hwidPanel);
        
        centerPanel.add(Box.createVerticalStrut(20));
        
        loginButton = new JButton("验证登录");
        loginButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        loginButton.setBackground(ACCENT_COLOR);
        loginButton.setForeground(Color.WHITE);
        loginButton.setBorderPainted(false);
        loginButton.setFocusPainted(false);
        loginButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        loginButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        loginButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        loginButton.addActionListener(e -> doLogin());
        loginButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                loginButton.setBackground(ACCENT_COLOR.brighter());
            }
            @Override
            public void mouseExited(MouseEvent e) {
                loginButton.setBackground(ACCENT_COLOR);
            }
        });
        centerPanel.add(loginButton);
        
        centerPanel.add(Box.createVerticalStrut(15));
        
        statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLabel.setForeground(TEXT_COLOR);
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        centerPanel.add(statusLabel);
        
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        
        frame.setContentPane(mainPanel);
        frame.setVisible(true);
        
        keyField.addActionListener(e -> doLogin());
        
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                exit();
            }
        });
        
        frame.getRootPane().registerKeyboardAction(
            e -> exit(),
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW
        );
    }
    
    private void doLogin() {
        String key = keyField.getText().trim();
        
        if (key.isEmpty()) {
            showStatus("请输入密钥", true);
            return;
        }
        
        loginButton.setEnabled(false);
        loginButton.setText("验证中...");
        showStatus("正在连接服务器...", false);
        
        SwingWorker<AuthClient.AuthResult, Void> worker = new SwingWorker<>() {
            @Override
            protected AuthClient.AuthResult doInBackground() {
                return AuthClient.verify(key);
            }
            
            @Override
            protected void done() {
                try {
                    AuthClient.AuthResult result = get();
                    if (result.success()) {
                        showStatus("验证成功！正在启动...", false);
                        statusLabel.setForeground(SUCCESS_COLOR);
                        authenticated.set(true);
                        Timer timer = new Timer(800, e -> {
                            frame.dispose();
                            latch.countDown();
                        });
                        timer.setRepeats(false);
                        timer.start();
                    } else {
                        showStatus(result.message(), true);
                        loginButton.setEnabled(true);
                        loginButton.setText("验证登录");
                    }
                } catch (Exception e) {
                    showStatus("验证过程出错: " + e.getMessage(), true);
                    loginButton.setEnabled(true);
                    loginButton.setText("验证登录");
                }
            }
        };
        worker.execute();
    }
    
    private void showStatus(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.setForeground(isError ? ERROR_COLOR : new Color(150, 150, 150));
    }
    
    private void exit() {
        frame.dispose();
        latch.countDown();
        System.exit(0);
    }
}
