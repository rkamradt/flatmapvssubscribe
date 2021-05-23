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
    public static void main(String [] args) { // using subscribe exclusively
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
                                    System.out.println("message " + odata);
                            }, oerr -> System.out.println("error writing message"));
                    }, derr -> System.out.println("error querying message"));
                }, werr -> System.out.println("error posting message"));
        }, merr -> System.out.println("error reading message"));
    }
    public static void mainX(String [] args) { // using flatMap to hide the subscribes
        MessageQueue messageQueue = new MessageQueue();
        WebClient webClient = new WebClient();
        Database database = new Database();
        messageQueue.read("incoming")
                .flatMap(mdata -> webClient.post("/endpoint", mdata))
                .map(wdata -> database.queryBuilder(wdata))
                .flatMap(query -> database.lookup(query))
                .flatMap(ddata -> messageQueue.write("outbound", ddata))
                .subscribe(result -> System.out.println("message " + result),
                        err -> System.out.println("error " + err));
    }

}
