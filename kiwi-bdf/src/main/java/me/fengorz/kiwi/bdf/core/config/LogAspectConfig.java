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

package me.fengorz.kiwi.bdf.core.config;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.CodeSignature;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.sdk.annotation.log.LogMarker;
import me.fengorz.kiwi.common.sdk.constant.GlobalConstants;

/**
 * @Description 日志切面配置
 * @Author zhanshifeng
 * @Date 2022/4/17 22:30
 */
@Slf4j
@Aspect
@Component
@Profile({"dev", "prod"})
public class LogAspectConfig {

    public LogAspectConfig() {
        log.info("LogAspectConfig set up.");
    }

    @Pointcut("@annotation(me.fengorz.kiwi.common.sdk.annotation.log.LogMarker)")
    public void pointcut() {}

    @Around("pointcut()")
    public Object logMethodAround(ProceedingJoinPoint point) throws Throwable {
        MethodSignature signature = (MethodSignature)point.getSignature();
        LogMarker logMarker = signature.getMethod().getAnnotation(LogMarker.class);
        Instant before = Instant.now();
        Object result = point.proceed();
        String methodName =
            point.getTarget().getClass().getSimpleName() + GlobalConstants.SYMBOL_DOT + signature.getMethod().getName();
        log.info("===> [log marker] method path: {}, params: {}, took {} ms, result is {}", methodName,
            logMarker.isPrintParameter() ? getParameters(point) : LOG_METHOD_RESULT_DISABLE,
            logMarker.isPrintExecutionTime() ? Duration.between(before, Instant.now()).toMillis()
                : LOG_METHOD_RESULT_DISABLE,
            logMarker.isPrintReturnValue() ? result : LOG_METHOD_RESULT_DISABLE);
        return result;
    }

    private Map<String, Object> getParameters(JoinPoint joinPoint) {
        CodeSignature signature = (CodeSignature)joinPoint.getSignature();
        String[] parameterNames = signature.getParameterNames();
        if (ArrayUtils.isEmpty(parameterNames)) {
            return null;
        }
        Map<String, Object> map = new HashMap<>(parameterNames.length);
        for (int i = 0; i < parameterNames.length; i++) {
            map.put(parameterNames[i], joinPoint.getArgs()[i]);
        }
        return map;
    }

    private static final String LOG_METHOD_RESULT_DISABLE = "[disable]";

}
