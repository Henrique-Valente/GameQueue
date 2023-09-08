package game_server_proj;

import java.net.Socket;

public class Player {
    private SessionToken token;
    public String userName;
    private String password;
    private String rank; // Beginner, Intermediate and Expert
    private Logger logger = new Logger();
    private Long waitTime;
    public Socket playerSocket;

    public Player(String userName, String password){
        this.userName = userName;
        this.password = password;
    }

    public String getRank(){
        return rank;
    }

    public void start_wait(){
        waitTime = System.currentTimeMillis();
    }
    
    public Long get_waitTime(){
        return waitTime;
    }

    public String updateRank(Boolean up) {
        String old_rank = rank;
        switch (rank) {
            case "Beginner":
                rank = up ? "Intermediate" : "Beginner";
                break;
            case "Intermediate":
                rank = up ? "Expert" : "Beginner";
                break;
            case "Expert":
                rank = up ? "Expert" : "Intermediate";
                break;
        }
        if(!old_rank.equals(rank))
            logger.updateRank(userName, password, rank);
        return rank;
    }

    public SessionToken getPlayerToken(){
        return this.token;
    }

    public void generatePlayerToken(){
        this.token = new SessionToken(3600); // 1 Hour 
        token.generateToken();
    }

    public boolean logIn(){
        rank = logger.checkLogIn(userName, password);
        if(!rank.equals("")) return true;
        return false;
    }

    public void setSocket(Socket inputSocket){
        this.playerSocket = inputSocket;
    }

    public String registerPlayer(){
        rank = "Beginner";

        if (logger.checkUserExists(userName)) {
            return "Username already exists! Please try another one";
        }

        else if (userName.contains("Password") || userName.contains("Username")) {
            return "The Username cannot contain \"Username\" or \"Password\"!";
        }

        else if (password.contains("Password") || password.contains("Username")) {
            return "The Password cannot contain \"Username\" or \"Password\"!";
        }
        else{
            logger.logInfo("accounts.txt","\nUsername: " + userName + "\nPassword: " + password + "\nRank: Beginner\n");
            return "User registered successfully!";
        }
    }
    
    @Override
    public boolean equals(Object obj) {
        if(obj == null) return false;

        if(obj == this) return true;

        Player objPlayer = (Player) obj;

        return objPlayer.userName.equals(this.userName) && objPlayer.password.equals(this.password);
    }

    @Override
    public String toString() {
        return "Username: " + userName + " Rank: " + rank + " Socket: " + playerSocket.toString();
    }
}