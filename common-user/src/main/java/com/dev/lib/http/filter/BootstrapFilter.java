package com.dev.lib.http.filter;

import com.dev.lib.biz.bootstrap.BootstrapError;
import com.dev.lib.biz.bootstrap.repo.IBootstrapQueryRepo;
import com.dev.lib.web.model.ServerResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class BootstrapFilter extends OncePerRequestFilter {

    private final IBootstrapQueryRepo bootstrapQueryRepo;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {

        if (!bootstrapQueryRepo.isSystemInitialized()) {
            ServerResponse.fail(BootstrapError.SYSTEM_NOT_INITIALIZE).to(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

}
