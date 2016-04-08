package ch.netzwerg.feign;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import feign.Param;
import feign.RequestLine;
import feign.hystrix.HystrixFeign;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import rx.Observable;
import rx.observers.TestSubscriber;

import java.util.List;

public class GitHubClientDemo {

    private interface GitHub {
        @RequestLine("GET /repos/{owner}/{repo}/contributors")
        Observable<List<Contributor>> contributors(@Param("owner") String owner, @Param("repo") String repo);
    }

    private static final class Contributor {

        private final String login;
        private final int contributions;

        @JsonCreator
        public Contributor(@JsonProperty("login") String login, @JsonProperty("contributions") int contributions) {
            this.login = login;
            this.contributions = contributions;
        }

        String getLogin() {
            return login;
        }

        int getContributions() {
            return contributions;
        }

    }

    public static void main(String... args) throws InterruptedException {

        GitHub gitHub = HystrixFeign.builder()
                .encoder(new JacksonEncoder())
                .decoder(new JacksonDecoder())
                .target(GitHub.class, "https://api.github.com");

        Observable<Contributor> contributors = gitHub.contributors("netflix", "feign").
                flatMap(Observable::from); // convert Observable<List<Contributor> to Observable<Contributor>

        TestSubscriber<Contributor> subscriber = new TestSubscriber<>();
        contributors.subscribe(subscriber);
        subscriber.awaitTerminalEvent();
        subscriber.getOnNextEvents().forEach(
                contributor -> System.out.println(contributor.getLogin() + " (" + contributor.getContributions() + ")")
        );

    }

}
