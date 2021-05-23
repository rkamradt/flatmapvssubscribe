package net.kamradtfamily;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Hello world!
 *
 */
public class App 
{
    final static Random r = new Random();
    public static class Executor implements Comparable {
        private int priority;
        private int todo;
        private final String name;
        public Executor(String name) {
            this.name = name;
            this.priority = 0;
            this.todo = r.nextInt(100);
        }
        public boolean hasMoreWork() {
            return todo > 0;
        }
        public void block() {
            System.out.println("priority was " + priority);
            priority = 0;
            IntStream.range(0, r.nextInt(5))
                    .forEach(i -> doWork());
        }
        public void doWork() {
            System.out.println("doing work for " + this);
            todo--;
            priority++;
            try {
                Thread.sleep(100); // hard at work!
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        @Override
        public int compareTo(Object o) {
            return priority-((Executor)o).priority;
        }
        public String toString() {
            return "Executor " + name;
        }
    }
    public static void mainx( String[] args )
    {
        final PriorityQueue<Executor> pq = IntStream.rangeClosed(0, r.nextInt(10))
                .mapToObj(i -> new Executor(Integer.toString(i)))
                .collect(PriorityQueue::new, PriorityQueue::offer, AbstractQueue::addAll);

        Stream.generate(() -> Optional.ofNullable(pq.poll()))
                .sequential()
                .takeWhile(Optional::isPresent)
                .map(Optional::get)
                .forEach(s -> {
                    System.out.println("blocking on " + s);
                    s.block();
                    if(s.hasMoreWork()) {
                        pq.offer(s);
                    }
                });

    }
    static class MessageQueue {
        Flux<String> read(String name) {
            return Flux.just("test1", "test2");
        }
        Mono<String> write(String name, String outgoing) {
            return Mono.just("written " + outgoing);
        }
    }
    static class WebClient {
        Mono<String> post(String endpoint, String data) {
            return Mono.just("posted " + data);
        }
    }
    static class Database {
        String queryBuilder(String data) {
            return "query " + data;
        }
        Mono<String> lookup(String query) {
            return Mono.just("database result from " + query);
        }
    }
    public static void mains(String [] args) { // using subscribe exclusively
        MessageQueue messageQueue = new MessageQueue();
        WebClient webClient = new WebClient();
        Database database = new Database();
        messageQueue.read("incoming")
            .subscribe(mdata -> {
                webClient.post("/endpoint", mdata)
                    .subscribe(wdata -> {
                    String query = database.queryBuilder(wdata);
                    database.lookup(query)
                        .subscribe(ddata -> {
                            messageQueue.write("outgoing", ddata)
                                .subscribe(odata -> {
                                    System.out.println("message written");
                            }, oerr -> System.out.println("error writing message"));
                    }, derr -> System.out.println("error querying message"));
                }, werr -> System.out.println("error posting message"));
        }, merr -> System.out.println("error reading message"));
    }
    public static void main(String [] args) { // using flatMap to hide the subscribes
        MessageQueue messageQueue = new MessageQueue();
        WebClient webClient = new WebClient();
        Database database = new Database();
        messageQueue.read("incoming")
                .flatMap(mdata -> webClient.post("/endpoint", mdata))
                .map(wdata -> database.queryBuilder(wdata))
                .flatMap(query -> database.lookup(query))
                .flatMap(ddata -> messageQueue.write("outbound", ddata))
                .subscribe(result -> System.out.println("message written " + result),
                        err -> System.out.println("error " + err));
    }

}
