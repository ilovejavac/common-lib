package com.dev.lib.jpa;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

@Component
public class TransactionHelper implements ApplicationContextAware {

    private static TransactionTemplate template;

    @Override
    public void setApplicationContext(ApplicationContext ctx) throws BeansException {

        PlatformTransactionManager tm = ctx.getBean(PlatformTransactionManager.class);
        template = new TransactionTemplate(tm);
    }

    public static void run(Runnable action) {

        template.executeWithoutResult(status -> action.run());
    }

    public static <T> T call(Supplier<T> action) {

        return template.execute(status -> action.get());
    }

    public static void runNew(Runnable action) {

        TransactionTemplate newTx = new TransactionTemplate(template.getTransactionManager());
        newTx.setPropagationBehaviorName("PROPAGATION_REQUIRES_NEW");
        newTx.executeWithoutResult(status -> action.run());
    }

    public static <T> T callNew(Supplier<T> action) {

        TransactionTemplate newTx = new TransactionTemplate(template.getTransactionManager());
        newTx.setPropagationBehaviorName("PROPAGATION_REQUIRES_NEW");
        return newTx.execute(status -> action.get());
    }

}
