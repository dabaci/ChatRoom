package server;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.*;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.sql.*;
import java.util.HashSet;

/**
 * Each client is assigned a ServerWorker which the server uses to handle interactions with the client.
 *
 * @author Devrim Abaci
 */
public class ServerWorker extends Thread {
    // TODO Initialize usersURL
    private static final String usersURL = null;
    private final Socket clientSocket;
    private final Server server;
    private String username;
    private OutputStream out;

    public ServerWorker(Socket clientSocket, Server server) {
        this.clientSocket = clientSocket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            handleClientSocket();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Initializes in and out with the client's input and output streams and handles client commands
     * @throws IOException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     */
    private void handleClientSocket() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        var in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        this.out = clientSocket.getOutputStream();

        while (true) {
            String[] tokens = in.readLine().split(" ");

            if (tokens.length > 0) {
                String cmd = tokens[0];
                if (cmd.equalsIgnoreCase("login") && handleLogin(tokens)) {
                    // Only break if login is successful
                    break;
                } else if (cmd.equalsIgnoreCase("create") && handleCreateAccount(tokens)) {
                    // Only break if account creation is successful
                    break;
                }
            }
        }
   }

    private boolean handleCreateAccount(String[] tokens)
            throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
        if (tokens.length == 4) {
            
            username = tokens[1];
            String password = tokens[2];
            String confirmPassword = tokens[3];

            if (password.equals(confirmPassword)) {
                // TODO create hash and store in database
               
                var random = new SecureRandom();
                var salt = new byte[32];
                random.nextBytes(salt);
                byte[] hash = getHash(password, salt);
                
                // TODO Convert hash and salt to Strings and update database
                send("Account created.\nLogged in as " + username + ".");
                return true;
            } else {
                send("Passwords do not match.");
                return false;
            }
        } else {
            send("Incorrect usage of command 'create'.");
            return false;
        }
    }

    // TODO implement
    private boolean handleLogin(String[] tokens) throws IOException {
        if (tokens.length == 3) {
            String username = tokens[1];
            String password = tokens[2];
        } else {
            return false;
        }

        send("Logged in as " + username + ".");
        return true;
    }

    private void send(String msg) throws IOException {
        out.write(msg.getBytes());
    }

    private byte[] getHash(String password, byte[] salt) throws NoSuchAlgorithmException, InvalidKeySpecException {
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 65536, 512);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PKBDF2WithHmacSHA512");
        return factory.generateSecret(spec).getEncoded();
    }

    // Possibly move to a util class?
    private static ResultSet query(String url, String query) {
        Connection connection = null;
        try {
            connection = DriverManager.getConnection(url);
            Statement statement = connection.createStatement();
            // TODO Could return multiple ResultSets.
            return statement.executeQuery(query);
        } catch (SQLException ex) {
            printSQLException(ex);
        } finally {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException ex) {
                printSQLException(ex);
            }
        }

        return null;
    }

    // TODO Check parameter names
    private static void update(String url, String query) {
        Connection connection = null;
        try {
            connection = DriverManager.getConnection(url);
            Statement statement = connection.createStatement();
            statement.executeUpdate(query);
        } catch (SQLException ex) {
            printSQLException(ex);
        } finally {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException ex) {
                printSQLException(ex);
            }
        }
    }

    // Possibly move to a util class?
    public static void printSQLException(SQLException ex) {
        for (Throwable e : ex) {
            if (e instanceof SQLException) {
                e.printStackTrace(System.err);
                System.err.println("SQLState: " + ((SQLException)e).getSQLState());
                System.err.println("Error Code: " + ((SQLException)e).getErrorCode());
                System.err.println("Message: " + e.getMessage());
                Throwable t = ex.getCause();
                while (t != null) {
                    System.out.println("Cause: " + t);
                    t = t.getCause();
                }
            }
        }
    }
}
