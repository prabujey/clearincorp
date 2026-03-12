//package com.trivine.llc.api.service;
//
//import com.trivine.llc.api.entity.Company;
//import com.trivine.llc.api.repository.CompanyRepository;
//import io.github.bonigarcia.wdm.WebDriverManager;
//import lombok.RequiredArgsConstructor;
//import org.openqa.selenium.*;
//import org.openqa.selenium.chrome.ChromeDriver;
//import org.openqa.selenium.chrome.ChromeDriverService;
//import org.openqa.selenium.chrome.ChromeOptions;
//import org.openqa.selenium.interactions.Actions;
//import org.openqa.selenium.support.ui.ExpectedConditions;
//import org.openqa.selenium.support.ui.Select;
//import org.openqa.selenium.support.ui.WebDriverWait;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//
//import java.time.Duration;
//import java.util.List;
//import java.util.Random;
//
//@Service
//@RequiredArgsConstructor
//public class WebScrapingService {
//    private final CompanyRepository companyRepository;
//    private static final String TEXAS = "https://mycpa.cpa.state.tx.us/coa/search.do";
//    private static final String WYOMING = "https://wyobiz.wyo.gov/Business/FilingSearch.aspx";   //problem
//    private static final String RHODE_ISLAND = "https://business.sos.ri.gov/corp/CorpSearch/CorpSearchInput.asp";
//    private static final String WASHINGTON = "https://ccfs.sos.wa.gov";              //problem
//    private static final String TENNESSEE = "https://tncab.tnsos.gov/business-entity-search";
//    private static final String SOUTH_CALIFORNIA = "https://bizfileonline.sos.ca.gov/search/business";  //problem
//    private static final String UTAH = "https://businessregistration.utah.gov/NameAvailabilitySearch";   //problem
//    private static final String WEST_VIRGINIA = "https://apps.wv.gov/SOS/BusinessEntitySearch/Default.aspx"; //problem
//    private static final String MISSOURI = "https://bsd.sos.mo.gov/BusinessEntity/BESearch.aspx";  //problem
//    private static final String NEVADA = "https://esos.nv.gov/EntitySearch/OnlineEntitySearch";   //problem
//    private static final String NEW_JERSEY = "https://www.njportal.com/DOR/BusinessNameSearch/Search/BusinessName";
//    private static final String NEW_YORK = "https://apps.dos.ny.gov/publicInquiry/";       //problem
//    private static final String NORTH_CAROLINA = "https://www.sosnc.gov/online_services/search/by_title/_Business_Registration";    //problem
//    private static final String ALABAMA = "https://www.sos.alabama.gov/search/node";   //problem
//    private static final String ARKANSAS = "https://www.sos.arkansas.gov/business-commercial-services-bcs";  //problem
//    private static final String CALIFORNIA = "https://bizfileonline.sos.ca.gov/search/business";  //problem
//    private static final String COLORADO = "https://www.coloradosos.gov/biz/NameCriteria.do";  //problem
//    private static final String CONNECTICUT = "https://portal.ct.gov/search-results/?q=#gsc.tab=0";  //problem
//    private static final String DELAWARE = "https://icis.corp.delaware.gov/ecorp/entitysearch/namesearch.aspx";  //problem
//    private static final String FLORIDA = "https://search.sunbiz.org/Inquiry/CorporationSearch/ByName";  //problem
//    private static final String GEORGIA = "https://sos.ga.gov/index.php/corporations";  //problem
//    private static final String HAWAII = "https://hbe.ehawaii.gov/documents/search.html";  //problem
//    private static final String IDAHO = "https://sosbiz.idaho.gov/search/business";  //PROBLEM
//
//
//    public String checkCompanyOnline(String companyName, String state) {
//
//        String result;
//        switch (state.toLowerCase().trim()) {
//            // Kishore
//            case "texas": result = searchTexas(companyName);break; //c contains match
//            case "wyoming": result = searchWyoming(companyName);break;
//            case "rhode island": result = searchRhodeIsland(companyName);break;  //c
//            case "washington": result = searchWashington(companyName);break; //exact match c
//            case "tennessee": result = searchTennessee(companyName);break; // c any word contain match (reload each time)
//            case "south california": result = searchSouthCalifornia(companyName);break; // prooblem
//            case "utah": result = searchUtah(companyName); break;
//            case "west virginia": result = searchWestVirginia(companyName); break;  //FOUND
//            case "hawaii": result = searchHawaii(companyName); break;  //not sure about website
//            case "missouri": result = searchMissouri(companyName); break;
//
//            // Abinesh
//            case "nevada": result = searchNevada(companyName); break;
//            case "new jersey": result = searchNewJersey(companyName); break;
//            case "new york": result = searchNewYork(companyName); break;
//            case "north carolina": result = searchNorthCarolina(companyName); break;
//            case "alabama": result = searchAlabama(companyName); break;
//            case "arkansas": result = searchArkansas(companyName); break;
//            case "california": result = searchCalifornia(companyName); break;
//            case "colorado": result = searchColorado(companyName); break;
//            case "connecticut": result = searchConnecticut(companyName); break;
//            case "delaware": result = searchDelaware(companyName); break;
//
//            // Surya Balan
//            case "florida": result = searchFlorida(companyName); break;
//            case "georgia": result = searchGeorgia(companyName); break;
//            case "idaho": result = searchIdaho(companyName); break;
//            case "iowa": result = searchIowa(companyName); break;
//            case "kansas": result = searchKansas(companyName); break;
//            case "kentucky": result = searchKentucky(companyName); break;
//            case "louisiana": result = searchLouisian(companyName); break;
//            case "maine": result = searchMaine(companyName); break;
//            case "maryland": result = searchMaryland(companyName); break;
//            case "massachusetts": result = searchMassachusetts(companyName); break;
////            case "michigan" : result=searchMichigan(companyName);break;//captcha problem
//            case "mississippi": result = searchMississippi(companyName); break;
//            default: return "⚠ Unsupported state: " + state;
//        }
//        return result ;
//    }
//
//    private String searchTexas(String companyName) {
//        WebDriver driver = getHeadlessDriver("chrome");
//
//        try {
//            driver.get(TEXAS);
//            driver.navigate().refresh();
//
//            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
//
//            // Wait for the input box and enter the name
//            WebElement input = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("name")));
//            input.clear();
//            input.sendKeys(companyName);
//
//            // Wait for any potential overlay to disappear
//            try {
//                wait.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector(".icon-bar")));
//            } catch (TimeoutException ignored) {
//                // If overlay doesn't disappear, continue anyway (we'll use JS click as fallback)
//            }
//
//            // Try clicking the button via JavaScript to bypass overlay issues
//            WebElement submitBtn = driver.findElement(By.id("submitBtn"));
//            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", submitBtn);
//
//            Thread.sleep(5000); // Wait for search results to load
//
//            boolean notFound = !driver.findElements(By.xpath("//td[contains(@class, 'dataTables_empty') and text()='No data available in table']")).isEmpty();
//
//            return notFound ? "Not Found" : "Found";
//
//        } catch (Exception e) {
//            e.printStackTrace(); // Log the error
//            return "Error: " + e.getMessage(); // Return full exception message
//        } finally {
//            driver.quit();
//        }
//    }
//
//
//
//    private String searchWyoming(String companyName) {
//        WebDriver driver = getHeadlessDriver("chrome");
//
//        try {
//            driver.get(WYOMING);
//
//            // Use short, explicit waits only
//            WebDriverWait wait20 = new WebDriverWait(driver, Duration.ofSeconds(20));
//            WebDriverWait wait10 = new WebDriverWait(driver, Duration.ofSeconds(10));
//
//            // ---- Handle bot challenge without exiting early ----
//            // Poll up to ~8s for the real input to appear; if we see the challenge, just keep polling.
//            long end = System.nanoTime() + Duration.ofSeconds(8).toNanos();
//            WebElement input = null;
//            while (System.nanoTime() < end && input == null) {
//                String html = "";
//                try { html = driver.getPageSource(); } catch (Exception ignored) {}
//
//                boolean challenge = html.contains("/TSPD/") ||
//                        html.contains("testing whether you are a human");
//
//                if (!challenge) {
//                    try {
//                        input = new WebDriverWait(driver, Duration.ofSeconds(2))
//                                .until(ExpectedConditions.presenceOfElementLocated(
//                                        By.id("MainContent_txtFilingName")));
//                    } catch (TimeoutException ignored) {
//                        // Not there yet—small pause then loop
//                    }
//                }
//
//                if (input == null) {
//                    try { Thread.sleep(300); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
//                }
//            }
//
//            // If still not found, do a last bounded wait (so total remains ≤ ~20s)
//            if (input == null) {
//                input = wait10.until(ExpectedConditions.presenceOfElementLocated(
//                        By.id("MainContent_txtFilingName")));
//            }
//
//            // ---- Perform the search ----
//            input.clear();
//            input.sendKeys(companyName);
//
//            WebElement searchButton = wait20.until(ExpectedConditions.elementToBeClickable(
//                    By.id("MainContent_cmdSearch")));
//            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", searchButton);
//
//            // Wait briefly for either header or a result table
//            new WebDriverWait(driver, Duration.ofSeconds(8)).until(ExpectedConditions.or(
//                    ExpectedConditions.presenceOfElementLocated(By.id("MainContent_lblResultsHeader")),
//                    ExpectedConditions.presenceOfElementLocated(By.cssSelector("table"))
//            ));
//
//            boolean notFound = driver.findElements(By.id("MainContent_lblResultsHeader")).stream()
//                    .anyMatch(el -> el.getText().trim().equalsIgnoreCase("No Results Found."));
//
//            return notFound ? "Not Found" : "Found";
//
//        } catch (TimeoutException te) {
//            // Keep it quick to avoid API GW 29s cap
//            return "Not Found";
//        } catch (Exception e) {
//            return "Not Found";
//        } finally {
//            try { driver.quit(); } catch (Exception ignored) {}
//        }
//    }
//
//
//
//    private String searchRhodeIsland(String companyName) {
//        WebDriver driver = getHeadlessDriver("chrome");
//
//
//        try {
//            driver.get(RHODE_ISLAND);
//            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
//
//            wait.until(ExpectedConditions.presenceOfElementLocated(By.name("EntityName"))).sendKeys(companyName);
//            driver.findElement(By.xpath("//input[@type='button' and contains(@value, 'Search')]")).click();
//
//            Thread.sleep(5000);
//
//            boolean notFound = !driver.findElements(By.xpath("//strong[contains(text(), 'No Records Matched Your')]")).isEmpty();
//
//            return notFound ? "Not Found" : "Found";
//
//        } catch (Exception e) {
//            e.printStackTrace(); // Log the error
//            return "Error: " + e.getMessage(); // Return full exception message
//        } finally {
//            driver.quit();
//        }
//    }
//
//    private String searchWashington(String companyName) {
//        WebDriver driver = getHeadlessDriver("chrome");
//
//        try {
//            driver.get(WASHINGTON);
//            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
//
//            // 1. Enter company name
//            WebElement input = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("BusinessName")));
//            input.clear();
//            input.sendKeys(companyName);
//
//            // 2. Wait until the loader is truly gone (not just invisible but removed/faded)
//            wait.until(driver1 -> {
//                WebElement loader = driver1.findElement(By.id("loaderDiv"));
//                String style = loader.getAttribute("style");
//                return style.contains("display: none") || !loader.isDisplayed();
//            });
//
//            // 3. Click Search using JS to bypass any ghost overlays
//            WebElement searchButton = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[contains(@class, 'btn-search')]")));
//            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", searchButton);
//
//            // 4. Wait again for the loader to be fully gone
//            wait.until(driver1 -> {
//                WebElement loader = driver1.findElement(By.id("loaderDiv"));
//                String style = loader.getAttribute("style");
//                return style.contains("display: none") || !loader.isDisplayed();
//            });
//
//            // 5. Optional wait for result population
//            Thread.sleep(1000);
//
//            // 6. Check result status
//            boolean noResults = !driver.findElements(By.xpath("//tbody[contains(@class, 'ng-scope')]//td[contains(text(), 'No Value Found.')]")).isEmpty();
//            return noResults ? "NotFound" : "Found";
//
//        } catch (TimeoutException e) {
//            return "NotFound-Time out";
//        } catch (Exception e) {
//            e.printStackTrace();
//            return "Error: " + e.getMessage();
//        } finally {
//            driver.quit();
//        }
//    }
//
//
//    private String searchTennessee(String companyName) {
//        WebDriver driver = getHeadlessDriver("chrome");
//
//        try {
//            driver.get(TENNESSEE);
//            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
//
//            // Wait for and fill the business name input (no dynamic ID)
//            WebElement input = wait.until(ExpectedConditions.presenceOfElementLocated(
//                    By.xpath("//input[@placeholder='Business Name' and @data-bind='value:Name']")));
//            input.sendKeys(companyName);
//
//            // Wait for the Search button using stable selector
//            WebElement searchButton = wait.until(ExpectedConditions.presenceOfElementLocated(
//                    By.xpath("//button[@title='Search' and .//span[text()='Search']]")));
//
//            // Scroll into view and click using JavaScript to prevent interception
//            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", searchButton);
//            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", searchButton);
//
//            // Optional: better to replace Thread.sleep with smart wait if possible
//            Thread.sleep(5000);
//
//            // Check for "No Records Available" message
//            boolean notFound = !driver.findElements(By.xpath(
//                    "//div[contains(@class, 'k-grid-norecords-template') and contains(text(), 'No Records Available')]"
//            )).isEmpty();
//
//            return notFound ? "Not Found" : "Found";
//
//        } catch (Exception e) {
//            e.printStackTrace(); // Log error
//            return "Error: " + e.getMessage(); // Return detailed message
//        } finally {
//            driver.quit(); // Always close the driver
//        }
//    }
//
//
//    private String searchSouthCalifornia(String companyName) {
//        WebDriver driver = getHeadlessDriver("chrome");
//
//
//        try {
//            driver.get(SOUTH_CALIFORNIA);
//            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
//
//            wait.until(ExpectedConditions.presenceOfElementLocated(
//                    By.cssSelector("input.search-input[placeholder='Search by name or file number']"))).sendKeys(companyName);
//
//            wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("button.search-button"))).click();
//            Thread.sleep(7000);
//
//            boolean notFound = !driver.findElements(By.xpath("//h3[contains(text(), 'No results were found for')]")).isEmpty();
//
//            return notFound ? "Not Found" : "Found";
//
//        } catch (Exception e) {
//            e.printStackTrace(); // Log the error
//            return "Error: " + e.getMessage(); // Return full exception message
//        } finally {
//            driver.quit();
//        }
//    }
//
//
//
//
//
//    private String searchUtah(String companyName) {
//        WebDriver driver = getHeadlessDriver("chrome");
//
//
//        try {
//            driver.get(UTAH);
//            driver.navigate().refresh();
//
//            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
//
//            wait.until(ExpectedConditions.presenceOfElementLocated(
//                    By.id("SharedSteps__NameAvailabilitySearch_txtBox_SearchValue"))).sendKeys(companyName);
//
//            wait.until(ExpectedConditions.elementToBeClickable(By.id("btnSearch"))).click();
//            Thread.sleep(5000);
//
//            boolean notFound = !driver.findElements(By.xpath("//tbody/tr/td[contains(@class, 'norecords') and contains(text(), 'The Name is Available')]")).isEmpty();
//            return notFound ? "Not Found" : "Found";
//
//        } catch (Exception e) {
//            e.printStackTrace(); // Log the error
//            return "Error: " + e.getMessage(); // Return full exception message
//        } finally {
//            driver.quit();
//        }
//    }
//
//
//
//
//    private String searchWestVirginia(String companyName) {
//        WebDriver driver = getHeadlessDriver("chrome");
//
//
//        try {
//            driver.get(WEST_VIRGINIA);
//            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
//
//            wait.until(ExpectedConditions.presenceOfElementLocated(By.id("phMain_txtOrganizationName"))).sendKeys(companyName);
//
//            Thread.sleep(1000);
//
//            wait.until(ExpectedConditions.elementToBeClickable(By.id("phMain_btnSearch"))).click();
//            Thread.sleep(5000);
//
//            boolean notFound = !driver.findElements(By.xpath("//div[contains(@class, 'errorMessage') and contains(text(), 'No Records Found.')]")).isEmpty();
//
//            return notFound ? "Not Found" : "Found";
//
//        } catch (Exception e) {
//            e.printStackTrace(); // Log the error
//            return "Error: " + e.getMessage(); // Return full exception message return "Not Found";
//        } finally {
//            driver.quit();
//        }
//    }
//
//    public String searchKansas(String entityName) {
//        // Setup WebDriver using WebDriverManager
//        WebDriver driver = getHeadlessDriver("chrome");
//
//
//        try {
//            // for state Kansas
//            driver.get("https://www.sos.ks.gov/eforms/BusinessEntity/Search.aspx");
//
//            // Maximize window
//            driver.manage().window().maximize();
//
//            // Locate the input box and enter the entity name
//            try {
//                Thread.sleep(new Random().nextInt(3000) + 2000); // Random 2-7 seconds wait
//            } catch (InterruptedException e) {
//                e.printStackTrace(); // Print stack trace for debugging
//                Thread.currentThread().interrupt(); // Restore the interrupted status
//            }
//
//            JavascriptExecutor js = (JavascriptExecutor) driver;
//            js.executeScript("window.scrollBy(0,300);");
//// Random 2-7 seconds wait
//            Actions actions = new Actions(driver);
//            actions.moveToElement(driver.findElement(By.id("MainContent_txtSearchEntityName"))).perform();
//
//            WebElement inputBox = driver.findElement(By.id("MainContent_txtSearchEntityName")); // Update with actual ID
//            inputBox.sendKeys(entityName);
//
//            // Click the search button
//            WebElement searchButton = driver.findElement(By.id("MainContent_btnSearchEntity")); // Update with actual ID
//            searchButton.click();
//
//            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
//
//            By searchResults = By.id("MainContent_gvSearchResults");
//            By noEntitiesFound = By.id("MainContent_lblNoEntitiesFound");
//
//// Wait until either of the elements becomes visible
//            wait.until(ExpectedConditions.or(
//                    ExpectedConditions.visibilityOfElementLocated(searchResults),
//                    ExpectedConditions.visibilityOfElementLocated(noEntitiesFound)
//            ));
//
//            // Get the list of search results
//            List<WebElement> results = driver.findElements(By.xpath("//span[@id='MainContent_lblNoEntitiesFound']"));
//            List<WebElement> results1 = driver.findElements(By.xpath("//table[@id='MainContent_gvSearchResults']"));
//
//
//            // Check if the entity is present in the results
//            boolean entityNotFound = results.stream()
//                    .map(WebElement::getText) // Extract text
//                    .anyMatch(text -> text.startsWith("No Results Found"));
//            boolean entityFound1 = results1.stream()
//                    .map(WebElement::getText)
//                    .map(String::trim)
//                    .anyMatch(text -> text.contains(entityName.trim()));
//
//
//            for (WebElement result : results) {
//                System.out.println(result.getText()); // Print each result's text
//            }
//            System.out.println("Search Results:");
//            for (WebElement result : results1) {
//                System.out.println(result.getText()); // Print each result's text
//            }
//            // Print result
//            if (entityNotFound) {
//                return "Not Found";
//
//            } else if (entityFound1) {
//                return "Found";
//            } else {
//                return "Not Found";
//            }
//        } finally {
//            // Close the browser
//            driver.quit();
//        }
//    }
//
//    public String searchKentucky(String entityName) {
//        // Setup WebDriver using WebDriverManager
//        WebDriver driver = getHeadlessDriver("chrome");
//
//
//        try {
//            // for state kenctucky
//            driver.get("https://sosbes.sos.ky.gov/BusSearchNProfile/search.aspx?na=true");
//
//            // Maximize window
//            driver.manage().window().maximize();
//
//            // Locate the input box and enter the entity name
//            try {
//                Thread.sleep(new Random().nextInt(3000) + 2000); // Random 2-7 seconds wait
//            } catch (InterruptedException e) {
//                e.printStackTrace(); // Print stack trace for debugging
//                Thread.currentThread().interrupt(); // Restore the interrupted status
//            }
//
//            JavascriptExecutor js = (JavascriptExecutor) driver;
//            js.executeScript("window.scrollBy(0,300);");
//            // Random 2-7 seconds wait
//            Actions actions = new Actions(driver);
//            actions.moveToElement(driver.findElement(By.id("MainContent_txtSearchNA"))).perform();
//
//            WebElement inputBox = driver.findElement(By.id("MainContent_txtSearchNA")); // Update with actual ID
//            inputBox.sendKeys(entityName);
//
//            // Click the search button
//            WebElement searchButton = driver.findElement(By.id("MainContent_BSearchNA")); // Update with actual ID
//            searchButton.click();
//
//            // Wait for search results to load
//            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
//            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("MainContent_LblResults"))); // Update with actual ID
//
//            // Get the list of search results
//            List<WebElement> results = driver.findElements(By.xpath("//span[@id='MainContent_LblResults']"));
//            List<WebElement> results1 = driver.findElements(By.xpath("//table[@id='MainContent_gvSearchResults']"));
//
//
//            // Check if the entity is present in the results
//            boolean entityNotFound = results.stream()
//                    .map(WebElement::getText) // Extract text
//                    .anyMatch(text -> text.startsWith("0 Results Found"));
//            boolean entityFound1 = results1.stream()
//                    .map(WebElement::getText)
//                    .map(String::trim)
//                    .anyMatch(text -> text.contains(entityName.trim()));
//
//
//            for (WebElement result : results) {
//                System.out.println(result.getText()); // Print each result's text
//            }
//            System.out.println("Search Results:");
//            for (WebElement result : results1) {
//                System.out.println(result.getText()); // Print each result's text
//            }
//            // Print result
//            if (entityNotFound) {
//                return "Not Found";
//
//            } else if (entityFound1) {
//                return "Found";
//            } else {
//                return "Not Found";
//            }
//        } finally {
//            // Close the browser
//            driver.quit();
//        }
//    }
//    public String searchLouisian(String entityName) {
//        // Setup WebDriver using WebDriverManager
//        WebDriver driver = getHeadlessDriver("chrome");
//
//
//        try {
//            // for state Louisian
//            driver.get("https://coraweb.sos.la.gov/CommercialSearch/CommercialSearch.aspx");
//
//            // Maximize window
//            driver.manage().window().maximize();
//
//            // Locate the input box and enter the entity name
//            try {
//                Thread.sleep(new Random().nextInt(5000) + 2000); // Random 2-7 seconds wait
//            } catch (InterruptedException e) {
//                e.printStackTrace(); // Print stack trace for debugging
//                Thread.currentThread().interrupt(); // Restore the interrupted status
//            }
//
//            JavascriptExecutor js = (JavascriptExecutor) driver;
//            js.executeScript("window.scrollBy(0,300);");
//            Actions actions = new Actions(driver);
//            actions.moveToElement(driver.findElement(By.id("ctl00_cphContent_txtEntityName"))).perform();
//
//            WebElement inputBox = driver.findElement(By.id("ctl00_cphContent_txtEntityName")); // Update with actual ID
//            inputBox.sendKeys(entityName);
//
//            // Click the search button
//            WebElement searchButton = driver.findElement(By.id("btnSearch")); // Update with actual ID
//            searchButton.click();
//
//            // Wait for search results to load
//            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
//            wait.until(ExpectedConditions.or(
//                    ExpectedConditions.visibilityOfElementLocated(By.id("ctl00_cphContent_pnlButtonsTop")),  // Success case
//                    ExpectedConditions.visibilityOfElementLocated(By.id("ctl00_cphContent_trBoldMessage")) // Error case
//            )); // Update with actual ID
//
//            // Get the list of search results
//            List<WebElement> results = driver.findElements(By.id("ctl00_cphContent_pnlButtonsTop")); // Adjust XPath
//
//
//            // Print result
//            if (results.isEmpty()) {
//                return "Not Found";
//            } else {
//                return "Found";
//            }
//        } finally {
//            // Close the browser
//            driver.quit();
//        }
//    }
//    public String searchMississippi(String entityName) {
//
//        WebDriver driver = getHeadlessDriver("chrome");
//
//
//        try {
//            //for state Mississippi
//            driver.get("https://corp.sos.ms.gov/corp/portal/c/page/corpBusinessIdSearch/portal.aspx#clear=1");
//            driver.manage().window().maximize();
//
//            Thread.sleep(new Random().nextInt(5000) + 2000); // Random wait to mimic human interaction
//
//            JavascriptExecutor js = (JavascriptExecutor) driver;
//            js.executeScript("window.scrollBy(0,300);");
//
//            Actions actions = new Actions(driver);
//            WebElement inputBox = driver.findElement(By.id("businessNameTextBox"));
//            actions.moveToElement(inputBox).perform();
//            inputBox.sendKeys(entityName);
//
//            WebElement searchButton = driver.findElement(By.id("businessNameSearchButton"));
//            searchButton.click();
//
//            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
//            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("businessSearchResultsDiv")));
//
//            // **Check if "No Match Found" message appears**
//            try {
//                WebElement noMatchElement = driver.findElement(By.xpath("//span[contains(text(), 'No Matches Found.')]"));
//                if (noMatchElement.isDisplayed()) {
//                    return "Not Found";
//                }
//            } catch (Exception e) {
//                // If no "No Match Found" message, assume entity is present
//            }
//
//            return "Found";
//        } catch (Exception e) {
//            e.printStackTrace();
//            return "An error occurred while checking the entity.";
//        } finally {
//            driver.quit();
//        }
//    }
//    public String searchMassachusetts(String entityName) {
//        WebDriver driver = getHeadlessDriver("chrome");
//
//
//        try {
//            // for state Massachusetts
//            driver.get("https://corp.sec.state.ma.us/corpweb/corpsearch/CorpSearch.aspx");
//            driver.manage().window().maximize();
//
//            Thread.sleep(new Random().nextInt(5000) + 2000); // Random wait to mimic human interaction
//
//            JavascriptExecutor js = (JavascriptExecutor) driver;
//            js.executeScript("window.scrollBy(0,300);");
//
//            Actions actions = new Actions(driver);
//            WebElement inputBox = driver.findElement(By.id("MainContent_txtEntityName"));
//            actions.moveToElement(inputBox).perform();
//            inputBox.sendKeys(entityName);
//
//            WebElement searchButton = driver.findElement(By.id("MainContent_btnSearch"));
//            searchButton.click();
//
//            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
//            wait.until(ExpectedConditions.or(
//                    ExpectedConditions.visibilityOfElementLocated(By.id("MainContent_SearchControl_ltNumOfRecords")),
//                    ExpectedConditions.visibilityOfElementLocated(By.id("MainContent_lblMessage"))
//            ));
//
//            if (!driver.findElements(By.id("MainContent_lblMessage")).isEmpty()) {
//                return "Not Found";
//            } else {
//                return "Found";
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace(); // Log the error
//            return "Error: " + e.getMessage(); // Return full exception message
//        } finally {
//            driver.quit();
//        }
//    }
//    public String searchIowa(String entityName) {
//        WebDriver driver = getHeadlessDriver("chrome");
//
//
//        try {
//            // state of Iowa
//            driver.get("https://sos.iowa.gov/search/business/(S(zslobrjan2amayujybt0bx45))/search.aspx");
//            driver.manage().window().maximize();
//
//            // Wait randomly to mimic human interaction
//            Thread.sleep(new Random().nextInt(5000) + 2000);
//
//            // Scroll to input field
//            JavascriptExecutor js = (JavascriptExecutor) driver;
//            js.executeScript("window.scrollBy(0,300);");
//
//            // Locate and enter entity name
//            Actions actions = new Actions(driver);
//            WebElement inputBox = driver.findElement(By.id("txtName"));
//            actions.moveToElement(inputBox).perform();
//            inputBox.sendKeys(entityName);
//
//            // Click search button (Handling reCAPTCHA)
//            WebElement searchButton = driver.findElement(By.name("btnNumber"));
//            js.executeScript("arguments[0].click();", searchButton);  // Click using JS executor
//
//            // Wait for results to load
//            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
//            wait.until(ExpectedConditions.or(
//                    ExpectedConditions.visibilityOfElementLocated(By.id("noResults")),
//                    ExpectedConditions.visibilityOfElementLocated(By.className("results-count"))
//            ));
//
//
//            // Check for "No Match Found"
//            if (!driver.findElements(By.id("noResults")).isEmpty()) {
//                return "Not Found";
//            } else {
//                return "Found";
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace(); // Log the error
//            return "Error: " + e.getMessage(); // Return full exception message
//        } finally {
//            driver.quit();
//        }
//    }
//    public String searchMichigan(String entityName) {
//
//        WebDriver driver = getHeadlessDriver("chrome");
//
//
//
//        try {
//            // state of michigan
//            driver.get("https://cofs.lara.state.mi.us/SearchApi/Search/Search");
//            driver.manage().window().maximize();
//
//            // Wait randomly to mimic human interaction
//            Thread.sleep(new Random().nextInt(5000) + 2000);
//
//            // Scroll to input field
//            JavascriptExecutor js = (JavascriptExecutor) driver;
//            js.executeScript("window.scrollBy(0,300);");
//
//            // Locate and enter entity name
//            Actions actions = new Actions(driver);
//            WebElement inputBox = driver.findElement(By.id("txtEntityName"));
//            actions.moveToElement(inputBox).perform();
//            inputBox.sendKeys(entityName);
//            Thread.sleep(2000);
//
//            // Click search button
//            WebElement searchButton = driver.findElement(By.id("SearchSubmit"));
//            searchButton.click();
//
//            // Wait for results to load
//            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
//            Thread.sleep(10000);
//            wait.until(ExpectedConditions.or(ExpectedConditions.visibilityOfElementLocated(By.id("errorMessage")),
//                    ExpectedConditions.visibilityOfElementLocated(By.id("TotalRecords"))));
//            // Get the list of search results
//            List<WebElement> results = driver.findElements(By.xpath("//label[@id='TotalRecords']"));
//            List<WebElement> results1 = driver.findElements(By.xpath("//label[@id='errorMessage']"));
//            // Debugging Statements
//            for (WebElement result : results1) {
//                System.out.println(" result: "+result.getText()); // Print each result's text
//            }
//            for (WebElement result : results) {
//                System.out.println(" result: "+result.getText()); // Print each result's text
//            }
//            System.out.println("Error Message Exists: " + results1);
//            System.out.println("Entity Data Exists: " + results);
//
//            // Corrected Logic
//            if (results.isEmpty()) {
//                return "Not Found";
//            }
//            else  {
//                return "Found";
//            }
//
//
//        } catch (Exception e) {
//            e.printStackTrace(); // Log the error
//            return "Error: " + e.getMessage(); // Return full exception message
//        } finally {
//            driver.quit();
//        }
//    }
//
//
//
//    private String searchIdaho(String companyName) {
//        WebDriver driver = getHeadlessDriver("chrome");
//        try {
//            driver.get(IDAHO);
//            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
//            wait.until(ExpectedConditions.presenceOfElementLocated(By.className("search-input")))
//                    .sendKeys(companyName);
//            driver.findElement(By.className("search-button")).click();
//            boolean noRecords = !driver.findElements(By.xpath("//div[contains(@class, 'empty-placeholder-wrapper')]")).isEmpty();
//            boolean hasResults = !driver.findElements(By.xpath("//div[contains(@class, 'table-wrapper')]")).isEmpty();
//
//            return hasResults ? "Found" : "Not Found";
//        } catch (Exception e) {
//            e.printStackTrace(); // Log the error
//            return "Error: " + e.getMessage(); // Return full exception message
//        } finally {
//            driver.quit();
//        }
//    }
//    public String searchMaine(String companyName) {
//        WebDriver driver = getHeadlessDriver("chrome");
//
//        long startTime = System.currentTimeMillis();
//
//        try {
//            driver.get("https://apps3.web.maine.gov/nei-sos-icrs/ICRS?MainPage=x");
//            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
//
//            // Locate the search input box and enter text
//            WebElement searchInput = wait.until(ExpectedConditions.presenceOfElementLocated(
//                    By.name("WAISqueryString")));
//            searchInput.clear();
//            searchInput.sendKeys(companyName);
//
//            // Locate and click the search button
//            WebElement searchButton = wait.until(ExpectedConditions.elementToBeClickable(
//                    By.cssSelector(".g-recaptcha.button.blue")));
//
//            searchButton.click();
//            try {
//                WebElement resultElement = wait.until(
//                        ExpectedConditions.presenceOfElementLocated(
//                                By.xpath("//b[contains(text(), 'Showing') or contains(text(), 'Nothing found')]")
//                        )
//                );
//
//                String resultText = resultElement.getText();
//                return resultText.contains("Nothing found")
//                        ? "Not Found: "
//                        : "Found";
//
//            } catch (TimeoutException e) {
//                return "⛔ Error: No result message found within the timeout.";
//            }
//
//
//        } catch (TimeoutException e) {
//            return "⚠ Error: Search took too long. Please try again later.";
//        } catch (Exception e) {
//            e.printStackTrace(); // Log the error
//            return "Error: " + e.getMessage(); // Return full exception message
//        } finally {
//            driver.quit();
//            long endTime = System.currentTimeMillis();
//            System.out.println("Search Execution Time: " + (endTime - startTime) + " ms");
//        }
//    }
//
//    public String searchMissouri(String companyName) {
//        WebDriver driver = getHeadlessDriver("chrome");
//
//        try {
//            driver.get(MISSOURI);
//
//            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
//
//            // Switch to the first iframe (adjust if iframe index or id/name differs)
//            driver.switchTo().frame(0);
//
//            // Wait for the business name input field inside iframe and enter company name
//            WebElement nameInput = wait.until(ExpectedConditions.presenceOfElementLocated(
//                    By.id("ctl00_ctl00_ContentPlaceHolderMain_ContentPlaceHolderMainSingle_ppBESearch_bsPanel_tbBusinessName")));
//            nameInput.clear();
//            nameInput.sendKeys(companyName);
//
//            // Select "Exact Match" option in dropdown
//            WebElement selectElem = wait.until(ExpectedConditions.presenceOfElementLocated(
//                    By.id("ctl00_ctl00_ContentPlaceHolderMain_ContentPlaceHolderMainSingle_ppBESearch_bsPanel_ddlNameSearchMethod")));
//            Select select = new Select(selectElem);
//            select.selectByValue("3");  // Exact Match
//
//            // Click the Search button inside iframe
//            WebElement searchBtn = wait.until(ExpectedConditions.elementToBeClickable(
//                    By.id("ctl00_ctl00_ContentPlaceHolderMain_ContentPlaceHolderMainSingle_ppBESearch_bsPanel_stdbtnSearch_divStandardButtonTop")));
//            searchBtn.click();
//
//            // Wait for either results or the "No records to display." message
//            WebDriverWait resultWait = new WebDriverWait(driver, Duration.ofSeconds(30));
//            resultWait.until(ExpectedConditions.or(
//                    ExpectedConditions.presenceOfElementLocated(By.xpath("//div[normalize-space(text())='No records to display.']")),
//                    ExpectedConditions.presenceOfElementLocated(By.xpath("//table//tr[td]"))  // generic check for any table row with data
//            ));
//
//            // Check for the "No records to display." message
//            boolean noResults = !driver.findElements(By.xpath("//div[normalize-space(text())='No records to display.']")).isEmpty();
//
//            // Switch back to the main document
//            driver.switchTo().defaultContent();
//
//            return noResults ? "Not Found" : "Found";
//
//        } catch (Exception e) {
//            System.out.println(driver.getPageSource());  // For debugging: print page source
//            e.printStackTrace();
//            return "Error: " + e.getMessage();
//        } finally {
//            driver.quit();
//        }
//    }
//
//
//
//    public String searchNevada(String companyName) {
//        WebDriver driver = getHeadlessDriver("chrome");
//
//        try {
//            driver.get(NEVADA);
//            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
//
//            WebElement entityInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("BusinessSearch_Index_txtEntityName")));
//            entityInput.sendKeys(companyName);
//            Thread.sleep(1000);
//            wait.until(ExpectedConditions.elementToBeClickable(By.id("BusinessSearch_Index_rdExactMatch"))).click();
//            wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//input[@type='submit' or @value='Search']"))).click();
//
//            boolean noRecords = !driver.findElements(By.xpath("//li[@class='error_message' and contains(text(), 'No records found')]")).isEmpty();
//            boolean hasResults = !driver.findElements(By.id("grid_businessList")).isEmpty();
//
//            return noRecords ? "Not Found" : "Found";
//        } catch (Exception e) {
//            e.printStackTrace(); // Log the error
//            return "Error: " + e.getMessage(); // Return full exception message
//        } finally {
//            driver.quit();
//        }
//    }
//
//    public String searchNewJersey(String companyName) {
//        WebDriver driver = getHeadlessDriver("chrome");
//
//        try {
//            driver.get(NEW_JERSEY);
//            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
//
//            WebElement businessInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("BusinessName")));
//            businessInput.sendKeys(companyName);
//            wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//input[@type='submit' and @value='Search']"))).click();
//            Thread.sleep(2000);
//            boolean noResults = !driver.findElements(By.xpath("//tbody/tr/td[contains(text(), 'No Results Found')]")).isEmpty();
//            boolean hasResults = !driver.findElements(By.id("DataTables_Table_0")).isEmpty();
//
//            return noResults ? "Not Found" : "Found";
//        } catch (Exception e) {
//            e.printStackTrace(); // Log the error
//            return "Error: " + e.getMessage(); // Return full exception message
//        } finally {
//            driver.quit();
//        }
//    }
//
//    public String searchNewYork(String companyName) {
//        WebDriver driver = getHeadlessDriver("chrome");
//
//        try {
//            driver.get(NEW_YORK);
//
//            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
//            driver.navigate().refresh();
//            new Select(wait.until(ExpectedConditions.presenceOfElementLocated(By.id("searchFunctionality"))))
//                    .selectByVisibleText("Contains");
//
//            WebElement checkbox = wait.until(ExpectedConditions.elementToBeClickable(By.id("LimitedLiabilityCompany")));
//            if (!checkbox.isSelected()) checkbox.click();
//            Thread.sleep(1000);
//            WebElement entityInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("entityname")));
//            entityInput.sendKeys(companyName);
//            wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[contains(text(), 'Search the Database')]"))).click();
//            Thread.sleep(1000);
//            boolean noResults = !driver.findElements(By.xpath("//p[contains(@class, 'messages') and contains(text(), 'No business entities were found')]")).isEmpty();
//            boolean hasResults = !driver.findElements(By.cssSelector(".v-data-table__wrapper table tbody")).isEmpty();
//
//            return noResults ? "Not Found" : "Found";
//        } catch (Exception e) {
//            e.printStackTrace(); // Log the error
//            return "Error: " + e.getMessage(); // Return full exception message
//        } finally {
//            driver.quit();
//        }
//    }
//
//    public String searchNorthCarolina(String companyName) {
//        WebDriver driver = getHeadlessDriver("chrome");
//
//        try {
//            driver.get(NORTH_CAROLINA);
//            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
//
//            WebElement entityInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("SearchCriteria")));
//            entityInput.sendKeys(companyName);
//            wait.until(ExpectedConditions.elementToBeClickable(By.id("SubmitButton"))).click();
//            Thread.sleep(1000);
//            List<WebElement> noRecordsMessage = driver.findElements(By.id("resultsSection"));
//            Thread.sleep(2000);
//            boolean noResults = !noRecordsMessage.isEmpty();
//            return noResults ? "Found" : "Not Found";
//        } catch (Exception e) {
//            e.printStackTrace(); // Log the error
//            return "Error: " + e.getMessage(); // Return full exception message
//        } finally {
//            driver.quit();
//        }
//    }
//
//    public String searchAlabama(String companyName) {
//        WebDriver driver = getHeadlessDriver("chrome");
//
//        try {
//            driver.get(ALABAMA);
//            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
//
//            WebElement entityInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("edit-keys")));
//            entityInput.sendKeys(companyName);
//            wait.until(ExpectedConditions.elementToBeClickable(By.id("edit-submit"))).click();
//
//            boolean noResults = !driver.findElements(By.xpath("//div[contains(text(), 'Your search yielded no results.')]")).isEmpty();
//            boolean hasResults = !driver.findElements(By.xpath("//table[contains(@class, 'views-table')]")).isEmpty();
//
//            return hasResults ? "Found" : "Not Found";
//        } catch (Exception e) {
//            e.printStackTrace(); // Log the error
//            return "Error: " + e.getMessage(); // Return full exception message
//        } finally {
//            driver.quit();
//        }
//    }
//
//    public String searchArkansas(String companyName) {
//        WebDriver driver = getHeadlessDriver("chrome");
//
//        try {
//            driver.get(ARKANSAS);
//            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
//
//            WebElement entityInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("search-text")));
//            entityInput.sendKeys(companyName);
//            entityInput.sendKeys(Keys.ENTER);
//
//            boolean noRecords = !driver.findElements(By.xpath("//div[contains(@class, 'col-md-7') and contains(text(), 'Check your spelling')]")).isEmpty();
//            boolean hasResults = !driver.findElements(By.xpath("//div[contains(@id, 'col-md-7')]")).isEmpty();
//
//            return hasResults ? "Found" : "Not Found";
//        } catch (Exception e) {
//            e.printStackTrace(); // Log the error
//            return "Error: " + e.getMessage(); // Return full exception message
//        } finally {
//            driver.quit();
//        }
//    }
//
//    public String searchCalifornia(String companyName) {
//        WebDriver driver = getHeadlessDriver("chrome");
//
//        try {
//            driver.get(CALIFORNIA);
//            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
//
//            wait.until(ExpectedConditions.presenceOfElementLocated(By.className("search-input"))).sendKeys(companyName, Keys.ENTER);
//
//            boolean noRecords = !driver.findElements(By.xpath("//div[contains(@class, 'empty-placeholder-wrapper')]")).isEmpty();
//            boolean hasResults = !driver.findElements(By.xpath("//div[contains(@id, 'flex-row')]")).isEmpty();
//
//            return hasResults ? "Found" : "Not Found";
//        } catch (Exception e) {
//            e.printStackTrace(); // Log the error
//            return "Error: " + e.getMessage(); // Return full exception message
//        } finally {
//            driver.quit();
//        }
//    }
//
//    public String searchColorado(String companyName) {
//        WebDriver driver = getHeadlessDriver("chrome");
//
//        try {
//            driver.get(COLORADO);
//            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
//
//            wait.until(ExpectedConditions.presenceOfElementLocated(By.className("gsc-i-id1"))).sendKeys(companyName, Keys.ENTER);
//
//            boolean noRecords = !driver.findElements(By.xpath("//div[contains(@class, 'gs-snippet')]")).isEmpty();
//            boolean hasResults = !driver.findElements(By.xpath("//div[contains(@id, 'gsc-result-info')]")).isEmpty();
//
//            return hasResults ? "Found" : "Not Found";
//        } catch (Exception e) {
//            e.printStackTrace(); // Log the error
//            return "Error: " + e.getMessage(); // Return full exception message
//        } finally {
//            driver.quit();
//        }
//    }
//
//    public String searchConnecticut(String companyName) {
//        WebDriver driver = getHeadlessDriver("chrome");
//
//
//        try {
//            driver.get(CONNECTICUT);
//            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
//
//            wait.until(ExpectedConditions.presenceOfElementLocated(By.className("cg-c-search__input"))).sendKeys(companyName, Keys.ENTER);
//
//            boolean noRecords = !driver.findElements(By.xpath("//div[contains(@class, 'gs-snippet')]")).isEmpty();
//            boolean hasResults = !driver.findElements(By.xpath("//div[contains(@id, 'gsc-result-info')]")).isEmpty();
//
//            return hasResults ? "Found" : "Not Found";
//        } catch (Exception e) {
//            e.printStackTrace(); // Log the error
//            return "Error: " + e.getMessage(); // Return full exception message
//        } finally {
//            driver.quit();
//        }
//    }
//
//    private String searchDelaware(String companyName) {
//        WebDriver driver = getHeadlessDriver("chrome");
//
//        try {
//            driver.get(DELAWARE);
//
//            // Short, explicit waits only
//            WebDriverWait wait20 = new WebDriverWait(driver, Duration.ofSeconds(20));
//
//            // Best-effort cookie banner close (non-blocking)
//            try {
//                WebElement accept = driver.findElement(By.id("onetrust-accept-btn-handler"));
//                if (accept.isDisplayed()) accept.click();
//            } catch (Exception ignored) {}
//
//            // --- Locate the Entity/Business Name input (top doc; then first-level iframes) ---
//            WebElement input = null;
//            By[] tries = new By[] {
//                    By.id("fb-search-input"),
//                    By.cssSelector("input[name*='entity' i][type='text']"),
//                    By.cssSelector("input[id*='entity' i][type='text']"),
//                    By.cssSelector("input[name*='business' i][type='text']"),
//                    By.cssSelector("input[id*='business' i][type='text']"),
//                    By.xpath("//label[contains(translate(.,'ENTITY','entity'),'entity name')]/following::input[1]")
//            };
//
//            // Try in top document
//            for (By by : tries) {
//                try {
//                    input = new WebDriverWait(driver, Duration.ofSeconds(2))
//                            .until(ExpectedConditions.presenceOfElementLocated(by));
//                    break;
//                } catch (TimeoutException ignored) {}
//            }
//
//            // If not found, try first-level iframes (quick probes)
//            if (input == null) {
//                var frames = driver.findElements(By.cssSelector("iframe, frame"));
//                for (int i = 0; i < frames.size() && input == null; i++) {
//                    try {
//                        driver.switchTo().defaultContent();
//                        driver.switchTo().frame(i);
//                        for (By by : tries) {
//                            try {
//                                input = new WebDriverWait(driver, Duration.ofSeconds(2))
//                                        .until(ExpectedConditions.presenceOfElementLocated(by));
//                                break;
//                            } catch (TimeoutException ignored) {}
//                        }
//                    } catch (Exception ignored) {}
//                }
//                // If still not found, reset to default content
//                if (input == null) driver.switchTo().defaultContent();
//            }
//
//            if (input == null) return "Not Found"; // bounded exit
//
//            // Type the name
//            input.clear();
//            input.sendKeys(companyName);
//
//            // --- Click Search/Submit (prefer same form; JS fallback) ---
//            WebElement submitBtn = null;
//            By[] btns = new By[] {
//                    By.cssSelector("button[type='submit']"),
//                    By.xpath("//button[normalize-space()='Search' or normalize-space()='Submit']"),
//                    By.xpath("//input[@type='submit' and (@value='Search' or @value='Submit')]"),
//                    By.xpath("(ancestor::form//button|ancestor::form//input[@type='submit'])[1]")
//            };
//
//            // Try buttons near the input first
//            for (By by : btns) {
//                var inForm = input.findElements(by);
//                if (!inForm.isEmpty()) { submitBtn = inForm.get(0); break; }
//            }
//            // Then anywhere on the page
//            if (submitBtn == null) {
//                for (By by : btns) {
//                    var any = driver.findElements(by);
//                    if (!any.isEmpty()) { submitBtn = any.get(0); break; }
//                }
//            }
//
//            if (submitBtn != null) {
//                try { submitBtn.click(); }
//                catch (Exception e) { ((JavascriptExecutor) driver).executeScript("arguments[0].click();", submitBtn); }
//            } else {
//                input.sendKeys(Keys.ENTER);
//            }
//
//            // --- Fast, bounded results wait (≤ 8s) ---
//            WebDriverWait wait8 = new WebDriverWait(driver, Duration.ofSeconds(8));
//            wait8.until(ExpectedConditions.or(
//                    ExpectedConditions.presenceOfElementLocated(By.cssSelector("table tbody tr")),
//                    ExpectedConditions.presenceOfElementLocated(By.cssSelector("#search-result-count, [id*='result-count']")),
//                    ExpectedConditions.presenceOfElementLocated(By.xpath("//td[contains(@class,'dataTables_empty') and contains(.,'No data')]")),
//                    ExpectedConditions.presenceOfElementLocated(By.xpath("//*[contains(translate(.,'NO RECORD','no record'),'no record')]"))
//            ));
//
//            boolean hasRows   = !driver.findElements(By.cssSelector("table tbody tr")).isEmpty();
//            boolean hasCount  = !driver.findElements(By.cssSelector("#search-result-count, [id*='result-count']")).isEmpty();
//            boolean noRecords =
//                    !driver.findElements(By.xpath("//td[contains(@class,'dataTables_empty') and contains(.,'No data')]")).isEmpty() ||
//                            !driver.findElements(By.xpath("//*[contains(translate(.,'NO RECORD','no record'),'no record')]")).isEmpty();
//
//            boolean notFound = noRecords || (!hasRows && !hasCount);
//            return notFound ? "Not Found" : "Found";
//
//        } catch (TimeoutException te) {
//            return "Not Found"; // quick exit on wait timeout
//        } catch (Exception e) {
//            return "Not Found";
//        } finally {
//            try { driver.quit(); } catch (Exception ignored) {}
//        }
//    }
//
//
//
//    public String searchFlorida(String companyName) {
//        WebDriver driver = getHeadlessDriver("chrome");
//
//        try {
//            driver.get(FLORIDA);
//            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
//
//            wait.until(ExpectedConditions.presenceOfElementLocated(By.className("form-control"))).sendKeys(companyName, Keys.ENTER);
//
//            boolean noRecords = !driver.findElements(By.xpath("//div[contains(@class, 'ezsearch-no-results')]")).isEmpty();
//            boolean hasResults = !driver.findElements(By.xpath("//div[contains(@class, 'ezsearch-result-count')]/p")).isEmpty();
//
//            return hasResults ? "Found" : "Not Found";
//        } catch (Exception e) {
//            e.printStackTrace(); // Log the error
//            return "Error: " + e.getMessage(); // Return full exception message
//        } finally {
//            driver.quit();
//        }
//    }
//
//    public String searchGeorgia(String companyName) {
//        WebDriver driver = getHeadlessDriver("chrome");
//
//        try {
//            driver.get(GEORGIA);
//            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
//
//            wait.until(ExpectedConditions.presenceOfElementLocated(By.id("search-block-division__input")))
//                    .sendKeys(companyName, Keys.ENTER);
//
//            boolean noRecords = !driver.findElements(By.xpath("//div[contains(@class, 'alert-danger')]")).isEmpty();
//            boolean hasResults = !driver.findElements(By.xpath("//div[contains(@class, 'alert-success')]/p")).isEmpty();
//
//            return hasResults ? "Found" : "Not Found";
//        } catch (Exception e) {
//            e.printStackTrace(); // Log the error
//            return "Error: " + e.getMessage(); // Return full exception message
//        } finally {
//            driver.quit();
//        }
//    }
//    private String searchHawaii(String companyName) {
//        WebDriver driver = getHeadlessDriver("chrome");
//
//        try {
//            driver.get(HAWAII);
//            driver.navigate().refresh();
//
//            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
//
//            // 1. Input the company name
//            WebElement input = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("query")));
//            input.clear();
//            input.sendKeys(companyName);
//
//            // 2. Set search type to "Begins with" or "Contains"
//            WebElement searchType = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("beginsWith")));
//            Select searchTypeSelect = new Select(searchType);
//            searchTypeSelect.selectByValue("true"); // or "false" for "Contains..."
//
//            // 3. Click the Search button using JavaScript (more reliable for dynamic pages)
//            WebElement searchBtn = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("button.doSearch")));
//            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", searchBtn);
//
//            // 4. Wait for either results table or "No Results" modal
//            Thread.sleep(500); // short buffer
//            boolean noResults = false;
//            try {
//                wait.withTimeout(Duration.ofSeconds(10))
//                        .until(ExpectedConditions.visibilityOfElementLocated(By.id("noResults")));
//                noResults = true;
//            } catch (TimeoutException e) {
//                // noResults dialog didn't appear, assume results are present
//            }
//
//            return noResults ? "Not Found" : "Found";
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            return "Error: " + e.getMessage();
//        } finally {
//            driver.quit();
//        }
//    }
//
//    public String searchMaryland(String entityName) {
//        WebDriver driver = getHeadlessDriver("chrome");
//
//        try {
//            //for state Maryland
//            driver.get("https://egov.maryland.gov/BusinessExpress/EntitySearch");
//            driver.manage().window().maximize();
//
//            Thread.sleep(new Random().nextInt(5000) + 2000); // Random wait to mimic human interaction
//
//            JavascriptExecutor js = (JavascriptExecutor) driver;
//            js.executeScript("window.scrollBy(0,300);");
//
//            Actions actions = new Actions(driver);
//            WebElement inputBox = driver.findElement(By.id("BusinessName"));
//            actions.moveToElement(inputBox).perform();
//            inputBox.sendKeys(entityName);
//
//            WebElement searchButton = driver.findElement(By.id("searchBus1"));
//            searchButton.click();
//
//            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
//            wait.until(ExpectedConditions.or(
//                    ExpectedConditions.visibilityOfElementLocated(By.id("businessFound")),  // Success case
//                    ExpectedConditions.visibilityOfElementLocated(By.className("textNotice")) // Error case
//            ));
//
//
//            // **Check if "No Match Found" message appears**
//            try {
//                WebElement noMatchElement = driver.findElement(By.className("textNotice"));
//                //need to print web element
//                System.out.println(noMatchElement.getText());
//                if (noMatchElement.isDisplayed()) {
//                    return "Not Found";
//                }
//            } catch (Exception e) {
//                // If no "No Match Found" message, assume entity is present
//            }
//
//            return "Found";
//        } catch (Exception e) {
//            e.printStackTrace(); // Log the error
//            return "Error: " + e.getMessage(); // Return full exception message
//        } finally {
//            driver.quit();
//        }
//    }
//
//    public String checkCompanyOffline(String companyName, String state) {
//        List <Company> companyList = companyRepository.findByState(state);
//
//        for(Company company: companyList){
//            if(company.getCompanyName()!=null && company.getCompanyName().equals(companyName)){
//                return "Found";
//            }
//        }
//        return "Not Found";
//    }
//
////    private WebDriver getHeadlessDriver(String browser) {
////        if ("chrome".equalsIgnoreCase(browser)) {
////            ChromeOptions options = new ChromeOptions();
////            options.addArguments("--headless=new", "--disable-gpu", "--no-sandbox", "--disable-dev-shm-usage", "--disable-blink-features=AutomationControlled");
////            return new ChromeDriver(options);
////        }
////        throw new IllegalArgumentException("Unsupported browser: " + browser);
////    }
//
//
//    public static WebDriver getHeadlessDriver(String browser) {
//        if (!"chrome".equalsIgnoreCase(browser)) {
//            throw new IllegalArgumentException("Unsupported browser: " + browser);
//        }
//
//        // If container envs are set, use bundled binaries
//        String driverPath = System.getenv("CHROMEDRIVER_PATH");
//        String chromeBin  = System.getenv("CHROME_BIN");
//
//        ChromeOptions options = new ChromeOptions();
//        options.addArguments(
//                "--headless=new",
//                "--no-sandbox",
//                "--disable-dev-shm-usage",
//                "--window-size=1366,768",
//                "--disable-gpu",
//                "--disable-software-rasterizer",
//                "--disable-blink-features=AutomationControlled"
//        );
//
//        if (chromeBin != null && !chromeBin.isBlank()) {
//            // Running in container/ECS
//            System.setProperty("webdriver.chrome.driver",
//                    driverPath != null ? driverPath : "/usr/local/bin/chromedriver");
//            options.setBinary(chromeBin);
//            ChromeDriverService svc = new ChromeDriverService.Builder()
//                    .withLogOutput(System.out) // helpful logs
//                    .build();
//            return new ChromeDriver(svc, options);
//        } else {
//
//            io.github.bonigarcia.wdm.WebDriverManager.chromedriver().setup();
//            return new ChromeDriver(options);
//        }
//    }
//
//
//}


