package com.dillos.dillobot.scheduled;

import java.time.LocalDate;
import java.util.List;

import com.dillos.dillobot.entities.DiscordChannel;
import com.dillos.dillobot.entities.Subscription;
import com.dillos.dillobot.entities.Subscription.SubscriptionType;
import com.dillos.dillobot.services.DiscordChannelService;
import com.dillos.dillobot.services.DiscordUserService;
import com.dillos.dillobot.services.JDAService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class Birthday {

    Logger log = LoggerFactory.getLogger(Birthday.class);

    JDAService jdaService;

    DiscordChannelService discordChannelService;

    DiscordUserService discordUserService;

    @Autowired
    public Birthday(JDAService jdaService, DiscordChannelService discordChannelService, DiscordUserService discordUserService) {
        this.jdaService = jdaService;
        this.discordChannelService = discordChannelService;
        this.discordUserService = discordUserService;
    }

    @Scheduled(cron = "0 0 0 * * *", zone = "America/New_York")
    @Transactional
    public void sayHappyBirthday() {
        List<DiscordChannel> subscribedChannels = discordChannelService.get();
        
        Subscription subscription = discordChannelService.getSubscription(SubscriptionType.BIRTHDAY);

        subscribedChannels.removeIf(channel -> {
            return !channel.getSubscriptions().contains(subscription);
        });

        log.info("Running happy birthday scheduled event on {} channels...", subscribedChannels.size());
        
        LocalDate now = LocalDate.now();
    
        discordUserService.get().stream().filter(discordUser -> {
            if (discordUser.getBirthday() != null) {
                int birthday = discordUser.getBirthday().getDayOfYear();
                int today = now.getDayOfYear();

                if (!discordUser.getBirthday().isLeapYear()) {
                    birthday++;
                } if (!now.isLeapYear()) {
                    today++;
                }

                return birthday == today;
            }

            return false;
        }).forEach(discordUserWhoseBirthdayIsToday -> {
            log.info(
                "Saying happy birthday to {} in {} channels...",
                discordUserWhoseBirthdayIsToday.getName(),
                subscribedChannels.size()
            );

            subscribedChannels.stream().forEach(channel -> {
                jdaService.getJda().getTextChannelById(
                    channel.getId()
                ).sendMessage(
                    "Happy Birthday " + discordUserWhoseBirthdayIsToday.getAt() + "!"
                ).queue();
            });
        });
    }
}
