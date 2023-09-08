package game_server_proj;

import java.net.*;
import java.io.*;
import java.util.Scanner;
 
public class Client {

    private static void modifyStorage(File storage, String token) throws IOException{
        FileWriter fileWriter = new FileWriter(storage);
        fileWriter.write(token);
        fileWriter.close();
    }

    private static boolean checkForStorage() throws IOException{
        String currentDirectory = System.getProperty("user.dir");
        System.out.println(currentDirectory);
        File file = new File(currentDirectory + "/Storage.txt");

        return file.isFile();
    }

    private static String getToken(File storage) throws FileNotFoundException {
        try (FileReader fileReader = new FileReader(storage);
             BufferedReader bufferedReader = new BufferedReader(fileReader)) {
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }
            return stringBuilder.toString();
        } catch (IOException e) {
            throw new FileNotFoundException("Failed to read file: " + storage.getAbsolutePath());
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 2) return;

        Scanner myScanner = new Scanner(System.in);
        String token = "No token";
        String userName = "";
        String password = "";
        String auth = "";
        Boolean validToken = false;
 
        String hostname = args[0];
        int port = Integer.parseInt(args[1]);

        if(checkForStorage()){
            String currentDirectory = System.getProperty("user.dir");
            File storage = new File(currentDirectory + "/Storage.txt");
            token = getToken(storage);
        }

        else {
            System.out.println("Please type log if you wish to Log-in or reg if you wish to create a new account.");

            auth = myScanner.nextLine();
            System.out.println();

            while(!auth.equals("log") && !auth.equals("reg")){
                System.out.println("Invalid request.\nPlease type log if you wish to Log-in or reg if you wish to create a new account.");
                auth = myScanner.nextLine();
                System.out.println();
            }

            System.out.print("Username: ");
            userName = myScanner.nextLine();

            System.out.print("Password: ");
            password = myScanner.nextLine();
            System.out.println();
        }
        
        String gameType;
 
        try (Socket socket = new Socket(hostname, port)) {
            InputStream input = socket.getInputStream();
            OutputStream output = socket.getOutputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            PrintWriter writer = new PrintWriter(output, true); // for input text
            BufferedReader userInputReader = new BufferedReader(new InputStreamReader(System.in));

            writer.println(token); // Always sends the token for the server to check, even if it has no token

            String response = reader.readLine();

            if(response.equals("Valid token")) validToken = true;

            while(!response.equals("Logged in")){
                if(validToken) break;

                if(token.equals("No token")){
                    writer.println(auth);
                    writer.println(userName);
                    writer.println(password);

                    response = reader.readLine();
                } 
    
                else if(response.equals("Token rejected")){
                    System.out.println("Please type log if you wish to Log-in or reg if you wish to create a new account.");
    
                    auth = myScanner.nextLine();
                    System.out.println();
    
                    while(!auth.equals("log") && !auth.equals("reg")){
                        System.out.println("Invalid request.\nPlease type log if you wish to Log-in or reg if you wish to create a new account.");
                        auth = myScanner.nextLine();
                        System.out.println();
                    }
    
                    System.out.print("Username: ");
                    userName = myScanner.nextLine();
    
                    System.out.print("Password: ");
                    password = myScanner.nextLine();
                    System.out.println();
                    
                    writer.println(auth);
                    writer.println(userName);
                    writer.println(password);

                    response = reader.readLine();
                }

                else{
                    System.out.println(response);
                    System.exit(0);
                } 
            }

            if(!validToken){
                System.out.println("Welcome " + userName + 
                "! which gamemode do you wish to play? Type normal or ranked!");

                gameType = myScanner.nextLine();

                while(!gameType.equals("normal") && !gameType.equals("ranked")){
                    System.out.println("Please provide a valid game type!");
                    gameType = myScanner.nextLine();
                }

                writer.println(gameType);

                token = reader.readLine(); 
                File storage = new File("Storage.txt");

                modifyStorage(storage, token);
            }
                        
            while (true) {
                response = reader.readLine();
                if (response == null)
                    continue;
                if (response.equals("get_answer")) {
                    String userInput;
                    System.out.println("Answer: ");
                    while ((userInput = userInputReader.readLine()) == null) {
                        continue;
                    }
                    //System.out.println("sent to server: " + userInput);
                    // Send the user input to the server
                    writer.println(userInput);
                    continue;
                }
                else if (response.equals("END_CONNECTION"))
                    break;
                System.out.println(response);
            }

            System.out.println("Connection closed");

        } catch (UnknownHostException ex) {
            System.out.println("Server not found: " + ex.getMessage());
        } catch (IOException ex) {
            System.out.println("I/O error: " + ex.getMessage());
        }

        myScanner.close();
    }
}