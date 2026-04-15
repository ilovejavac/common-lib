package com.dev.lib.jpa;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.persistence.EntityManagerFactory;
import java.util.function.Supplier;

@Component
public class TransactionHelper implements ApplicationContextAware {

    private static TransactionTemplate template;
    private static ApplicationContext ctx;

    @Override
    public void setApplicationContext(ApplicationContext ctx) throws BeansException {

        TransactionHelper.ctx = ctx;
        PlatformTransactionManager tm = ctx.getBean(PlatformTransactionManager.class);
        template = new TransactionTemplate(tm);
    }

    public static void run(Runnable action) {

        template.executeWithoutResult(status -> action.run());
    }

    public static <T> T call(Supplier<T> action) {

        return template.execute(status -> action.get());
    }

    public static void runWithEntityManagerFactory(EntityManagerFactory emf, Runnable action) {

        resolveTemplate(emf).executeWithoutResult(status -> action.run());
    }

    public static <T> T callWithEntityManagerFactory(EntityManagerFactory emf, Supplier<T> action) {

        return resolveTemplate(emf).execute(status -> action.get());
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

    private static TransactionTemplate resolveTemplate(EntityManagerFactory emf) {

        PlatformTransactionManager resolved = resolveTransactionManager(emf);
        if (template.getTransactionManager() == resolved) {
            return template;
        }
        return new TransactionTemplate(resolved);
    }

    private static PlatformTransactionManager resolveTransactionManager(EntityManagerFactory emf) {

        if (ctx == null || emf == null) {
            return template.getTransactionManager();
        }
        for (JpaTransactionManager tm : ctx.getBeansOfType(JpaTransactionManager.class).values()) {
            if (tm.getEntityManagerFactory() == emf) {
                return tm;
            }
        }
        return template.getTransactionManager();
    }

}
