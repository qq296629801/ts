package com.ts.platform.admin;

import com.ts.platform.common.BusinessException;
import com.ts.platform.user.User;
import com.ts.platform.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminUserService {

    private final UserRepository userRepository;

    public AdminUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Map<String, Object> list(int page, int size) {
        Page<User> result = userRepository.findAll(
                PageRequest.of(Math.max(page - 1, 0), size, Sort.by(Sort.Direction.DESC, "createdAt")));
        List<Map<String, Object>> items = result.getContent().stream().map(u -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", u.getId());
            m.put("phone", u.getPhone() != null ? u.getPhone() : "");
            m.put("nickname", u.getNickname());
            m.put("role", u.getRole());
            m.put("status", u.getStatus());
            m.put("createdAt", u.getCreatedAt().toString());
            return m;
        }).toList();
        return Map.of("items", items, "total", result.getTotalElements());
    }

    @Transactional
    public void setStatus(Long userId, int status) {
        User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException(404, "用户不存在"));
        user.setStatus(status);
        userRepository.save(user);
    }
}
