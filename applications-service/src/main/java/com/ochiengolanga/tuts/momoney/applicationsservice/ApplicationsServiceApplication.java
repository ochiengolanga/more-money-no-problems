package com.ochiengolanga.tuts.momoney.applicationsservice;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.hystrix.EnableHystrix;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@EnableBinding(ApplicationsStreams.class)
@EnableDiscoveryClient
@EnableFeignClients
@EnableHystrix
@SpringBootApplication
public class ApplicationsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApplicationsServiceApplication.class, args);
    }
}

@RestController
@RequestMapping("/applications")
class LoanApplicationsController {

    private final LoanApplicationService loanApplicationService;

    LoanApplicationsController(LoanApplicationService loanApplicationService) {
        this.loanApplicationService = loanApplicationService;
    }

    @PostMapping
    public LoanApplication createLoanApplication(@RequestBody LoanApplication loanApplication) {
        return loanApplicationService.createApplication(loanApplication);
    }

    @GetMapping
    public List<LoanApplication> getLoanApplications() {
        return loanApplicationService.getApplications();
    }
}

@Data
@Document(collection = "loan_applications")
class LoanApplication {
    @Id
    private String id;

    @Version
    private Long version;
    private Long memberId;
    private BigDecimal amount;
    private LoanApplicationStatus status = LoanApplicationStatus.NEW;
    private int score;
    private LocalDateTime scoreDate;

    public void setScore(int score) {
        this.score = score;
        this.scoreDate = LocalDateTime.now();
    }

    enum LoanApplicationStatus {
        NEW, APPROVED, DECLINED
    }
}

@Data
class ScoreDTO {
    private int score;
}

interface LoanApplicationRepository extends MongoRepository<LoanApplication, String> {}

@Service
class LoanApplicationService {
    private final ApplicationsStreams applicationsStreams;
    private final LoanApplicationRepository loanApplicationRepository;
    private final ScoringService scoringService;

    LoanApplicationService(LoanApplicationRepository loanApplicationRepository,
                           ApplicationsStreams applicationsStreams,
                           ScoringService scoringService) {
        this.loanApplicationRepository = loanApplicationRepository;
        this.applicationsStreams = applicationsStreams;
        this.scoringService = scoringService;
    }

    public LoanApplication createApplication(LoanApplication loanApplication) {
        LoanApplication savedApplication = loanApplicationRepository.save(loanApplication);
        this.loanApplicationEventHandler(savedApplication, applicationsStreams.loanApplicationCreatedOutboundChannel());

        return savedApplication;
    }

    public List<LoanApplication> getApplications() {
        return loanApplicationRepository.findAll();
    }

    public void scoreApplication(LoanApplication loanApplication) {
        loanApplicationRepository.findById(loanApplication.getId())
            .ifPresent(foundApplication -> {
                scoringService.getMemberScore(foundApplication.getMemberId())
                    .ifPresent(scoreDTO -> {
                        loanApplication.setScore(scoreDTO.getScore());

                        if(loanApplication.getScore() <= 8) {
                            loanApplication.setStatus(LoanApplication.LoanApplicationStatus.DECLINED);
                        } else {
                            loanApplication.setStatus(LoanApplication.LoanApplicationStatus.APPROVED);
                        }

                        this.loanApplicationProcessedHandler(loanApplicationRepository.save(loanApplication));
                    });

            });
    }

    private void loanApplicationProcessedHandler(LoanApplication loanApplication) {
        if(loanApplication.getStatus().equals(LoanApplication.LoanApplicationStatus.APPROVED)) {
            this.loanApplicationEventHandler(loanApplication, applicationsStreams.loanApplicationApprovedOutboundChannel());
        } else if(loanApplication.getStatus().equals(LoanApplication.LoanApplicationStatus.DECLINED)) {
            this.loanApplicationEventHandler(loanApplication, applicationsStreams.loanApplicationDeclinedOutboundChannel());
        }
    }

    private void loanApplicationEventHandler(LoanApplication loanApplication, MessageChannel messageChannel) {
        messageChannel.send(MessageBuilder.withPayload(loanApplication).build());
    }
}

@Slf4j
@Component
class ApplicationsStreamEventHandler {
    private final LoanApplicationService loanApplicationService;

    ApplicationsStreamEventHandler(LoanApplicationService loanApplicationService) {
        this.loanApplicationService = loanApplicationService;
    }

    @StreamListener(ApplicationsStreams.LOANAPPLICATION_CREATED_INBOUND)
    public void handleLoanApplicationCreated(LoanApplication loanApplication) {
        log.info("Received new loan application {}", loanApplication);

        loanApplicationService.scoreApplication(loanApplication);
    }
}

interface ApplicationsStreams {
    String LOANAPPLICATION_CREATED_INBOUND      = "loanapplication-created-in";
    String LOANAPPLICATION_CREATED_OUTBOUND     = "loanapplication-created-out";
    String LOANAPPLICATION_APPROVED_OUTBOUND    = "loanapplication-approved";
    String LOANAPPLICATION_DECLINED_OUTBOUND    = "loanapplication-declined";

    @Input(LOANAPPLICATION_CREATED_INBOUND)
    SubscribableChannel loanApplicationCreatedInboundChannel();

    @Output(LOANAPPLICATION_CREATED_OUTBOUND)
    MessageChannel loanApplicationCreatedOutboundChannel();

    @Output(LOANAPPLICATION_APPROVED_OUTBOUND)
    MessageChannel loanApplicationApprovedOutboundChannel();

    @Output(LOANAPPLICATION_DECLINED_OUTBOUND)
    MessageChannel loanApplicationDeclinedOutboundChannel();
}

@Component
class ScoringService {
    private final ScoringServiceFeignClient feignClient;

    ScoringService(ScoringServiceFeignClient feignClient) {
        this.feignClient = feignClient;
    }

    @HystrixCommand(fallbackMethod = "getMemberScoreFallback")
    public Optional<ScoreDTO> getMemberScore(Long memberId) {
        return Optional.of(feignClient.getMemberScore(memberId));
    }

    public Optional<ScoreDTO> getMemberScoreFallback(Long memberId) {
        return Optional.empty();
    }
}

@FeignClient(name = "scoring-service")
interface ScoringServiceFeignClient {
    @RequestMapping(method = RequestMethod.GET, path = "/scores")
    ScoreDTO getMemberScore(@RequestParam("memberId") Long memberId);
}