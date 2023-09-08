package game_server_proj;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.function.Consumer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.CountDownLatch;

public class Game implements Runnable {
    private List<Socket> userSockets = new LinkedList<>();
    private List<Player> playerList; //List of players (used to update rank)
    private BlockingLinkedList<Game> activeGames;
    private ExecutorService executorService;
    private int num_rounds = 5;

    public Game(List<Player> playerList, BlockingLinkedList<Game> activeGames) {
        this.playerList = playerList;
        for(Player p : playerList){
            this.userSockets.add(p.playerSocket);
        }
        this.activeGames = activeGames;
    }

    @Override
    public void run() {
        try {
            start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start() throws IOException {
        System.out.println("Starting game with " + userSockets.size() + " players!");
        executorService = Executors.newFixedThreadPool(userSockets.size());
        List<Long> punctuactions = gameloop();
        sendMessageToAll("END_CONNECTION\n");   // end connections
        executorService.shutdown();
        updateRanks(punctuactions); // NEEDS TO BE FINISHED
        endGame();
    }

    public List<Long> gameloop(){

        // Pairs of questions and answers
        List<Map.Entry<String, String>> q_a = get_quiz();
        // Punctuaction of both players at the moment
        List<Long> punctuactions = new ArrayList<Long>(Collections.nCopies(userSockets.size(), 0L));

        //Welcoming Message
        sendMessageToAll("\n\tWelcome to Speed Trivia!\n");

        // GameLoop consisting of: 1-Ask the question 2-Get the answers
        // 3-Give FeedBack 4-Punctuate answers (update points)
        for(int i = 0; i < num_rounds; i++){
            sendMessageToAll("\nQUESTION "+(i+1)+":" + q_a.get(i).getKey() +"\n");
            List<Map.Entry<String, Long>> answers = get_answers();
            sendFeedbackOnAnswerToAll(answers,q_a.get(i).getValue());
            punctuactions = punctuate_answers(answers,q_a.get(i).getValue(), punctuactions);
        }

        System.out.println(punctuactions);
        //Show Game Result to all Players and their punctuation
        sendPunctuationToAll(punctuactions);

        return punctuactions;
    }

    //Get the answers from all players, compares with correct answer and punctuate those answers
    //according to the time needed to answer the question
    public List<Long> punctuate_answers(List<Map.Entry<String, Long>> answers,
                                        String correct_answer, List<Long> punctuations) {

        correct_answer = correct_answer.toLowerCase();
        for (int i = 0; i < answers.size(); i++) {
            String answer = answers.get(i).getKey().toLowerCase();
            long answerTime = answers.get(i).getValue();
            long currentPunctuation = punctuations.get(i);

            // Decrease punctuation for correct answers that took longer than 10 seconds
            if (answerTime > 10000)
                currentPunctuation = Math.max(0, currentPunctuation - (answerTime - 10000));
                // Increase punctuation for correct answers within 10 seconds
            else if (answer.equals(correct_answer))
                currentPunctuation += (10000 - answerTime);
            // Decrease punctuation for wrong answers
            if(!answer.equals(correct_answer))
                currentPunctuation = Math.max(0, currentPunctuation - 5000);
            punctuations.set(i, currentPunctuation);
        }
        return punctuations;
    }


    //Get n random questions from the file "questions.txt"
    // Question -> (Question, Correct_answer)
    public List<Map.Entry<String, String>> get_quiz(){

        List<Map.Entry<String, String>> quiz = new ArrayList<Map.Entry<String, String>>();

        try{
            File myObj = new File("questions.txt");
            String question = "", correct_ans = "";
            Scanner myReader = new Scanner(myObj);
            int num_questions = Integer.parseInt(myReader.nextLine());
            List<Integer> combination = generateRandomNumbers(num_questions, num_rounds);
            for(int i = 0; i < num_questions; i++){
                myReader.nextLine();
                question = myReader.nextLine();
                correct_ans = myReader.nextLine();
                if(combination.contains(i))
                    quiz.add(new AbstractMap.SimpleEntry<String, String>(question.substring(2), correct_ans.substring(3)));
            }
            myReader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        Collections.shuffle(quiz);
        return quiz;
    }

    //Helper function for get_quiz() function -> randomly selects questions using combinations
    public static List<Integer> generateRandomNumbers(int n, int combinationSize) {
        if (combinationSize > n) {
            throw new IllegalArgumentException("Combination size cannot be greater than the maximum number.");
        }
        List<Integer> numbers = new ArrayList<>();
        for (int i = 0; i < n; i++)
            numbers.add(i);
        Collections.shuffle(numbers);
        List<Integer> randomNumbers = numbers.subList(0, combinationSize);
        Collections.sort(randomNumbers); // Increase performance when reading from file

        return randomNumbers;
    }

    //General Function to operate with multiple sockets
    private void executeForAllSockets(Consumer<Socket> action) {
        if (executorService.isShutdown()) return;

        // Concurrency -> Make threads wait for others to finish
        CountDownLatch latch = new CountDownLatch(userSockets.size());

        for (int i = 0; i < userSockets.size(); i++) {
            final int index = i;
            executorService.execute(() -> {
                Socket socket = userSockets.get(index);
                action.accept(socket);
                latch.countDown(); // Signal thread completion
            });
        }
        try {
            latch.await(); // Wait for all threads to complete
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    //Send Punctuation and Game Result to the player
    public void sendPunctuationToAll(List<Long> punctuations) {
        long winnerPunctuation = Collections.max(punctuations);
        executeForAllSockets(socket -> {
            try {
                final int index = userSockets.indexOf(socket); // Obtain the index of the socket
                OutputStream output = socket.getOutputStream();
                if (punctuations.get(index).equals(winnerPunctuation))
                    output.write("\n========================\n\tYou Win!\n========================\n".getBytes());
                else
                    output.write("\nYou Lost!\n".getBytes());
                output.write(("Your punctuation was: " + Long.toString(punctuations.get(index)) + "\n\n").getBytes());
                output.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    //Send Feedback on answer to all the player
    public void sendFeedbackOnAnswerToAll(List<Map.Entry<String, Long>> answers, String correct_answer) {
        executeForAllSockets(socket -> {
            try {
                final int index = userSockets.indexOf(socket); // Obtain the index of the socket
                OutputStream output = socket.getOutputStream();
                if (answers.get(index).getKey().toLowerCase().equals(correct_answer.toLowerCase()))
                    output.write("Correct Answer!\n".getBytes());
                else
                    output.write(("Wrong Answer! Correct answer was: " + correct_answer + "\n").getBytes());
                output.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    //Send a message to all players
    public void sendMessageToAll(String message) {
        executeForAllSockets(socket -> {
            try {
                OutputStream output = socket.getOutputStream();
                output.write(message.getBytes());
                output.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    //Get and answer from all players
    public List<Map.Entry<String, Long>> get_answers() {
        Map.Entry<String, Long> defaultValue = new AbstractMap.SimpleEntry<>("Default", 0L);
        List<Map.Entry<String, Long>> messagesReceived = new ArrayList<>(Collections.nCopies(userSockets.size(), defaultValue));

        executeForAllSockets(socket -> {
            try {
                final int index = userSockets.indexOf(socket); // Obtain the index of the socket
                OutputStream output = socket.getOutputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String msg;
                long startTime = System.currentTimeMillis();
                output.write("get_answer\n".getBytes());
                while ((msg = reader.readLine()) == null) continue;
                messagesReceived.set(index, Map.entry(msg, (System.currentTimeMillis() - startTime)));
                output.write("Waiting for other players...\n".getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        return messagesReceived;
    }

    //Updates Ranks of players according to punctuactions
    public void updateRanks(List<Long> punctuactions){
        long winnerPunctuation = Collections.max(punctuactions);
        for(int i = 0; i < playerList.size(); i++){
            if(punctuactions.get(i).equals(winnerPunctuation))
                playerList.get(i).updateRank(true);
            else
                playerList.get(i).updateRank(false);
        }
        return;
    }

    //Make necessary clean-ups to end the game
    public void endGame() throws IOException {
        for (Socket socket : userSockets)
            socket.close();

        try {
            activeGames.remove(this);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
