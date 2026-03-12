package com.trivine.llc.api.service.utility;

import jakarta.mail.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import jakarta.mail.internet.MimeMessage;

import java.io.UnsupportedEncodingException;

@Slf4j
@Service
public class SendEmailService {

    @Autowired
    private JavaMailSender mailSender;

    /**
     * Escapes HTML special characters to prevent XSS attacks in email templates.
     * All user-provided data must be escaped before inserting into HTML templates.
     */
    private String escapeHtml(String input) {
        if (input == null) return "";
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }

    public Boolean sendEmail(String fromEmail, String toEmail, String subject, String bodyHtml) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, /* multipart */ true, "UTF-8");

            // Headers
            String fromName = "ClearInCorp";
            helper.setFrom(fromEmail,fromName);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(bodyHtml, true); // true => HTML

            // Ensure Date/Message-ID are set before sending
            message.saveChanges();

            // 1) Send via SMTP
            mailSender.send(message);
            return true;

        } catch (MailException e) {
            log.error("Error sending email: {}", e.getMessage(), e);
            return false;
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public  Boolean sendConsumerWelcomeEmail(String fromEmail, String toEmail, String subject, String firstName) {
                String emailTemplate =
                "<!DOCTYPE html>" +
                        "<html lang='en'>" +
                        "<head>" +
                        "<meta charset='UTF-8' />" +
                        "<meta name='viewport' content='width=device-width' />" +
                        "<title>ClearInCorp - Welcome Email</title>" +
                        "<style>" +
                        "body { font-family: Arial, sans-serif; background-color: #ffffff; margin: 0; padding: 0; }" +
                        ".container { max-width: 650px; padding: 20px;" +
                        " box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08); color: #333; }" +
                        "h2 { color: #0d47a1; margin-top: 0; }" +
                        "p { font-size: 15px; line-height: 1.6; }" +
                        "ul { padding-left: 20px; font-size: 15px; }" +
                        "li { margin-bottom: 8px; }" +
                        "ol { padding-left: 20px; font-size: 15px; }" +
                        "strong { font-weight: bold; }" +
                        ".button { display: inline-block; background: #0d47a1; color: #fff; padding: 12px 20px; border-radius: 6px;" +
                        " text-decoration: none; font-weight: bold; margin-top: 20px; }" +
                        ".footer { margin-top: 30px; font-size: 13px; color: #777; border-top: 1px solid #ddd; padding-top: 15px; }" +
                        "a { color: #0d47a1; text-decoration: none; }" +
                        "p > a.button  { color: #FFFFFF; text-decoration: none; }" +
                        "</style>" +
                        "</head>" +
                        "<body>" +
                        "<div class='container'>" +
                        "<p>Hello there,</p>" +
                        "<p>Congratulations on taking the leap! By signing up with <strong>ClearInCorp</strong>, you've made a smart move toward building something meaningful.</p>" +
                        "<p>Starting a business can feel overwhelming, but here's what we know works:</p>" +
                        "<h3>Why Getting It Right Matters:</h3>" +
                        "<ul>" +
                        "<li>Most new ventures face compliance headaches that could have been avoided with proper setup</li>" +
                        "<li>LLCs create a protective barrier between your personal finances and business risks</li>" +
                        "<li>Properly registered businesses have significantly better long-term success rates</li>" +
                        "<li>Our streamlined process eliminates the guesswork and red tape</li>" +
                        "</ul>" +
                        "<h3>Here's How We Make It Simple:</h3>" +
                        "<ol>" +
                        "<li><strong>Select Your Structure</strong> – We'll help you choose what works best for your goals</li>" +
                        "<li><strong>Share Your Vision</strong> – Tell us about your business so we can customize everything perfectly</li>" +
                        "<li><strong>Relax While We Work</strong> – We handle all the paperwork, filing, and compliance details</li>" +
                        "</ol>" +
                        "<p><a class='button' href='https://clearincorp.com'>Get Started Today →</a></p>" +
                        "<p>Questions? Concerns? Brilliant ideas you want to bounce off someone? Our team is standing by and genuinely excited to help you succeed.</p>" +
                        "<p>Here's to building something amazing,<br><strong>The ClearInCorp Team</strong></p>" +
                        "<div class='footer'>" +
                        "<p><em>Need support?</em> Drop us a line at " +
                        "<a href='mailto:support@clearincorp.com'>support@clearincorp.com</a><br>" +
                        "<em>Learn more:</em> Visit us at " +
                        "<a href='https://clearincorp.com'>clearincorp.com</a></p>" +
                        "</div>" +
                        "</div>" +
                        "</body>" +
                        "</html>";


        String bodyHtml = String.format(emailTemplate, firstName);

        return sendEmail(fromEmail, toEmail, subject, bodyHtml);
    }

    public Boolean sendVendorWelcomeEmail(String fromEmail, String toEmail, String subject, String firstName) {
        String emailTemplate =
                "<!DOCTYPE html>" +
                        "<html>" +
                        "<head>" +
                        "<style>" +
                        "body { font-family: Arial, sans-serif; margin: 0; padding: 0; background-color: #ffffff; }" +
                        ".container { max-width: 600px;padding: 20px;" +
                        " box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1); color: #333; }" +
                        "h2 { margin-top: 0; }" +
                        "p, li { font-size: 16px; line-height: 1.6; }" +
                        "ul { padding-left: 20px; }" +
                        ".button { display: inline-block; background: #0d47a1; color: #fff; padding: 12px 20px; border-radius: 6px;" +
                        " text-decoration: none; font-weight: bold; margin-top: 20px; }" +
                        ".muted { color: #667085; font-size: 14px; }" +
                        ".footer { margin-top: 30px; padding-top: 15px; border-top: 1px solid #ddd; font-size: 14px; color: #555; }" +
                        "a { color: #0d47a1; text-decoration: none; }" +
                        "a:hover { text-decoration: underline; }" +
                        "</style>" +
                        "</head>" +
                        "<body>" +
                        "<div class='container'>" +
                        "<p>Hi %s,</p>" +
                        "<p>You're set up as a <strong>Vendor</strong>. From here, you can manage your consumer accounts" +
                        " and guide their LLC filings smoothly.</p>" +
                        "<h3>What you can do:</h3>" +
                        "<ul>" +
                        "<li>Create and manage <strong>Consumer</strong> profiles assigned to you.</li>" +
                        "<li>Start and update company formation steps (RegForm flows) for your consumers.</li>" +
                        "<li>Upload and manage required documents; track review and filing statuses.</li>" +
                        "<li>View invoices and service details tied to your consumers.</li>" +
                        "<li>Collaborate with our filing team via notes and status updates.</li>" +
                        "</ul>" +
                        "<p class='muted'>Note: Vendor permissions are scoped to your assigned consumers and companies.</p>" +
                        "<a class='button' href='https://clearincorp.com'>Go to Vendor Dashboard</a>" +
                        "<div class='footer'>" +
                        "<p><strong>Questions?</strong> We’re here to help at " +
                        "<a href='mailto:support@clearincorp.com'>support@clearincorp.com</a></p>" +
                        "<p>Stay secure,<br><strong>The ClearInCorp Team</strong></p>" +
                        "<p><em>Need support?</em> Drop us a line at " +
                        "<a href='mailto:support@clearincorp.com'>support@clearincorp.com</a><br>" +
                        "<em>Learn more:</em> Visit us at " +
                        "<a href='https://clearincorp.com'>clearincorp.com</a></p>" +
                        "</div>" +
                        "</div>" +
                        "</body>" +
                        "</html>";
        String bodyHtml = String.format(emailTemplate, escapeHtml(firstName));
        return sendEmail(fromEmail, toEmail, subject, bodyHtml);
    }



    public Boolean sendSuperFilerWelcomeEmail(String fromEmail, String toEmail, String subject, String firstName) {
        String emailTemplate =
                "<!DOCTYPE html>" +
                        "<html>" +
                        "<head>" +
                        "<style>" +
                        "body { font-family: Arial, sans-serif; margin: 0; padding: 0; background-color: #ffffff; }" +
                        ".container { max-width: 600px;padding: 20px;" +
                        " box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1); color: #333; }" +
                        "h2 { margin-top: 0; }" +
                        "p, li { font-size: 16px; line-height: 1.6; }" +
                        "ul { padding-left: 20px; }" +
                        ".button { display: inline-block; background: #0d47a1; color: #fff; padding: 12px 20px; border-radius: 6px;" +
                        " text-decoration: none; font-weight: bold; margin-top: 20px; }" +
                        ".footer { margin-top: 30px; padding-top: 15px; border-top: 1px solid #ddd; font-size: 14px; color: #555; }" +
                        "a { color: #0d47a1; text-decoration: none; }" +
                        "a:hover { text-decoration: underline; }" +
                        "</style>" +
                        "</head>" +
                        "<body>" +
                        "<div class='container'>" +
                        "<p>Hi %s,</p>" +
                        "<p>You're onboarded as a <strong>SuperFiler</strong>. Your workspace focuses on document validation," +
                        " compliance checks, and state filing execution.</p>" +
                        "<h3>Your key actions:</h3>" +
                        "<ul>" +
                        "<li>Review and validate formation data and uploaded documents.</li>" +
                        "<li>Prepare, generate, and upload state-specific filing packages.</li>" +
                        "<li>Update status timelines (e.g., Review → Ready to File → Filed → Completed).</li>" +
                        "<li>Raise exceptions/clarifications and leave notes for Vendors/Admin.</li>" +
                        "<li>Ensure compliance and SLA targets for each filing.</li>" +
                        "</ul>" +
                        "<a class='button' href='https://clearincorp.com/superfiler'>Open SuperFiler Workspace</a>" +
                        "<div class='footer'>" +
                        "<p><strong>Questions?</strong> We’re here to help at " +
                        "<a href='mailto:support@clearincorp.com'>support@clearincorp.com</a></p>" +
                        "<p>Stay secure,<br><strong>The ClearInCorp Team</strong></p>" +
                        "<p><em>Need support?</em> Drop us a line at " +
                        "<a href='mailto:support@clearincorp.com'>support@clearincorp.com</a><br>" +
                        "<em>Learn more:</em> Visit us at " +
                        "<a href='https://clearincorp.com'>clearincorp.com</a></p>" +
                        "</div>" +
                        "</div>" +
                        "</body>" +
                        "</html>";

        String bodyHtml = String.format(emailTemplate, escapeHtml(firstName));
        return sendEmail(fromEmail, toEmail, subject, bodyHtml);
    }



    public Boolean sendFilingFailureEmail(String fromEmail, String toEmail, String subject,
                                          String firstName, String companyName,
                                          String failureCategory, String failureDescription,
                                          String nextSteps) {

        String emailTemplate =
                "<!DOCTYPE html>" +
                        "<html>" +
                        "<head>" +
                        "<style>" +
                        "body { font-family: Arial, sans-serif; margin: 0; padding: 0; background-color: #ffffff; }" +
                        ".container { max-width: 600px;padding: 20px;" +
                        " box-shadow: 0 4px 12px rgba(0,0,0,0.08); color: #333; }" +
                        "h2 { margin-top: 0; }" +
                        "p, li { font-size: 16px; line-height: 1.6; }" +
                        ".badge { display: inline-block; background: #fff0f3; color: #c81e45; padding: 4px 10px; border-radius: 999px;" +
                        " font-size: 12px; font-weight: 600; }" +
                        ".footer { margin-top: 30px; padding-top: 15px; border-top: 1px solid #ddd; font-size: 14px; color: #555; }" +
                        "a { color: #0d47a1; text-decoration: none; }" +
                        "a:hover { text-decoration: underline; }" +
                        "</style>" +
                        "</head>" +
                        "<body>" +
                        "<div class='container'>" +
                        "<h2>Hi %s,</h2>" +   // name
                        "<p>We encountered an issue while processing a filing for your company <strong>%s</strong>.</p>" +   // company
                        "<p><strong>Category:</strong> %s</p>" +   // category
                        "<p><strong>Details:</strong> %s</p>" +    // details
                        "<p><strong>Next Steps:</strong></p>" +
                        "<p>%s</p>" +   // next steps
                        "<p>Please note that our team has been alerted and will contact you shortly to help resolve this.</p>" +
                        "<div class='footer'>" +
                        "<p><strong>Questions?</strong> We’re here to help at <a href='mailto:support@clearincorp.com'>support@clearincorp.com</a></p>" +
                        "<p>Stay secure,<br><strong>The ClearInCorp Team</strong></p>" +
                        "<p><em>Need support?</em> Drop us a line at <a href='mailto:support@clearincorp.com'>support@clearincorp.com</a><br>" +
                        "<em>Learn more:</em> Visit us at <a href='https://clearincorp.com'>clearincorp.com</a></p>" +
                        "</div>" +
                        "</div>" +
                        "</body>" +
                        "</html>";

        String bodyHtml = String.format(emailTemplate,
                escapeHtml(firstName),
                escapeHtml(companyName),
                escapeHtml(failureCategory),
                escapeHtml(failureDescription),
                escapeHtml(nextSteps));
        return sendEmail(fromEmail, toEmail, subject, bodyHtml);
    }

    // Method for sending the Token Email
    public Boolean sendTokenEmailInZoho(String fromEmail, String toEmail, String subject, String tokenValue,String firstName) {
                String emailTemplate = """
    <!DOCTYPE html>
    <html lang="en">
    <head>
      <meta charset="UTF-8" />
      <meta name="viewport" content="width=device-width" />
      <title>ClearInCorp - Login Code</title>
      <style>
        body {
          font-family: Arial, sans-serif;
          margin: 0;
          padding: 0;
          background-color: #ffffff;
        }
        .container {
          max-width: 600px;
          padding: 30px;
          border: 1px solid #ddd;
          line-height: 1.6;
          color: #333;
          box-shadow: 0 4px 12px rgba(0,0,0,0.08);
          text-align: left;
        }
        h1, h2, h3 {
          margin-top: 0;
        }
        .code {
          font-size: 24px;
          font-weight: bold;
          color: #000;
          margin: 15px 0;
        }
        .steps {
          margin: 20px 0;
          padding-left: 20px;
        }
        .highlight {
          background-color: #ffffff;
          font-weight: bold;
          padding: 2px 4px;
          border-radius: 3px;
        }
        .footer {
          margin-top: 30px;
          padding-top: 15px;
          border-top: 1px solid #ddd;
          font-size: 14px;
          color: #555;
        }
        a {
          color: #0d47a1;
          text-decoration: none;
        }
        a:hover {
          text-decoration: underline;
        }
      </style>
    </head>
    <body>
      <div class="container">
        <p>Hi %s,</p>
        <p>Your secure login code for <strong>ClearInCorp</strong> is:</p>
        <p class="code">%s</p>
        <p>
          This code will expire soon for your security, so please use it within
          the next few minutes to access your account.
        </p>

        <h3>Quick Steps:</h3>
        <ol class="steps">
          <li>Go to the <a href="https://clearincorp.com">ClearInCorp</a> login page</li>
          <li>Enter this 6-digit code when prompted</li>
          <li>You’re in and ready to continue building your business!</li>
        </ol>

        <p>
          <strong>Need a fresh code?</strong> If this one expires, simply click
          <span class="highlight">“Resend Code”</span> on the login page and
          we’ll send you another one right away.
        </p>

        <p>
          <strong>Didn't request this?</strong> No worries — just ignore this email
          and your account remains secure.
        </p>

        <div class="footer">
          <p>
            <strong>Questions?</strong> We’re here to help at
            <a href="mailto:support@clearincorp.com">support@clearincorp.com</a>
          </p>
          <p>Stay secure,<br><strong>The ClearInCorp Team</strong></p>
          <p>
            <em>Need support?</em> Drop us a line at
            <a href="mailto:support@clearincorp.com">support@clearincorp.com</a><br>
            <em>Learn more:</em> Visit us at
            <a href="https://clearincorp.com">clearincorp.com</a>
          </p>
        </div>
      </div>
          </body>
                    </html>""";



        String bodyHtml = String.format(emailTemplate, escapeHtml(firstName), escapeHtml(tokenValue));

        return sendEmail(fromEmail, toEmail, subject, bodyHtml);
    }

    public  Boolean ReminderEmail(String fromEmail, String toEmail, String subject, String firstName) {
            String emailTemplate = "<html>" +
                    "<head>" +
                    "<style>" +
                    "body { font-family: Arial, sans-serif; margin: 0; padding: 0; background-color: #ffffff; }" +
                    ".container { max-width: 600px;padding: 20px;" +
                    "box-shadow: 0 4px 12px rgba(0,0,0,0.1); color: #333; }" +
                    "p, li { font-size: 16px; line-height: 1.6; }" +
                    "ul, ol { padding-left: 20px; }" +
                    ".button { display: inline-block; background: #d32f2f; color: #ffffff; padding: 12px 20px; border-radius: 6px; " +
                    "text-decoration: none; font-weight: bold; margin-top: 20px; }" +
                    ".footer { margin-top: 30px; font-size: 14px; color: #777; border-top: 1px solid #ddd; padding-top: 15px; }" +
                    "</style>" +
                    "</head>" +
                    "<body>" +
                    "<div class='container'>" +
                    "<p>Dear %s,</p>" +
                    "<p>We noticed you started your business formation but haven't completed the process. You’re just one step away from securing your business legally.</p>" +
                    "<h3>Why Complete Your Registration Today?</h3>" +
                    "<ul>" +
                    "<li>82%% of successful businesses register legally before launching.</li>" +
                    "<li>Having an official business entity boosts credibility with clients, banks, and investors.</li>" +
                    "<li>Your business name may be claimed by someone else if you delay.</li>" +
                    "</ul>" +
                    "<h3>What’s Left to Do?</h3>" +
                    "<ol>" +
                    "<li>Select your business type.</li>" +
                    "<li>Provide the necessary details.</li>" +
                    "<li>Submit your registration for processing.</li>" +
                    "</ol>" +
                    "<a class='button' href='https://clearInCorp.com'>Complete Your Business Setup</a>" +
                    "<p>If you need help, our experts are available to assist you.</p>" +
                    "<div class='footer'>" +
                    "<p>Best regards,</p>" +
                    "<p><strong> Clear In Corp</strong></p>" +
                    "<p><a href='mailto:contact@clearInCorp.com'>contact@clearInCorp.com</a></p>" +
                    "<p><a href='https://clearInCorp.com'>clearInCorp.com</a></p>" +
                    "</div>" +
                    "</div>" +
                    "</body>" +
                    "</html>";

            String bodyHtml = String.format(emailTemplate, escapeHtml(firstName));

            return sendEmail(fromEmail, toEmail, subject, bodyHtml);
    }

    public Boolean sendPendingSetupReminderEmail(String fromEmail, String toEmail, String subject, String firstName) {

            String emailTemplate = "<html>" +
                    "<head>" +
                    "<style>" +
                    "body { font-family: Arial, sans-serif; background-color: #ffffff; margin: 0; padding: 0; }" +
                    ".container { max-width: 600px; padding: 20px;" +
                    "box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1); color: #333333; }" +
                    "p, li { font-size: 16px; line-height: 1.6; }" +
                    "ul { padding-left: 20px; }" +
                    ".button { display: inline-block; background-color: #2e7d32; color: #ffffff; padding: 12px 20px; " +
                    "border-radius: 6px; text-decoration: none; font-weight: bold; margin-top: 20px; }" +
                    ".footer { margin-top: 30px; font-size: 14px; color: #777777; border-top: 1px solid #dddddd; padding-top: 15px; }" +
                    "</style>" +
                    "</head>" +
                    "<body>" +
                    "<div class='container'>" +
                    "<p>Dear %s,</p>" +
                    "<p>Did you know that businesses that register properly grow 35%% faster in their first year? By completing your registration, you ensure:</p>" +
                    "<ul>" +
                    "<li>Legal protection from personal liability.</li>" +
                    "<li>Easier access to business bank accounts, loans, and investors.</li>" +
                    "<li>Tax advantages that can save you thousands annually.</li>" +
                    "</ul>" +
                    "<p>With <strong>Clear In Corp</strong>, setting up your business is quick, affordable, and hassle-free.</p>" +
                    "<a class='button' href='https://clearInCorp.com'>Complete Registration Now</a>" +
                    "<p>Need guidance? Our team is ready to help.</p>" +
                    "<div class='footer'>" +
                    "<p>Best regards,</p>" +
                    "<p><strong> Clear In Corp</strong><br>" +
                    "<a href='mailto:start@clearInCorp.com'>start@clearInCorp.com</a><br>" +
                    "<a href='https://clearInCorp.com'>clearInCorp.com</a></p>" +
                    "</div>" +
                    "</div>" +
                    "</body>" +
                    "</html>";

            String bodyHtml = String.format(emailTemplate, escapeHtml(firstName));

            return sendEmail(fromEmail, toEmail, subject, bodyHtml);

    }

    public Boolean sendLimitedTimeReminderEmail(String fromEmail, String toEmail, String subject, String firstName) {
        String emailTemplate = "<html>" +
                "<head>" +
                "<style>" +
                "body { font-family: Arial, sans-serif; background-color: #ffffff; margin: 0; padding: 0; }" +
                ".container { max-width: 600px; padding: 20px;" +
                "box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1); color: #333333; }" +
                "p, li { font-size: 16px; line-height: 1.6; }" +
                "ul { padding-left: 20px; }" +
                ".button { display: inline-block; background-color: #c62828; color: #ffffff; padding: 12px 20px; " +
                "border-radius: 6px; text-decoration: none; font-weight: bold; margin-top: 20px; }" +
                ".footer { margin-top: 30px; font-size: 14px; color: #777777; border-top: 1px solid #dddddd; padding-top: 15px; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='container'>" +
                "<p>Dear %s,</p>" +
                "<p>Your business formation is still incomplete, and time may be running out to secure your business name. Every day, thousands of new businesses are registered—don’t risk losing yours.</p>" +
                "<p><strong>Why Register Now?</strong></p>" +
                "<ul>" +
                "<li>One in three businesses fail due to incomplete legal formalities.</li>" +
                "<li>Registered businesses qualify for tax deductions and funding opportunities.</li>" +
                "<li>Legal registration builds trust with customers and business partners.</li>" +
                "</ul>" +
                "<a class='button' href='https://clearInCorp.com'>Complete Registration Now</a>" +
                "<p>Have questions? Our support team is available to assist you.</p>" +
                "<div class='footer'>" +
                "<p>Best regards,</p>" +
                "<p><strong> Clear In Corp</strong><br>" +
                "<a href='mailto:start@clearInCorp.com'>start@clearInCorp.com</a><br>" +
                "<a href='https://clearInCorp.com'>clearInCorp.com</a></p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";

        String bodyHtml = String.format(emailTemplate, escapeHtml(firstName));

        return sendEmail(fromEmail, toEmail, subject, bodyHtml);
    }

    public  Boolean sendFollowUpReminderEmail(String fromEmail, String toEmail, String subject, String firstName) {
            String emailTemplate = "<html>" +
                    "<head>" +
                    "<style>" +
                    "body { font-family: Arial, sans-serif; background-color: #ffffff; margin: 0; padding: 0; }" +
                    ".container { max-width: 600px;padding: 20px;" +
                    "box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1); color: #333333; }" +
                    "p, li { font-size: 16px; line-height: 1.6; }" +
                    "ul { padding-left: 20px; }" +
                    ".button { display: inline-block; background-color: #00695c; color: #ffffff; padding: 12px 20px; " +
                    "border-radius: 6px; text-decoration: none; font-weight: bold; margin-top: 20px; }" +
                    ".footer { margin-top: 30px; font-size: 14px; color: #777777; border-top: 1px solid #dddddd; padding-top: 15px; }" +
                    "</style>" +
                    "</head>" +
                    "<body>" +
                    "<div class='container'>" +
                    "<p>Dear %s,</p>" +
                    "<p>We understand that launching a business takes time. However, waiting too long could mean:</p>" +
                    "<ul>" +
                    "<li>Missing out on potential tax benefits.</li>" +
                    "<li>Losing your preferred business name to someone else.</li>" +
                    "<li>Delays in opening a business bank account and securing funding.</li>" +
                    "</ul>" +
                    "<p>At Clear In Corp, we make registration simple and stress-free. Our process is trusted by thousands of entrepreneurs who successfully formed their businesses.</p>" +
                    "<a class='button' href='https://clearInCorp.com'>Resume Your Registration</a>" +
                    "<p>If you have any concerns, our expert team is ready to assist you.</p>" +
                    "<div class='footer'>" +
                    "<p>Best regards,</p>" +
                    "<p><strong> Clear In Corp</strong><br>" +
                    "<a href='mailto:start@clearInCorp.com'>start@clearInCorp.com</a><br>" +
                    "<a href='https://clearInCorp.com'>clearInCorp.com</a></p>" +
                    "</div>" +
                    "</div>" +
                    "</body>" +
                    "</html>";

            String bodyHtml = String.format(emailTemplate, escapeHtml(firstName));

        return sendEmail(fromEmail, toEmail, subject, bodyHtml);
    }

    public Boolean sendPaymentConfirmationEmail(String fromEmail, String toEmail, String subject,
                                                       String firstName, String paymentAmount, String transactionId,
                                                       String paymentDate, String businessName) {
            String emailTemplate = "<html>" +
                    "<head>" +
                    "<style>" +
                    "body { font-family: Arial, sans-serif; background-color: #ffffff; margin: 0; padding: 0; }" +
                    ".container { max-width: 620px; padding: 20px 30px;" +
                    " box-shadow: 0 4px 12px rgba(0,0,0,0.1); color: #222; }" +
                    "p, li { font-size: 16px; line-height: 1.6; margin: 0 0 10px; }" +
                    "h2, h3 { margin: 22px 0 10px; }" +
                    "ul, ol { padding-left: 22px; margin: 8px 0 16px; }" +
                    ".hi { margin-bottom: 14px; }" +
                    ".hl { background:#ffffff; padding:0 3px; border-radius:3px; }" +
                    ".footer { margin-top: 26px; border-top: 1px solid #e5e5e5; padding-top: 14px; color:#555; font-size:14px; }" +
                    "a { color:#0d47a1; text-decoration:none; } a:hover { text-decoration:underline; }" +
                    "</style>" +
                    "</head>" +
                    "<body>" +
                    "<div class='container'>" +

                    "<p class='hi'>Hi <span class='hl'>%s</span>,</p>" +

                    "<p>Fantastic news! Your payment has been processed successfully, and we're already getting to work on making <span class='hl'>%s</span> a reality.</p>" +

                    "<h3>Your Payment Summary:</h3>" +
                    "<ul>" +
                    "<li><strong>Amount:</strong> $%s</li>" +
                    "<li><strong>Transaction ID:</strong> %s</li>" +
                    "<li><strong>Date:</strong> %s</li>" +
                    "<li><strong>Company:</strong> %s</li>" +
                    "</ul>" +

                    "<h3>What's Happening Behind the Scenes:</h3>" +
                    "<p><strong>Right Now</strong> – Our team is preparing your documents for state filing</p>" +
                    "<p><strong>Next 3–5 Business Days</strong> – State authorities will review and approve your formation</p>" +
                    "<p><strong>Upon Approval</strong> – You'll receive your complete LLC package including:</p>" +
                    "<ul>" +
                    "<li>Certificate of Formation</li>" +
                    "<li>EIN (if requested)</li>" +
                    "<li>Operating Agreement template</li>" +
                    "<li>Compliance guidelines</li>" +
                    "</ul>" +

                    "<h3>Your Action Items:</h3>" +
                    "<ul>" +
                    "<li><strong>Keep watching your inbox</strong> – We'll update you at every major milestone</li>" +
                    "<li><strong>EIN processing</strong> – If you ordered this service, expect it within 1–2 business days after formation</li>" +
                    "<li><strong>Additional services</strong> – Explore business banking and ongoing compliance support in your account</li>" +
                    "</ul>" +

                    "<h3>Questions or Concerns?</h3>" +
                    "<ul>" +
                    "<li><strong>Email:</strong> <a href='mailto:support@clearincorp.com'>support@clearincorp.com</a></li>" +
                    "<li><strong>Phone:</strong> (555) 123-4567</li>" +
                    "</ul>" +

                    "<p>Thanks for trusting us with this important milestone, %s. Starting a business is a big step, and we're thrilled to be part of your journey!</p>" +

                    "<p><strong>The ClearInCorp Team</strong></p>" +

                    "<div class='footer'>" +
                    "<p><em>Track your progress:</em> <a href='https://clearincorp.com'>Login to your dashboard</a><br>" +
                    "<em>Need immediate help?</em> <a href='mailto:support@clearincorp.com'>support@clearincorp.com</a></p>" +
                    "</div>" +
                    "</div>" +
                    "</body>" +
                    "</html>";

        String safeFirstName = escapeHtml(firstName);
        String safeBusinessName = escapeHtml(businessName);
        String bodyHtml = String.format(emailTemplate,
                safeFirstName, safeBusinessName,
                escapeHtml(paymentAmount), escapeHtml(transactionId),
                escapeHtml(paymentDate), safeBusinessName, safeFirstName);

            return sendEmail(fromEmail, toEmail, subject, bodyHtml);
    }

    public Boolean sendFilingSubmittedEmail(String fromEmail, String toEmail, String subject,
                                                   String firstName, String businessName, String filingDate,
                                                   String stateOfFormation) {
            String emailTemplate ="<html>" +
                    "<head>" +
                    "<style>" +
                    "body { font-family: Arial, sans-serif; background:#f4f4f4; margin:0; padding:0; }" +
                    ".container { max-width:650px; margin:20px; padding:20px;" +
                    " box-shadow:0 0 15px rgba(0,0,0,0.1); color:#333; }" +
                    "p, li { font-size:16px; line-height:1.6; margin:0 0 10px; }" +
                    "h3 { margin:22px 0 10px; }" +
                    "ul { padding-left:22px; }" +
                    ".spacer { height:10px; }" +
                    "a { color:#0d47a1; text-decoration:none; } a:hover { text-decoration:underline; }" +
                    "</style>" +
                    "</head>" +
                    "<body>" +
                    "<div class='container'>" +

                    "<p>Hi %s,</p>" + // 1 name

                    "<p>Exciting update! We've just successfully submitted %s's formation documents to the State of %s. " + // 2 companyName, 3 state
                    "Your LLC is now officially in the queue for approval.</p>" +

                    "<h3>Filing Confirmation:</h3>" +
                    "<ul>" +
                    "<li><strong>Company:</strong> %s</li>" +                               // 4 companyName_again
                    "<li><strong>Filed:</strong> %s</li>" +                                  // 5 filedDateTime
                    "<li><strong>State:</strong> %s</li>" +                                  // 6 state_again1
                    "<li><strong>Status:</strong> Submitted and Being Processed</li>" +
                    "</ul>" +

                    "<h3>What's Next in the Pipeline:</h3>" +
                    "<p><strong>State Review</strong> – %s will review your documents (typically 3–5 business days)</p>" + // 8 state_again2
                    "<p><strong>Approval &amp; Documents</strong> – Once approved, you'll receive:</p>" +
                    "<ul>" +
                    "<li>Official Certificate of Formation</li>" +
                    "<li>EIN confirmation (if requested)</li>" +
                    "<li>Operating Agreement template</li>" +
                    "<li>Compliance checklist for your new LLC</li>" +
                    "</ul>" +

                    "<h3>Future Planning – After approval, we'll help you with:</h3>" +
                    "<ul>" +
                    "<li>Business banking setup guidance</li>" +
                    "<li>License requirements for your industry</li>" +
                    "<li>Ongoing compliance support</li>" +
                    "</ul>" +

                    "<h3>Stay in the Loop:</h3>" +
                    "<ul>" +
                    "<li><strong>Automatic updates</strong> – We'll email you the moment %s approves your filing</li>" + // 10 state_again3
                    "<li><strong>Track anytime</strong> – Check status in your <a href='https://clearincorp.com/dashboard'>account dashboard</a></li>" +
                    "<li><strong>Questions?</strong> Our team is standing by at <a href='mailto:support@clearincorp.com'>support@clearincorp.com</a> or (555) 123-4567</li>" +
                    "</ul>" +

                    "<p>You're so close to making it official, %s! %s is known for efficient processing, so we expect good news very soon.</p>" + // 9 name_again, 10 state_again3 (already used above; reuse if preferred)

                    "<p><strong>The ClearInCorp Team</strong></p>" +

                    "</div>" +
                    "</body>" +
                    "</html>";


        String safeFirstName = escapeHtml(firstName);
        String safeBusinessName = escapeHtml(businessName);
        String safeState = escapeHtml(stateOfFormation);
        String bodyHtml = String.format(emailTemplate,
                safeFirstName, safeBusinessName, safeState,
                safeBusinessName, escapeHtml(filingDate), safeState,
                safeState, safeState, safeFirstName, safeState);


            return sendEmail(fromEmail, toEmail, subject, bodyHtml);
    }

    public Boolean sendCompanyFormationCompletedEmail(String fromEmail, String toEmail, String subject,
                                                             String firstName, String businessName,
                                                             String state, String date) {
            String emailTemplate = "<html>" +
                    "<head>" +
                    "<style>" +
                    "body { font-family: Arial, sans-serif; background:#f9f9f9; margin:0; padding:0; }" +
                    ".container { max-width:650px; margin:20px; padding:20px;" +
                    " box-shadow:0 0 15px rgba(0,0,0,0.1); color:#333; }" +
                    "p, li { font-size:16px; line-height:1.6; margin:0 0 10px; }" +
                    "h3 { margin:22px 0 10px; }" +
                    "ul { padding-left:22px; }" +
                    ".hl { background:#ffffff; padding:0 3px; border-radius:3px; }" +
                    ".footer { margin-top:30px; font-size:14px; color:#777; border-top:1px solid #ddd; padding-top:15px; }" +
                    "a { color:#0d47a1; text-decoration:none; } a:hover { text-decoration:underline; }" +
                    "</style>" +
                    "</head>" +
                    "<body>" +
                    "<div class='container'>" +
                    "<p>%s,</p>" +  // name
                    "<p><strong>We did it!</strong> The State of <span class='hl'>%s</span> has officially approved your LLC formation. %s is now a real, legally recognized business entity – and you're officially a business owner!</p>" +
                    "<h3>Your Official Company Profile:</h3>" +
                    "<ul>" +
                    "<li><strong>Company:</strong> %s</li>" +
                    "<li><strong>Registered:</strong> %s</li>" +
                    "<li><strong>State:</strong> %s</li>" +
                    "<li><strong>Status:</strong> OFFICIALLY APPROVED </li>" +
                    "</ul>" +
                    "<h3>Your Business Documents (Ready for Download):</h3>" +
                    "<p><strong>Certificate of Formation</strong> – Your official proof of business registration</p>" +
                    "<p><strong>EIN Confirmation</strong> – Your federal tax ID (if requested)</p>" +
                    "<p><strong>Operating Agreement Template</strong> – Customize for your specific needs</p>" +
                    "<p><strong>Complete Formation Package</strong> – All state-issued documents</p>" +
                    "<p>[<a href='#'>Download All Documents →</a>] | [<a href='#'>Access Your Dashboard →</a>]</p>" +
                    "<h3>Ready for Your Next Big Moves?</h3>" +
                    "<p>Now that you're officially in business, here are the essential next steps:</p>" +
                    "<p><strong>Business Banking</strong> – Use your Certificate of Formation and EIN to open a business account</p>" +
                    "<p><strong>Licenses & Permits</strong> – Research what your industry requires in <span class='hl'>%s</span></p>" +
                    "<p><strong>Tax Setup</strong> – Register for state taxes and understand your filing requirements</p>" +
                    "<p><strong>Launch Strategy</strong> – Build your website, establish your brand, and start marketing</p>" +

                    "<h3>We're Still Here for You:</h3>" +
                    "<p>Your formation is complete, but your journey is just beginning. Our team is ready to help with:</p>" +
                    "<ul>" +
                    "<li>Ongoing compliance support</li>" +
                    "<li>Additional business services</li>" +
                    "<li>Questions about your new LLC</li>" +
                    "</ul>" +

                    "<h3>Reach us at:</h3>" +
                    "<ul>" +
                    "<li><strong>Email:</strong> <a href='mailto:support@clearincorp.com'>support@clearincorp.com</a></li>" +
                    "<li><strong>Phone:</strong> (555) 123-4567</li>" +
                    "<li><strong>Resources:</strong> <a href='https://clearincorp.com/new-business-guide'>clearincorp.com/new-business-guide</a></li>" +
                    "</ul>" +

                    "<p><span class='hl'>%s</span>, take a moment to celebrate this milestone. You've officially joined the ranks of business owners, and that's no small feat. We're proud to have been part of making your vision a reality.</p>" +

                    "<p>Here's to the bright future of <span class='hl'>%s</span>!</p>" +

                    "<p><strong>The Entire ClearInCorp Team</strong></p>" +

                    "<div class='footer'>" +
                    "<p><em>Your documents:</em> Download from your dashboard<br>" +
                    "<em>Need guidance?</em> Check out our new business owner's guide<br>" +
                    "<em>Questions?</em> We're always here: <a href='mailto:support@clearincorp.com'>support@clearincorp.com</a></p>" +
                    "</div>" +

                    "</div>" +
                    "</body>" +
                    "</html>";


        String safeFirstName = escapeHtml(firstName);
        String safeBusinessName = escapeHtml(businessName);
        String safeState = escapeHtml(state);
        String bodyHtml = String.format(emailTemplate,
                safeFirstName, safeState, safeBusinessName,
                safeBusinessName, escapeHtml(date), safeState,
                safeState, safeFirstName, safeBusinessName);

            return sendEmail(fromEmail, toEmail, subject, bodyHtml);

}

    public Boolean sendCompanyFormationInProgressEmail(String fromEmail, String toEmail, String subject,
                                                       String firstName, String businessName,
                                                       String state, String date) {
        String emailTemplate = "<html>" +
                "<head>" +
                "<style>" +
                "body { font-family: Arial, sans-serif; background:#ffffff; margin:0; padding:0; }" +
                ".container { max-width:650px; margin:20px;padding:20px;" +
                " box-shadow:0 0 15px rgba(0,0,0,0.1); color:#333; }" +
                "p, li { font-size:16px; line-height:1.6; margin:0 0 10px; }" +
                "h3 { margin:22px 0 10px; }" +
                "ul { padding-left:22px; }" +
                ".hl { background:#ffffff; border-radius:3px; padding:0 3px; }" +
                ".footer { margin-top:30px; font-size:14px; color:#777; border-top:1px solid #ddd; padding-top:15px; }" +
                "a { color:#0d47a1; text-decoration:none; } a:hover { text-decoration:underline; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='container'>" +

                "<p>Hey <span class='hl'>%s</span>,</p>" +

                "<p>Great news! We've received everything we need to bring <span class='hl'>%s</span> into the world, and our team is already hard at work making it happen.</p>" +

                "<h3>Your Formation Summary:</h3>" +
                "<ul>" +
                "<li><strong>Company Name:</strong> %s</li>" +
                "<li><strong>Formation State:</strong> %s</li>" +
                "<li><strong>Started:</strong> %s</li>" +
                "<li><strong>Status:</strong> In Progress with ths State</li>" +
                "</ul>" +

                "<h3>Here's What We're Handling:</h3>" +
                "<p><strong>Right Now</strong> – Preparing and filing your official documents with Texas</p>" +
                "<p><strong>Soon</strong> – Keeping you updated every step of the way</p>" +
                "<p><strong>Very Soon</strong> – Delivering your official LLC documents the moment they're approved</p>" +

                "<h3>What You Need to Do:</h3>" +
                "<p>Absolutely nothing! Just keep an eye on your inbox for updates. We've got this covered from start to finish.</p>" +

                "<h3>Questions Along the Way?</h3>" +
                "<ul>" +
                "<li><strong>Email:</strong> <a href='mailto:support@clearincorp.com'>support@clearincorp.com</a></li>" +
                "<li><strong>Phone:</strong> (555) 123-4567</li>" +
                "</ul>" +

                "<p>We're genuinely excited to be part of your business journey, <span class='hl'>%s</span>. Building something new takes courage, and we're honored you chose us to help make it official.</p>" +

                "<p>Here's to the future of <span class='hl'>%s</span>!</p>" +

                "<p><strong>The ClearInCorp Team</strong></p>" +

                "<div class='footer'>" +
                "<p>Follow your progress: <a href='https://clearincorp.com/dashboard'>clearincorp.com/dashboard</a><br>" +
                "Need help? <a href='mailto:support@clearincorp.com'>support@clearincorp.com</a></p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";

        String safeFirstName = escapeHtml(firstName);
        String safeBusinessName = escapeHtml(businessName);
        String bodyHtml = String.format(emailTemplate,
                safeFirstName, safeBusinessName, safeBusinessName,
                escapeHtml(state), escapeHtml(date),
                safeFirstName, safeBusinessName);

        return sendEmail(fromEmail, toEmail, subject, bodyHtml);

    }


}
