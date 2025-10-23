The project is a modern, responsive expense tracker application built in Java, featuring a side-tab layout, data visualization through charts, comprehensive reporting, and account management capabilities. 
It includes functionality for tracking expenses, setting budgets, generating reports, and managing user accounts with options to change usernames and passwords, all within a visually enhanced user interface.

Project Overview
The application, named Spendora, is designed as a smart expense tracker with a focus on user experience and data visualization. 
It integrates libraries such as FlatLaf for modern UI themes, JFreeChart for generating pie and bar charts, and iTextPDF for exporting data to PDF reports. 
The software connects to a MySQL database named expensetrackermoderndb to store user credentials, expenses, and bud
get information, with a hardcoded connection using root credentials that should be modified for security.

Features and Functionality
Key features include expense tracking with categories like Food, Transport, and Bills, budget management with visual status indicators (Safe, Warning, Exceeded), and data export capabilities to both CSV and PDF formats. 
The application provides a dashboard with a monthly spending summary and AI-generated savings suggestions based on spending patterns, calculated when monthly expenses exceed 2000 units. 
Users can toggle between light and dark themes, and the interface includes custom UI components like rounded panels, glowing buttons, and gradient backgrounds for enhanced aesthetics.

Technical Implementation
The codebase follows an object-oriented structure with a main ModernExpenseTracker class extending JFrame, managing multiple panels for authentication, expenses, budgets, charts, reports, and account settings through CardLayout. 
Data persistence is handled via JDBC with prepared statements to prevent SQL injection, and passwords are hashed using SHA-256 before storage. 
The application uses SwingWorker threads for database operations to maintain UI responsiveness during data loading and processing.
