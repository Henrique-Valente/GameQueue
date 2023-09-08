package game_server_proj;

import java.util.UUID;

public class SessionToken {
    private long expirationTime;
    private String token; 
    private long generatedTime; 

    public SessionToken(long expirationTime) {
        this.expirationTime = expirationTime;
    }

    // Function to generate the token
    public boolean generateToken(){
        if(this.token != null) return false; // In case the token already exists for this user

        // Generate a unique token with UUID
        this.token = UUID.randomUUID().toString();

        // Get time at which the token was generated
        this.generatedTime = System.currentTimeMillis();

        return true;
    }

    // Get the token
    public String getToken(){
        return this.token;
    }

    // Checks if the token has already expired
    public boolean isExpired() {
        return System.currentTimeMillis() > (expirationTime + generatedTime);
    }
}