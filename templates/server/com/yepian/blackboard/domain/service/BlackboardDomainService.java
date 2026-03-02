package com.yepian.blackboard.domain.service;

import com.yepian.blackboard.domain.model.Blackboard;

public class BlackboardDomainService {
    public boolean validateHeadings(Blackboard blackboard) {
        String content = blackboard.getMarkdown();
        return content.contains("## 驾驶舱总览")
            && content.contains("## 当前状态")
            && content.contains("## 风险与阻塞")
            && content.contains("## 本周目标")
            && content.contains("## 决策记录");
    }
}
