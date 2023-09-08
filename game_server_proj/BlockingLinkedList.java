package game_server_proj;

import java.net.Socket;
import java.util.LinkedList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class BlockingLinkedList<T> {
    private LinkedList<T> list = new LinkedList<>();
    private Lock lock = new ReentrantLock();
    private Condition notEmpty = lock.newCondition();

    public BlockingLinkedList() {
    }

    // Add an element to the list
    public void add(T element) throws InterruptedException {
        lock.lock();
        try {
            list.add(element);
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    // Remove element from list
    public void remove(T element) throws InterruptedException {
        lock.lock();
        try {
            while (list.isEmpty()) {
                notEmpty.await();
            }
            
            list.remove(element);
        } finally {
            lock.unlock();
        }
    }

    // Remove element from list
    public void remove(int index) throws InterruptedException {
        lock.lock();
        try {
            while (list.isEmpty()) {
                notEmpty.await();
            }
            
            list.remove(index);
        } finally {
            lock.unlock();
        }
    }

    // Get an element from the list
    public T get(int index) throws InterruptedException {
        lock.lock();
        T element;
        try {
            element = list.get(index);
        } finally {
            lock.unlock();
        }
        return element;
    }

    // Get element position in list
    public int position(T elementToFind) throws InterruptedException {
        lock.lock();
        int counter = 1;
        try{
            for (T element : list) {
                if(elementToFind.equals(element)) return counter;
                counter++;
            }
            return -1; // If the player was not found returns -1
        } finally {
            lock.unlock();
        }
    }

    // Check wether element is in list
    public boolean contains(T elementToFind) throws InterruptedException {
        lock.lock();
        try{    
            for (T element : list) {
                if(elementToFind.equals(element)) return true;
            }
            return false; 
        } finally {
            lock.unlock();
        }
    }

    // Get list size
    public int size() throws InterruptedException {
        lock.lock();
        try{
            return list.size();
        } finally {
            lock.unlock();
        }
    }

    // Replace socket of player in list
    public boolean replace(Player targetPlayer, Socket newSocket) throws InterruptedException {
        lock.lock();
        try {
            for (T element : list) {
                if (element instanceof Player) {
                    Player player = (Player) element;
                    if (player.equals(targetPlayer)) {
                        player.setSocket(newSocket);
                        return true; // Player found and socket updated
                    }
                }
            }

            return false; // Player not found
        } finally {
            lock.unlock();
        }
    }

    // Replace socket of player in list
    public void set(int index, T new_element) throws InterruptedException {
        lock.lock();
        try {
            list.set(index, new_element);
        } finally {
            lock.unlock();
        }
    }

    // Print List
    @Override
    public String toString() {
        String returnString = "";

        for (T element : list) {
            returnString += element.toString() + "\n";
        }

        return returnString;
    }

    public Player getPlayerByToken(String token) {

        lock.lock();
        try {
            for (T element : list) {
                if (element instanceof Player) {
                    Player player = (Player) element;
                    if (player.getPlayerToken().getToken().equals(token)) {
                        return player;
                    }
                }
            }
    
            return null;
        } finally {
            lock.unlock();
        }
    }
}