package info.mengnan.dialogerai.server.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import info.mengnan.dialogerai.repository.entity.ChatMember;
import info.mengnan.dialogerai.repository.entity.ChatTeam;
import info.mengnan.dialogerai.repository.entity.ChatTeamMember;
import info.mengnan.dialogerai.repository.enums.MemberRole;
import info.mengnan.dialogerai.repository.enums.MemberStatus;
import info.mengnan.dialogerai.repository.repo.MemberRepository;
import info.mengnan.dialogerai.repository.repo.TeamMemberRepository;
import info.mengnan.dialogerai.repository.repo.TeamRepository;
import info.mengnan.dialogerai.server.exception.BusinessException;
import info.mengnan.dialogerai.server.param.ErrorCode;
import info.mengnan.dialogerai.server.param.auth.MemberResponse;
import info.mengnan.dialogerai.server.param.auth.MemberUpdateRequest;
import info.mengnan.dialogerai.server.param.auth.RegisterRequest;
import info.mengnan.dialogerai.server.param.team.CreateTeamMemberRequest;
import info.mengnan.dialogerai.server.param.team.MemberTeamContext;
import info.mengnan.dialogerai.server.param.team.TeamMemberResponse;
import info.mengnan.dialogerai.server.param.team.TeamOverviewResponse;
import info.mengnan.dialogerai.server.param.team.UpdateTeamMemberRequest;
import info.mengnan.dialogerai.server.param.team.UpdateTeamRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamRepository teamRepository;

    public ChatMember findById(Long id) {
        return memberRepository.findById(id);
    }

    public ChatTeam findTeamById(Long teamId) {
        return teamRepository.findById(teamId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void register(RegisterRequest request) {
        if (!StringUtils.hasText(request.getShareCode()))
            throw new BusinessException(ErrorCode.SHARE_CODE_REQUIRED);

        ChatTeam team = teamRepository.findByShareCode(request.getShareCode().trim());
        if (team == null)
            throw new BusinessException(ErrorCode.SHARE_CODE_INVALID);

        if (memberRepository.findByUsername(request.getUsername()) != null)
            throw new BusinessException(ErrorCode.MEMBER_USERNAME_EXISTS);

        String phone = request.getPhone();
        if (StringUtils.hasText(phone) && memberRepository.findByPhone(phone) != null)
            throw new BusinessException(ErrorCode.MEMBER_PHONE_EXISTS);

        ChatMember member = new ChatMember();
        member.setUsername(request.getUsername());
        member.setPassword(request.getPassword());
        member.setPhone(request.getPhone());
        member.setStatus(MemberStatus.ENABLED);
        memberRepository.insert(member);

        insertTeamMembership(team.getId(), member.getId(), MemberRole.MEMBER, MemberStatus.ENABLED);

        log.info("member registered via shareCode: memberId={}, teamId={}, username={}",
                member.getId(), team.getId(), request.getUsername());
    }

    public MemberResponse authenticate(String username, String encryptedPassword) {
        ChatMember member = memberRepository.findByUsername(username);
        if (member == null || !encryptedPassword.equals(member.getPassword()))
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);

        if (member.getStatus() != MemberStatus.ENABLED)
            throw new BusinessException(ErrorCode.MEMBER_DISABLED);

        ChatTeamMember membership = loadMembership(member.getId());
        if (membership != null && membership.getStatus() != MemberStatus.ENABLED)
            throw new BusinessException(ErrorCode.MEMBER_DISABLED);

        return buildMemberResponse(member);
    }

    public MemberResponse buildMemberResponse(ChatMember member) {
        MemberTeamContext ctx = resolveTeamContext(member.getId());

        MemberResponse response = new MemberResponse();
        response.setId(member.getId());
        response.setUsername(member.getUsername());
        response.setPhone(member.getPhone());
        response.setAvatar(member.getAvatar());
        response.setStatus(member.getStatus());
        response.setRole(ctx != null ? ctx.role() : null);
        response.setTeamId(ctx != null ? ctx.teamId() : null);
        response.setOwnerId(ctx != null ? ctx.ownerId() : null);
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
            updateMember.setPassword(request.getPassword());
        memberRepository.updateById(updateMember);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteMember(Long memberId) {
        memberRepository.deleteById(memberId);
        teamMemberRepository.deleteByMemberId(memberId);
        log.info("team member removed: memberId={}", memberId);
    }

    public MemberTeamContext resolveTeamContext(Long memberId) {
        ChatTeamMember membership = loadMembership(memberId);
        if (membership == null)
            return null;

        ChatTeam team = teamRepository.findById(membership.getTeamId());
        if (team == null)
            return null;

        return new MemberTeamContext(
                memberId, team.getId(), team.getOwnerId(), membership.getRole(),
                team.getDefaultChatModelId(), team.getDefaultImageModelId());
    }

    public List<Long> listTeamMemberIds(Long teamId) {
        return teamMemberRepository.listMemberIds(teamId);
    }

    public boolean isTeamMember(MemberTeamContext ctx, Long memberId) {
        return ctx != null && isTeamMember(ctx.teamId(), memberId);
    }

    public boolean isTeamMember(Long teamId, Long memberId) {
        if (teamId == null || memberId == null)
            return false;
        ChatTeamMember membership = loadMembership(memberId);
        return membership != null && teamId.equals(membership.getTeamId());
    }

    @Transactional(rollbackFor = Exception.class)
    public TeamMemberResponse createTeamMember(CreateTeamMemberRequest request) {
        MemberTeamContext ownerCtx = resolveTeamContext(request.getOwnerId());
        if (ownerCtx == null || !ownerCtx.isOwner())
            throw new BusinessException(ErrorCode.MEMBER_MANAGE_DENIED);

        if (memberRepository.findByUsername(request.getUsername()) != null)
            throw new BusinessException(ErrorCode.MEMBER_USERNAME_EXISTS);

        if (StringUtils.hasText(request.getPhone()) && memberRepository.findByPhone(request.getPhone()) != null)
            throw new BusinessException(ErrorCode.MEMBER_PHONE_EXISTS);

        ChatMember member = buildTeamMember(request);
        memberRepository.insert(member);

        ChatTeamMember teamMember = insertTeamMembership(
                ownerCtx.teamId(), member.getId(), MemberRole.MEMBER, MemberStatus.ENABLED);

        log.info("team member created: teamId={}, memberId={}, username={}", ownerCtx.teamId(), member.getId(), member.getUsername());
        return toTeamMemberResponse(member, teamMember);
    }

    public TeamOverviewResponse getOverview(Long memberId, ChatTeam team) {
        ChatMember owner = memberRepository.findById(team.getOwnerId());
        ChatTeamMember ownerMembership = loadMembership(team.getOwnerId());
        MemberTeamContext ctx = resolveTeamContext(memberId);

        TeamOverviewResponse overview = new TeamOverviewResponse();
        overview.setTeamName(team.getName());
        overview.setDefaultChatModelId(team.getDefaultChatModelId());
        overview.setDefaultImageModelId(team.getDefaultImageModelId());
        if (ctx != null && ctx.isOwner())
            overview.setShareCode(team.getShareCode());
        overview.setOwner(toTeamMemberResponse(owner, ownerMembership));
        overview.setMembers(listTeamMembers(team.getId()));
        overview.setCurrentUserId(memberId);
        return overview;
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateTeam(Long ownerId, UpdateTeamRequest request) {
        ChatTeam team = teamRepository.findByOwnerId(ownerId);
        if (team == null)
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);

        String name = StringUtils.hasText(request.getName()) ? request.getName().trim() : team.getName();
        Long defaultChatModelId = request.getDefaultChatModelId();
        Long defaultImageModelId = request.getDefaultImageModelId();

        LambdaUpdateWrapper<ChatTeam> wrapper = new LambdaUpdateWrapper<ChatTeam>()
                .eq(ChatTeam::getId, team.getId())
                .set(ChatTeam::getName, name)
                .set(ChatTeam::getDefaultChatModelId, defaultChatModelId)
                .set(ChatTeam::getDefaultImageModelId, defaultImageModelId);

        if (StringUtils.hasText(request.getShareCode())) {
            String shareCode = request.getShareCode().trim();
            ChatTeam occupied = teamRepository.findByShareCode(shareCode);
            if (occupied != null && !occupied.getId().equals(team.getId()))
                throw new BusinessException(ErrorCode.SHARE_CODE_INVALID);
            wrapper.set(ChatTeam::getShareCode, shareCode);
            team.setShareCode(shareCode);
        }

        teamRepository.update(wrapper);
        log.info("team updated: ownerId={}, name={}, defaultChatModelId={}, defaultImageModelId={}, shareCode={}",
                ownerId, name, defaultChatModelId, defaultImageModelId, team.getShareCode());
    }

    public List<TeamMemberResponse> listTeamMembers(Long teamId) {
        List<ChatTeamMember> memberships = teamMemberRepository.findMembersByTeamId(teamId);
        if (memberships.isEmpty())
            return List.of();

        List<Long> memberIds = memberships.stream().map(ChatTeamMember::getMemberId).toList();
        return memberRepository.findByIds(memberIds).stream()
                .map(member -> toTeamMemberResponse(member, findMembershipInList(memberships, member.getId())))
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public void disableMember(Long memberId) {
        updateMemberStatus(memberId, MemberStatus.DISABLED);
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
        if (StringUtils.hasText(request.getPassword()))
            updateMember.setPassword(request.getPassword());
        memberRepository.updateById(updateMember);

        if (request.getStatus() != null)
            updateMemberStatus(memberId, request.getStatus());

        log.info("team member updated: memberId={}", memberId);
    }

    private ChatTeamMember loadMembership(Long memberId) {
        return teamMemberRepository.findByMemberId(memberId);
    }

    private ChatTeamMember insertTeamMembership(Long teamId, Long memberId, MemberRole role, MemberStatus status) {
        ChatTeamMember teamMember = new ChatTeamMember();
        teamMember.setTeamId(teamId);
        teamMember.setMemberId(memberId);
        teamMember.setRole(role);
        teamMember.setStatus(status);
        teamMemberRepository.insert(teamMember);
        return teamMember;
    }

    private void updateMemberStatus(Long memberId, MemberStatus status) {
        ChatMember updateMember = new ChatMember();
        updateMember.setId(memberId);
        updateMember.setStatus(status);
        memberRepository.updateById(updateMember);

        ChatTeamMember membership = loadMembership(memberId);
        if (membership != null) {
            membership.setStatus(status);
            teamMemberRepository.updateById(membership);
        }
    }

    private ChatMember buildTeamMember(CreateTeamMemberRequest request) {
        ChatMember member = new ChatMember();
        member.setUsername(request.getUsername());
        member.setPassword(request.getPassword());
        member.setPhone(request.getPhone());
        member.setStatus(MemberStatus.ENABLED);
        return member;
    }

    private TeamMemberResponse toTeamMemberResponse(ChatMember member, ChatTeamMember teamMember) {
        TeamMemberResponse response = new TeamMemberResponse();
        response.setId(member.getId());
        response.setUsername(member.getUsername());
        response.setPhone(member.getPhone());
        response.setStatus(teamMember != null ? teamMember.getStatus() : member.getStatus());
        response.setRole(teamMember != null ? teamMember.getRole() : null);
        response.setCreatedAt(member.getCreatedAt());
        return response;
    }

    private ChatTeamMember findMembershipInList(List<ChatTeamMember> memberships, Long memberId) {
        return memberships.stream()
                .filter(m -> m.getMemberId().equals(memberId))
                .findFirst()
                .orElse(null);
    }
}
