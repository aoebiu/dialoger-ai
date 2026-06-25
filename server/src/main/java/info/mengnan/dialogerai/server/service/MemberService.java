package info.mengnan.dialogerai.server.service;

import info.mengnan.dialogerai.repository.entity.ChatMember;
import info.mengnan.dialogerai.repository.entity.ChatMemberRelation;
import info.mengnan.dialogerai.repository.enums.MemberRole;
import info.mengnan.dialogerai.repository.enums.MemberStatus;
import info.mengnan.dialogerai.repository.repo.MemberRelationRepository;
import info.mengnan.dialogerai.repository.repo.MemberRepository;
import info.mengnan.dialogerai.server.exception.BusinessException;
import info.mengnan.dialogerai.server.param.ErrorCode;
import info.mengnan.dialogerai.server.param.auth.MemberResponse;
import info.mengnan.dialogerai.server.param.auth.MemberUpdateRequest;
import info.mengnan.dialogerai.server.param.auth.RegisterRequest;
import info.mengnan.dialogerai.server.param.team.CreateTeamMemberRequest;
import info.mengnan.dialogerai.server.param.team.TeamMemberResponse;
import info.mengnan.dialogerai.server.param.team.TeamOverviewResponse;
import info.mengnan.dialogerai.server.param.team.UpdateTeamMemberRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final MemberRelationRepository memberRelationRepository;

    public void register(RegisterRequest request) {
        if (memberRepository.find(request.getUsername()) != null)
            throw new BusinessException(ErrorCode.MEMBER_USERNAME_EXISTS);

        String phone = request.getPhone();
        if (StringUtils.hasText(phone) && memberRepository.findByPhone(phone) != null)
            throw new BusinessException(ErrorCode.MEMBER_PHONE_EXISTS);

        ChatMember member = new ChatMember();
        member.setUsername(request.getUsername());
        member.setPassword(encryptPassword(request.getPassword()));
        member.setNickname(request.getNickname());
        member.setPhone(request.getPhone());
        member.setStatus(MemberStatus.ENABLED);
        member.setRole(MemberRole.OWNER);
        memberRepository.insert(member);
        log.info("owner registered: memberId={}, username={}", member.getId(), request.getUsername());
    }


    public MemberResponse authenticate(String username, String password) {
        ChatMember member = memberRepository.find(username);
        if (member == null || !encryptPassword(password).equals(member.getPassword()))
            throw new BusinessException(ErrorCode.MEMBER_AUTH_FAILED);

        if (member.getStatus() != MemberStatus.ENABLED)
            throw new BusinessException(ErrorCode.MEMBER_DISABLED);

        return toMemberResponse(member);
    }

    public MemberResponse getMemberInfo(Long memberId) {
        ChatMember member = memberRepository.find(memberId);
        if (member == null)
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        return toMemberResponse(member);
    }

    public void updateMemberInfo(MemberUpdateRequest request) {
        ChatMember member = memberRepository.find(request.getMemberId());
        if (member == null)
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);

        String phone = request.getPhone();
        if (StringUtils.hasText(phone)) {
            ChatMember existing = memberRepository.findByPhone(phone);
            if (existing != null && !existing.getId().equals(member.getId()))
                throw new BusinessException(ErrorCode.MEMBER_PHONE_EXISTS);
        }

        ChatMember updateMember = new ChatMember();
        updateMember.setId(member.getId());
        updateMember.setNickname(request.getNickname());
        updateMember.setPhone(request.getPhone());
        updateMember.setAvatar(request.getAvatar());

        if (StringUtils.hasText(request.getPassword())) {
            if (!StringUtils.hasText(request.getOldPassword())
                    || !encryptPassword(request.getOldPassword()).equals(member.getPassword()))
                throw new BusinessException(ErrorCode.MEMBER_OLD_PASSWORD_WRONG);
            updateMember.setPassword(encryptPassword(request.getPassword()));
        }

        memberRepository.update(updateMember);
    }

    public List<MemberResponse> getAllMembers(Long memberId) {
        return memberRepository.listAll().stream()
                .map(this::toMemberResponse)
                .toList();
    }

    @Transactional
    public void deleteMember(Long ownerId, Long memberId) {
        requireOwner(ownerId, memberId);
        memberRepository.delete(memberId);
        memberRelationRepository.delete(memberId);
        log.info("team member removed: ownerId={}, memberId={}", ownerId, memberId);
    }

    /** 解析资源归属 owner，模型与 Token 用量统一走此 ID */
    public Long resolveResourceOwnerId(Long memberId) {
        ChatMemberRelation relation = memberRelationRepository.find(memberId);
        return relation != null ? relation.getOwnerId() : memberId;
    }

    @Transactional
    public TeamMemberResponse createMember(Long ownerId, CreateTeamMemberRequest request) {
        requireOwner(ownerId, null);

        if (memberRepository.find(request.getUsername()) != null)
            throw new BusinessException(ErrorCode.MEMBER_USERNAME_EXISTS);

        if (StringUtils.hasText(request.getPhone()) && memberRepository.findByPhone(request.getPhone()) != null)
            throw new BusinessException(ErrorCode.MEMBER_PHONE_EXISTS);

        ChatMember member = buildTeamMember(request);
        memberRepository.insert(member);

        ChatMemberRelation relation = new ChatMemberRelation();
        relation.setOwnerId(ownerId);
        relation.setMemberId(member.getId());
        relation.setStatus(MemberStatus.ENABLED);
        memberRelationRepository.insert(relation);

        log.info("team member created: ownerId={}, memberId={}, username={}",
                ownerId, member.getId(), member.getUsername());
        return toTeamMemberResponse(member);
    }

    public TeamOverviewResponse getOverview(Long currentUserId) {
        ChatMember currentUser = memberRepository.find(currentUserId);
        if (currentUser == null)
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);

        Long ownerId = resolveResourceOwnerId(currentUserId);
        ChatMember owner = memberRepository.find(ownerId);
        if (owner == null)
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);

        TeamOverviewResponse overview = new TeamOverviewResponse();
        overview.setOwner(toTeamMemberResponse(owner));
        overview.setMembers(listTeamMembers(ownerId));
        overview.setCurrentUserId(currentUserId);
        return overview;
    }

    public List<TeamMemberResponse> listMembers(Long ownerId) {
        requireOwner(ownerId, null);
        return listTeamMembers(ownerId);
    }


    public void disableMember(Long ownerId, Long memberId) {
        requireOwner(ownerId, memberId);
        ChatMember member = memberRepository.find(memberId);
        if (member == null)
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        member.setStatus(MemberStatus.DISABLED);
        memberRepository.update(member);
    }

    public void updateTeamMember(Long ownerId, Long memberId, UpdateTeamMemberRequest request) {
        requireOwner(ownerId, memberId);

        String phone = request.getPhone();
        if (StringUtils.hasText(phone)) {
            ChatMember existing = memberRepository.findByPhone(phone);
            if (existing != null && !existing.getId().equals(memberId))
                throw new BusinessException(ErrorCode.MEMBER_PHONE_EXISTS);
        }

        ChatMember updateMember = new ChatMember();
        updateMember.setId(memberId);
        updateMember.setNickname(request.getNickname());
        updateMember.setPhone(request.getPhone());
        if (request.getStatus() != null)
            updateMember.setStatus(request.getStatus());
        if (StringUtils.hasText(request.getPassword()))
            updateMember.setPassword(encryptPassword(request.getPassword()));
        memberRepository.update(updateMember);

        if (request.getStatus() != null) {
            ChatMemberRelation relation = memberRelationRepository.find(memberId);
            if (relation != null) {
                relation.setStatus(request.getStatus());
                memberRelationRepository.update(relation);
            }
        }
        log.info("team member updated: ownerId={}, memberId={}", ownerId, memberId);
    }

    private List<TeamMemberResponse> listTeamMembers(Long ownerId) {
        return memberRepository.list(memberRelationRepository.listMemberIds(ownerId)).stream()
                .map(this::toTeamMemberResponse)
                .toList();
    }

    private void requireOwner(Long ownerId, Long memberId) {
        ChatMember owner = memberRepository.find(ownerId);
        if (owner == null || owner.getRole() != MemberRole.OWNER)
            throw new BusinessException(ErrorCode.MEMBER_OWNER_REQUIRED);
        if (memberId == null)
            return;
        ChatMemberRelation relation = memberRelationRepository.find(memberId);
        if (relation == null || !relation.getOwnerId().equals(ownerId))
            throw new BusinessException(ErrorCode.MEMBER_MANAGE_DENIED);
        ChatMember member = memberRepository.find(memberId);
        if (member == null)
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
    }

    private ChatMember buildTeamMember(CreateTeamMemberRequest request) {
        ChatMember member = new ChatMember();
        member.setUsername(request.getUsername());
        member.setPassword(encryptPassword(request.getPassword()));
        member.setNickname(request.getNickname());
        member.setPhone(request.getPhone());
        member.setStatus(MemberStatus.ENABLED);
        member.setRole(MemberRole.MEMBER);
        return member;
    }

    private MemberResponse toMemberResponse(ChatMember member) {
        ChatMemberRelation relation = memberRelationRepository.find(member.getId());
        MemberResponse response = new MemberResponse();
        response.setId(member.getId());
        response.setUsername(member.getUsername());
        response.setNickname(member.getNickname());
        response.setPhone(member.getPhone());
        response.setAvatar(member.getAvatar());
        response.setStatus(member.getStatus());
        response.setRole(member.getRole() != null ? member.getRole() : MemberRole.OWNER);
        response.setOwnerId(relation != null ? relation.getOwnerId() : null);
        return response;
    }

    private TeamMemberResponse toTeamMemberResponse(ChatMember member) {
        TeamMemberResponse response = new TeamMemberResponse();
        response.setId(member.getId());
        response.setUsername(member.getUsername());
        response.setNickname(member.getNickname());
        response.setPhone(member.getPhone());
        response.setStatus(member.getStatus());
        response.setRole(member.getRole());
        response.setCreatedAt(member.getCreatedAt());
        return response;
    }

    private String encryptPassword(String password) {
        return DigestUtils.md5DigestAsHex(password.getBytes(StandardCharsets.UTF_8));
    }


}
