package com.meltwater.smsc;

import com.meltwater.smsc.model.Account;
import com.meltwater.smsc.service.AccountService;
import com.meltwater.smsc.service.SubscriptionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SpringBootApplication
@EnableScheduling
@Slf4j
public class SmscApplication {

	private static final Pattern REGISTER_NUMBER_PATTERN = Pattern.compile("^(number[\\d]+) (\\+[\\d]{11})$");
	private static final Pattern SUBSCRIBE_NUMBER_PATTERN = Pattern.compile("^(subscribe) (number[\\d]+)$");
	private static final Pattern UNSUBSCRIBE_NUMBER_PATTERN = Pattern.compile("^(unsubscribe) (number[\\d]+)$");
	private static final Pattern REGISTER_GROUP_PATTERN = Pattern.compile("^(group[\\d]+) (.*)");
	private static final Pattern MESSAGE_PATTERN = Pattern.compile("^(message) (number[\\d]+) (.*) \"(.*)\"");

	private AccountService accountService;
	private SubscriptionService subscriptionService;

	public SmscApplication(AccountService accountService, SubscriptionService subscriptionService) {
		this.accountService = accountService;
		this.subscriptionService = subscriptionService;
	}

	public void run() throws URISyntaxException, IOException {
		Path path=Paths.get(getClass().getClassLoader().getResource("input_file.txt").toURI());
		Files.lines(path)
			.filter(l -> !l.isEmpty())
			.forEach(line -> {
				String operation = line.substring(0, line.indexOf(" "));
				String sanitizedOperation = operation.replaceAll("\\d", "");
				executeOperation(sanitizedOperation, line);
			});
	}

	private void executeOperation(String operation, String line) {
		log.debug("executing operation {} for line {}", operation, line);
		switch (operation) {
            case "number":
				registerNumber(line);
				break;
			case "group":
				registerGroup(line);
				break;
			case "subscribe":
				subscribeNumber(line);
				break;
			case "unsubscribe":
				unsubscribeNumber(line);
				break;
			case "message":
				sendMessage(line);
				break;
			case "sleep":
				sleep(line);
				break;
			default:
				log.error("Operation {} is unknown for line {}", operation, line);
        }
	}

	private void registerNumber(String line) {
		Matcher matcher = REGISTER_NUMBER_PATTERN.matcher(line);
		if (matcher.find()) {
			try {
				Account account = new Account(matcher.group(1), matcher.group(2));
				accountService.registerNumber(account);
			} catch (RuntimeException ex) {
				log.error("Something went wrong on number registering", ex);
			}
        } else {
			log.info("No match found for registering number in line: " + line);
		}
	}


	private void registerGroup(String line) {
		Matcher matcher = REGISTER_GROUP_PATTERN.matcher(line);
		if (matcher.find()) {
			try {
				List<String> numberPatterns = getNumberPatterns(matcher.group(2));
				accountService.registerGroup(matcher.group(1), numberPatterns);
			} catch (RuntimeException ex) {
				log.error("Something went wrong on group registering", ex);
			}
		} else {
			log.info("No match found for registering group in line: " + line);
		}
	}

	private void subscribeNumber(String line) {
		Matcher matcher = SUBSCRIBE_NUMBER_PATTERN.matcher(line);
		if (matcher.find()) {
			try {
				subscriptionService.subscribeNumber(matcher.group(2));
			} catch (RuntimeException ex) {
				log.error("Something went wrong on number subscription", ex);
			}
		} else {
			log.info("No match found for subscribing number in line: " + line);
		}
	}

	private void unsubscribeNumber(String line) {
		Matcher matcher = UNSUBSCRIBE_NUMBER_PATTERN.matcher(line);
		if (matcher.find()) {
			try {
				subscriptionService.unsubscribeNumber(matcher.group(2));
			} catch (RuntimeException ex) {
				log.error("Something went wrong on number unsubscription", ex);
			}
		} else {
			log.info("No match found for unsubscribing number in line: " + line);
		}
	}

	private void sendMessage(String line) {
		Matcher matcher = MESSAGE_PATTERN.matcher(line);
		if (matcher.find()) {
			try {
				List<String> numberPatterns = getNumberPatterns(matcher.group(3));
				String sourceName = matcher.group(2);
				String message = matcher.group(4);
				if (numberPatterns.get(0).equals("broadcast")) {
					subscriptionService.broadCastMessage(sourceName, message);
				} else if (numberPatterns.get(0).startsWith("group")) {
					subscriptionService.sendGroupMessage(sourceName, numberPatterns.get(0), message);
				} else {
					subscriptionService.sendMessage(sourceName, numberPatterns, message);
				}
			} catch (RuntimeException ex) {
				log.error("Something went wrong on message sending", ex);
			}
		} else {
			log.info("No match found for registering group in line: " + line);
		}
	}

	private void sleep(String line) {
		try {
			long sleepDuration = Long.valueOf(line.split(" ")[1]);
			log.info("Sleeping for {} seconds...", sleepDuration);
			TimeUnit.SECONDS.sleep(sleepDuration);
		} catch (InterruptedException e) {
			log.error("Sleep interrupted", e);
		}
	}

	private List<String> getNumberPatterns(String string) {
		return Arrays.asList(string.split(","));
	}

	public static void main(String[] args) {
		ConfigurableApplicationContext ctx =SpringApplication.run(SmscApplication.class, args);
		AccountService accountService = ctx.getBean(AccountService.class);
		SubscriptionService subscriptionService = ctx.getBean(SubscriptionService.class);

		SmscApplication smscApplication = new SmscApplication(accountService, subscriptionService);
		try {
			smscApplication.run();
		} catch (Exception ex) {
			log.error("Something went wrong ", ex);
		}
	}
}
