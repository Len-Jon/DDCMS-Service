package com.webank.databrain.filter;

import com.webank.databrain.bo.LoginInfoBo;
import com.webank.databrain.dao.entity.AccountInfoEntity;
import com.webank.databrain.dao.mapper.AccountInfoMapper;
import com.webank.databrain.enums.AccountType;
import com.webank.databrain.handler.JwtTokenHandler;
import io.jsonwebtoken.Claims;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtTokenHandler handler;

    @Autowired
    private AccountInfoMapper accountInfoMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        // get token
        String token = request.getHeader("token");
        if(StringUtils.isEmpty(token)){
            // 放行
            filterChain.doFilter(request, response);
            return;
        }

        // parse token
        String did;
        try {
            Claims claims = handler.parseToken(token);
            did = claims.getSubject();
        }catch (Exception e){
            throw new RuntimeException("token非法.");
        }

        // get account info
        AccountInfoEntity entity = accountInfoMapper.selectByDid(did);
        if(null == entity){
            throw new RuntimeException("用户不存在.");
        }

        List<String> permissionList = new ArrayList<>();

        // 需要从数据库的权限表中查询，然后封装
        permissionList.add(AccountType.getAccountType(entity.getAccountType()).getRoleName());

        LoginInfoBo bo = new LoginInfoBo(entity, permissionList);
        //save to ContextHolder
        //获取权限信息封装到Authentication
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(bo, null, bo.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);

        filterChain.doFilter(request, response);
    }
}
