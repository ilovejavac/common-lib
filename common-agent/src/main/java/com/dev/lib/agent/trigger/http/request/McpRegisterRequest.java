package com.dev.lib.agent.trigger.http.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class McpRegisterRequest {

    @NotBlank(message = "serverId must not be blank")
    @Size(max = 100, message = "serverId is too long")
    private String serverId;

    @NotBlank(message = "name must not be blank")
    @Size(max = 100, message = "name is too long")
    private String name;

    @NotBlank(message = "url must not be blank")
    @Size(max = 500, message = "url is too long")
    private String url;

    @Size(max = 1000, message = "description is too long")
    private String description;
}
