# Brief description

This project was made using Java.
The main objective was to make a game queue with multiple game types that allowed multiple clients to be connected at once, and multiple games to run at once. 
To achieve this, a thread pool approached was used, in conjuction with custom made classes that allowed concurrency without breaking the data.

# Running instructions

Ideally, it should be possible to use the present Makefile to run everything smoothly. From the folder where it is present, everything can be compiled with the comand **"make compile-all"**. After that, for demonstration purposes, there are already 5 accounts ready to be used. 

All .class files are stored on a folder next to the .java files.

### Running the server

The server can be started using the command **"make run-server**", or, alternatively, the main function is present on the **Server.java** file. By default it is on localhost on port 8082.


### Running the clients

The clients can be started by using the command **"make run-client-X"**. To run a new client, or if the Makefile script doesn't work, it is necessary to use a new folder since all clients create a new text file used for storage purposes, to simulate different environments.

# Features

### Game

The game is a Trivia similar to Kahoot. Players will all receive questions and get a score based on how well they performed given if they got the correct answer and how long they took.

### Log-in and Registration

Players must create accounts and log-in to their accounts to play the game.

### Token

In order to not lose their place in the queue, a token based system that let's players resume broken connections and still keep their place was implemented . The player doesn't have to log-in again if there is a valid token present in their storage file.

### Game modes

There are two game modes based on the player's choice.

##### Ranked

In this game mode, players can compete to increase their rank, and will be queued with other players depending on their rank.

##### Normal 

In this game mode, players will face other random players based on the order of arrival.