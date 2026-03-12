package com.trivine.llc.api.service;

import com.trivine.llc.api.constants.ServiceConstants;
import com.trivine.llc.api.entity.Company;
import com.trivine.llc.api.entity.LoginUser;
import com.trivine.llc.api.repository.CompanyRepository;
import com.trivine.llc.api.service.utility.SendEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailSchedulerService {

    private static final String FROM_EMAIL = ServiceConstants.FROM_EMAIL;
    private static final String EVERYDAY_SUBJECT = ServiceConstants.EVERYDAY_SUBJECT;
    private static final String EVERY_3DAYS_SUBJECT = ServiceConstants.EVERY_3DAYS_SUBJECT;
    private static final String EVERY_7DAYS_SUBJECT = ServiceConstants.EVERY_7DAYS_SUBJECT;
    private static final String EVERY1_4DAYs_SUBJECT = ServiceConstants.EVERY_14DAYS_SUBJECT;
    private final CompanyRepository companyRepository;
    private final SendEmailService sendEmailService;


    public void sendDailyEmail() {
        LocalDate cutoffDate = LocalDate.now().minusDays(1);
        LocalDateTime startOfDay = cutoffDate.atStartOfDay();
        LocalDateTime endOfDay = cutoffDate.plusDays(1).atStartOfDay();

        List<Company> companies =
                companyRepository.findCompaniesCreatedExactlyOnDateWithPendingSteps(startOfDay, endOfDay);


        if (companies.isEmpty()) {
            log.info("No companies found for 1-day reminder on {}.", cutoffDate);
        } else {
            // Proceed with sending emails

            for (Company company : companies) {
                LoginUser loginUser=company.getLoginUser();
                String email="";
                if (loginUser.getEmail() != null) {
                    email = loginUser.getEmail();
                }
                else {
                    continue;
                }

                // Safely extract user's first name
                String firstName = "User";
                if (loginUser.getFirstName() != null) {
                    firstName = company.getLoginUser().getFirstName();
                }

                // Send using the static ReminderEmail method
                Boolean success = sendEmailService.ReminderEmail(FROM_EMAIL, email, EVERYDAY_SUBJECT, firstName);

                if (!success) {
                    log.error("Failed to send 1 Day reminder email for companyId={}", company.getCompanyId());
                } else {
                    log.info("Reminder 1 Day email sent for companyId={}", company.getCompanyId());
                }
            }
        }

    }

    public void sendEvery3DaysEmail() {
        LocalDate cutoffDate = LocalDate.now().minusDays(3);
        LocalDateTime startOfDay = cutoffDate.atStartOfDay();
        LocalDateTime endOfDay = cutoffDate.plusDays(3).atStartOfDay();

        List<Company> companies =
                companyRepository.findCompaniesCreatedExactlyOnDateWithPendingSteps(startOfDay, endOfDay);


        if (companies.isEmpty()) {
            log.info("No companies found for 1-day reminder on {}.", cutoffDate);
        } else {
            // Proceed with sending emails

            for (Company company : companies) {
                LoginUser loginUser=company.getLoginUser();
                String email="";
                if (loginUser.getEmail() != null) {
                    email = loginUser.getEmail();
                }
                else {
                    continue;
                }

                // Safely extract user's first name
                String firstName = "User";
                if (loginUser.getFirstName() != null) {
                    firstName = company.getLoginUser().getFirstName();
                }

                // Send using the static ReminderEmail method
                Boolean success = sendEmailService.sendPendingSetupReminderEmail(FROM_EMAIL, email, EVERY_3DAYS_SUBJECT, firstName);

                if (!success) {
                    log.error("Failed to send 3 Day reminder email for companyId={}", company.getCompanyId());
                } else {
                    log.info("Reminder email 3 Day sent for companyId={}", company.getCompanyId());
                }
            }
        }
    }

    public void sendWeeklyEmail() {
        LocalDate cutoffDate = LocalDate.now().minusDays(7);
        LocalDateTime startOfDay = cutoffDate.atStartOfDay();
        LocalDateTime endOfDay = cutoffDate.plusDays(7).atStartOfDay();

        List<Company> companies =
                companyRepository.findCompaniesCreatedExactlyOnDateWithPendingSteps(startOfDay, endOfDay);


        if (companies.isEmpty()) {
            log.info("No companies found for 1-day reminder on {}.", cutoffDate);
        } else {
            // Proceed with sending emails

            for (Company company : companies) {
                LoginUser loginUser=company.getLoginUser();
                String email="";
                if (loginUser.getEmail() != null) {
                    email = loginUser.getEmail();
                }
                else {
                    continue;
                }

                // Safely extract user's first name
                String firstName = "User";
                if (loginUser.getFirstName() != null) {
                    firstName = company.getLoginUser().getFirstName();
                }

                // Send using the static ReminderEmail method
                Boolean success = sendEmailService.sendLimitedTimeReminderEmail(FROM_EMAIL, email, EVERY_7DAYS_SUBJECT, firstName);

                if (!success) {
                    log.error("Failed to send 7 Day reminder email for companyId={}", company.getCompanyId());
                } else {
                    log.info("Reminder email 7 Day sent for companyId={}", company.getCompanyId());
                }
            }
        }
    }

    public void sendBiWeeklyEmail() {
        LocalDate cutoffDate = LocalDate.now().minusDays(14);
        LocalDateTime startOfDay = cutoffDate.atStartOfDay();
        LocalDateTime endOfDay = cutoffDate.plusDays(14).atStartOfDay();

        List<Company> companies =
                companyRepository.findCompaniesCreatedExactlyOnDateWithPendingSteps(startOfDay, endOfDay);


        if (companies.isEmpty()) {
            log.info("No companies found for 1-day reminder on {}.", cutoffDate);
        } else {
            // Proceed with sending emails

            for (Company company : companies) {
                LoginUser loginUser=company.getLoginUser();
                String email="";
                if (loginUser.getEmail() != null) {
                    email = loginUser.getEmail();
                }
                else {
                    continue;
                }

                // Safely extract user's first name
                String firstName = "User";
                if (loginUser.getFirstName() != null) {
                    firstName = company.getLoginUser().getFirstName();
                }

                // Send using the static ReminderEmail method
                Boolean success = sendEmailService.sendFollowUpReminderEmail(FROM_EMAIL, email, EVERY1_4DAYs_SUBJECT, firstName);

                if (!success) {
                    log.error("Failed to send 14 Day reminder email for companyId={}", company.getCompanyId());
                } else {
                    log.info("Reminder email 14 Day sent for companyId={}", company.getCompanyId());
                }
            }
        }
    }
}





