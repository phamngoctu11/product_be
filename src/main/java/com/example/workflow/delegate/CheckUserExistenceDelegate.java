package com.example.workflow.delegate;

import com.example.workflow.exception.AppException;
import com.example.workflow.exception.ConstantErrorCode;
import com.example.workflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component("checkUserExistenceDelegate")
@RequiredArgsConstructor
public class CheckUserExistenceDelegate implements JavaDelegate {
    private final UserRepository userRepository;

    @Override
    public void execute(DelegateExecution execution) {
        String username = (String) execution.getVariable("username");
        String email = (String) execution.getVariable("email");
        String phone = (String) execution.getVariable("phone");
        execution.setVariable("userExisted", validateUniqueUserIdentity(username, email, phone));
    }

    private boolean validateUniqueUserIdentity(String username, String email, String phone) {
        boolean exist = false;
        if (userRepository.existsByUsername(username)) {
            exist = true;
        }
        if (StringUtils.hasText(email) && userRepository.existsByEmail(email)) {
            exist = true;
       }
        if (userRepository.existsByPhone(phone)) {
            exist = true;
        }
        return exist;
}
}
