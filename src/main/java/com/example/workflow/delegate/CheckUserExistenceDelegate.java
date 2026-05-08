package com.example.workflow.delegate; // Sửa lại package cho đúng dự án của bạn

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
        String email = (String) execution.getVariable("email");

        boolean isUsernameExist = userRepository.existsByUsername(username);
        boolean isEmailExist = userRepository.existsByEmail(email);

        // Nếu 1 trong 2 cái bị trùng, thì coi như User đã tồn tại
        boolean isExisted = isUsernameExist || isEmailExist;
        execution.setVariable("userExisted", isExisted);

        if (isExisted) {
            String errorMsg = "";
            if (isUsernameExist) {
                errorMsg = "Tên đăng nhập '" + username + "' đã tồn tại!";
                System.out.println(">>> Camunda: " + errorMsg);
            } else if (isEmailExist) {
                errorMsg = "Email '" + email + "' đã được sử dụng!";
                System.out.println(">>> Camunda: " + errorMsg);
            }
            // Lưu lý do lỗi vào Camunda để văng ra cho Frontend biết
            execution.setVariable("errorMessage", errorMsg);
        } else {
            System.out.println(">>> Camunda: Thông tin Username và Email đều HỢP LỆ (Chưa có).");
        }
    }
}