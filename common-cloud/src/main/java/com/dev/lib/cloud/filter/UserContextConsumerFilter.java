package com.dev.lib.cloud.filter;

import com.alibaba.fastjson2.JSON;
import com.dev.lib.security.util.SecurityContextHolder;
import com.dev.lib.security.util.UserDetails;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.*;

@Activate(group = CommonConstants.CONSUMER)
public class UserContextConsumerFilter implements Filter {

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        // 把当前登录用户信息传递给下游服务
        if (SecurityContextHolder.isLogin()) {
            UserDetails user = SecurityContextHolder.get();
            RpcContext.getClientAttachment().setAttachment("user", JSON.toJSON(user));
        }
        return invoker.invoke(invocation);
    }

}