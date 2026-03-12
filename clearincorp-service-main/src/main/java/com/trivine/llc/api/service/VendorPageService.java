package com.trivine.llc.api.service;

import com.trivine.llc.api.constants.RoleEnum;

import com.trivine.llc.api.constants.ServiceConstants;
import com.trivine.llc.api.dto.UserCompanyDto;
import com.trivine.llc.api.dto.VendorPageDto;
import com.trivine.llc.api.entity.LoginUser;
import com.trivine.llc.api.entity.UserCompany;
import com.trivine.llc.api.exception.ResourceNotFoundException;
import com.trivine.llc.api.mapper.LoginUserMapper;
import com.trivine.llc.api.mapper.UserCompanyMapper;
import com.trivine.llc.api.repository.LoginUserRepository;
import com.trivine.llc.api.repository.UserCompanyRepository;
import com.trivine.llc.api.service.utility.SendEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.endpoints.internal.Value;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class VendorPageService {

    private final LoginUserMapper loginUserMapper;
    private final LoginUserRepository loginUserRepository;
    private final UserCompanyMapper userCompanyMapper;
    private final UserCompanyRepository userCompanyRepository;
    private final UserCompanyService userCompanyService;
    private final SendEmailService sendEmailService;
    private final EmailTokenService emailTokenService;

    private final Long VendorId = RoleEnum.Vendor.getRoleId();

    public List<VendorPageDto> findByLoginUser() {
        log.info("Fetching all active vendors...");

        List<LoginUser> vendors = loginUserRepository.findByDeletedFalseAndRoleId_Id(VendorId);
        List<VendorPageDto> result = new ArrayList<>();

        for (LoginUser user : vendors) {
            VendorPageDto dto = new VendorPageDto();
            dto.setLoginUserDto(loginUserMapper.toDto(user));
            dto.setUserCompanyDto(userCompanyMapper.toDto(user.getUserCompanyId()));
            result.add(dto);
        }

        return result;
    }


    @Transactional
    public VendorPageDto save(VendorPageDto dto) {
        log.info("Saving new Vendor: {}", dto.getLoginUserDto().getEmail());

        if (!dto.getLoginUserDto().getRoleId().getId().equals(VendorId)) {
            throw new IllegalStateException("Only vendors can be created");
        }

        // Save UserCompany first
        UserCompanyDto savedCompanyDto = userCompanyService.save(dto.getUserCompanyDto());
        dto.getLoginUserDto().setUserCompanyId(savedCompanyDto);

        LoginUser loginUser = loginUserMapper.toEntity(dto.getLoginUserDto());
        loginUserRepository.save(loginUser);

        Boolean otpSent= sendEmailService.sendVendorWelcomeEmail(ServiceConstants.FROM_EMAIL, dto.getLoginUserDto().getEmail(), ServiceConstants.VENDOR_WELCOME_SUBJECT, dto.getLoginUserDto().getFirstName());
        if (!otpSent) {
            log.warn("Failed to send OTP email to: {}", dto.getLoginUserDto().getEmail());
        }
        return dto;
    }

    @Transactional
    public VendorPageDto update(VendorPageDto dto) {
        log.info("Updating Vendor ID: {}", dto.getLoginUserDto().getLoginUserId());

        if (!dto.getLoginUserDto().getRoleId().getId().equals(VendorId)) {
            throw new IllegalStateException("Only vendors can be updated");
        }

        LoginUser existing = loginUserRepository.findById(dto.getLoginUserDto().getLoginUserId())
                .orElseThrow(() -> new ResourceNotFoundException("SuperFiler not found with ID: " + dto.getLoginUserDto().getLoginUserId()));

        String emailToDelete=null;
        if(!dto.getLoginUserDto().getEmail().equals(existing.getEmail())){
            emailToDelete=existing.getEmail();
        }

        LoginUser loginUser = loginUserMapper.toEntity(dto.getLoginUserDto());
        UserCompany userCompany = userCompanyMapper.toEntity(dto.getUserCompanyDto());
            loginUserRepository.save(loginUser);
            userCompanyRepository.save(userCompany);
        if(emailToDelete!=null){
            emailTokenService.deleteUser(emailToDelete);
        }
        return dto;
    }
    @Transactional
    public void deleteById(Long loginUserId) {
        log.info("Deleting Vendor ID: {}", loginUserId);

        LoginUser user = loginUserRepository.findById(loginUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found"));

        if (!user.getRoleId().getId().equals(VendorId)) {
            throw new IllegalStateException("Only vendors can be deleted");
        }

        user.setDeleted(true);

        UserCompany userCompany = userCompanyRepository.findById(user.getUserCompanyId().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Company for vendor not found"));

        userCompany.setIsDeleted(true);

        loginUserRepository.save(user);
        userCompanyRepository.save(userCompany);
    }


}

