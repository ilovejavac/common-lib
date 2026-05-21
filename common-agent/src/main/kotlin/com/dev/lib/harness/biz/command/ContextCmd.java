package com.dev.lib.harness.biz.command;

import com.dev.lib.harness.biz.model.ModelEffort;
import com.dev.lib.harness.biz.model.ModelSummary;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

public class ContextCmd {

    @Getter
    @Setter
    public static class Input {

        private String message;

        private String model;

        private List<String> skills;

    }

    @Getter
    @Setter
    public static class Turn {

        private String model;

        private String cwd;

        private ModelEffort effort;

        private ModelSummary summary;

    }

    @Getter
    @Setter
    public static class Clear {

        private String id;

    }

    @Getter
    @Setter
    public static class Answer {

        private String id;

        private Map<Integer, String> answer;

    }

    @Getter
    @Setter
    public static class Interrupt {
        // 终止
    }

    @Getter
    @Setter
    public static class Approval {
        // 审批
    }

    @Getter
    @Setter
    public static class Compact {
        // 压缩
    }

}
