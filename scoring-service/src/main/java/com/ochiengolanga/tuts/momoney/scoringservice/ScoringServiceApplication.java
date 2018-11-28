package com.ochiengolanga.tuts.momoney.scoringservice;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Random;

@EnableDiscoveryClient
@SpringBootApplication
public class ScoringServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScoringServiceApplication.class, args);
    }
}

@RestController
@RequestMapping("/scores")
class ScoresRestController {
    private final ScoresService scoresService;

    ScoresRestController(ScoresService scoresService) {
        this.scoresService = scoresService;
    }

    @GetMapping
    public Score getMemberScore(@RequestParam("memberId") Long memberId) {
        return scoresService.getMemberScore(memberId);
    }
}

@Data
@NoArgsConstructor
class Score {
    private int score;

    public Score(int score) {
        this.score = score;
    }
}

@Service
class ScoresService {
    /**
     * Dummy scoring method
     *
     * @param memberId
     * @return
     */
    public Score getMemberScore(Long memberId) {
        return new Score(new Random().nextInt(10));
    }
}