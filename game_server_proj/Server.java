package game_server_proj;

import java.io.*;
import java.net.*;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
 
public class Server {
    private static BlockingLinkedList<Player> rankedPlayerQueue; //Ranked Game waiting queue (List for efficiency reasons)
    private static BlockingQueue<Player> playerQueue; // Game waiting queue
    private static BlockingLinkedList<Game> gameList; // List of running games
    private static BlockingLinkedList<Long> num_rank_players; // Ranked Game waiting queue stats
    static final int NUM_PLAYERS = 2; // Number of players per game
    static final int MAX_GAMES = 5; //Max number of running games
    static final int RELAX_RANK_WAIT_TIME = 20000; //Wait time that relaxes player allocation 
    static final ExecutorService threadPool = Executors.newFixedThreadPool(MAX_GAMES);
    static final ExecutorService clientThreadPool = Executors.newCachedThreadPool();

    public static void transferPlayers(Boolean ranked, String rank) throws IOException, InterruptedException{ // Method to transfer from queue to a game
        List<Player> playersReady = new LinkedList<>();
        OutputStream output;

        if(!ranked)
            playersReady = playerQueue.removeElements(NUM_PLAYERS);
        else{
/*             String rank = "Expert";
            if(num_rank_players.get(0) >= NUM_PLAYERS)
                rank = "Beginner";
            else if(num_rank_players.get(1) >= NUM_PLAYERS)
                rank = "Intermediate";
            //select players from rankedq ueue with same rank*/
            for (int i = 0; i < rankedPlayerQueue.size(); i++) {
                if (playersReady.size() >= NUM_PLAYERS) {break;}
                Player player = rankedPlayerQueue.get(i);
                Long wait_time = System.currentTimeMillis()-player.get_waitTime();
                //Adds player if rank is the same or if wait time is bigger than 20 sec (relaxed selection)
                if (player.getRank().equals(rank) || wait_time >= RELAX_RANK_WAIT_TIME) {
                    playersReady.add(player);
                    add_num_rank(player, -1);
                }
            }
            //remove player from rankedqueue
            for(Player p : playersReady){
                rankedPlayerQueue.remove(p);
            }
        }

        //Send Message to Players that a game has been found
        for (Player player : playersReady) {
            try {
                output = player.playerSocket.getOutputStream();
                PrintWriter writer = new PrintWriter(output,true);
                writer.println("Game found! Joining...");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        updatePlayersAboutQueue(ranked);

        Game game = new Game(playersReady, gameList);
        threadPool.execute(game);
        gameList.add(game);
    }

    public static void add_num_rank(Player player, int inc){
        try{
            if(player.getRank().equals("Beginner"))
                num_rank_players.set(0,num_rank_players.get(0)+inc);
            else if(player.getRank().equals("Intermediate"))
                num_rank_players.set(1,num_rank_players.get(1)+inc);
            else
                num_rank_players.set(2,num_rank_players.get(2)+inc);
        } catch(InterruptedException e){e.printStackTrace();}
        return;
    }
    
    public static void updatePlayersAboutQueue(Boolean ranked){
        clientThreadPool.submit(() -> {
            try{
                if(ranked){
                    for (int i = 0; i < rankedPlayerQueue.size(); i++) {
                        Player player = rankedPlayerQueue.get(i);
                        PrintWriter writer = new PrintWriter(player.playerSocket.getOutputStream(),true);
                        writer.println("Your position in queue is " + rankedPlayerQueue.position(player));
                    }
                }else{
                    Iterator<Player> iterator = playerQueue.iterator();
                    while (iterator.hasNext()) {
                        Player player = iterator.next();
                        PrintWriter writer = new PrintWriter(player.playerSocket.getOutputStream(),true);
                        writer.println("Your position in queue is " + playerQueue.position(player));
                    }
                }
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
        });
        
    }

    //Function to continuously check wether or not a game is ready to start
    public static void check_launch_game(){
        try {
            while(true){
                if(playerQueue.size() >= NUM_PLAYERS)
                    transferPlayers(false, "");

                Long bored_beginner_players = 0L;
                Long bored_intermediate_players = 0L;
                Long bored_expert_players = 0L;
                for(int i = 0; i < rankedPlayerQueue.size(); i++){
                    Long wait_time = System.currentTimeMillis() - rankedPlayerQueue.get(i).get_waitTime();
                    if(wait_time >= RELAX_RANK_WAIT_TIME){
                        switch(rankedPlayerQueue.get(i).getRank()){
                            case "Beginner":
                                bored_beginner_players++;
                                break;
                            case "Intermediate":
                                bored_intermediate_players++;
                                break;
                            case "Expert":
                                bored_expert_players++;
                                break;
                        }
                    }
                }
                Long num_players_beginnerGame = num_rank_players.get(0) + bored_intermediate_players + bored_expert_players;
                Long num_players_IntermediateGame = num_rank_players.get(1) + bored_beginner_players + bored_expert_players;
                Long num_players_ExpertGame = num_rank_players.get(2) + bored_beginner_players + bored_intermediate_players;
                if(num_players_beginnerGame>= NUM_PLAYERS)
                    transferPlayers(true, "Beginner");
                else if(num_players_IntermediateGame>= NUM_PLAYERS)
                    transferPlayers(true, "Intermediate");
                else if(num_players_ExpertGame>= NUM_PLAYERS)
                    transferPlayers(true, "Expert");
                Thread.sleep(100); //sleep 100ms
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return;
    }
 
    public static void main(String[] args) {
        if (args.length < 1) return;
 
        int port = Integer.parseInt(args[0]);
        
        gameList = new BlockingLinkedList<>();
        playerQueue = new BlockingQueue<>();
        rankedPlayerQueue = new BlockingLinkedList<>();
        num_rank_players = new BlockingLinkedList<>();
        try{
            num_rank_players.add(0L); //Beginner
            num_rank_players.add(0L); //Intermediate
            num_rank_players.add(0L); //Expert
        } catch(InterruptedException e){e.printStackTrace();}

        try (ServerSocket serverSocket = new ServerSocket(port)) {
 
            System.out.println("Server is listening on port " + port);
            //thread to continuosly check game launch conditions
            clientThreadPool.submit(() -> check_launch_game());
            while (true) {
                Socket socket = serverSocket.accept();

                // Handle each client connection in a new thread
                clientThreadPool.submit(() -> {
                    try {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        PrintWriter writer = new PrintWriter(socket.getOutputStream(),true);

                        String token = reader.readLine();
                        
                        Player player = playerQueue.getPlayerByToken(token);
                        Player rankedPlayer = rankedPlayerQueue.getPlayerByToken(token);

                        if(player != null){
                            playerQueue.replace(player, socket); // Replace socket
                            writer.println("Valid token");
                            writer.println("Waiting for game...");
                            writer.println("Your position in queue is " + playerQueue.position(player));
                        }

                        else if(rankedPlayer != null){
                            rankedPlayerQueue.replace(rankedPlayer, socket);
                            writer.println("Valid token");
                            writer.println("Waiting for game...");
                            writer.println("Your position in queue is " + rankedPlayerQueue.position(rankedPlayer));
                        }

                        else{
                            writer.println("Token rejected");

                            String auth = reader.readLine();
                            String userName = reader.readLine();
                            String password = reader.readLine();

                            String gameType;

                            player = new Player(userName, password);

                            if(auth.equals("log")){

                                // If player already created account
                                if(player.logIn()){
                                    writer.println("Logged in");

                                    gameType = reader.readLine();

                                    switch (gameType) {
                                        case "ranked":
                                            player.setSocket(socket);
                                            rankedPlayerQueue.add(player);
                                            player.generatePlayerToken();
                                            writer.println(player.getPlayerToken().getToken());
                                            add_num_rank(player,1);
                                            player.start_wait();
                                            writer.println("Waiting for game...");
                                            writer.println("Your position in queue is " + rankedPlayerQueue.position(player));
                                            break;
                                        
                                        case "normal":
                                            player.setSocket(socket);
                                            playerQueue.add(player);
                                            player.generatePlayerToken();
                                            writer.println(player.getPlayerToken().getToken());
                                            writer.println("Waiting for game...");
                                            writer.println("Your position in queue is " + playerQueue.position(player));
    
                                            break;
    
                                        default:
                                            writer.println("Something went wrong with the game type selection");
                                            socket.close();
                                            break;
                                    }
                                }
    
                                else{
                                    writer.println("Wrong credentials! Please try again");
                                    socket.close();
                                }
                            }

                            // If player wants to create account
                            else if(auth.equals("reg")){
                                String response = player.registerPlayer();

                                if(response.equals("User registered successfully!")){
                                    writer.println("Logged in");

                                    gameType = reader.readLine();

                                    switch (gameType) {
                                        case "ranked":
                                            player.setSocket(socket);
                                            rankedPlayerQueue.add(player);
                                            player.generatePlayerToken();
                                            writer.println(player.getPlayerToken().getToken());
                                            add_num_rank(player,1);
                                            player.start_wait();
                                            writer.println("Waiting for game...");
                                            writer.println("Your position in queue is " + rankedPlayerQueue.position(player));
                                        
                                            break;
                                        
                                        case "normal":
                                            player.setSocket(socket);
                                            playerQueue.add(player);
                                            player.generatePlayerToken();
                                            writer.println(player.getPlayerToken().getToken());
                                            writer.println("Waiting for game...");
                                            writer.println("Your position in queue is " + playerQueue.position(player));
    
                                            break;
    
                                        default:
                                            writer.println("Something went wrong with the game type selection");
                                            socket.close();
                                            break;
                                    }
                                }

                                else{
                                    writer.println(response);
                                    socket.close();
                                }
                            }
                            
                            else{
                                writer.println("Something went wrong! Check your input");
                                socket.close();
                            }
                        }

                    } catch (IOException | InterruptedException e) {
                        e.printStackTrace();
                    }
                });
            }
        } catch (IOException ex) {
            System.out.println("Server exception: " + ex.getMessage());
            ex.printStackTrace();
        }

        // Shutdown the executor service
        threadPool.shutdown();
        clientThreadPool.shutdown();
    }
}