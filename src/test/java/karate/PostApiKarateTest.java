package karate;

import com.intuit.karate.junit5.Karate;

class PostApiKarateTest {

    @Karate.Test
    Karate testAll() {
        // Pass baseUrl via system property 'karate.baseUrl' — Karate surfaces
        // all 'karate.*' system properties as JS variables in feature files.
        return Karate.run("classpath:karate/post-api.feature");
    }
}
