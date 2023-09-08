package game_server_proj;

import java.net.Socket;
import java.util.LinkedList;
import java.util.List;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class BlockingQueue<T> {
    private Queue<T> queue = new LinkedList<T>();
    private Lock lock = new ReentrantLock();
    private Condition notEmpty = lock.newCondition();

    public BlockingQueue() {
    }

    // Add an element to the queue
    public void add(T element) throws InterruptedException {
        lock.lock();
        try {
            queue.add(element);
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    // Remove element from queue
    public T remove() throws InterruptedException {
        lock.lock();
        try {
            while (queue.isEmpty()) {
                notEmpty.await();
            }
            T item = queue.remove();
            return item;
        } finally {
            lock.unlock();
        }
    }

    // Get element position in queue
    public int position(T elementToFind) throws InterruptedException {
        lock.lock();

        int counter = 1;

        try{
            for (T element : queue) {
                if(elementToFind.equals(element)) return counter;
                counter++;
            }

            return -1; // If the player was not found returns -1
        } finally {
            lock.unlock();
        }
    }

    // Check wether element is in queue
    public boolean contains(T elementToFind) throws InterruptedException {
        lock.lock();

        try{    
            for (T element : queue) {
                if(elementToFind.equals(element)) return true;
            }

            return false; 
        } finally {
            lock.unlock();
        }
    }

    // Get queue size
    public int size() throws InterruptedException {
        lock.lock();

        try{
            return queue.size();
        } finally {
            lock.unlock();
        }
    }

    // Remove queue element
    public List<T> removeElements(int numberOfElements) throws InterruptedException {
        lock.lock();

        try{
            List<T> elements = new LinkedList<>();

            for(int i=0; i<numberOfElements; i++){
                T element = this.remove();
                elements.add(element);
            }

            return elements;

        } finally {
            lock.unlock();
        }
    }

    // Replace element in queue
    public boolean replace(Player targetPlayer, Socket newSocket) throws InterruptedException {
        lock.lock();

        try {

            for (T element : queue) {
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

        // Replace element in queue
    public Iterator<T> iterator() throws InterruptedException {
        lock.lock();

        try {
            return new Iterator<T>() {
                @Override
                public boolean hasNext() {
                    return !queue.isEmpty();
                }
                @Override
                public T next() {
                    if (queue.isEmpty()) {
                        throw new java.util.NoSuchElementException();
                    }
                    return queue.poll();
                }
            };
        } finally {
            lock.unlock();
        }
    }


    // Print queue
    @Override
    public String toString() {
        String returnString = "";

        for (T element : queue) {
            returnString += element.toString() + "\n";
        }

        return returnString;
    }

    public Player getPlayerByToken(String token) {

        lock.lock();
        try {
            for (T element : queue) {
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
