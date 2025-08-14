package com.datacent.agent.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;


/**
 * 员工配置查询请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeConfigQueryRequest {

    private String threadId;

    private String userId;
}