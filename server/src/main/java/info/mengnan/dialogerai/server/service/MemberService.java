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
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final MemberRelationRepository memberRelationRepository;

    public ChatMember findById(Long id) {
        return memberRepository.findById(id);
    }

    public ChatMemberRelation findRelationByMemberId(Long memberId) {
        return memberRelationRepository.findByMemberId(memberId);
    }

    public boolean matchesPassword(String rawPassword, String encryptedPassword) {
        return encryptPassword(rawPassword).equals(encryptedPassword);
    }

    public void register(RegisterRequest request) {
        if (memberRepository.findByUsername(request.getUsername()) != null)
            throw new BusinessException(ErrorCode.MEMBER_USERNAME_EXISTS);

        String phone = request.getPhone();
        if (StringUtils.hasText(phone) && memberRepository.findByPhone(phone) != null)
            throw new BusinessException(ErrorCode.MEMBER_PHONE_EXISTS);

        ChatMember member = new ChatMember();
        member.setUsername(request.getUsername());
        member.setPassword(encryptPassword(request.getPassword()));
        member.setPhone(request.getPhone());
        member.setStatus(MemberStatus.ENABLED);
        member.setRole(MemberRole.OWNER);
        memberRepository.insert(member);
        log.info("owner registered: memberId={}, username={}", member.getId(), request.getUsername());
    }

    public MemberResponse authenticate(String username, String password) {
        ChatMember member = memberRepository.findByUsername(username);
        if (member == null || !encryptPassword(password).equals(member.getPassword()))
            throw new BusinessException(ErrorCode.MEMBER_AUTH_FAILED);

        if (member.getStatus() != MemberStatus.ENABLED)
            throw new BusinessException(ErrorCode.MEMBER_DISABLED);

        return toMemberResponse(member);
    }

    public MemberResponse toMemberResponse(ChatMember member) {
        ChatMemberRelation relation = memberRelationRepository.findByMemberId(member.getId());
        MemberResponse response = new MemberResponse();
        response.setId(member.getId());
        response.setUsername(member.getUsername());
        response.setPhone(member.getPhone());
        response.setAvatar(member.getAvatar());
        response.setStatus(member.getStatus());
        response.setRole(member.getRole() != null ? member.getRole() : MemberRole.OWNER);
        response.setOwnerId(relation != null ? relation.getOwnerId() : null);
        return response;
    }

    public void updateMemberInfo(Long memberId, MemberUpdateRequest request) {
        String phone = request.getPhone();
        if (StringUtils.hasText(phone)) {
            ChatMember existing = memberRepository.findByPhone(phone);
            if (existing != null && !existing.getId().equals(memberId))
                throw new BusinessException(ErrorCode.MEMBER_PHONE_EXISTS);
        }

        ChatMember updateMember = new ChatMember();
        updateMember.setId(memberId);
        updateMember.setPhone(request.getPhone());
        updateMember.setAvatar(request.getAvatar());
        if (StringUtils.hasText(request.getPassword()))
            updateMember.setPassword(encryptPassword(request.getPassword()));
        memberRepository.updateById(updateMember);
    }

    @Transactional
    public void deleteMember(Long memberId) {
        memberRepository.deleteById(memberId);
        memberRelationRepository.deleteByMemberId(memberId);
        log.info("team member removed: memberId={}", memberId);
    }

    /** 解析资源归属 owner，模型与 Token 用量统一走此 ID */
    public Long resolveResourceOwnerId(Long memberId) {
        ChatMemberRelation relation = memberRelationRepository.findByMemberId(memberId);
        return relation != null ? relation.getOwnerId() : memberId;
    }

    /** 返回当前用户所在团队的全部 memberId（含 owner 本身） */
    public List<Long> resolveTeamMemberIds(Long memberId) {
        Long ownerId = resolveResourceOwnerId(memberId);
        List<Long> ids = new ArrayList<>();
        ids.add(ownerId);
        ids.addAll(memberRelationRepository.listMemberIds(ownerId));
        return ids;
    }

    public boolean isOwner(Long memberId) {
        ChatMember member = memberRepository.findById(memberId);
        return member != null && member.getRole() == MemberRole.OWNER;
    }

    @Transactional
    public TeamMemberResponse createTeamMember(Long ownerId, CreateTeamMemberRequest request) {
        if (memberRepository.findByUsername(request.getUsername()) != null)
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

    public TeamOverviewResponse getOverview(Long memberId, ChatMember owner) {
        TeamOverviewResponse overview = new TeamOverviewResponse();
        overview.setOwner(toTeamMemberResponse(owner));
        overview.setMembers(listTeamMembers(owner.getId()));
        overview.setCurrentUserId(memberId);
        return overview;
    }

    public List<TeamMemberResponse> listTeamMembers(Long ownerId) {
        List<Long> memberIds = memberRelationRepository.listMemberIds(ownerId);
        if (memberIds.isEmpty())
            return List.of();
        return memberRepository.findByIds(memberIds).stream()
                .map(this::toTeamMemberResponse)
                .toList();
    }

    public void disableMember(Long memberId) {
        ChatMember updateMember = new ChatMember();
        updateMember.setId(memberId);
        updateMember.setStatus(MemberStatus.DISABLED);
        memberRepository.updateById(updateMember);
    }

    public void updateTeamMember(Long memberId, UpdateTeamMemberRequest request) {
        String phone = request.getPhone();
        if (StringUtils.hasText(phone)) {
            ChatMember existing = memberRepository.findByPhone(phone);
            if (existing != null && !existing.getId().equals(memberId))
                throw new BusinessException(ErrorCode.MEMBER_PHONE_EXISTS);
        }

        ChatMember updateMember = new ChatMember();
        updateMember.setId(memberId);
        updateMember.setPhone(request.getPhone());
        if (request.getStatus() != null)
            updateMember.setStatus(request.getStatus());
        if (StringUtils.hasText(request.getPassword()))
            updateMember.setPassword(encryptPassword(request.getPassword()));
        memberRepository.updateById(updateMember);

        if (request.getStatus() != null) {
            ChatMemberRelation relation = memberRelationRepository.findByMemberId(memberId);
            if (relation != null) {
                relation.setStatus(request.getStatus());
                memberRelationRepository.updateById(relation);
            }
        }
        log.info("team member updated: memberId={}", memberId);
    }

    private ChatMember buildTeamMember(CreateTeamMemberRequest request) {
        ChatMember member = new ChatMember();
        member.setUsername(request.getUsername());
        member.setPassword(encryptPassword(request.getPassword()));
        member.setPhone(request.getPhone());
        member.setStatus(MemberStatus.ENABLED);
        member.setRole(MemberRole.MEMBER);
        return member;
    }

    private TeamMemberResponse toTeamMemberResponse(ChatMember member) {
        TeamMemberResponse response = new TeamMemberResponse();
        response.setId(member.getId());
        response.setUsername(member.getUsername());
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
