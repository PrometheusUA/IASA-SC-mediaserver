package ua.kpi.iasa.sc.mediaserver.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.sql.Timestamp;

@AllArgsConstructor
@Data
public class ResourceBackDTO {
    private String createdBy;
    private long id;
    private Timestamp createdAt;
    private String additionalInfo;
    private String discipline;
    private String link;
    private String teacher;
}
