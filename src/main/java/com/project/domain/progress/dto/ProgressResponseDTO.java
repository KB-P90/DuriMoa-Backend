package com.project.domain.progress.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgressResponseDTO {

    private OverallProgressDTO overall;
    private PersonalProgressDTO personal;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OverallProgressDTO {

        private Long targetAmount;
        private Long currentAmount;
        private Integer progressRate;
        private Integer completedItemCount;
        private Integer totalItemCount;
        private List<GoalItemProgressDTO> items;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GoalItemProgressDTO {

        private Long goalItemId;
        private String category;
        private Long targetAmount;
        private Long currentAmount;
        private Integer progressRate;
        private Boolean completed;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PersonalProgressDTO {

        private List<MemberProgressDTO> members;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberProgressDTO {

        private Long userId;
        private String name;
        private Boolean assetShared;
        private Long assetAmount;
        private Long targetAmount;
        private Long currentAmount;
        private Double progressRate;
    }

}
