package org.rathercruel.bot.commands.roles;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.json.JSONArray;
import org.json.JSONObject;
import org.rathercruel.bot.main.BotConfiguration;
import org.rathercruel.bot.main.JsonConfig;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;

/**
 * @author Rather Cruel
 */
public class SetRoleMessage extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getName().equalsIgnoreCase("set-message-reactions")) {
            Guild guild = event.getGuild();
            boolean hasNoPermissions = true;
            for (int i = 0; i < BotConfiguration.moderatorRoleIDs.size(); i++) {
                if (event.getMember().getRoles().contains(guild.getRoleById(BotConfiguration.moderatorRoleIDs.get(i)))) {
                    i = BotConfiguration.moderatorRoleIDs.size();
                    String reaction = event.getOption("reaction").getAsString();
                    String role = event.getOption("role").getAsRole().getId();
                    String messageID = event.getOption("message-id").getAsString();

                    TextChannel channel = event.getChannel().asTextChannel();
                    Emoji emoji = Emoji.fromFormatted(reaction);

                    channel.addReactionById(messageID, emoji).queue();
                    event.reply("Added " + emoji.getAsReactionCode() + " for " +
                            event.getGuild().getRoleById(role).getAsMention() + ".").setEphemeral(true).queue();

                    BotConfiguration.emojiRoles.put(emoji, Long.parseLong(role));
                    StringBuilder sb = new StringBuilder();
                    String dir = System.getProperty("user.dir");
                    File file = new File(dir + "\\config.json");
                    Scanner sc = null;
                    try {
                        sc = new Scanner(file);
                    } catch (FileNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                    while (sc.hasNext()) {
                        sb.append(sc.nextLine());
                    }
                    JSONObject object = new JSONObject(sb.toString());
                    JSONObject data = object.getJSONObject("bot_data");
                    JSONObject reactRoles = data.getJSONObject("roles").getJSONObject("react_roles");
                    JSONArray roleArr = reactRoles.getJSONArray("roles");
                    JSONObject jsonRole = new JSONObject();
                    jsonRole.put("emoji", emoji.getFormatted());
                    jsonRole.put("id", Long.parseLong(role));

                    System.out.println(emoji.getFormatted() + ": " + event.getGuild().getRoleById(role).getName() + " has been added to JSON");
                    roleArr.put(jsonRole);
                    try {
                        JsonConfig jsonConfig = new JsonConfig();
                        jsonConfig.update(object);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    hasNoPermissions = false;
                }
            }
            if (hasNoPermissions) {
                String errorMessage = BotConfiguration.noPermissionMessage;
                event.reply(errorMessage.replace("[member]", event.getMember().getAsMention()))
                        .setEphemeral(true).queue();
            }
        }
    }

    @Override
    public void onMessageReactionAdd(MessageReactionAddEvent event) {
        if (BotConfiguration.giveRoleOnReact) {
            User user = event.getUser();
            Member member = event.getMember();
            Guild guild = event.getGuild();
            Emoji userReaction = event.getReaction().getEmoji().asUnicode();
            Role role = guild.getRoleById(BotConfiguration.emojiRoles.get(userReaction));

            if (!user.isBot()) {
                if (!member.getRoles().contains(role))
                    guild.addRoleToMember(member, role).queue();
                else
                    guild.removeRoleFromMember(member, role).queue();
                event.getReaction().removeReaction(user).queue();
            }
        }
    }
}
