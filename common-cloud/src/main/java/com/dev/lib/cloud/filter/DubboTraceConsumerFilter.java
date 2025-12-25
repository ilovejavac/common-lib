package com.dev.lib.cloud.filter;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.*;
import org.slf4j.MDC;

@Activate(group = CommonConstants.CONSUMER)
public class DubboTraceConsumerFilter implements Filter {

    private static final String TRACE_ID = "trace_id";

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        // 从 MDC 获取当前的 traceId，放入 RPC 上下文传递给下游
        String traceId = MDC.get(TRACE_ID);
        if (traceId != null) {
            RpcContext.getClientAttachment().setAttachment(TRACE_ID, traceId);
        }
        return invoker.invoke(invocation);
    }
}