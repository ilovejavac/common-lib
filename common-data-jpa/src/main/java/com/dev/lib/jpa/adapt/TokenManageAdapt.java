package com.dev.lib.jpa.adapt;

import com.dev.lib.jpa.infra.token.AccessTokenPo;
import com.dev.lib.jpa.infra.token.AccessTokenPoToTokenItemMapper;
import com.dev.lib.jpa.infra.token.AccessTokenRepo;
import com.dev.lib.security.model.TokenItem;
import com.dev.lib.security.model.TokenItemToAccessTokenPoMapper;
import com.dev.lib.security.model.TokenType;
import com.dev.lib.security.service.TokenManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TokenManageAdapt implements TokenManager {
    private final AccessTokenRepo repo;
    private final AccessTokenPoToTokenItemMapper accessMapper;
    private final TokenItemToAccessTokenPoMapper tokenMapper;

    @Override
    public Object createToken(TokenItem tokenItem) {
        return repo.save(tokenMapper.convert(tokenItem)).getMetadata();
    }

    @Override
    public TokenItem getToken(String tokenKey) {
        return repo.load(new AccessTokenRepo.Q().setTokenKey(tokenKey))
                .map(accessMapper::convert)
                .orElse(null);
    }

    @Override
    public TokenItem getToken(TokenType tokenType, String tokenKey) {
        return repo.load(new AccessTokenRepo.Q()
                        .setTokenType(tokenType)
                        .setTokenKey(tokenKey))
                .map(accessMapper::convert)
                .orElse(null);
    }

    @Override
    public boolean validateToken(String tokenKey) {
        return false;
    }

    @Override
    public boolean validateToken(TokenType tokenType, String tokenKey) {
        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refreshToken(String tokenKey, long timeout) {
        Optional<AccessTokenPo> loadToken = repo.load(new AccessTokenRepo.Q().setTokenKey(tokenKey));
        loadToken.ifPresent(it -> {
            it.setExpireTime(timeout);
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refreshToken(String tokenKey, String tokenValue) {
        Optional<AccessTokenPo> loadToken = repo.load(new AccessTokenRepo.Q().setTokenKey(tokenKey));
        loadToken.ifPresent(it -> {
            it.setTokenValue(tokenValue);
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refreshToken(String tokenKey, Map<String, Object> metadata) {
        Optional<AccessTokenPo> loadToken = repo.load(new AccessTokenRepo.Q().setTokenKey(tokenKey));
        loadToken.ifPresent(it -> {
            it.setMetadata(metadata);
        });
    }

    @Override
    public void deleteToken(String tokenKey) {
        repo.load(new AccessTokenRepo.Q().setTokenKey(tokenKey)).ifPresent(repo::delete);
    }

    @Override
    public void deleteToken(TokenType tokenType, String tokenKey) {
        repo.load(new AccessTokenRepo.Q()
                .setTokenType(tokenType)
                .setTokenKey(tokenKey)
        ).ifPresent(repo::delete);
    }

    @Override
    public void deleteUserTokens(String userId) {
        repo.load(new AccessTokenRepo.Q()
                .setUserId(userId)
        ).ifPresent(repo::delete);
    }

    @Override
    public void deleteUserTokens(String userId, TokenType tokenType) {
        repo.load(new AccessTokenRepo.Q()
                .setUserId(userId)
                .setTokenType(tokenType)
        ).ifPresent(repo::delete);
    }

    @Override
    public List<TokenItem> getUserTokens(String userId) {
        return repo.loads(new AccessTokenRepo.Q().setUserId(userId))
                .stream().map(accessMapper::convert).toList();
    }

    @Override
    public List<TokenItem> getUserTokens(String userId, TokenType tokenType) {
        return repo.loads(new AccessTokenRepo.Q()
                .setUserId(userId)
                .setTokenType(tokenType)
        ).stream().map(accessMapper::convert).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cleanExpiredTokens() {
        List<AccessTokenPo> loadedToken =
                repo.loads(new AccessTokenRepo.Q().setExpireTimeLt(System.currentTimeMillis()));
        repo.deleteAll(loadedToken);
        return loadedToken.size();
    }

    @Override
    public List<TokenItem> getClientTokens(String clientId) {
        return repo.loads(new AccessTokenRepo.Q()
                .setClientId(clientId)
        ).stream().map(accessMapper::convert).toList();
    }

    @Override
    public List<String> searchTokenKeys(String prefix, String keyword, int start, int size) {
        return repo.page(new AccessTokenRepo.Q().setTokenKeyLike(keyword).setTokenKeyStartWith(prefix)
                .setStart(start)
                .setLimit(size)
        ).getContent().stream().map(AccessTokenPo::getTokenKey).toList();
    }
}
