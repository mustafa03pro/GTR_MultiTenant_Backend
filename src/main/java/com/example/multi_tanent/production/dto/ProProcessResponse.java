package com.example.multi_tanent.production.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.stream.Collectors;
import com.example.multi_tanent.production.entity.ProProcess;

@Data
@Builder
public class ProProcessResponse {

    private Long id;
    private String name;

    private Long locationId;
    private String locationName;

    private List<ProProcessWorkGroupResponse> workGroups;

    @Data
    @Builder
    public static class ProProcessWorkGroupResponse {
        private Long workGroupId;
        private String workGroupName;
        private String workGroupNumber;
        private Integer sequenceIndex;
    }

    public static ProProcessResponse fromEntity(ProProcess entity) {
        return ProProcessResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .locationId(entity.getLocation() != null ? entity.getLocation().getId() : null)
                .locationName(entity.getLocation() != null ? entity.getLocation().getName() : null)
                .workGroups(entity.getWorkGroups() != null ? entity.getWorkGroups().stream()
                        .map(wg -> ProProcessWorkGroupResponse.builder()
                                .workGroupId(wg.getWorkGroup().getId())
                                .workGroupName(wg.getWorkGroup().getName())
                                .workGroupNumber(wg.getWorkGroup().getNumber())
                                .sequenceIndex(wg.getSequenceIndex())
                                .build())
                        .collect(Collectors.toList())
                        : null)
                .build();
    }
}