package com.xaxage.be.adapter.in.web.dto;

import com.xaxage.be.domain.model.LearnMethod;
import com.xaxage.be.domain.model.Skill;

import java.util.List;

public record SkillResponse(String name, List<LearnMethodResponse> methods) {

    public static SkillResponse from(Skill skill) {
        return new SkillResponse(skill.name(), skill.methods().stream()
                .map(LearnMethodResponse::from)
                .toList());
    }

    public record LearnMethodResponse(String method, Integer level) {

        static LearnMethodResponse from(LearnMethod learnMethod) {
            return new LearnMethodResponse(learnMethod.method(), learnMethod.level());
        }
    }
}
