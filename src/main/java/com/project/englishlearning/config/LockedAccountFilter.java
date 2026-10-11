package com.project.englishlearning.config;
import com.project.englishlearning.repository.UserRepository;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import java.io.IOException;

/** Re-check persisted status and role so existing sessions cannot retain revoked permissions. */
public class LockedAccountFilter extends OncePerRequestFilter {
    private final UserRepository users;
    public LockedAccountFilter(UserRepository users) { this.users=users; }
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
        var authentication=SecurityContextHolder.getContext().getAuthentication();
        if(authentication!=null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken)) {
            var user=users.findByUsername(authentication.getName());
            boolean roleChanged=user.isPresent() && user.get().getRole()!=null && authentication.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals(user.get().getRole()));
            if(user.isEmpty() || "LOCKED".equalsIgnoreCase(user.get().getStatus()) || roleChanged) {
                new SecurityContextLogoutHandler().logout(request,response,authentication);
                response.setHeader("Cache-Control","no-store");
                if("GET".equals(request.getMethod()) && !request.getRequestURI().startsWith(request.getContextPath()+"/api/")) response.sendRedirect(request.getContextPath()+(roleChanged ? "/login?permissionsChanged" : "/login?locked"));
                else response.sendError(HttpServletResponse.SC_FORBIDDEN,roleChanged ? "Quyền truy cập đã thay đổi. Vui lòng đăng nhập lại." : "Tài khoản đã bị khóa.");
                return;
            }
        }
        chain.doFilter(request,response);
    }
}
