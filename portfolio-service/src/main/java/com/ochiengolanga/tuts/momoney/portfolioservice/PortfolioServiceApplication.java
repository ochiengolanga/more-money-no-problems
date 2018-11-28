package com.ochiengolanga.tuts.momoney.portfolioservice;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@EnableBinding(PortfolioStreams.class)
@EnableDiscoveryClient
@SpringBootApplication
public class PortfolioServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PortfolioServiceApplication.class, args);
    }
}

@Slf4j
@Component
class PortfolioStreamsListener {
    @StreamListener(PortfolioStreams.LOANAPPLICATION_APPROVED)
    public void handleLoanApplicationApprovedEvent(LoanApplicationApprovedEvent event) {
        log.info("Received loan application approved event {}", event);
    }

    @StreamListener(PortfolioStreams.MEMBER_CREATED)
    public void handleMemberCreatedEvent(MemberCreatedEvent memberCreatedEvent) {
        log.info("Received member created event {}", memberCreatedEvent);
        // create member account with opening balances
    }
}

interface PortfolioStreams {
    String LOANAPPLICATION_APPROVED = "loanapplication-approved";
    String MEMBER_CREATED           = "member-created";

    @Input(LOANAPPLICATION_APPROVED)
    SubscribableChannel loanApplicationApprovedChannel();

    @Input(MEMBER_CREATED)
    SubscribableChannel memberCreatedChannel();
}

/**
 * Represents a loan application event notification and state
 *
 */
@Data
class LoanApplicationApprovedEvent {
    private String id;
    private BigDecimal amount;
    private LocalDateTime scoreDate;
}

/**
 * Represents a member created event notification and state
 *
 */
@Data
class MemberCreatedEvent {
    private Long id;
    private String name;
}