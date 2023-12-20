import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Main {
    // Ścieżka do pliku tekstowego
    private static final String FILE_PATH = "C:/Users/jakub/Desktop/przyklad.txt";
    // Flaga używana do sygnalizowania czy zatrzymać wątki
    private static volatile boolean stopThreads = false;

    public static void main(String[] args) {
        // Odczyt liczby wątków jakie mamy zadane
        int numThreads = Integer.parseInt(args[0]);

        // Utworzenie wątków
        Thread[] threads = new Thread[numThreads + 1];

        // Pętla uruchamia kolejno wątki i ich metody start
        for (int i = 0; i <= numThreads; i++) {
            threads[i] = new Thread(new FileReaderThread());
            threads[i].start();
        }

        // Czekamy na kliknięcie k do zatrzymania wątków
        System.out.println("Press 'k' to stop the threads...");
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNext()) {
            char input = scanner.next().charAt(0);  // Oczytujemy wpisany symbol przez użytkownika
            if (input == 'k') {
                // Ustawiamy flagę i czekamy na skończenie pracy wątków
                stopThreads = true;
                for (Thread thread : threads) {
                    try {
                        thread.join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                break;
            }
        }
        scanner.close();
    }

    static class FileReaderThread implements Runnable {
        private Set<String> lines = new HashSet<>();    // lines to linie każdego wątku które są już odczytane
        private BufferedReader reader;
        private Lock lock = new ReentrantLock();        // tworzymy locka aby zatrzymać działanie wątków gdy odczytujemy
        private Condition linesAvailable = lock.newCondition(); // tworzymy warunek w którym wątek będzie musiał poczekać

        @Override
        public void run() {
            try {
                reader = new BufferedReader(new FileReader(FILE_PATH));
                String line;
                while ((line = reader.readLine()) != null && !stopThreads) {
                    lock.lock();    // blokujemy możliwość czytania pliku innym wątkom
                    try {
                        // Czekamy aż dana linia nie była odczytana
                        while (lines.contains(line) && !stopThreads) {
                            linesAvailable.await();
                        }

                        // Dodajemy linię do przeczytanych lini i ją wypisujemy
                        lines.add(line);
                        System.out.println("Thread " + Thread.currentThread().getName() + ": " + line);

                        // Informujemy, że dana linia została odczytana
                        linesAvailable.signalAll();
                    } finally {
                        lock.unlock(); // Odblokowujemy zasoby
                    }
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}