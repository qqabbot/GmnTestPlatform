package com.testing.automation.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.quartz.SchedulerFactoryBeanCustomizer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;

@Configuration
public class QuartzConfig {

    @Autowired
    private ApplicationContext applicationContext;

    @Bean
    public SchedulerFactoryBeanCustomizer schedulerFactoryBeanCustomizer() {
        return (SchedulerFactoryBean schedulerFactoryBean) -> {
            AutowiringSpringBeanJobFactory jobFactory = new AutowiringSpringBeanJobFactory();
            jobFactory.setApplicationContext(applicationContext);
            schedulerFactoryBean.setJobFactory(jobFactory);
            schedulerFactoryBean.setWaitForJobsToCompleteOnShutdown(true);
            schedulerFactoryBean.setOverwriteExistingJobs(true);
        };
    }

    /**
     * Custom JobFactory that autowires Spring beans into Quartz jobs
     */
    public static class AutowiringSpringBeanJobFactory extends SpringBeanJobFactory {
        private ApplicationContext applicationContext;

        @Override
        public void setApplicationContext(ApplicationContext applicationContext) {
            super.setApplicationContext(applicationContext);
            this.applicationContext = applicationContext;
        }

        @Override
        protected Object createJobInstance(org.quartz.spi.TriggerFiredBundle bundle) throws Exception {
            Object job = super.createJobInstance(bundle);
            if (applicationContext != null) {
                applicationContext.getAutowireCapableBeanFactory().autowireBean(job);
            }
            return job;
        }
    }
}
