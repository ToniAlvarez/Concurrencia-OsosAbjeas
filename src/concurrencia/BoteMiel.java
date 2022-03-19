package concurrencia;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class BoteMiel implements Runnable {
    private static final int PHILOSOPHERS = 5;
    private static final int EAT_COUNT = 100;
    private BoteMonitor monitor;
    private int id;

    private BoteMiel(int id, BoteMonitor b) {
        this.id = id;
        this.monitor = b;
    }

    private void think() {
        try {
            Thread.sleep(50);
        } catch (InterruptedException ignored) {
        }
    }

    private void eat() {
        System.out.printf("%d start eat\n", id);
        try {
            Thread.sleep(100);
        } catch (InterruptedException ignored) {
        }
        System.out.printf("%d end eat\n", id);

    }

    @Override
    public void run() {
        for (int i = 0; i < EAT_COUNT; i++) {
            think();
            monitor.pick(id);
            eat();
            monitor.release(id);
        }
    }

    public static void main(String[] args) throws InterruptedException {
        Thread[] threads = new Thread[PHILOSOPHERS];
        BoteMonitor p = new BoteMonitor(PHILOSOPHERS);
        int i;
        for (i = 0; i < PHILOSOPHERS; i++) {
            threads[i] = new Thread(new BoteMiel(i, p));
            threads[i].start();
        }
        for (i = 0; i < PHILOSOPHERS; i++) {
            threads[i].join();
        }
    }
}

class BoteMonitor {
    private final Lock lock = new ReentrantLock();
    private Integer n;
    private Integer[] forks;
    private ArrayList<Condition> canEat = new ArrayList<>();

    public BoteMonitor(int n) {
        this.n = n;
        forks = new Integer[n];
        Arrays.fill(forks, 2);
        for (int i = 0; i < n; i++) {
            canEat.add(lock.newCondition());
        }
    }

    private int left(int i) {
        return (i + n - 1) % n;
    }

    private int right(int i) {
        return (i + 1) % n;
    }

    void pick(int i) {
        lock.lock();
        try {
            while (forks[i] != 2) {
                canEat.get(i).await();
            }
            forks[left(i)]--;
            forks[right(i)]--;
        } catch (InterruptedException ignored) {
        } finally {
            lock.unlock();
        }
    }

    void release(int i) {
        lock.lock();
        try {
            forks[left(i)]++;
            forks[right(i)]++;
            if (forks[left(i)] == 2) {
                canEat.get(left(i)).signal();
            }
            if (forks[right(i)] == 2) {
                canEat.get(right(i)).signal();
            }
        } finally {
            lock.unlock();
        }
    }
}