package com.trivine.llc.api.service;

import com.trivine.llc.api.constants.RoleEnum;
import com.trivine.llc.api.constants.ServiceConstants;
import com.trivine.llc.api.dto.LoginUserDto;
import com.trivine.llc.api.dto.ProfileUpdateDto;
import com.trivine.llc.api.entity.LoginUser;
import com.trivine.llc.api.entity.UserCompany;
import com.trivine.llc.api.exception.ResourceNotFoundException;
import com.trivine.llc.api.mapper.LoginUserMapper;
import com.trivine.llc.api.repository.LoginUserRepository;
import com.trivine.llc.api.service.utility.SendEmailService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class LoginUserService {

    private final LoginUserRepository loginUserRepository;
    private final LoginUserMapper loginUserMapper;
    private final Long ConsumerId = RoleEnum.Consumer.getRoleId();
    private final Long SuperFilerId = RoleEnum.SuperFiler.getRoleId();
    private final Long adminCompanyId = ServiceConstants.ADMIN_COMPANY_ID;
    private final SendEmailService sendEmailService;
    private final S3Service s3Service;
    private final EmailTokenService emailTokenService;


    public List<LoginUserDto> findAllConsumers(Long creatorId) {
        log.info("Fetching all Consumers for Vendor ID: {}", creatorId);

        LoginUser vendor = loginUserRepository.findById(creatorId)
                .orElseThrow(() -> new ResourceNotFoundException("LoginUser not found with ID: " + creatorId));

        return loginUserRepository.findByDeletedFalseAndUserCompanyId_Id(vendor.getUserCompanyId().getId())
                .stream()
                .filter(u -> u.getRoleId().getId().equals(ConsumerId))
                .map(loginUserMapper::toDto)
                .toList();
    }


    @Transactional
    public LoginUserDto saveConsumer(LoginUserDto dto, Long creatorId) {
        log.info("Saving Consumer by creator ID: {}", creatorId);

        if (!dto.getRoleId().getId().equals(ConsumerId)) {
            throw new IllegalStateException("Only users with the Consumer role can be created");
        }

        LoginUser creator = loginUserRepository.findById(creatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Creator not found with ID: " + creatorId));

        if (loginUserRepository.existsByEmail(dto.getEmail())) {
            throw new DataIntegrityViolationException("Email already exists");
        }

        LoginUser user = loginUserMapper.toEntity(dto);
        user.setCreatedOn(LocalDateTime.now());
        user.setCreatedBy(creator.getFirstName());
        user.setIsActive(true);
        user.setDeleted(false);
        user.setUserCompanyId(creator.getUserCompanyId());
        Boolean otpSent= sendEmailService.sendConsumerWelcomeEmail(ServiceConstants.FROM_EMAIL, dto.getEmail(), ServiceConstants.CONSUMER_WELCOME_SUBJECT, dto.getFirstName());
        if (!otpSent) {
            log.warn("Failed to send welcome email for new consumer");
            throw new IllegalStateException("Email Not Found or Failed to Send");
        }

        return loginUserMapper.toDto(loginUserRepository.save(user));
    }
    @Transactional
    public void updateConsumer(LoginUserDto dto) {
        log.info("Updating Consumer ID: {}", dto.getLoginUserId());

        if (!dto.getRoleId().getId().equals(ConsumerId)) {
            throw new IllegalStateException("Only Consumers can be updated");
        }

        LoginUser entity = loginUserRepository.findById(dto.getLoginUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + dto.getLoginUserId()));

        String emailToDelete=null;
        if(!dto.getEmail().equals(entity.getEmail())){
            emailToDelete=entity.getEmail();
        }
        entity.setFirstName(dto.getFirstName());
        entity.setLastName(dto.getLastName());
        entity.setEmail(dto.getEmail());
        entity.setPhoneNumber(dto.getPhoneNumber());
        LoginUser loginUser =loginUserRepository.save(entity);
        if(emailToDelete!=null){
            emailTokenService.deleteUser(emailToDelete);
        }
    }

    public void deleteConsumer(Long id) {
        log.info("Deleting Consumer ID: {}", id);

        LoginUser user = loginUserRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LoginUser not found with ID: " + id));

        if (!user.getRoleId().getId().equals(ConsumerId)) {
            throw new IllegalStateException("Only Consumers can be deleted");
        }

        user.setDeleted(true);
        loginUserRepository.save(user);
    }


    @Transactional
    public LoginUserDto saveSuperFiler(LoginUserDto dto) {
        log.info("Saving new SuperFiler");

        if (!dto.getRoleId().getId().equals(SuperFilerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only SuperFilers can be created");
        }

        if (loginUserRepository.existsByEmail(dto.getEmail())) {
            throw new DataIntegrityViolationException("A SuperFiler with that email already exists");
        }

        LoginUser user = loginUserMapper.toEntity(dto);
        user.setCreatedOn(LocalDateTime.now());
        user.setCreatedBy("admin");
        user.setIsActive(true);
        user.setDeleted(false);
        user.setUserCompanyId(new UserCompany(adminCompanyId));
        Boolean otpSent= sendEmailService.sendSuperFilerWelcomeEmail(ServiceConstants.FROM_EMAIL, dto.getEmail(), ServiceConstants.SUPER_FILER_WELCOME_SUBJECT, dto.getFirstName());
        if (!otpSent) {
            log.warn("Failed to send welcome email for new SuperFiler");
            throw new IllegalStateException("Email Not Found or Failed to Send");
        }
        return loginUserMapper.toDto(loginUserRepository.save(user));
    }

    @Transactional
    public LoginUserDto updateSuperFiler(LoginUserDto dto) {
        log.info("Updating SuperFiler ID: {}", dto.getLoginUserId());

        if (!dto.getRoleId().getId().equals(SuperFilerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only SuperFilers can be updated");
        }

        LoginUser existing = loginUserRepository.findById(dto.getLoginUserId())
                .orElseThrow(() -> new ResourceNotFoundException("SuperFiler not found with ID: " + dto.getLoginUserId()));

        // Check for duplicate email if updated
        if (!existing.getEmail().equals(dto.getEmail()) && loginUserRepository.existsByEmail(dto.getEmail())) {
            throw new DataIntegrityViolationException("A SuperFiler with that email already exists");
        }
        String emailToDelete=null;
        if(!dto.getEmail().equals(existing.getEmail())){
            emailToDelete=existing.getEmail();
        }
        // Update relevant fields
        existing.setFirstName(dto.getFirstName());
        existing.setLastName(dto.getLastName());
        existing.setEmail(dto.getEmail());
        existing.setPhoneNumber(dto.getPhoneNumber());
        existing.setUserCompanyId(new UserCompany(adminCompanyId));

        LoginUserDto loginUserDto=loginUserMapper.toDto(loginUserRepository.save(existing));
        if(emailToDelete!=null){
            emailTokenService.deleteUser(emailToDelete);
        }
        return loginUserDto;
    }

    @Transactional
    public void deleteSuperFiler(Long id) {
        log.info("Deleting SuperFiler ID: {}", id);

        LoginUser user = loginUserRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SuperFiler not found with ID: " + id));

        if (!user.getRoleId().getId().equals(SuperFilerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only SuperFilers can be deleted");
        }

        user.setDeleted(true);
        loginUserRepository.save(user);
    }

    public List<LoginUserDto> findAllSuperFiler() {
        log.info("Fetching all SuperFilers for admin company");

        List<LoginUser> users = loginUserRepository.findByDeletedFalseAndUserCompanyId_Id(adminCompanyId);
        List<LoginUser> superFilers = users.stream()
                .filter(u -> u.getRoleId().getId().equals(SuperFilerId))
                .toList();

        return loginUserMapper.toDtoList(superFilers);
    }



        @Transactional
        public ProfileUpdateDto updateProfile(ProfileUpdateDto profileUpdateDto) throws IOException {

            LoginUser loginUser = loginUserRepository.findById(profileUpdateDto.getLoginUserId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "LoginUser not found"));

            // Check if a new profile image was provided as a Base64 string
            if (profileUpdateDto.getProfileImageUrl() != null && !profileUpdateDto.getProfileImageUrl().isEmpty()) {

                // Regex to extract the MIME type and the Base64 data
                Pattern pattern = Pattern.compile("^data:(.+);base64,(.*)$");
                Matcher matcher = pattern.matcher(profileUpdateDto.getProfileImageUrl());

                if (matcher.matches()) {
                    String contentType = matcher.group(1);
                    String base64Data = matcher.group(2);

                    if (!("image/jpeg".equals(contentType) || "image/png".equals(contentType))) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file type. Only JPG and PNG are allowed.");
                    }

                    // Decode the Base64 string to a byte array
                    byte[] imageBytes = Base64.getDecoder().decode(base64Data);

                    String fileKey = "User_Document/" + loginUser.getLoginUserId() + "/" + "profileImg";
                    s3Service.uploadFileWithContentType(fileKey, imageBytes, contentType);
                    String url =s3Service.generatePresignedGetUrl(fileKey);
                    profileUpdateDto.setProfileImageUrl(url);
                } else {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Base64 format.");
                }
            }

            // Update other profile fields from the DTO
            if (profileUpdateDto.getFirstName() != null && !profileUpdateDto.getFirstName().isEmpty()) {
                loginUser.setFirstName(profileUpdateDto.getFirstName());
            }
            if (profileUpdateDto.getLastName() != null && !profileUpdateDto.getLastName().isEmpty()) {
                loginUser.setLastName(profileUpdateDto.getLastName());
            }
            if (profileUpdateDto.getPhoneNumber() != null && !profileUpdateDto.getPhoneNumber().isEmpty()) {
                loginUser.setPhoneNumber(profileUpdateDto.getPhoneNumber());
            }


            // Save the changes to the database
            loginUserRepository.save(loginUser);

            // It is recommended to return the saved entity or a response DTO
            return profileUpdateDto;
        }
    }

