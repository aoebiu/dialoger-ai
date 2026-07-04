package info.mengnan.dialogerai.server.service;

import info.mengnan.dialogerai.repository.entity.ChatMember;
import info.mengnan.dialogerai.repository.entity.ChatMemberRelation;
import info.mengnan.dialogerai.repository.entity.ChatTeam;
import info.mengnan.dialogerai.repository.enums.MemberRole;
import info.mengnan.dialogerai.repository.enums.MemberStatus;
import info.mengnan.dialogerai.repository.repo.MemberRelationRepository;
import info.mengnan.dialogerai.repository.repo.MemberRepository;
import info.mengnan.dialogerai.repository.repo.TeamRepository;
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
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final MemberRelationRepository memberRelationRepository;
    private final TeamRepository teamRepository;

    public ChatMember findById(Long id) {
        return memberRepository.findById(id);
    }

    public ChatTeam findTeamById(Long teamId) {
        return teamRepository.findById(teamId);
    }

    public ChatTeam findTeamByOwnerId(Long ownerId) {
        return teamRepository.findByOwnerId(ownerId);
    }

    public ChatMemberRelation findRelationByMemberId(Long memberId) {
        return memberRelationRepository.findByMemberId(memberId);
    }

    public boolean matchesPassword(String rawPassword, String encryptedPassword) {
        return encryptPassword(rawPassword).equals(encryptedPassword);
    }

    @Transactional(rollbackFor = Exception.class)
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

        ChatTeam team = new ChatTeam();
        team.setOwnerId(member.getId());
        teamRepository.insert(team);

        log.info("owner registered: memberId={}, teamId={}, username={}", member.getId(), team.getId(), request.getUsername());
    }

    public MemberResponse authenticate(String username, String password) {
        ChatMember member = memberRepository.findByUsername(username);
        if (member == null || !encryptPassword(password).equals(member.getPassword()))
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);

        if (member.getStatus() != MemberStatus.ENABLED)
            throw new BusinessException(ErrorCode.MEMBER_DISABLED);

        return toMemberResponse(member);
    }

    public MemberResponse toMemberResponse(ChatMember member) {
        ChatTeam team = findTeamByMemberId(member.getId());
        MemberResponse response = new MemberResponse();
        response.setId(member.getId());
        response.setUsername(member.getUsername());
        response.setPhone(member.getPhone());
        response.setAvatar(member.getAvatar());
        response.setStatus(member.getStatus());
        response.setRole(member.getRole() != null ? member.getRole() : MemberRole.OWNER);
        response.setTeamId(team != null ? team.getId() : null);
        response.setOwnerId(team != null ? team.getOwnerId() : null);
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

    @Transactional(rollbackFor = Exception.class)
    public void deleteMember(Long memberId) {
        memberRepository.deleteById(memberId);
        memberRelationRepository.deleteByMemberId(memberId);
        log.info("team member removed: memberId={}", memberId);
    }

    /** 解析当前用户所属团队 ID */
    public Long resolveTeamId(Long memberId) {
        ChatMemberRelation relation = memberRelationRepository.findByMemberId(memberId);
        if (relation != null) return relation.getTeamId();

        ChatTeam team = teamRepository.findByOwnerId(memberId);
        return team != null ? team.getId() : null;
    }

    /** 解析资源归属 Owner 的 memberId（API Key、模型配置等挂在 Owner 账号下） */
    public Long resolveResourceOwnerId(Long memberId) {
        ChatTeam team = findTeamByMemberId(memberId);
        return team != null ? team.getOwnerId() : memberId;
    }

    public List<Long> listTeamMemberIds(Long teamId) {
        ChatTeam team = teamRepository.findById(teamId);
        if (team == null) return List.of();
        return Stream.concat(Stream.of(team.getOwnerId()), memberRelationRepository.listMemberIds(teamId).stream()).toList();
    }

    public boolean isTeamMember(Long teamId, Long memberId) {
        ChatTeam team = teamRepository.findById(teamId);
        if (team == null) return false;
        if (team.getOwnerId().equals(memberId)) return true;

        ChatMemberRelation relation = memberRelationRepository.findByMemberId(memberId);
        return relation != null && teamId.equals(relation.getTeamId());
    }

    public boolean isOwner(Long memberId) {
        ChatMember member = memberRepository.findById(memberId);
        return member != null && member.getRole() == MemberRole.OWNER;
    }

    @Transactional(rollbackFor = Exception.class)
    public TeamMemberResponse createTeamMember(CreateTeamMemberRequest request) {
        Long ownerId = request.getOwnerId();
        ChatTeam team = teamRepository.findByOwnerId(ownerId);
        if (team == null)
            throw new BusinessException(ErrorCode.MEMBER_MANAGE_DENIED);

        if (memberRepository.findByUsername(request.getUsername()) != null)
            throw new BusinessException(ErrorCode.MEMBER_USERNAME_EXISTS);

        if (StringUtils.hasText(request.getPhone()) && memberRepository.findByPhone(request.getPhone()) != null)
            throw new BusinessException(ErrorCode.MEMBER_PHONE_EXISTS);

        ChatMember member = buildTeamMember(request);
        memberRepository.insert(member);

        ChatMemberRelation relation = new ChatMemberRelation();
        relation.setTeamId(team.getId());
        relation.setMemberId(member.getId());
        relation.setStatus(MemberStatus.ENABLED);
        memberRelationRepository.insert(relation);

        log.info("team member created: teamId={}, memberId={}, username={}", team.getId(), member.getId(), member.getUsername());
        return toTeamMemberResponse(member);
    }

    public TeamOverviewResponse getOverview(Long memberId, ChatTeam team) {
        ChatMember owner = memberRepository.findById(team.getOwnerId());
        TeamOverviewResponse overview = new TeamOverviewResponse();
        overview.setOwner(toTeamMemberResponse(owner));
        overview.setMembers(listTeamMembers(team.getId()));
        overview.setCurrentUserId(memberId);
        return overview;
    }

    public List<TeamMemberResponse> listTeamMembers(Long teamId) {
        List<Long> memberIds = memberRelationRepository.listMemberIds(teamId);
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

    public void updateTeamMember(UpdateTeamMemberRequest request) {
        Long memberId = request.getId();
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

    private ChatTeam findTeamByMemberId(Long memberId) {
        ChatMemberRelation relation = memberRelationRepository.findByMemberId(memberId);
        if (relation != null) return teamRepository.findById(relation.getTeamId());
        return teamRepository.findByOwnerId(memberId);
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
