package com.ochiengolanga.tuts.momoney.membersservice;

import lombok.Data;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import javax.persistence.*;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

@EnableBinding(MembersStreams.class)
@EnableDiscoveryClient
@SpringBootApplication
public class MembersServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(MembersServiceApplication.class, args);
    }
}

@RestController
@RequestMapping("/members")
class MembersRestController {
    private final MembersService membersService;

    MembersRestController(MembersService membersService) {
        this.membersService = membersService;
    }

    @PostMapping
    public Member createMember(@RequestBody @Valid Member newMemberPayload) {
        return membersService.createNewMember(newMemberPayload);
    }

    @GetMapping
    public List<Member> getMembers() {
        return membersService.getMembers();
    }

    @GetMapping("/{memberId}")
    public Member getMember(@PathVariable("memberId") Long memberId) {
        return membersService.getMember(memberId);
    }
}

@Data
@Entity
@Table(name = "members")
class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Version
    private Long version;

    @NotNull
    private String name;

    @NotNull
    private String phoneNumber;
}

interface MemberRepository extends JpaRepository<Member, Long> {}

@Service
class MembersService {
    private final MemberRepository memberRepository;
    private final MembersStreams membersStreams;

    MembersService(MemberRepository memberRepository, MembersStreams membersStreams) {
        this.memberRepository = memberRepository;
        this.membersStreams = membersStreams;
    }

    public Member createNewMember(Member newMemberPayload) {
        Member savedMember = memberRepository.save(newMemberPayload);

        this.memberCreatedEventHandler(savedMember);

        return savedMember;
    }

    private void memberCreatedEventHandler(Member member) {
        MessageChannel messageChannel = membersStreams.memberCreatedChannel();
        messageChannel.send(MessageBuilder.withPayload(member).build());
    }

    public List<Member> getMembers() {
        return memberRepository.findAll();
    }

    public Member getMember(Long memberId) {
        return memberRepository.findById(memberId).orElse(null);
    }
}

interface MembersStreams {
    String MEMBER_CREATED = "member-created";

    @Output(MEMBER_CREATED)
    MessageChannel memberCreatedChannel();
}