// File: ModernExpenseTracker.java
// A modern, responsive expense tracker with a side-tab layout, charts, reports, and more.
// FINAL VERSION: Includes account management features (change username/password) and full UI enhancements.

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.itextpdf.text.Document;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PiePlot;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Objects;
import java.util.Vector;
import java.util.stream.IntStream;

public class ModernExpenseTracker extends JFrame {

    // --- Core Components ---
    private Connection connection;
    private int currentUserId = -1;
    private String currentUsername = "";

    // --- Layouts ---
    private final CardLayout mainCardLayout = new CardLayout();
    private final JPanel mainPanel = new JPanel(mainCardLayout);
    private final CardLayout contentCardLayout = new CardLayout();
    private final JPanel contentPanel = new JPanel(contentCardLayout);

    // --- UI Elements ---
    private JTextField loginUserField, registerUserField;
    private JPasswordField loginPassField, registerPassField;
    private DefaultTableModel expensesTableModel, budgetsTableModel;
    private final JLabel monthlySpendLabel = new JLabel("₹0.00", SwingConstants.CENTER);
    private final JTextArea savingsSuggestionsArea = new JTextArea("Loading suggestions...");
    private ChartPanel pieChartPanel, barChartPanel;
    private final JLabel welcomeLabel = new JLabel("Welcome!", SwingConstants.CENTER);


    // UI Improvement: Define a consistent color palette
    private final Color NAV_BACKGROUND = new Color(30, 32, 47);
    private final Color ACCENT_COLOR = new Color(135, 206, 250);

    public ModernExpenseTracker() {
        setTitle("Spendora - Smart Expense Tracker");
        setSize(1280, 800);
        setMinimumSize(new Dimension(1100, 700));
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        connectToDatabase();
        initUI();
    }

