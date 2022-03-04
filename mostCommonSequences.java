import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.List;
import java.util.LinkedList;
import java.util.Comparator;

class Task implements Runnable {
    public static final String VALID_WORD = "((\\w+'\\w+)|(\\w+-?\\w+)|(\\w+))";
    private String file;
    private boolean processArgs;

    public Task(String s, boolean b) {
        file = s;
        processArgs = b;
    }

    public void run() {
        if(processArgs) {
            processArgs(file);
        } else {
            processStdIn();
        }
    }

    private static void processStdIn() {
        InputStreamReader reader = new InputStreamReader(System.in);
        try (BufferedReader br = new BufferedReader(reader)) {
            readFile(br);
        } catch (IOException e) {
            System.out.println("File not found");
        }
    }

    private static void processArgs(String file) {
        try (BufferedReader br = Files.newBufferedReader(Paths.get(file))) {
            readFile(br);
        } catch (IOException e) {
            System.out.println("File at path " + file + " not found");
        }
    }

    private static void readFile(BufferedReader br) throws IOException {
        String line;
        List<String> words = new LinkedList<>();

        while ((line = br.readLine()) != null) {
            if (line.trim().length() == 0) {
                continue;
            }

            Pattern p = Pattern.compile(VALID_WORD);
            Matcher m = p.matcher(line);
            while (m.find()) {
                words.add(m.group().toLowerCase());
            }

            while (words.size() >= 3) {
                String phrase = words.get(0) + " " + words.get(1) + " " + words.get(2);
                if (mostCommonSequences.sequences.containsKey(phrase)) {
                    mostCommonSequences.sequences.put(phrase, mostCommonSequences.sequences.get(phrase) + 1);
                } else {
                    mostCommonSequences.sequences.put(phrase, 1);
                }
                words.remove(0);
            }
        }
    }
}
public class mostCommonSequences {
    public static ConcurrentHashMap<String, Integer> sequences = new ConcurrentHashMap<>();
    static final int thread_count = 3;
    private static ExecutorService pool = Executors.newFixedThreadPool(thread_count);

    public static void main(String[] args) {
        if (args == null || args.length == 0) {
            //take from and process stdin
            pool.execute(new Task(null, false));
        } else if (args != null && args.length > 0) {
            //take from args
            for (String filePath : args) {
                pool.execute(new Task(filePath, true));
            }
        } else {
            System.out.println("No File inputted, quitting program");
        }

        pool.shutdown();
        try {
            pool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            pool.shutdownNow();
        }

        //output logic
        if (sequences.size() > 0) {
            sequences.entrySet()
                    .stream()
                    .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                    .limit(100)
                    .forEach(e -> {
                        System.out.printf("Phrase: %s | Frequency: %d\n", e.getKey(), e.getValue());
                    });
        } else {
            System.out.println("No three word sequences detected");
        }
    }
}
