/*
 *
 * Copyright [2019~2025] [codingByFeng]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 *
 */

package me.fengorz.kiwi.dict.crawler.config;

import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.sdk.annotation.ScheduledAwake;
import me.fengorz.kiwi.common.sdk.util.spring.SpringUtils;
import me.fengorz.kiwi.dict.crawler.component.Enabler;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;

@Slf4j
@Aspect
public class EnablerAspectConfig {

    private final Enabler enabler;

    public EnablerAspectConfig() {
        log.info("EnablerAspectConfig set up.");
        this.enabler = SpringUtils.getBean(Enabler.class);
    }

    @Pointcut("@annotation(me.fengorz.kiwi.common.mq.MqAwake) && within(me.fengorz.kiwi.dict.crawler..*)")
    public void mqPointcut() {
    }

    @Pointcut("@annotation(me.fengorz.kiwi.common.sdk.annotation.ScheduledAwake) && within(me.fengorz.kiwi.dict.crawler..*)")
    public void schedulePointcut() {
    }

    @Around("mqPointcut()")
    public Object mqAround(ProceedingJoinPoint point) throws Throwable {
        if (!enabler.isMqEnable()) {
            log.info("Enabler didn't enabled MQ.");
            return new Object();
        }
        log.info("Enabler enabled MQ.");
        return point.proceed();
    }

    @Around("schedulePointcut()")
    public Object scheduleAround(ProceedingJoinPoint point) throws Throwable {
        String key;
        MethodSignature signature = (MethodSignature) point.getSignature();
        ScheduledAwake scheduledAwake = signature.getMethod().getAnnotation(ScheduledAwake.class);
        key = scheduledAwake.key();
        if (StringUtils.isBlank(key) || !enabler.isSchedulerEnable(key)) {
            log.info("Enabler[{}] didn't enabled Scheduler.", key);
            return new Object();
        }
        log.info("Enabler[{}] enabled Scheduler.", key);
        return point.proceed();
    }

}
