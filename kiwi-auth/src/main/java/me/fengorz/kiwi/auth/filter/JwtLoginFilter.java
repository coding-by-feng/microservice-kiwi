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

package me.fengorz.kiwi.auth.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.fengorz.kiwi.common.api.entity.EnhancerUser;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;

/**
 * @Description TODO @Author Kason Zhan @Date 2021/11/20 12:58 PM
 */
public class JwtLoginFilter extends AbstractAuthenticationProcessingFilter {

    protected JwtLoginFilter(String defaultFilterProcessesUrl) {
        super(defaultFilterProcessesUrl);
    }

    protected JwtLoginFilter(RequestMatcher requiresAuthenticationRequestMatcher) {
        super(requiresAuthenticationRequestMatcher);
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
        throws AuthenticationException, IOException, ServletException {
        EnhancerUser enhancerUser = new ObjectMapper().readValue(request.getInputStream(), EnhancerUser.class);
        return getAuthenticationManager().authenticate(
            new UsernamePasswordAuthenticationToken(enhancerUser.getUsername(), enhancerUser.getPassword()));
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain,
        Authentication authResult) throws IOException, ServletException {
        Collection<? extends GrantedAuthority> authorities = authResult.getAuthorities();
        StringBuffer as = new StringBuffer();
        for (GrantedAuthority authority : authorities) {
            as.append(authority.getAuthority()).append(",");
        }
        // String jwt = Jwts.builder()
        // .claim("authorities", as)//配置用户角色
        // .setSubject(authResult.getName())
        // .setExpiration(new Date(System.currentTimeMillis() + 10 * 60 * 1000))
        // .signWith(SignatureAlgorithm.HS512, "sang@123")
        // .compact();
        // resp.setContentType("application/json;charset=utf-8");
        // PrintWriter out = resp.getWriter();
        // out.write(new ObjectMapper().writeValueAsString(jwt));
        // out.flush();
        // out.close();
    }
}
