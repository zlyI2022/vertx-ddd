package com.packagename.blackboard.domain.model;

import lombok.Data;

@Data
public class Blackboard {
    private final String id;
    private final String departmentId;
    private final String markdown;
}
