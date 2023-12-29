package com.oneclicktech.spring;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import org.mybatis.spring.annotation.MapperScan;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.oneclicktech.spring.job.ProcessJob;

@SpringBootApplication
@MapperScan("com.oneclicktech.spring.mapper")
@EnableScheduling
@ComponentScan
public class SpringBootCMSApplication extends SpringBootServletInitializer {

	public static void main(String[] args) {
		SpringApplication.run(SpringBootCMSApplication.class, args);
		try {
			// onStartup();
		} catch (Exception e) {

		}
	}

	private static void onStartup() throws SchedulerException {
		System.out.println(" ** SpringBootCMSApplication >> onStartup >> [START]");
		JobDetail job = JobBuilder.newJob(ProcessJob.class).usingJobData("param", "value") // add a parameter
				.build();

		Date afterFiveSeconds = Date
				.from(LocalDateTime.now().plusSeconds(5).atZone(ZoneId.systemDefault()).toInstant());
		Trigger trigger = TriggerBuilder.newTrigger().startAt(afterFiveSeconds).build();

		SchedulerFactory schedulerFactory = new StdSchedulerFactory();
		Scheduler scheduler = schedulerFactory.getScheduler();
		scheduler.start();
		scheduler.scheduleJob(job, trigger);
		System.out.println(" ** SpringBootCMSApplication >>  onStartup >> [END]");
	}
}
