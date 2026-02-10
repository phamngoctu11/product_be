package com.example.workflow.delegate;

import com.example.workflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("checkUserExistenceDelegate")
@RequiredArgsConstructor
public class CheckUserExistenceDelegate implements JavaDelegate {

    private final UserRepository userRepository;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String username = (String) execution.getVariable("username");
        boolean isExisted = userRepository.existsByUsername(username);
        execution.setVariable("userExisted", isExisted);
        if (isExisted) {
            System.out.println(">>> Camunda: Kiểm tra Username '" + username + "' -> ĐÃ TỒN TẠI.");
        } else {
            System.out.println(">>> Camunda: Kiểm tra Username '" + username + "' -> HỢP LỆ (Chưa có).");
        }
    }
}