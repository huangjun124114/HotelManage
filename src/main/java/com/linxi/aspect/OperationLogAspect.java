package com.linxi.aspect;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.linxi.annotation.OperationLog;
import com.linxi.entity.SysOperationLog;
import com.linxi.entity.SysUser;
import com.linxi.mapper.SysOperationLogMapper;
import com.linxi.mapper.SysUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Date;

/**
 * 操作日志切面
 * 自动拦截标注了 @OperationLog 的方法，记录操作日志到数据库
 */
@Slf4j
@Aspect
@Component
public class OperationLogAspect {

    @Autowired
    private SysOperationLogMapper sysOperationLogMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Around("@annotation(com.linxi.annotation.OperationLog)")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        // 执行目标方法
        long startTime = System.currentTimeMillis();
        Object result = null;
        Exception exception = null;
        try {
            result = point.proceed();
            return result;
        } catch (Exception e) {
            exception = e;
            throw e;
        } finally {
            try {
                saveLog(point, startTime, exception);
            } catch (Exception e) {
                log.error("保存操作日志失败", e);
            }
        }
    }

    private void saveLog(ProceedingJoinPoint point, long startTime, Exception exception) {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        OperationLog annotation = method.getAnnotation(OperationLog.class);

        SysOperationLog log = new SysOperationLog();

        // 注解信息
        log.setModuleName(annotation.module());
        log.setOperationType(annotation.type());
        log.setRequestUrl(annotation.description());

        // 请求信息
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            log.setRequestMethod(request.getMethod());
            log.setRequestUrl(request.getRequestURI());
            log.setIpAddress(getIpAddress(request));
        }

        // 用户信息
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            String username = auth.getName();
            log.setUsername(username);
            // 查询用户真实姓名
            try {
                SysUser user = sysUserMapper.selectOne(
                        new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username)
                );
                if (user != null) {
                    log.setUserId(user.getId());
                    log.setRealName(user.getRealName());
                }
            } catch (Exception e) {
                // 忽略查询失败
            }
        } else if ("LOGIN".equals(annotation.type())) {
            // 登录操作：从方法参数中获取用户名
            Object[] args = point.getArgs();
            if (args != null && args.length > 0 && args[0] != null) {
                try {
                    // 尝试通过反射获取username字段
                    Class<?> clazz = args[0].getClass();
                    java.lang.reflect.Field usernameField = clazz.getDeclaredField("username");
                    usernameField.setAccessible(true);
                    String loginUsername = (String) usernameField.get(args[0]);
                    if (loginUsername != null) {
                        log.setUsername(loginUsername);
                        SysUser user = sysUserMapper.selectOne(
                                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, loginUsername)
                        );
                        if (user != null) {
                            log.setUserId(user.getId());
                            log.setRealName(user.getRealName());
                        }
                    }
                } catch (Exception e) {
                    // 忽略反射失败
                }
            }
        }

        // 结果状态
        if (exception != null) {
            log.setResultStatus(0);
            String errMsg = exception.getMessage();
            log.setErrorMessage(errMsg != null && errMsg.length() > 500 ? errMsg.substring(0, 500) : errMsg);
        } else {
            log.setResultStatus(1);
        }

        log.setCreateTime(DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
        sysOperationLogMapper.insert(log);
    }

    private String getIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多代理时取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
