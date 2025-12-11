package com.dev.lib.jpa.infra.token;

import com.dev.lib.entity.EntityStatus;
import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.jpa.entity.BaseRepository;
import com.dev.lib.security.model.TokenType;
import lombok.Data;

public interface AccessTokenRepo extends BaseRepository<AccessTokenPo> {

    QAccessTokenPo $ = QAccessTokenPo.accessTokenPo;

    @Data
    class Q extends DslQuery<AccessTokenPo> {

        private TokenType    tokenType;

        private String       tokenKey;

        private String       userId;

        private EntityStatus status;

        private String       clientId;

        private Long expireTimeLt;

        private String tokenKeyLike;

        private String tokenKeyStartWith;

    }

}
