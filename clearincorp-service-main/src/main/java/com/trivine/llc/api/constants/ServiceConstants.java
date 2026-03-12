package com.trivine.llc.api.constants;

public class ServiceConstants {

    // Email Constants
    public static final String FROM_EMAIL = "noreply@clearincorp.com";
    public static final String TOKEN_SUBJECT = "ClearInCorp - Your secure access code is ready";
    public static final String CONSUMER_WELCOME_SUBJECT = "ClearInCorp - Your Business Formation Journey Starts Now";
    public static final String VENDOR_WELCOME_SUBJECT="Welcome to Clear In Corp – Vendor Portal";
    public static final String SUPER_FILER_WELCOME_SUBJECT="Welcome to Clear In Corp – Filing Operations";

    public static final String PAYMENT_SUBJECT = "ClearInCorp – Your Payment confirmed. Your LLC formation begins now";
    public static final String IN_PROGRESS_SUBJECT ="ClearInCorp - Your LLC formation is officially underway!";
    public static final String FILED_SUBJECT = "ClearInCorp – Your Company Successfully filed! Now we wait for State approval";
    public static final String FILED_SUCCESS_SUBJECT = "APPROVED! %s is officially yours";
    public static final String FILED_FAILED_SUBJECT = "Your ClearInCorp Filing was Failure";
    public static final String EVERYDAY_SUBJECT = "Don’t Miss Out: Complete Your Business Registration";
    public static final String EVERY_3DAYS_SUBJECT = "Your Business Setup is Still Pending";
    public static final String EVERY_7DAYS_SUBJECT = "Limited Time: Get Your Business Registered Now";
    public static final String EVERY_14DAYS_SUBJECT = "Let’s Get Back on Track – Your Business Awaits";
    public static final int OTP_LENGTH = 6;
    public static final int OTP_EXPIRY_MINUTES = 5;

    // Billing Constants
    public static final String BILL_FROM_NAME = "ClearInCorp";
    public static final String BILL_FROM_EMAIL = "admin@clearincorp.com";
    public static final String BILL_FROM_ADDRESS = "333 Preston Road Ste 300 #1216. Frisco, Texas";
    public static final String BILL_FROM_PHONE = "123 4567 890";

    //Async Constants
    public static final int ASYNC_CORE_POOL_SIZE = 10;
    public static final int ASYNC_MAX_POOL_SIZE = 20;
    public static final int ASYNC_QUEUE_CAPACITY = 100;
    public static final String ASYNC_THREAD_NAME_PREFIX = "AsyncThread-";

    //Email Based Rate Limiting Constants
    public static final int MAX_REQUESTS = 50;
    public static final long RATE_LIMIT_TIME_WINDOW_MS = 60_000;


    //login validation
    public static final String DEFAULT_COUNTRY = "USA";


    // IP Based Rate Limiting Constants
    public static final int RATE_LIMIT_MAX_REQUESTS = 1000;
    public static final long IP_RATE_LIMIT_TIME_WINDOW_MS = 60_000;
    public static final String RATE_LIMIT_CLEANUP_RATE_MS = "60000";

    // Application-Specific Constants
    public static final long DEFAULT_VENDOR_USER_ID = 2L;
    public static final long ADMIN_COMPANY_ID = 1L;
}