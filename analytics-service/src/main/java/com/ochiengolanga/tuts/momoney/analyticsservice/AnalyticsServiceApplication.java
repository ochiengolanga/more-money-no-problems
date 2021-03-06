package com.ochiengolanga.tuts.momoney.analyticsservice;

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

@EnableBinding(AnalyticsStreams.class)
@EnableDiscoveryClient
@SpringBootApplication
public class AnalyticsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AnalyticsServiceApplication.class, args);
    }
}

@Slf4j
@Component
class AnalyticsStreamsListener {
    @StreamListener(AnalyticsStreams.LOANAPPLICATION_APPROVED)
    public void handleLoanApplicationApprovedEvent(LoanApplicationApprovedEvent event) {
        log.info("Received loan application approved event {}", event);
    }

    @StreamListener(AnalyticsStreams.LOANAPPLICATION_CREATED)
    public void handleLoanApplicationCreatedEvent(LoanApplicationCreatedEvent event) {
        log.info("Received loan application created event {}", event);
    }

    @StreamListener(AnalyticsStreams.LOANAPPLICATION_DECLINED)
    public void handleLoanApplicationDeclinedEvent(LoanApplicationDeclinedEvent event) {
        log.info("Received loan application approved event {}", event);
    }

    @StreamListener(AnalyticsStreams.MEMBER_CREATED)
    public void handleMemberCreatedEvent(MemberCreatedEvent memberCreatedEvent) {
        log.info("Received member created event {}", memberCreatedEvent);
    }
}

interface AnalyticsStreams {
    String LOANAPPLICATION_APPROVED = "loanapplication-approved";
    String LOANAPPLICATION_CREATED  = "loanapplication-created";
    String LOANAPPLICATION_DECLINED = "loanapplication-declined";
    String MEMBER_CREATED           = "member-created";

    @Input(LOANAPPLICATION_APPROVED)
    SubscribableChannel loanApplicationApprovedChannel();

    @Input(LOANAPPLICATION_CREATED)
    SubscribableChannel loanApplicationCreatedChannel();

    @Input(LOANAPPLICATION_DECLINED)
    SubscribableChannel loanApplicationDeclinedChannel();

    @Input(MEMBER_CREATED)
    SubscribableChannel memberCreatedChannel();
}

/**
 * Represents a loan application event notification and state
 *
 */
@Data
abstract class LoanApplicationEvent {
    private String id;
    private BigDecimal amount;
}

@Data
class LoanApplicationCreatedEvent extends LoanApplicationEvent {
    private LocalDateTime createdDate;
}

@Data
class LoanApplicationApprovedEvent extends LoanApplicationEvent {
    private LocalDateTime approvedDate;
}

@Data
class LoanApplicationDeclinedEvent extends LoanApplicationEvent {
    private LocalDateTime declinedDate;
}

/**Date
 * Represents a member created event notification and state
 *
 */
@Data
class MemberCreatedEvent {
    private Long id;
    private String name;
}