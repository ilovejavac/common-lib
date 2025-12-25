package com.dev.lib.cloud.filter;

import com.alibaba.fastjson2.JSON;
import com.dev.lib.security.util.SecurityContextHolder;
import com.dev.lib.security.util.UserDetails;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.*;

@Activate(group = CommonConstants.PROVIDER)
public class UserContextProviderFilter implements Filter {

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {

        try {
            UserDetails user = JSON.parseObject(
                    RpcContext.getServerAttachment().getAttachment("user"),
                    UserDetails.class
            );

            if (user != null) {
                // 恢复用户上下文，B 服务就能直接用 SecurityContextHolder.get()
                SecurityContextHolder.set(user);
            }
            return invoker.invoke(invocation);
        } finally {
            SecurityContextHolder.clear();
        }
    }

}