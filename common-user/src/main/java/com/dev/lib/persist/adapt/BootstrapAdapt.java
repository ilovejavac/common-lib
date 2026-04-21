package com.dev.lib.persist.adapt;

import com.dev.lib.biz.bootstrap.repo.IBootstrapQueryRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BootstrapAdapt implements IBootstrapQueryRepo {


    @Override
    public Boolean isSystemInitialized() {

        return null;
    }

}
