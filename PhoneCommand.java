package com.roleplay.phone.command;

import com.roleplay.phone.PhonePlugin;
import com.roleplay.phone.call.CallManager;
import com.roleplay.phone.config.PhoneConfig;
import com.roleplay.phone.gps.GpsManager;
import com.roleplay.phone.gps.editor.GpsEditorManager;
import com.roleplay.phone.gps.model.GpsNode;
import com.roleplay.phone.gps.model.GpsSession;
import com.roleplay.phone.gps.visual.GpsArrowRenderer;
import com.roleplay.phone.gui.PhoneGuiManager;
import com.roleplay.phone.item.PhoneItemFactory;
import com.roleplay.phone.lang.MessageManager;
import com.roleplay.phone.model.PhoneProfile;
import com.roleplay.phone.sms.SmsManager;
import com.roleplay.phone.storage.ProfileManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class PhoneCommand extends Command {
   private final ProfileManager profileManager;
   private final CallManager callManager;
   private final PhoneGuiManager guiManager;
   private final PhoneConfig phoneConfig;
   private final GpsManager gpsManager;
   private final GpsEditorManager editorManager;
   private final GpsArrowRenderer arrowRenderer;
   private final SmsManager smsManager;
   private final MessageManager messageManager;

   public PhoneCommand(ProfileManager profileManager, CallManager callManager, PhoneGuiManager guiManager, PhoneConfig phoneConfig, GpsManager gpsManager, GpsEditorManager editorManager, GpsArrowRenderer arrowRenderer, SmsManager smsManager, MessageManager messageManager) {
      super("phone");
      this.profileManager = profileManager;
      this.callManager = callManager;
      this.guiManager = guiManager;
      this.phoneConfig = phoneConfig;
      this.gpsManager = gpsManager;
      this.editorManager = editorManager;
      this.arrowRenderer = arrowRenderer;
      this.smsManager = smsManager;
      this.messageManager = messageManager;
      this.setDescription("Roleplay Phone, GPS & Messaging command suite");
      this.setUsage("/phone <open|get|sms|call|contact|gps|reload>");
      this.setAliases(List.of("cell", "mobile", "gps", "sms"));
   }

   public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
      if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
         if (!sender.hasPermission("phone.command.reload") && !sender.isOp()) {
            this.messageManager.send(sender, "general.no-permission");
            return true;
         } else {
            this.phoneConfig.reload();
            this.messageManager.reload();
            this.guiManager.getGuiConfig().reload();
            this.gpsManager.reloadRadii();
            this.arrowRenderer.getArrowConfig().loadFromConfig(PhonePlugin.getInstance().getConfig());
            this.messageManager.send(sender, "general.config-reloaded");
            return true;
         }
      } else if (sender instanceof Player) {
         Player player = (Player)sender;
         if (args.length > 0 && args[0].equalsIgnoreCase("gps")) {
            this.handleGpsSubcommand(player, args);
            return true;
         } else if (args.length > 0 && args[0].equalsIgnoreCase("get")) {
            this.handleGetPhone(player, args);
            return true;
         } else {
            String carriedNumber = this.callManager.getFirstCarriedPhoneNumber(player);
            if (carriedNumber == null) {
               if (this.phoneConfig.isRequireInHand()) {
                  this.messageManager.send(player, "general.must-hold-phone");
               } else {
                  this.messageManager.send(player, "general.no-phone");
               }

               return true;
            } else if (args.length != 0 && !args[0].equalsIgnoreCase("open")) {
               switch (args[0].toLowerCase()) {
                  case "sms":
                  case "text":
                  case "msg":
                     this.handleSmsCommand(player, carriedNumber, args);
                     break;
                  case "accept":
                     boolean accepted = this.callManager.acceptCall(player);
                     if (!accepted) {
                        this.messageManager.send(player, "calls.no-incoming-call");
                     }
                     break;
                  case "decline":
                     this.callManager.declineCall(player);
                     this.messageManager.send(player, "calls.call-declined");
                     break;
                  case "hangup":
                     if (this.callManager.isInCall(player.getUniqueId())) {
                        this.callManager.endCall(player.getUniqueId(), "Hung up by user.");
                     } else {
                        this.messageManager.send(player, "calls.not-in-call");
                     }
                     break;
                  case "call":
                     if (args.length < 2) {
                        player.sendMessage(Component.text("Usage: /phone call <number>", NamedTextColor.RED));
                        return true;
                     }

                     String targetNumber = args[1];
                     CallManager.DialResult result = this.callManager.startCall(player, carriedNumber, targetNumber);
                     this.handleDialFeedback(player, result, targetNumber);
                     break;
                  case "contact":
                     this.handleContact(player, carriedNumber, args);
                     break;
                  default:
                     this.sendHelp(player);
               }

               return true;
            } else {
               this.guiManager.openMainMenu(player, carriedNumber);
               return true;
            }
         }
      } else {
         sender.sendMessage(Component.text("Only in-game players can use phone features.", NamedTextColor.RED));
         return true;
      }
   }

   private void handleSmsCommand(Player player, String carriedNumber, String[] args) {
      if (args.length < 3) {
         player.sendMessage(Component.text("Usage: /phone sms <number> <message...>", NamedTextColor.RED));
      } else {
         String targetNumber = args[1];
         String messageBody = String.join(" ", (CharSequence[])Arrays.copyOfRange(args, 2, args.length));
         SmsManager.SendResult result = this.smsManager.sendSms(player, carriedNumber, targetNumber, messageBody);
         switch (result) {
            case SUCCESS:
               this.messageManager.send(player, "sms.message-sent", "target", targetNumber);
               player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5F, 1.6F);
               break;
            case SUCCESS_STORED_OFFLINE:
               this.messageManager.send(player, "sms.message-sent", "target", targetNumber);
               this.messageManager.send(player, "sms.stored-offline");
               player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5F, 1.2F);
               break;
            case TARGET_NOT_FOUND:
               this.messageManager.send(player, "calls.number-not-found");
               break;
            case SENDER_AIRPLANE_MODE:
               this.messageManager.send(player, "calls.airplane-mode-caller");
               break;
            case TARGET_AIRPLANE_MODE:
               this.messageManager.send(player, "sms.target-airplane");
               break;
            case SELF_MESSAGE:
               player.sendMessage(Component.text("✖ You cannot text your own phone number.", NamedTextColor.RED));
               break;
            case EMPTY_MESSAGE:
               this.messageManager.send(player, "sms.empty-message");
         }

      }
   }

   private void handleGpsSubcommand(Player player, String[] args) {
      if (args.length < 2) {
         this.sendGpsHelp(player);
      } else {
         switch (args[1].toLowerCase()) {
            case "move":
               this.arrowRenderer.toggleMoveMode(player);
               break;
            case "editor":
               if (!player.hasPermission("phone.gps.admin") && !player.isOp()) {
                  this.messageManager.send(player, "general.no-permission");
                  return;
               }

               this.editorManager.toggleEditor(player);
               break;
            case "wand":
               if (!player.hasPermission("phone.gps.admin") && !player.isOp()) {
                  this.messageManager.send(player, "general.no-permission");
                  return;
               }

               player.getInventory().addItem(new ItemStack[]{this.editorManager.createWand()});
               player.sendMessage(Component.text("✔ Added GPS Configurator Wand to inventory.", NamedTextColor.GREEN));
               break;
            case "stop":
               if (this.gpsManager.isNavigating(player.getUniqueId())) {
                  this.gpsManager.stopNavigation(player, GpsSession.Status.CANCELLED);
                  this.arrowRenderer.stopTracking(player.getUniqueId());
               } else {
                  this.messageManager.send(player, "gps.no-active-route");
               }
               break;
            case "list":
               List<GpsNode> destinations = this.gpsManager.getDestinations();
               player.sendMessage(Component.text("---- GPS Destinations (" + destinations.size() + ") ----", NamedTextColor.GOLD));

               for(GpsNode node : destinations) {
                  String title = node.getDestinationName().isBlank() ? node.getId() : node.getDestinationName();
                  double dist = node.distance(player.getLocation());
                  player.sendMessage(((TextComponent)((TextComponent)Component.text("• ", NamedTextColor.GRAY).append(Component.text(title, NamedTextColor.YELLOW))).append(Component.text(" [" + node.getCategory() + "]", NamedTextColor.AQUA))).append(Component.text(String.format(" - %.0fm away", dist), NamedTextColor.GRAY)));
               }
               break;
            case "nav":
               if (args.length < 3) {
                  player.sendMessage(Component.text("Usage: /phone gps nav <destination>", NamedTextColor.RED));
                  return;
               }

               String query = String.join(" ", (CharSequence[])Arrays.copyOfRange(args, 2, args.length));
               GpsNode match = null;

               for(GpsNode dest : this.gpsManager.getDestinations()) {
                  if (dest.getDestinationName().equalsIgnoreCase(query) || dest.getId().equalsIgnoreCase(query)) {
                     match = dest;
                     break;
                  }
               }

               if (match == null) {
                  this.messageManager.send(player, "gps.unknown-destination", "destination", query);
                  return;
               }

               boolean started = this.gpsManager.startNavigation(player, match);
               if (started) {
                  GpsSession session = this.gpsManager.getSession(player.getUniqueId());
                  if (session != null) {
                     this.arrowRenderer.startTracking(player, session);
                  }
               }
               break;
            case "create":
               if (!player.hasPermission("phone.gps.admin") && !player.isOp()) {
                  this.messageManager.send(player, "general.no-permission");
                  return;
               }

               if (args.length < 4) {
                  player.sendMessage(Component.text("Usage: /phone gps create <id> <name>", NamedTextColor.RED));
                  return;
               }

               String id = args[2];
               String name = String.join(" ", (CharSequence[])Arrays.copyOfRange(args, 3, args.length));
               GpsNode node = new GpsNode(id, player.getLocation());
               node.setDestination(true);
               node.setDestinationName(name);
               this.gpsManager.registerNode(node);
               player.sendMessage(Component.text("✔ Created destination node: " + id + " (" + name + ")", NamedTextColor.GREEN));
               break;
            case "delete":
               if (!player.hasPermission("phone.gps.admin") && !player.isOp()) {
                  this.messageManager.send(player, "general.no-permission");
                  return;
               }

               if (args.length < 3) {
                  player.sendMessage(Component.text("Usage: /phone gps delete <nodeId>", NamedTextColor.RED));
                  return;
               }

               String id = args[2];
               this.gpsManager.removeNode(id);
               player.sendMessage(Component.text("✔ Deleted node: " + id, NamedTextColor.RED));
               break;
            default:
               this.sendGpsHelp(player);
         }

      }
   }

   private void handleGetPhone(Player sender, String[] args) {
      if (!sender.hasPermission("phone.command.get") && !sender.isOp()) {
         this.messageManager.send(sender, "general.no-permission");
      } else {
         Player target = sender;
         if (args.length >= 2) {
            Player query = Bukkit.getPlayer(args[1]);
            if (query == null || !query.isOnline()) {
               sender.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
               return;
            }

            target = query;
         }

         PhoneProfile newDeviceProfile = this.profileManager.createNewPhone(target.getUniqueId());
         ItemStack phoneItem = PhoneItemFactory.createPhoneItem(this.phoneConfig, newDeviceProfile, target.getName());
         target.getInventory().addItem(new ItemStack[]{phoneItem});
         target.sendMessage(Component.text("You received a Smartphone Pro! Number: " + newDeviceProfile.getPhoneNumber(), NamedTextColor.GREEN));
         if (!target.equals(sender)) {
            sender.sendMessage(Component.text("Gave new phone (" + newDeviceProfile.getPhoneNumber() + ") to " + target.getName(), NamedTextColor.GREEN));
         }

      }
   }

   private void handleContact(Player player, String deviceNumber, String[] args) {
      if (args.length < 2) {
         player.sendMessage(Component.text("Usage: /phone contact <add <number> | remove <name>>", NamedTextColor.RED));
      } else {
         String action = args[1].toLowerCase();
         PhoneProfile profile = this.profileManager.getProfile(deviceNumber);
         if (profile == null) {
            this.messageManager.send(player, "contacts.invalid-device");
         } else {
            if (action.equals("add")) {
               if (args.length < 3) {
                  player.sendMessage(Component.text("Usage: /phone contact add <number>", NamedTextColor.RED));
                  return;
               }

               String contactNumber = args[2];
               String ownerName = this.profileManager.getOwnerNameOfNumber(contactNumber);
               if (ownerName == null || ownerName.isBlank()) {
                  player.sendMessage(Component.text("✖ No registered player found with phone number " + contactNumber + "!", NamedTextColor.RED));
                  player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.6F, 0.5F);
                  return;
               }

               profile.addContact(ownerName, contactNumber);
               this.profileManager.saveProfile(profile);
               this.messageManager.send(player, "contacts.contact-added", "name", ownerName, "number", contactNumber);
               player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5F, 2.0F);
            } else if (action.equals("remove")) {
               if (args.length < 3) {
                  player.sendMessage(Component.text("Usage: /phone contact remove <name>", NamedTextColor.RED));
                  return;
               }

               String contactName = args[2];
               boolean removed = profile.removeContact(contactName);
               if (removed) {
                  this.profileManager.saveProfile(profile);
                  this.messageManager.send(player, "contacts.contact-removed", "name", contactName);
               } else {
                  this.messageManager.send(player, "contacts.contact-not-found", "name", contactName);
               }
            }

         }
      }
   }

   private void handleDialFeedback(Player player, CallManager.DialResult result, String number) {
      switch (result) {
         case SUCCESS:
            this.messageManager.send(player, "calls.dialing", "target", number);
            break;
         case BUSY:
            this.messageManager.send(player, "calls.line-busy");
            break;
         case AIRPLANE_MODE:
            this.messageManager.send(player, "calls.airplane-mode-caller");
            break;
         case OFFLINE:
         case TARGET_NOT_CARRYING_PHONE:
            this.messageManager.send(player, "calls.subscriber-unavailable");
            break;
         case SELF_CALL:
            this.messageManager.send(player, "calls.self-call");
            break;
         case ALREADY_IN_CALL:
            this.messageManager.send(player, "calls.already-in-call");
            break;
         case NUMBER_NOT_FOUND:
            this.messageManager.send(player, "calls.number-not-found");
      }

   }

   private void sendHelp(Player player) {
      player.sendMessage(Component.text("---- Phone, GPS & SMS Commands ----", NamedTextColor.GOLD));
      player.sendMessage(Component.text("/phone open ", NamedTextColor.YELLOW).append(Component.text("- Open Phone GUI", NamedTextColor.GRAY)));
      player.sendMessage(Component.text("/phone sms <number> <msg> ", NamedTextColor.YELLOW).append(Component.text("- Send text message", NamedTextColor.GRAY)));
      player.sendMessage(Component.text("/phone call <number> ", NamedTextColor.YELLOW).append(Component.text("- Place voice/text call", NamedTextColor.GRAY)));
      player.sendMessage(Component.text("/phone accept / decline ", NamedTextColor.YELLOW).append(Component.text("- Manage incoming calls", NamedTextColor.GRAY)));
      player.sendMessage(Component.text("/phone hangup ", NamedTextColor.YELLOW).append(Component.text("- Disconnect active call", NamedTextColor.GRAY)));
      player.sendMessage(Component.text("/phone gps <nav|stop|list|move> ", NamedTextColor.YELLOW).append(Component.text("- GPS navigation suite", NamedTextColor.GRAY)));
      player.sendMessage(Component.text("/phone contact <add <number> | remove <name>> ", NamedTextColor.YELLOW).append(Component.text("- Manage address book", NamedTextColor.GRAY)));
      if (player.hasPermission("phone.command.reload") || player.isOp()) {
         player.sendMessage(Component.text("/phone reload ", NamedTextColor.RED).append(Component.text("- Reload config, gui.yml & messages.yml", NamedTextColor.GRAY)));
      }

   }

   private void sendGpsHelp(Player player) {
      player.sendMessage(Component.text("---- GPS Navigation Commands ----", NamedTextColor.GOLD));
      player.sendMessage(Component.text("/phone gps nav <destination> ", NamedTextColor.YELLOW).append(Component.text("- Route to destination", NamedTextColor.GRAY)));
      player.sendMessage(Component.text("/phone gps stop ", NamedTextColor.YELLOW).append(Component.text("- Cancel active navigation", NamedTextColor.GRAY)));
      player.sendMessage(Component.text("/phone gps move ", NamedTextColor.YELLOW).append(Component.text("- Adjust arrow height & distance", NamedTextColor.GRAY)));
      player.sendMessage(Component.text("/phone gps list ", NamedTextColor.YELLOW).append(Component.text("- View available landmarks", NamedTextColor.GRAY)));
      if (player.hasPermission("phone.gps.admin") || player.isOp()) {
         player.sendMessage(Component.text("/phone gps editor ", NamedTextColor.AQUA).append(Component.text("- Toggle In-Game Setup Wand", NamedTextColor.GRAY)));
         player.sendMessage(Component.text("/phone gps wand ", NamedTextColor.AQUA).append(Component.text("- Give Configurator Wand", NamedTextColor.GRAY)));
         player.sendMessage(Component.text("/phone gps create <id> <name> ", NamedTextColor.AQUA).append(Component.text("- Create landmark at your feet", NamedTextColor.GRAY)));
         player.sendMessage(Component.text("/phone gps delete <id> ", NamedTextColor.AQUA).append(Component.text("- Delete a GPS node", NamedTextColor.GRAY)));
      }

   }

   public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) {
      if (!(sender instanceof Player player)) {
         return Collections.emptyList();
      } else {
         String carriedNumber = this.callManager.getFirstCarriedPhoneNumber(player);
         if (args.length == 1) {
            List<String> subs = new ArrayList(List.of("get", "gps"));
            if (carriedNumber != null) {
               subs.addAll(List.of("open", "sms", "call", "accept", "decline", "hangup", "contact"));
            }

            if (player.hasPermission("phone.command.reload") || player.isOp()) {
               subs.add("reload");
            }

            return subs.stream().filter((s) -> s.startsWith(args[0].toLowerCase())).toList();
         } else if (carriedNumber != null && args.length == 2 && args[0].equalsIgnoreCase("contact")) {
            return List.of("add", "remove").stream().filter((s) -> s.startsWith(args[1].toLowerCase())).toList();
         } else {
            if (carriedNumber != null && args.length == 3 && args[0].equalsIgnoreCase("contact") && args[1].equalsIgnoreCase("remove")) {
               PhoneProfile profile = this.profileManager.getProfile(carriedNumber);
               if (profile != null) {
                  return (new ArrayList(profile.getContacts().keySet())).stream().filter((s) -> s.toLowerCase().startsWith(args[2].toLowerCase())).toList();
               }
            }

            if (carriedNumber != null && args.length == 2 && args[0].equalsIgnoreCase("sms")) {
               PhoneProfile profile = this.profileManager.getProfile(carriedNumber);
               if (profile != null) {
                  List<String> suggestions = new ArrayList(profile.getContacts().values());
                  suggestions.addAll(profile.getConversations().keySet());
                  return suggestions.stream().filter((s) -> s.startsWith(args[1])).toList();
               }
            }

            if (args.length == 2 && args[0].equalsIgnoreCase("gps")) {
               List<String> gpsSubs = new ArrayList(List.of("nav", "stop", "list", "move"));
               if (player.hasPermission("phone.gps.admin") || player.isOp()) {
                  gpsSubs.addAll(List.of("editor", "wand", "create", "delete"));
               }

               return gpsSubs.stream().filter((s) -> s.startsWith(args[1].toLowerCase())).toList();
            } else if (args.length == 3 && args[0].equalsIgnoreCase("gps") && args[1].equalsIgnoreCase("nav")) {
               return this.gpsManager.getDestinations().stream().map((dest) -> dest.getDestinationName().isBlank() ? dest.getId() : dest.getDestinationName()).filter((name) -> name.toLowerCase().startsWith(args[2].toLowerCase())).toList();
            } else {
               return args.length == 3 && args[0].equalsIgnoreCase("gps") && args[1].equalsIgnoreCase("delete") ? (new ArrayList(this.gpsManager.getNodes().keySet())).stream().filter((id) -> id.toLowerCase().startsWith(args[2].toLowerCase())).toList() : Collections.emptyList();
            }
         }
      }
   }
}
