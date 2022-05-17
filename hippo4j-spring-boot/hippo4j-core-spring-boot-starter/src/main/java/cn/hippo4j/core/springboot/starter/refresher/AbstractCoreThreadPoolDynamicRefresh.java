/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.hippo4j.core.springboot.starter.refresher;

import cn.hippo4j.common.api.ThreadPoolDynamicRefresh;
import cn.hippo4j.common.config.ApplicationContextHolder;
import cn.hippo4j.common.notify.ThreadPoolNotifyAlarm;
import cn.hippo4j.core.executor.manage.GlobalNotifyAlarmManage;
import cn.hippo4j.core.executor.support.ThreadPoolBuilder;
import cn.hippo4j.core.springboot.starter.config.BootstrapCoreProperties;
import cn.hippo4j.core.springboot.starter.parser.ConfigParserHandler;
import cn.hippo4j.core.springboot.starter.refresher.event.Hippo4jCoreDynamicRefreshEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;

import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * Abstract core thread-pool dynamic refresh.
 *
 * @author chen.ma
 * @date 2022/2/26 12:42
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractCoreThreadPoolDynamicRefresh implements ThreadPoolDynamicRefresh, InitializingBean {

    protected final BootstrapCoreProperties bootstrapCoreProperties;

    protected final ExecutorService dynamicRefreshExecutorService = ThreadPoolBuilder.builder().singlePool("client.dynamic.refresh").build();

    @Override
    public void dynamicRefresh(String content) {
        Map<Object, Object> configInfo;
        try {
            configInfo = ConfigParserHandler.getInstance().parseConfig(content, bootstrapCoreProperties.getConfigFileType());
        } catch (Exception ex) {
            log.error("dynamic-thread-pool parse config file error, content: {}, fileType: {}", content, bootstrapCoreProperties.getConfigFileType(), ex);
            return;
        }
        BootstrapCoreProperties bindableCoreProperties = BootstrapCorePropertiesBinderAdapt.bootstrapCorePropertiesBinder(configInfo, bootstrapCoreProperties);
        ApplicationContextHolder.getInstance().publishEvent(new Hippo4jCoreDynamicRefreshEvent(this, bindableCoreProperties));
    }

    /**
     * Register notify alarm manage.
     */
    public void registerNotifyAlarmManage() {
        bootstrapCoreProperties.getExecutors().forEach(executorProperties -> {
            ThreadPoolNotifyAlarm threadPoolNotifyAlarm = new ThreadPoolNotifyAlarm(
                    executorProperties.getNotify().getIsAlarm(),
                    executorProperties.getNotify().getCapacityAlarm(),
                    executorProperties.getNotify().getActiveAlarm());
            threadPoolNotifyAlarm.setInterval(executorProperties.getNotify().getInterval());
            threadPoolNotifyAlarm.setReceives(executorProperties.receives());
            GlobalNotifyAlarmManage.put(executorProperties.getThreadPoolId(), threadPoolNotifyAlarm);
        });
    }
}