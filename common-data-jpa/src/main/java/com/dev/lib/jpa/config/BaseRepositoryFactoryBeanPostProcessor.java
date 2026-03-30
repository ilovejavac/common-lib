package com.dev.lib.jpa.config;

import com.dev.lib.jpa.entity.BaseRepository;
import com.dev.lib.jpa.entity.BaseRepositoryImpl;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;

public class BaseRepositoryFactoryBeanPostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {

        if (!(bean instanceof JpaRepositoryFactoryBean<?, ?, ?> repositoryFactoryBean)) {
            return bean;
        }

        Class<?> repositoryInterface = repositoryFactoryBean.getObjectType();
        if (repositoryInterface != null && BaseRepository.class.isAssignableFrom(repositoryInterface)) {
            repositoryFactoryBean.setRepositoryBaseClass(BaseRepositoryImpl.class);
        }

        return bean;
    }
}
