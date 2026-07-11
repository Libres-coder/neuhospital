package com.neusoft.hospital.module.family;

import com.neusoft.hospital.common.BizException;
import com.neusoft.hospital.module.auth.User;
import com.neusoft.hospital.module.auth.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

/**
 * Family member (亲情账户) management.
 *
 * <p>Authorization model: the authenticated user's id is the only key. Any
 * family record the service touches must have {@code ownerId == userId}.
 * We never look up by {@code id} alone — that would let any logged-in user
 * read/modify another user's family by guessing ids.</p>
 */
@Service
@RequiredArgsConstructor
public class FamilyService {

    /** Hard cap so a buggy client can't blow up the row count. */
    private static final int MAX_MEMBERS_PER_OWNER = 20;

    private final FamilyMemberRepository repo;
    private final UserRepository userRepository;

    /** Used by other modules to know the currently-booking patient. */
    private volatile String defaultMemberByUserCache = null;

    /**
     * List all family members owned by the calling user, auto-creating the
     * self ("本人") member on first call if missing. Mirrors what the Android
     * client used to do in {@code AuthRepository.verifySms}.
     */
    @Transactional
    public FamilyDtos.ListResponse listAndAutoSeed(String userId) {
        if (userId == null || userId.isBlank()) throw BizException.unauthorized("请先登录");
        List<FamilyMember> all = repo.findByOwnerIdOrderByIsDefaultDescNameAsc(userId);
        if (all.isEmpty()) {
            User u = userRepository.findById(userId)
                    .orElseThrow(() -> BizException.notFound("用户不存在"));
            FamilyMember self = new FamilyMember();
            self.setId("fm_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
            self.setOwnerId(userId);
            self.setName(u.getName() == null ? "本人" : u.getName());
            self.setPhone(u.getPhone());
            self.setIdCard("");
            self.setRelation("本人");
            self.setDefault(true);
            self.setCreateTime(System.currentTimeMillis());
            all = List.of(repo.save(self));
        }
        String defaultId = all.stream().filter(FamilyMember::isDefault).map(FamilyMember::getId).findFirst()
                .orElse(all.get(0).getId());
        defaultMemberByUserCache = defaultId;
        return new FamilyDtos.ListResponse(
                all.stream().map(FamilyDtos.FamilyMemberDto::from).toList(),
                defaultId
        );
    }

    @Transactional
    public FamilyDtos.FamilyMemberDto add(String userId, FamilyDtos.AddRequest req) {
        if (userId == null || userId.isBlank()) throw BizException.unauthorized("请先登录");
        long count = repo.countByOwnerId(userId);
        if (count >= MAX_MEMBERS_PER_OWNER) {
            throw BizException.badRequest("亲情账户已达上限（" + MAX_MEMBERS_PER_OWNER + "）");
        }
        String name = req.getName().trim();
        String phone = req.getPhone().trim();
        String idCard = req.getIdCard() == null ? "" : req.getIdCard().trim();
        String relation = req.getRelation().trim();
        if (!StringUtils.hasText(name) || !StringUtils.hasText(relation)) {
            throw BizException.badRequest("姓名和关系不能为空");
        }

        FamilyMember e = new FamilyMember();
        e.setId("fm_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        e.setOwnerId(userId);
        e.setName(name);
        e.setPhone(phone);
        e.setIdCard(idCard);
        e.setRelation(relation);
        e.setAvatar(req.getAvatar());
        e.setDefault(false); // existing default still wins
        e.setCreateTime(System.currentTimeMillis());
        return FamilyDtos.FamilyMemberDto.from(repo.save(e));
    }

    @Transactional
    public FamilyDtos.FamilyMemberDto update(String userId, String id, FamilyDtos.UpdateRequest req) {
        FamilyMember e = ownedBy(userId, id);
        if (req.getName() != null && !req.getName().isBlank()) e.setName(req.getName().trim());
        if (req.getPhone() != null && !req.getPhone().isBlank()) e.setPhone(req.getPhone().trim());
        if (req.getIdCard() != null) e.setIdCard(req.getIdCard().trim());
        if (req.getRelation() != null && !req.getRelation().isBlank()) e.setRelation(req.getRelation().trim());
        if (req.getAvatar() != null) e.setAvatar(req.getAvatar());
        e.setUpdateTime(System.currentTimeMillis());
        return FamilyDtos.FamilyMemberDto.from(repo.save(e));
    }

    @Transactional
    public void delete(String userId, String id) {
        FamilyMember e = ownedBy(userId, id);
        // Don't allow deleting the default member — flip isDefault to the next one first.
        if (e.isDefault()) {
            List<FamilyMember> all = repo.findByOwnerIdOrderByIsDefaultDescNameAsc(userId);
            if (all.size() <= 1) {
                throw BizException.badRequest("至少需要保留一位亲情账户成员");
            }
            // Promote the next one before deleting this default.
            FamilyMember next = all.stream().filter(x -> !x.getId().equals(id)).findFirst().orElseThrow();
            long now = System.currentTimeMillis();
            repo.clearDefault(userId, now);
            repo.markDefault(next.getId(), now);
        }
        repo.delete(e);
    }

    /**
     * Mark a family member as the currently-booking patient. Used so the
     * user can switch "我" → "妈妈" without logging out.
     */
    @Transactional
    public FamilyDtos.FamilyMemberDto setDefault(String userId, String id) {
        FamilyMember e = ownedBy(userId, id);
        long now = System.currentTimeMillis();
        repo.clearDefault(userId, now);
        repo.markDefault(id, now);
        e.setDefault(true);
        e.setUpdateTime(now);
        defaultMemberByUserCache = id;
        return FamilyDtos.FamilyMemberDto.from(e);
    }

    /** Used by AppointmentService (next up) to know whose name to put on the booking. */
    public String currentPatientMemberId(String userId) {
        if (userId == null) return null;
        String cached = defaultMemberByUserCache;
        if (cached != null) return cached;
        return repo.findByOwnerIdOrderByIsDefaultDescNameAsc(userId).stream()
                .filter(FamilyMember::isDefault).map(FamilyMember::getId).findFirst()
                .orElse(null);
    }

    /**
     * Lookup a family member ensuring ownership. This is the only entry
     * point that returns a raw entity — callers must not use {@code findById}
     * directly or they bypass ownership enforcement.
     */
    private FamilyMember ownedBy(String userId, String id) {
        if (userId == null) throw BizException.unauthorized("请先登录");
        FamilyMember e = repo.findById(id).orElseThrow(() -> BizException.notFound("亲情账户不存在"));
        if (!e.getOwnerId().equals(userId)) {
            // Don't leak existence — pretend it's missing.
            throw BizException.notFound("亲情账户不存在");
        }
        return e;
    }
}
