package com.dev.lib.mq

import com.dev.lib.mq.reliability.ReliabilityConfig

abstract class AbstractMQTemplate : MQTemplate {

    @JvmField
    protected var reliabilityConfig: ReliabilityConfig = ReliabilityConfig.DEFAULT

    override fun setReliabilityConfig(config: ReliabilityConfig) {
        this.reliabilityConfig = config
    }
}