package com.trivine.llc.api.service;

import com.trivine.llc.api.entity.Company;
import com.trivine.llc.api.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.*;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebScrapingService {
    private final CompanyRepository companyRepository;

    private static final String TEXAS = "https://mycpa.cpa.state.tx.us/coa/search.do";

    // ===== Public entry (use from controller) =====
    @Cacheable(cacheNames = "entityCheck", key = "#state + '|' + #companyName.toLowerCase().trim()")
    public String checkCompany(String companyName, String state) {
        String online = withHardCap(() -> checkCompanyOnlineRaw(companyName, state), Duration.ofSeconds(9));
        if ("Found".equalsIgnoreCase(online) || "Not Found".equalsIgnoreCase(online)) return online;
        return checkCompanyOffline(companyName, state); // fast fallback
    }

    // ===== Online (uncapped), called inside hard-cap =====
    private String checkCompanyOnlineRaw(String companyName, String state) {
        String s = state == null ? "" : state.toLowerCase(Locale.ROOT).trim();
        try {
            return switch (s) {
                case "texas" -> searchTexasFast(companyName);
                // TODO: add other states here as you convert them, like:
                // case "california" -> searchCaliforniaFast(companyName);
                default -> "⚠ Unsupported state: " + state;
            };
        } catch (Exception e) {
            log.warn("Online check error state={}, name='{}': {}", state, companyName, e.toString());
            return "Error";
        }
    }

    private static String withHardCap(Callable<String> work, Duration cap) {
        ExecutorService ex = Executors.newSingleThreadExecutor();
        try {
            Future<String> f = ex.submit(work);
            return f.get(cap.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException te) {
            return "Timeout";
        } catch (Exception e) {
            return "Error";
        } finally {
            ex.shutdownNow();
        }
    }

    // ===== Offline fallback (DB) =====
    public String checkCompanyOffline(String companyName, String state) {
        List<Company> list = companyRepository.findByState(state);
        for (Company c : list) {
            if (c.getCompanyName() != null && c.getCompanyName().equalsIgnoreCase(companyName)) {
                return "Found";
            }
        }
        return "Not Found";
    }

    // ===== Texas (fast, short waits, no long sleeps) =====
    private String searchTexasFast(String companyName) {
        WebDriver driver = null;
        try {
            driver = newHeadlessChrome();
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(10));
            driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(8));
            driver.manage().timeouts().implicitlyWait(Duration.ZERO);

            driver.get(TEXAS);
            WebElement input = new WebDriverWait(driver, Duration.ofSeconds(8))
                    .until(ExpectedConditions.presenceOfElementLocated(By.id("name")));
            input.clear();
            input.sendKeys(companyName);

            ((JavascriptExecutor) driver).executeScript("document.getElementById('submitBtn').click();");

            long deadline = System.nanoTime() + Duration.ofSeconds(8).toNanos();
            while (System.nanoTime() < deadline) {
                // "no data" row present?
                if (!driver.findElements(By.cssSelector("td.dataTables_empty")).isEmpty()) return "Not Found";
                // any result row?
                if (!driver.findElements(By.cssSelector("table.dataTable tbody tr")).isEmpty()) {
                    boolean emptyNow = !driver.findElements(By.cssSelector("td.dataTables_empty")).isEmpty();
                    return emptyNow ? "Not Found" : "Found";
                }
                try { Thread.sleep(150); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
            }
            return "Timeout";
        } catch (Exception e) {
            log.debug("Texas scrape error: ", e);
            return "Error";
        } finally {
            if (driver != null) try { driver.quit(); } catch (Exception ignore) {}
        }
    }

    // ===== Headless Chrome using Docker-provided Chrome/Driver =====
    private static WebDriver newHeadlessChrome() {
        String driverPath = System.getenv("CHROMEDRIVER_PATH");
        String chromeBin  = System.getenv("CHROME_BIN");

        ChromeOptions options = new ChromeOptions();
        options.addArguments(
                "--headless=new","--no-sandbox","--disable-dev-shm-usage",
                "--window-size=1366,768","--disable-gpu","--disable-software-rasterizer",
                "--disable-blink-features=AutomationControlled","--disable-extensions",
                "--blink-settings=imagesEnabled=false","--disable-features=Translate,BackForwardCache"
        );
        options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
        options.setExperimentalOption("useAutomationExtension", false);

        if (chromeBin != null && !chromeBin.isBlank()) {
            System.setProperty("webdriver.chrome.driver",
                    (driverPath != null && !driverPath.isBlank()) ? driverPath : "/usr/local/bin/chromedriver");
            options.setBinary(chromeBin);
            ChromeDriverService svc = new ChromeDriverService.Builder().withLogOutput(System.out).build();
            return new ChromeDriver(svc, options);
        } else {
            return new ChromeDriver(options); // local dev fallback
        }
    }
}