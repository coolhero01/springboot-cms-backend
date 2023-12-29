package com.oneclicktech.spring.job;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;

import java.text.MessageFormat;


public class ProcessJob implements Job {

    @Override
    public void execute(JobExecutionContext context) {
    	 System.out.println(" ** ProcessJob >> [START]");
         JobDataMap dataMap = context.getJobDetail().getJobDataMap();
         String param = dataMap.getString("param");
         System.out.println(MessageFormat.format("Job: {0}; Param: {1}",
                 getClass(), param));
    }

}