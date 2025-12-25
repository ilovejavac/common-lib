package com.dev.lib.cloud.filter;

import com.dev.lib.entity.id.IDWorker;
import com.dev.lib.entity.id.IntEncoder;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.*;
import org.slf4j.MDC;

@Activate(group = CommonConstants.PROVIDER)
public class DubboTraceProviderFilter implements Filter {

    private static final String TRACE_ID = "trace_id";

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        try {
            // 从 RPC 上下文获取上游传递的 traceId
            String traceId = RpcContext.getServerAttachment().getAttachment(TRACE_ID);
            
            // 如果没有（比如直接调用），则生成新的
            if (traceId == null || traceId.isEmpty()) {
                traceId = IntEncoder.encode52(IDWorker.nextID());
            }
            
            MDC.put(TRACE_ID, traceId);
            return invoker.invoke(invocation);
        } finally {
            MDC.clear();
        }
    }
}