    private void connectToDatabase() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            String url = "jdbc:mysql://localhost:3306/expense_tracker_modern_db?useSSL=false&serverTimezone=UTC";
            String user = "root";
            // ⚠ IMPORTANT: Replace with your actual MySQL password!
            String pass = "Haunted@123";
            connection = DriverManager.getConnection(url, user, pass);
        } catch (Exception e) {
            showErrorDialog("Database connection failed: " + e.getMessage());
            System.exit(1);
        }
    }

    private void initUI() {
        // UI Improvement: Softer background gradient
        GradientPanel backgroundPanel = new GradientPanel(new Color(40, 42, 58), new Color(28, 30, 43));
        backgroundPanel.setLayout(new BorderLayout());
        setContentPane(backgroundPanel);

        mainPanel.setOpaque(false);
        mainPanel.add(createAuthPanel("Login"), "Login");
        mainPanel.add(createAuthPanel("Register"), "Register");
        mainPanel.add(createAppPanel(), "App");

        backgroundPanel.add(mainPanel, BorderLayout.CENTER);
        mainCardLayout.show(mainPanel, "Login");
    }

    // --- Authentication Panels ---
    private JPanel createAuthPanel(String type) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 20, 10, 20);

        RoundedPanel formPanel = new RoundedPanel(30, new Color(255, 255, 255, 10));
        formPanel.setLayout(new GridBagLayout());
        formPanel.setBorder(new EmptyBorder(20, 40, 20, 40));

        JLabel titleLabel = new JLabel(type, UIManager.getIcon("FileChooser.homeFolderIcon"), SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 36));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setIconTextGap(15);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(10, 20, 20, 20);
        gbc.anchor = GridBagConstraints.CENTER;
        formPanel.add(titleLabel, gbc);

        gbc.insets = new Insets(10, 20, 10, 20);
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridy++;
        JLabel userLabel = new JLabel("Username:");
        userLabel.setForeground(Color.WHITE);
        userLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        formPanel.add(userLabel, gbc);

        gbc.gridy++;
        JTextField userField = (type.equals("Login")) ? (loginUserField = new RoundedTextField()) : (registerUserField = new RoundedTextField());
        formPanel.add(userField, gbc);

        gbc.gridy++;
        JLabel passLabel = new JLabel("Password:");
        passLabel.setForeground(Color.WHITE);
        passLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        formPanel.add(passLabel, gbc);

        gbc.gridy++;
        JPasswordField passField = (type.equals("Login")) ? (loginPassField = new RoundedPasswordField()) : (registerPassField = new RoundedPasswordField());
        formPanel.add(passField, gbc);

        gbc.gridy++;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(20, 20, 10, 20);
        gbc.anchor = GridBagConstraints.CENTER;
        GlowingButton actionButton = new GlowingButton(type);
        if (type.equals("Login")) {
            actionButton.addActionListener(e -> handleLogin());
        } else {
            actionButton.addActionListener(e -> handleRegister());
        }
        formPanel.add(actionButton, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(10, 20, 10, 20);
        JPanel switchPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 0));
        switchPanel.setOpaque(false);
        JLabel switchLabel = new JLabel(type.equals("Login") ? "Don't have an account?" : "Already have an account?");
        switchLabel.setForeground(Color.LIGHT_GRAY);
        JButton switchButton = new JButton(type.equals("Login") ? "Register" : "Login");
        styleLinkButton(switchButton);
        switchButton.addActionListener(e -> mainCardLayout.show(mainPanel, type.equals("Login") ? "Register" : "Login"));
        switchPanel.add(switchLabel);
        switchPanel.add(switchButton);
        formPanel.add(switchPanel, gbc);

        panel.add(formPanel, new GridBagConstraints());
        return panel;
    }

    // --- Main Application Panel ---
    private JPanel createAppPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JPanel navigationPanel = createNavigationPanel();
        navigationPanel.setPreferredSize(new Dimension(240, 0));

        contentPanel.setOpaque(false);
        contentPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        contentPanel.add(createExpensesPanel(), "Expenses");
        contentPanel.add(createBudgetsPanel(), "Budgets");
        contentPanel.add(createChartPanel(), "Chart");
        contentPanel.add(createReportsPanel(), "Reports");
        contentPanel.add(createAccountPanel(), "Account");

        panel.add(navigationPanel, BorderLayout.WEST);
        panel.add(contentPanel, BorderLayout.CENTER);
        return panel;
    }

    // --- Side Navigation ---
    private JPanel createNavigationPanel() {
        JPanel navPanel = new JPanel();
        navPanel.setLayout(new BoxLayout(navPanel, BoxLayout.Y_AXIS));
        navPanel.setBackground(NAV_BACKGROUND);
        navPanel.setBorder(new EmptyBorder(20, 10, 20, 10));

        JLabel title = new JLabel("Spendora");
        title.setFont(new Font("Segoe UI", Font.BOLD, 32));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        navPanel.add(title);
        navPanel.add(Box.createVerticalStrut(40));

        // ***** CHANGE IS HERE *****
        // Using a more visible and appropriate icon for the "Expenses" tab
        String[][] tabs = {
            {"Expenses", "FileChooser.listViewIcon"}, // <-- More visible icon
            {"Budgets", "FileView.hardDriveIcon"},
            {"Chart", "FileChooser.detailsViewIcon"},
            {"Reports", "FileView.floppyDriveIcon"},
            {"Account", "Tree.openIcon"}
        };

        for (String[] tabInfo : tabs) {
            String tabName = tabInfo[0];
            Icon icon = UIManager.getIcon(tabInfo[1]);
            JButton tabButton = createNavButton(tabName, icon);
            tabButton.addActionListener(e -> {
                switch (tabName) {
                    case "Chart" -> loadChartData();
                    case "Expenses" -> loadExpensesData();
                    case "Budgets" -> loadBudgetsData();
                }
                contentCardLayout.show(contentPanel, tabName);
            });
            navPanel.add(tabButton);
            navPanel.add(Box.createVerticalStrut(15));
        }

        navPanel.add(Box.createVerticalGlue());
        JButton themeButton = createNavButton("Toggle Theme", UIManager.getIcon("Actions.colorPicker"));
        themeButton.addActionListener(e -> toggleTheme());
        navPanel.add(themeButton);

        return navPanel;
    }

    // --- Content Panels for Each Tab ---
    private JPanel createChartPanel() {
        JPanel panel = new JPanel(new BorderLayout(20, 20));
        panel.setOpaque(false);

        RoundedPanel statsPanel = new RoundedPanel(20, new Color(0, 0, 0, 25));
        statsPanel.setLayout(new GridLayout(1, 2, 20, 20));
        statsPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel monthlySpendPanel = new JPanel(new BorderLayout());
        monthlySpendPanel.setOpaque(false);
        JLabel spendTitle = new JLabel("This Month's Spend", SwingConstants.CENTER);
        spendTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        spendTitle.setForeground(Color.WHITE);
        monthlySpendLabel.setFont(new Font("Segoe UI Semibold", Font.BOLD, 48));
        monthlySpendLabel.setForeground(ACCENT_COLOR);
        monthlySpendPanel.add(spendTitle, BorderLayout.NORTH);
        monthlySpendPanel.add(monthlySpendLabel, BorderLayout.CENTER);

        JPanel suggestionsPanel = new JPanel(new BorderLayout());
        suggestionsPanel.setOpaque(false);
        JLabel suggestionsTitle = new JLabel("Smart Savings Suggestions", SwingConstants.CENTER);
        suggestionsTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        suggestionsTitle.setForeground(Color.WHITE);
        savingsSuggestionsArea.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        savingsSuggestionsArea.setEditable(false);
        savingsSuggestionsArea.setOpaque(false);
        savingsSuggestionsArea.setForeground(Color.WHITE);
        savingsSuggestionsArea.setLineWrap(true);
        savingsSuggestionsArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(savingsSuggestionsArea);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(new EmptyBorder(10, 0, 0, 0));
        suggestionsPanel.add(suggestionsTitle, BorderLayout.NORTH);
        suggestionsPanel.add(scrollPane, BorderLayout.CENTER);

        statsPanel.add(monthlySpendPanel);
        statsPanel.add(suggestionsPanel);
        panel.add(statsPanel, BorderLayout.NORTH);

        RoundedPanel chartsPanel = new RoundedPanel(20, new Color(0, 0, 0, 25));
        chartsPanel.setLayout(new GridLayout(1, 2, 20, 20));
        chartsPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        pieChartPanel = new ChartPanel(null);
        pieChartPanel.setOpaque(false);
        barChartPanel = new ChartPanel(null);
        barChartPanel.setOpaque(false);
        chartsPanel.add(pieChartPanel);
        chartsPanel.add(barChartPanel);

        panel.add(chartsPanel, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createExpensesPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 15));
        panel.setOpaque(false);

        expensesTableModel = new DefaultTableModel(new String[]{"ID", "Date", "Category", "Amount", "Note"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable expensesTable = new JTable(expensesTableModel);
        JScrollPane scrollPane = new JScrollPane(expensesTable);
        styleTable(expensesTable, scrollPane);

        JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        controlsPanel.setOpaque(false);
        GlowingButton addButton = new GlowingButton("Add Expense", UIManager.getIcon("Tree.plusIcon"));
        GlowingButton editButton = new GlowingButton("Edit Selected", UIManager.getIcon("Actions.edit"));
        GlowingButton deleteButton = new GlowingButton("Delete Selected", UIManager.getIcon("Tree.minusIcon"));
        deleteButton.setBackground(new Color(231, 76, 60));

        addButton.addActionListener(e -> showAddExpenseDialog());
        editButton.addActionListener(e -> showEditExpenseDialog(expensesTable));
        deleteButton.addActionListener(e -> handleDeleteExpense(expensesTable));

        controlsPanel.add(addButton);
        controlsPanel.add(editButton);
        controlsPanel.add(deleteButton);

        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(controlsPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createBudgetsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 15));
        panel.setOpaque(false);

        budgetsTableModel = new DefaultTableModel(new String[]{"Category", "Limit", "Spent", "Status"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable budgetsTable = new JTable(budgetsTableModel);
        JScrollPane scrollPane = new JScrollPane(budgetsTable);
        styleTable(budgetsTable, scrollPane);
        budgetsTable.getColumn("Status").setCellRenderer(new BudgetStatusRenderer());

        JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        controlsPanel.setOpaque(false);
        GlowingButton setBudgetButton = new GlowingButton("Set/Update Budget", UIManager.getIcon("Actions.install"));
        setBudgetButton.addActionListener(e -> showSetBudgetDialog());
        controlsPanel.add(setBudgetButton);

        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(controlsPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createReportsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);

        GlowingButton exportCsvButton = new GlowingButton("Export Expenses to CSV", UIManager.getIcon("FileView.computerIcon"));
        GlowingButton exportPdfButton = new GlowingButton("Export Expenses to PDF", UIManager.getIcon("FileView.hardDriveIcon"));

        exportCsvButton.addActionListener(e -> exportToCSV());
        exportPdfButton.addActionListener(e -> exportToPDF());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        panel.add(exportCsvButton, gbc);
        gbc.gridy = 1;
        panel.add(exportPdfButton, gbc);

        return panel;
    }

    private JPanel createAccountPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);

        welcomeLabel.setFont(new Font("Segoe UI", Font.BOLD, 32));
        welcomeLabel.setForeground(Color.WHITE);

        // NEW: Account management buttons
        GlowingButton changeUserButton = new GlowingButton("Change Username", UIManager.getIcon("Actions.edit"));
        GlowingButton changePassButton = new GlowingButton("Change Password", UIManager.getIcon("PasswordField.keyIcon"));
        GlowingButton logoutButton = new GlowingButton("Logout", UIManager.getIcon("Actions.exit"));
        
        changeUserButton.addActionListener(e -> showChangeUsernameDialog());
        changePassButton.addActionListener(e -> showChangePasswordDialog());
        logoutButton.addActionListener(e -> handleLogout());
        
        logoutButton.setBackground(new Color(220, 20, 60)); // Crimson

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 20, 10);
        gbc.gridy = 0;
        panel.add(welcomeLabel, gbc);
        
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.gridy = 1;
        panel.add(changeUserButton, gbc);
        
        gbc.gridy = 2;
        panel.add(changePassButton, gbc);
        
        gbc.gridy = 3;
        gbc.insets = new Insets(20, 10, 10, 10); // More top margin for logout
        panel.add(logoutButton, gbc);

        return panel;
    }

    // --- Data Loading and Handling ---
    private void handleLogin() {
        String username = loginUserField.getText().trim();
        String password = new String(loginPassField.getPassword());
        if (username.isEmpty() || password.isEmpty()) {
            showInfoDialog("Username and password cannot be empty.");
            return;
        }

        new SwingWorker<Boolean, Void>() {
            private int userId;

            @Override
            protected Boolean doInBackground() throws Exception {
                String hashedPassword = hashPassword(password);
                String sql = "SELECT id, username FROM users WHERE username = ? AND password_hash = ?";
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setString(1, username);
                    ps.setString(2, hashedPassword);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            userId = rs.getInt("id");
                            currentUsername = rs.getString("username");
                            return true;
                        }
                    }
                }
                return false;
            }

            @Override
            protected void done() {
                try {
                    if (get()) {
                        currentUserId = userId;
                        loadExpensesData();
                        welcomeLabel.setText("Welcome, " + currentUsername);
                        mainCardLayout.show(mainPanel, "App");
                        contentCardLayout.show(contentPanel, "Expenses");
                    } else {
                        showErrorDialog("Invalid username or password.");
                    }
                } catch (Exception e) {
                    showErrorDialog("Login failed: " + e.getMessage());
                }
            }
        }.execute();
    }

    private void handleRegister() {
        String username = registerUserField.getText().trim();
        String password = new String(registerPassField.getPassword());
        if (username.isEmpty() || password.isEmpty()) {
            showInfoDialog("Username and password cannot be empty.");
            return;
        }

        new SwingWorker<Boolean, Void>() {
            private String errorMessage = null;

            @Override
            protected Boolean doInBackground() throws Exception {
                String hashedPassword = hashPassword(password);
                String sql = "INSERT INTO users (username, password_hash) VALUES (?, ?)";
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setString(1, username);
                    ps.setString(2, hashedPassword);
                    ps.executeUpdate();
                    return true;
                } catch (SQLIntegrityConstraintViolationException e) {
                    errorMessage = "Username already exists.";
                    return false;
                }
            }

            @Override
            protected void done() {
                try {
                    if (get()) {
                        showInfoDialog("Registration successful! Please log in.");
                        mainCardLayout.show(mainPanel, "Login");
                    } else {
                        showErrorDialog(errorMessage != null ? errorMessage : "Registration failed.");
                    }
                } catch (Exception e) {
                    showErrorDialog("Registration error: " + e.getMessage());
                }
            }
        }.execute();
    }

    private void loadChartData() {
        new SwingWorker<Void, Void>() {
            double monthlySpend = 0;
            String suggestions = "No suggestions available.";
            DefaultPieDataset pieData = new DefaultPieDataset();
            DefaultCategoryDataset barData = new DefaultCategoryDataset();

            @Override
            protected Void doInBackground() throws Exception {
                String spendSql = "SELECT SUM(amount) FROM expenses WHERE user_id = ? AND MONTH(expense_date) = MONTH(CURDATE()) AND YEAR(expense_date) = YEAR(CURDATE())";
                try (PreparedStatement ps = connection.prepareStatement(spendSql)) {
                    ps.setInt(1, currentUserId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) monthlySpend = rs.getDouble(1);
                    }
                }

                String chartSql = "SELECT category, SUM(amount) as total FROM expenses WHERE user_id = ? AND MONTH(expense_date) = MONTH(CURDATE()) AND YEAR(expense_date) = YEAR(CURDATE()) GROUP BY category ORDER BY total DESC";
                StringBuilder suggestionsBuilder = new StringBuilder("💡 Smart Savings Suggestions:\n\n");
                boolean foundSuggestion = false;
                try (PreparedStatement ps = connection.prepareStatement(chartSql)) {
                    ps.setInt(1, currentUserId);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            String category = rs.getString("category");
                            double total = rs.getDouble("total");
                            pieData.setValue(category, total);
                            barData.addValue(total, "Spend", category);

                            if (total > 2000) {
                                suggestionsBuilder.append(String.format("• High spending (₹%.2f) in '%s'. Consider reducing by 10%%.\n", total, category));
                                foundSuggestion = true;
                            }
                        }
                    }
                }
                if (!foundSuggestion) {
                    suggestionsBuilder.append("Your spending looks balanced. Keep it up!");
                }
                suggestions = suggestionsBuilder.toString();
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    monthlySpendLabel.setText(String.format("₹%.2f", monthlySpend));
                    savingsSuggestionsArea.setText(suggestions);
                    updateChartUI(pieData, barData);
                } catch (Exception e) {
                    showErrorDialog("Failed to load chart data: " + e.getMessage());
                }
            }
        }.execute();
    }

    private void loadExpensesData() {
        new SwingWorker<DefaultTableModel, Void>() {
            @Override
            protected DefaultTableModel doInBackground() throws Exception {
                DefaultTableModel model = new DefaultTableModel(new String[]{"ID", "Date", "Category", "Amount", "Note"}, 0) {
                    @Override
                    public boolean isCellEditable(int row, int column) { return false; }
                };
                String sql = "SELECT id, expense_date, category, amount, note FROM expenses WHERE user_id = ? ORDER BY expense_date DESC";
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setInt(1, currentUserId);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            Vector<Object> row = new Vector<>();
                            row.add(rs.getInt("id"));
                            row.add(rs.getDate("expense_date").toLocalDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
                            row.add(rs.getString("category"));
                            row.add(String.format("₹%.2f", rs.getDouble("amount")));
                            row.add(rs.getString("note"));
                            model.addRow(row);
                        }
                    }
                }
                return model;
            }

            @Override
            protected void done() {
                try {
                    expensesTableModel.setDataVector(get().getDataVector(), new Vector<>(Arrays.asList("ID", "Date", "Category", "Amount", "Note")));
                } catch (Exception e) {
                    showErrorDialog("Failed to load expenses: " + e.getMessage());
                }
            }
        }.execute();
    }

    private void loadBudgetsData() {
        new SwingWorker<DefaultTableModel, Void>() {
            @Override
            protected DefaultTableModel doInBackground() throws Exception {
                DefaultTableModel model = new DefaultTableModel(new String[]{"Category", "Limit", "Spent", "Status"}, 0) {
                    @Override
                    public boolean isCellEditable(int row, int column) { return false; }
                };
                String sql = "SELECT b.category, b.limit_amount, COALESCE((SELECT SUM(e.amount) FROM expenses e WHERE e.user_id = b.user_id AND e.category = b.category AND MONTH(e.expense_date) = MONTH(CURDATE()) AND YEAR(e.expense_date) = YEAR(CURDATE())), 0) as total_spent " +
                             "FROM budgets b " +
                             "WHERE b.user_id = ? " +
                             "GROUP BY b.category, b.limit_amount";
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setInt(1, currentUserId);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            Vector<Object> row = new Vector<>();
                            double limit = rs.getDouble("limit_amount");
                            double spent = rs.getDouble("total_spent");
                            String status;
                            if (spent > limit) status = "Exceeded";
                            else if (spent >= limit * 0.8) status = "Warning";
                            else status = "Safe";

                            row.add(rs.getString("category"));
                            row.add(String.format("₹%.2f", limit));
                            row.add(String.format("₹%.2f", spent));
                            row.add(status);
                            model.addRow(row);
                        }
                    }
                }
                return model;
            }

            @Override
            protected void done() {
                try {
                    DefaultTableModel model = get();
                    budgetsTableModel.setDataVector(model.getDataVector(), new Vector<>(Arrays.asList("Category", "Limit", "Spent", "Status")));

                    // Re-apply the custom renderer after setting new data
                    JPanel budgetsPanel = (JPanel) Arrays.stream(contentPanel.getComponents())
                        .filter(c -> contentPanel.getLayout().toString().contains(c.toString()) && c.getName() != null && c.getName().equals("Budgets"))
                        .findFirst().orElse(null);

                    if (budgetsPanel != null) {
                         JScrollPane scrollPane = (JScrollPane) budgetsPanel.getComponent(0);
                         JTable budgetsTable = (JTable) scrollPane.getViewport().getView();
                         budgetsTable.getColumn("Status").setCellRenderer(new BudgetStatusRenderer());
                    }

                } catch (Exception e) {
                    showErrorDialog("Failed to load budgets: " + e.getMessage());
                }
            }
        }.execute();
    }

    private void handleDeleteExpense(JTable table) {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            showInfoDialog("Please select an expense to delete.");
            return;
        }

        int expenseId = (int) table.getModel().getValueAt(selectedRow, 0);
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete this expense?", "Confirm Deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            new SwingWorker<Boolean, Void>() {
                @Override
                protected Boolean doInBackground() throws Exception {
                    String sql = "DELETE FROM expenses WHERE id = ? AND user_id = ?";
                    try (PreparedStatement ps = connection.prepareStatement(sql)) {
                        ps.setInt(1, expenseId);
                        ps.setInt(2, currentUserId);
                        int rowsAffected = ps.executeUpdate();
                        return rowsAffected > 0;
                    }
                }

                @Override
                protected void done() {
                    try {
                        if (get()) {
                            showInfoDialog("Expense deleted successfully.");
                            loadExpensesData();
                            loadChartData(); // Refresh chart data as well
                        } else {
                            showErrorDialog("Failed to delete the expense.");
                        }
                    } catch (Exception e) {
                        showErrorDialog("Error deleting expense: " + e.getMessage());
                    }
                }
            }.execute();
        }
    }

    // --- Dialogs for Add/Edit/Set ---
    private void showAddExpenseDialog() {
        JTextField amountField = new JTextField(10);
        String[] categories = {"Food", "Transport", "Shopping", "Bills", "Entertainment", "Health", "Other"};
        JComboBox<String> categoryCombo = new JComboBox<>(categories);
        JTextField noteField = new JTextField(20);
        JTextField dateField = new JTextField(LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")), 10);

        JPanel panel = new JPanel(new GridLayout(0, 2, 10, 10));
        panel.add(new JLabel("Amount (₹):"));
        panel.add(amountField);
        panel.add(new JLabel("Category:"));
        panel.add(categoryCombo);
        panel.add(new JLabel("Note:"));
        panel.add(noteField);
        panel.add(new JLabel("Date (dd-MM-yyyy):"));
        panel.add(dateField);

        int result = JOptionPane.showConfirmDialog(this, panel, "Add New Expense", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            try {
                double amount = Double.parseDouble(amountField.getText());
                String category = Objects.requireNonNull(categoryCombo.getSelectedItem()).toString();
                String note = noteField.getText();
                LocalDate date = LocalDate.parse(dateField.getText(), DateTimeFormatter.ofPattern("dd-MM-yyyy"));

                String sql = "INSERT INTO expenses (user_id, amount, category, note, expense_date) VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setInt(1, currentUserId);
                    ps.setDouble(2, amount);
                    ps.setString(3, category);
                    ps.setString(4, note);
                    ps.setDate(5, Date.valueOf(date));
                    ps.executeUpdate();
                    loadExpensesData();
                    loadChartData();
                }
            } catch (NumberFormatException ex) {
                showErrorDialog("Invalid amount. Please enter a number.");
            } catch (java.time.format.DateTimeParseException ex) {
                showErrorDialog("Invalid date format. Please use dd-MM-yyyy.");
            } catch (Exception ex) {
                showErrorDialog("Error adding expense: " + ex.getMessage());
            }
        }
    }

    private void showEditExpenseDialog(JTable table) {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            showInfoDialog("Please select an expense to edit.");
            return;
        }
        int expenseId = (int) table.getModel().getValueAt(selectedRow, 0);
        String currentDate = (String) table.getModel().getValueAt(selectedRow, 1);
        String currentCategory = (String) table.getModel().getValueAt(selectedRow, 2);
        String currentAmount = ((String) table.getModel().getValueAt(selectedRow, 3)).replace("₹", "");
        String currentNote = (String) table.getModel().getValueAt(selectedRow, 4);

        JTextField amountField = new JTextField(currentAmount, 10);
        String[] categories = {"Food", "Transport", "Shopping", "Bills", "Entertainment", "Health", "Other"};
        JComboBox<String> categoryCombo = new JComboBox<>(categories);
        categoryCombo.setSelectedItem(currentCategory);
        JTextField noteField = new JTextField(currentNote, 20);
        JTextField dateField = new JTextField(currentDate, 10);

        JPanel panel = new JPanel(new GridLayout(0, 2, 10, 10));
        panel.add(new JLabel("Amount (₹):"));
        panel.add(amountField);
        panel.add(new JLabel("Category:"));
        panel.add(categoryCombo);
        panel.add(new JLabel("Note:"));
        panel.add(noteField);
        panel.add(new JLabel("Date (dd-MM-yyyy):"));
        panel.add(dateField);

        int result = JOptionPane.showConfirmDialog(this, panel, "Edit Expense", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            try {
                double amount = Double.parseDouble(amountField.getText());
                String category = Objects.requireNonNull(categoryCombo.getSelectedItem()).toString();
                String note = noteField.getText();
                LocalDate date = LocalDate.parse(dateField.getText(), DateTimeFormatter.ofPattern("dd-MM-yyyy"));

                String sql = "UPDATE expenses SET amount = ?, category = ?, note = ?, expense_date = ? WHERE id = ? AND user_id = ?";
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setDouble(1, amount);
                    ps.setString(2, category);
                    ps.setString(3, note);
                    ps.setDate(4, Date.valueOf(date));
                    ps.setInt(5, expenseId);
                    ps.setInt(6, currentUserId);
                    ps.executeUpdate();
                    loadExpensesData();
                    loadChartData();
                }
            } catch (Exception ex) {
                showErrorDialog("Error updating expense: " + ex.getMessage());
            }
        }
    }

    private void showSetBudgetDialog() {
        String[] categories = {"Food", "Transport", "Shopping", "Bills", "Entertainment", "Health", "Other"};
        JComboBox<String> categoryCombo = new JComboBox<>(categories);
        JTextField limitField = new JTextField(10);

        JPanel panel = new JPanel(new GridLayout(0, 2, 10, 10));
        panel.add(new JLabel("Category:"));
        panel.add(categoryCombo);
        panel.add(new JLabel("Monthly Limit (₹):"));
        panel.add(limitField);

        int result = JOptionPane.showConfirmDialog(this, panel, "Set/Update Budget", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            try {
                double limit = Double.parseDouble(limitField.getText());
                String category = Objects.requireNonNull(categoryCombo.getSelectedItem()).toString();

                String sql = "INSERT INTO budgets (user_id, category, limit_amount) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE limit_amount = ?";
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setInt(1, currentUserId);
                    ps.setString(2, category);
                    ps.setDouble(3, limit);
                    ps.setDouble(4, limit); // For the UPDATE part
                    ps.executeUpdate();
                    loadBudgetsData();
                }
            } catch (NumberFormatException ex) {
                showErrorDialog("Invalid limit amount. Please enter a number.");
            } catch (Exception ex) {
                showErrorDialog("Error setting budget: " + ex.getMessage());
            }
        }
    }
    
     // --- Account Management Dialogs and Actions ---
    private void showChangeUsernameDialog() {
        String newUsername = JOptionPane.showInputDialog(this, "Enter new username:", "Change Username", JOptionPane.PLAIN_MESSAGE);
        if (newUsername != null && !newUsername.trim().isEmpty()) {
            if (newUsername.trim().equals(currentUsername)) {
                showInfoDialog("The new username is the same as the current one.");
                return;
            }
            new SwingWorker<Boolean, Void>() {
                private String errorMsg = "An unknown error occurred.";
                @Override
                protected Boolean doInBackground() throws Exception {
                    String sql = "UPDATE users SET username = ? WHERE id = ?";
                    try (PreparedStatement ps = connection.prepareStatement(sql)) {
                        ps.setString(1, newUsername.trim());
                        ps.setInt(2, currentUserId);
                        ps.executeUpdate();
                        return true;
                    } catch (SQLIntegrityConstraintViolationException e) {
                        errorMsg = "This username is already taken.";
                        return false;
                    } catch (SQLException e) {
                        errorMsg = "Database error: " + e.getMessage();
                        return false;
                    }
                }

                @Override
                protected void done() {
                    try {
                        if (get()) {
                            currentUsername = newUsername.trim();
                            welcomeLabel.setText("Welcome, " + currentUsername);
                            showInfoDialog("Username changed successfully!");
                        } else {
                            showErrorDialog(errorMsg);
                        }
                    } catch (Exception e) {
                        showErrorDialog("Failed to change username: " + e.getMessage());
                    }
                }
            }.execute();
        }
    }

    private void showChangePasswordDialog() {
        JPasswordField oldPassField = new JPasswordField(20);
        JPasswordField newPassField = new JPasswordField(20);
        JPasswordField confirmPassField = new JPasswordField(20);

        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.add(new JLabel("Current Password:"));
        panel.add(oldPassField);
        panel.add(new JLabel("New Password:"));
        panel.add(newPassField);
        panel.add(new JLabel("Confirm New Password:"));
        panel.add(confirmPassField);

        int result = JOptionPane.showConfirmDialog(this, panel, "Change Password", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            String oldPass = new String(oldPassField.getPassword());
            String newPass = new String(newPassField.getPassword());
            String confirmPass = new String(confirmPassField.getPassword());

            if (!newPass.equals(confirmPass)) {
                showErrorDialog("New passwords do not match.");
                return;
            }
            if (newPass.isEmpty()) {
                showErrorDialog("New password cannot be empty.");
                return;
            }

            new SwingWorker<String, Void>() {
                @Override
                protected String doInBackground() throws Exception {
                    // First, verify the old password
                    String hashedOldPass = hashPassword(oldPass);
                    String checkSql = "SELECT id FROM users WHERE id = ? AND password_hash = ?";
                    try (PreparedStatement ps = connection.prepareStatement(checkSql)) {
                        ps.setInt(1, currentUserId);
                        ps.setString(2, hashedOldPass);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (!rs.next()) {
                                return "Incorrect current password.";
                            }
                        }
                    }

                    // If old password is correct, update to the new one
                    String hashedNewPass = hashPassword(newPass);
                    String updateSql = "UPDATE users SET password_hash = ? WHERE id = ?";
                    try (PreparedStatement ps = connection.prepareStatement(updateSql)) {
                        ps.setString(1, hashedNewPass);
                        ps.setInt(2, currentUserId);
                        ps.executeUpdate();
                        return "Success";
                    }
                }

                @Override
                protected void done() {
                    try {
                        String status = get();
                        if ("Success".equals(status)) {
                            showInfoDialog("Password changed successfully.");
                        } else {
                            showErrorDialog(status);
                        }
                    } catch (Exception e) {
                        showErrorDialog("Failed to change password: " + e.getMessage());
                    }
                }
            }.execute();
        }
    }

    private void handleLogout() {
        currentUserId = -1;
        currentUsername = "";
        loginUserField.setText("");
        loginPassField.setText("");
        registerUserField.setText("");
        registerPassField.setText("");
        mainCardLayout.show(mainPanel, "Login");
    }


    // --- Helper & Utility Methods ---
    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private void exportToCSV() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save as CSV");
        fileChooser.setSelectedFile(new File("expenses.csv"));
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            try (FileWriter writer = new FileWriter(fileToSave)) {
                writer.append("ID,Date,Category,Amount,Note\n");
                for (int i = 0; i < expensesTableModel.getRowCount(); i++) {
                    writer.append(String.valueOf(expensesTableModel.getValueAt(i, 0))).append(',');
                    writer.append(String.valueOf(expensesTableModel.getValueAt(i, 1))).append(',');
                    writer.append(String.valueOf(expensesTableModel.getValueAt(i, 2))).append(',');
                    writer.append(String.valueOf(expensesTableModel.getValueAt(i, 3)).replace("₹", "")).append(',');
                    writer.append("\"").append(String.valueOf(expensesTableModel.getValueAt(i, 4))).append("\"\n");
                }
                showInfoDialog("Data exported successfully to " + fileToSave.getAbsolutePath());
            } catch (Exception ex) {
                showErrorDialog("Error exporting to CSV: " + ex.getMessage());
            }
        }
    }

    private void exportToPDF() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save as PDF");
        fileChooser.setSelectedFile(new File("expenses_report.pdf"));
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            Document document = new Document();
            try {
                PdfWriter.getInstance(document, new FileOutputStream(fileToSave));
                document.open();
                PdfPTable table = new PdfPTable(5); // 5 columns
                // Add headers
                IntStream.range(0, 5).forEach(i -> table.addCell(new PdfPCell(new Phrase(expensesTableModel.getColumnName(i)))));
                // Add data
                for (int i = 0; i < expensesTableModel.getRowCount(); i++) {
                    for (int j = 0; j < 5; j++) {
                        table.addCell(expensesTableModel.getValueAt(i, j).toString());
                    }
                }
                document.add(table);
                document.close();
                showInfoDialog("PDF report generated successfully at " + fileToSave.getAbsolutePath());
            } catch (Exception ex) {
                showErrorDialog("Error generating PDF: " + ex.getMessage());
            }
        }
    }

    private void updateChartUI(DefaultPieDataset pieData, DefaultCategoryDataset barData) {
        // Pie Chart
        JFreeChart pieChart = ChartFactory.createPieChart("Monthly Spend by Category", pieData, true, true, false);
        pieChart.setBackgroundPaint(null);
        pieChart.getTitle().setPaint(Color.WHITE);
        pieChart.getLegend().setBackgroundPaint(new Color(0,0,0,0));
        pieChart.getLegend().setItemPaint(Color.LIGHT_GRAY);
        PiePlot plot = (PiePlot) pieChart.getPlot();
        plot.setBackgroundPaint(null);
        plot.setOutlineVisible(false);
        plot.setLabelGenerator(null);
        pieChartPanel.setChart(pieChart);

        // Bar Chart
        JFreeChart barChart = ChartFactory.createBarChart("Category Spending", "Category", "Amount (₹)", barData);
        barChart.setBackgroundPaint(null);
        barChart.getTitle().setPaint(Color.WHITE);
        barChart.getLegend().setBackgroundPaint(new Color(0,0,0,0));
        barChart.getLegend().setItemPaint(Color.LIGHT_GRAY);
        barChart.getCategoryPlot().getDomainAxis().setTickLabelPaint(Color.LIGHT_GRAY);
        barChart.getCategoryPlot().getRangeAxis().setTickLabelPaint(Color.LIGHT_GRAY);
        barChart.getCategoryPlot().setBackgroundPaint(null);
        barChart.getCategoryPlot().setOutlineVisible(false);
        barChartPanel.setChart(barChart);
        
        repaint();
        revalidate();
    }

    // --- UI Styling Helpers ---
    private void styleLinkButton(JButton button) {
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setOpaque(false);
        button.setForeground(ACCENT_COLOR);
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    private JButton createNavButton(String text, Icon icon) {
        JButton button = new JButton(text, icon);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        button.setFont(new Font("Segoe UI", Font.BOLD, 16));
        button.setFocusPainted(false);
        button.setBorder(new EmptyBorder(10, 25, 10, 25));
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setIconTextGap(15);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }
    
    private void styleTable(JTable table, JScrollPane scrollPane) {
        table.setFillsViewportHeight(true);
        table.setRowHeight(30);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        scrollPane.getViewport().setBackground(NAV_BACKGROUND.brighter());
        scrollPane.setBorder(BorderFactory.createLineBorder(NAV_BACKGROUND, 1));
        
        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 16));
        header.setBackground(NAV_BACKGROUND);
        header.setForeground(Color.WHITE);
        
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        table.setDefaultRenderer(Object.class, centerRenderer);
    }
    
    private void toggleTheme() {
        try {
            if (UIManager.getLookAndFeel() instanceof FlatDarkLaf) {
                UIManager.setLookAndFeel(new FlatLightLaf());
            } else {
                UIManager.setLookAndFeel(new FlatDarkLaf());
            }
            SwingUtilities.updateComponentTreeUI(this);
        } catch (UnsupportedLookAndFeelException ex) {
            showErrorDialog("Theme could not be changed.");
        }
    }

    private void showInfoDialog(String message) {
        JOptionPane.showMessageDialog(this, message, "Information", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showErrorDialog(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }


    // --- Custom UI Component Inner Classes ---
    class GradientPanel extends JPanel {
        private final Color color1;
        private final Color color2;
        public GradientPanel(Color color1, Color color2) {
            this.color1 = color1;
            this.color2 = color2;
        }
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            int w = getWidth();
            int h = getHeight();
            GradientPaint gp = new GradientPaint(0, 0, color1, 0, h, color2);
            g2d.setPaint(gp);
            g2d.fillRect(0, 0, w, h);
        }
    }

    class RoundedPanel extends JPanel {
        private final int cornerRadius;
        private final Color backgroundColor;
        public RoundedPanel(int radius, Color bgColor) {
            super();
            cornerRadius = radius;
            backgroundColor = bgColor;
            setOpaque(false);
        }
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Dimension arcs = new Dimension(cornerRadius, cornerRadius);
            int width = getWidth();
            int height = getHeight();
            Graphics2D graphics = (Graphics2D) g;
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(backgroundColor);
            graphics.fillRoundRect(0, 0, width - 1, height - 1, arcs.width, arcs.height);
        }
    }

    class RoundedTextField extends JTextField {
        public RoundedTextField() {
            super(20);
            setOpaque(false);
            setBorder(new EmptyBorder(5, 10, 5, 10));
            setFont(new Font("Segoe UI", Font.PLAIN, 16));
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fill(new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, 20, 20));
            super.paintComponent(g2);
            g2.dispose();
        }
    }
    
    class RoundedPasswordField extends JPasswordField {
         public RoundedPasswordField() {
            super(20);
            setOpaque(false);
            setBorder(new EmptyBorder(5, 10, 5, 10));
            setFont(new Font("Segoe UI", Font.PLAIN, 16));
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fill(new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, 20, 20));
            super.paintComponent(g2);
            g2.dispose();
        }
    }
    
    class GlowingButton extends JButton {
        public GlowingButton(String text) { super(text); configureButton(); }
        public GlowingButton(String text, Icon icon) { super(text, icon); configureButton(); }

        private void configureButton() {
            setFont(new Font("Segoe UI", Font.BOLD, 14));
            setForeground(Color.WHITE);
            setBackground(new Color(70, 130, 180)); // Steel Blue
            setFocusPainted(false);
            setBorder(new EmptyBorder(12, 25, 12, 25));
            setContentAreaFilled(false);
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            if (getModel().isArmed()) {
                g2.setColor(getBackground().darker());
            } else if (getModel().isRollover()) {
                g2.setColor(getBackground().brighter());
            } else {
                g2.setColor(getBackground());
            }
            
            g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 30, 30));
            super.paintComponent(g2);
            g2.dispose();
        }
    }
    
    class BudgetStatusRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            String status = (String) value;
            c.setForeground(Color.WHITE);
            switch (status) {
                case "Exceeded" -> c.setBackground(new Color(231, 76, 60)); // Red
                case "Warning" -> c.setBackground(new Color(243, 156, 18)); // Orange
                case "Safe" -> c.setBackground(new Color(46, 204, 113));  // Green
                default -> c.setBackground(table.getBackground());
            }
            setHorizontalAlignment(JLabel.CENTER);
            return c;
        }
    }


    // --- Main Method ---
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (UnsupportedLookAndFeelException e) {
            System.err.println("Failed to initialize FlatLaf look and feel.");
        }

        SwingUtilities.invokeLater(() -> {
            ModernExpenseTracker app = new ModernExpenseTracker();
            app.setVisible(true);
        });
    }
}