package ui;


import java.util.Scanner;

public class Client {
    private final String serverURL;
    private final ServerFacade serverFacade;
    private boolean isRunning;

    public Client(String serverURL) {
        this.serverURL = serverURL;
        this.serverFacade = new ServerFacade(serverURL);
        this.isRunning = true;
    }

    public void run() {
        System.out.println("Welcome to 240 Chess! Type Help to get started");
        displayBoard();
        Scanner scanner = new Scanner(System.in);
        while(isRunning) {
            String command = scanner.nextLine().trim().toLowerCase();
            handleCommand(command, scanner);
        }
        scanner.close();
    }

    private void handleCommand(String command, Scanner scanner) {
        switch (command) {
            case "help":
                displayHelp();
                break;
            case "quit":
                isRunning = false;
                System.out.println("Goodbye!");
                break;
            case "login":
                handleLogin(scanner);
                break;
            case "register":
                handleRegister(scanner);
                break;
                default:
                   System.out.println("Invalid command, type Help for a list of commands");
        }
    }


    private void displayHelp() {
        System.out.println("Available commands: ");
        System.out.println("Help - Displays this help message");
        System.out.println("Login - Logs in to an existing account");
        System.out.println("Register - Registers a new user");
        System.out.println("Quit - Exit the client");
    }

    private void handleLogin(Scanner scanner) {
        System.out.println("Enter username: ");
        String username = scanner.nextLine().trim();
        System.out.println("Enter password: ");
        String password = scanner.nextLine().trim();
        try {
            String response = serverFacade.login(username, password);
            System.out.println("Login successful " + response);
        } catch (Exception e){
            System.out.println("Login failed: " + e.getMessage());
        }
    }

    private void handleRegister(Scanner scanner) {
        System.out.println("Enter username: ");
        String username = scanner.nextLine().trim();
        System.out.println("Enter password: ");
        String password = scanner.nextLine().trim();
        System.out.println("Enter email: ");
        String email = scanner.nextLine().trim();
        try {
            String response = serverFacade.register(username, password, email);
            System.out.println("Registration successful " + response);
        } catch (Exception e){
            System.out.println("Registration failed: " + e.getMessage());
        }
    }

    private void displayBoard() {
        System.out.println("Board");
    }

    public static void main(String[] args) {
        String serverURL = "http://localhost:8080";
        Client client = new Client(serverURL);
        client.run();
    }



}
