package game_server_proj;

import java.io.*;
import java.util.Scanner;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Logger {
    private final int USER_PW_POS = 10;
    private final int USER_RANK_POS = 6;
    private final Lock fileLock = new ReentrantLock();

    public Logger() {
    }

    public void createFile(String filename) {
        try {
            fileLock.lock(); // Acquire lock
            File myFile = new File(filename);

            if (myFile.createNewFile()) {
                System.out.println("File created: " + myFile.getName());
            } else {
                System.out.println("File already exists.");
            }
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        } finally {
            fileLock.unlock(); // Release lock in the finally block
        }
    }

    public void logInfo(String fileName, String infoToWrite) {
        try {
            fileLock.lock(); // Acquire lock
            FileWriter myWriter = new FileWriter(fileName, true);
            myWriter.write(infoToWrite);
            myWriter.close();
        } catch (Exception e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        } finally {
            fileLock.unlock(); // Release lock in the finally block
        }
    }

	public String checkLogIn(String userName, String password){
		boolean foundUserName = false;
		boolean foundUser = false;
		String foundUserNameRank = "";

		try {
			File accounts_file = new File("accounts.txt");
			Scanner myReader = new Scanner(accounts_file);

			while (myReader.hasNextLine()) {
				String data = myReader.nextLine();

				if(data.contains("Password:") && foundUserName){
					String existingPassword = data.substring(USER_PW_POS, data.length());

					if(password.equals(existingPassword)){
						foundUser = true;
						continue;
					}
					myReader.close();
					break;
				}

				if(data.contains("Username:")){
					String existingUserName = data.substring(USER_PW_POS, data.length());
					if(userName.equals(existingUserName)) foundUserName = true;
					continue;
				}

				if(data.contains("Rank:")){
					if(foundUser){
						foundUserNameRank = data.substring(USER_RANK_POS);
						myReader.close();
						break;
					}
				}

			}
			myReader.close();
		} catch (FileNotFoundException e) {
			System.out.println("An error occurred.");
			e.printStackTrace();
		}

		return foundUserNameRank;
	}

    public boolean checkUserExists(String userName) {
        try {
            fileLock.lock(); // Acquire lock
            File accounts_file = new File("accounts.txt");
            Scanner myReader = new Scanner(accounts_file);

            while (myReader.hasNextLine()) {
                String data = myReader.nextLine();

                if (data.contains("Username:")) {
                    String existingUserName = data.substring(USER_PW_POS, data.length());
                    if (userName.equals(existingUserName)) {
                        myReader.close();
                        return true;
                    }
                    continue;
                }
            }
            myReader.close();
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        } finally {
            fileLock.unlock(); // Release lock in the finally block
        }

        return false;
    }

    public Boolean updateRank(String userName, String password, String rank){
        try {
            fileLock.lock(); // Acquire lock
            File accounts_file = new File("accounts.txt");
            String file_contents = "";
            Scanner myReader = new Scanner(accounts_file);
            Boolean foundUser = false;
            while (myReader.hasNextLine()) {
                String data = myReader.nextLine();
                if (data.contains("Username:")) {
                    if (userName.equals(data.substring(USER_PW_POS, data.length()))) 
                        foundUser = true;
                }
                if (data.contains("Rank:") && foundUser) {
                    file_contents += "Rank: " + rank + "\n";
                    foundUser = false;
                }else{
                    file_contents += data + "\n";
                }
            }
            FileWriter writer = new FileWriter("accounts.txt");
            writer.write(file_contents);
            writer.close();
            myReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            fileLock.unlock(); // Release lock in the finally block
        }
        return false;
    }
}