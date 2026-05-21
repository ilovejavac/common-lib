package com.dev.lib.harness.biz.command;

import com.dev.lib.harness.biz.model.ModelProtocol;
import com.dev.lib.harness.biz.model.ModelProvide;
import lombok.Getter;
import lombok.Setter;

public class ModelCmd {

    @Getter
    @Setter
    public static class CreateModel {

        private String url;

        private String key;

        private String name;

        private ModelProvide type;

        private ModelProtocol protocol;

    }

    @Getter
    @Setter
    public static class UpdateModel extends CreateModel {

        private String id;

    }

}
