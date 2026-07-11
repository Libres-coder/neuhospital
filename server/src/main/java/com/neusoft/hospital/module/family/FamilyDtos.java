package com.neusoft.hospital.module.family;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

public final class FamilyDtos {

    private FamilyDtos() {}

    /** Wire format used both for list responses and add responses. */
    @Data
    public static class FamilyMemberDto {
        private String id;
        private String ownerId;
        private String name;
        private String phone;
        private String idCard;
        private String relation;
        private String avatar;
        private boolean isDefault;
        private long createTime;
        private Long updateTime;

        public static FamilyMemberDto from(FamilyMember e) {
            FamilyMemberDto d = new FamilyMemberDto();
            d.id = e.getId();
            d.ownerId = e.getOwnerId();
            d.name = e.getName();
            d.phone = e.getPhone();
            d.idCard = e.getIdCard();
            d.relation = e.getRelation();
            d.avatar = e.getAvatar();
            d.isDefault = e.isDefault();
            d.createTime = e.getCreateTime();
            d.updateTime = e.getUpdateTime();
            return d;
        }
    }

    /** Add a new family member. */
    @Data
    public static class AddRequest {
        @NotBlank
        @Size(min = 1, max = 64)
        private String name;

        @NotBlank
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        private String phone;

        // optional — elderly/children may not have an id card on file yet
        @Size(max = 32)
        private String idCard = "";

        @NotBlank
        @Pattern(regexp = "^(父母|子女|配偶|本人|其他)$", message = "关系必须是 父母/子女/配偶/本人/其他")
        private String relation;

        private String avatar;
    }

    /** Update an existing family member (only fields the user actually wants to change). */
    @Data
    public static class UpdateRequest {
        @Size(min = 1, max = 64)
        private String name;
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        private String phone;
        @Size(max = 32)
        private String idCard;
        @Pattern(regexp = "^(父母|子女|配偶|本人|其他)$", message = "关系必须是 父母/子女/配偶/本人/其他")
        private String relation;
        private String avatar;
    }

    @Data
    public static class ListResponse {
        private List<FamilyMemberDto> members;
        private String currentPatientId;

        public ListResponse(List<FamilyMemberDto> members, String currentPatientId) {
            this.members = members;
            this.currentPatientId = currentPatientId;
        }
    }
}
