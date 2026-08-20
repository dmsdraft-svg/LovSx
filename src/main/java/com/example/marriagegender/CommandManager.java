package com.example.marriagegender;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LlamaSpit;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class CommandManager implements TabExecutor {

    private static final double GENDER_CHANGE_COST = 100_000.0D;

    private final MarriageGenderPlugin plugin;
    private final DataManager data;
    private final EconomyManager economy;
    private final RequestManager requests;
    private final Messages messages;

    public CommandManager(MarriageGenderPlugin plugin) {
        this.plugin = plugin;
        this.data = plugin.getDataManager();
        this.economy = plugin.getEconomyManager();
        this.requests = plugin.getRequestManager();
        this.messages = plugin.getMessages();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);

        switch (name) {
            case "gender":
                return handleGender(sender, args);
            case "sex":
                return handleSex(sender, args);
            case "marry":
                return handleMarry(sender, args);
            case "divorce":
                return handleDivorce(sender);
            default:
                return false;
        }
    }

    private boolean handleGender(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sendGenderHelp(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("set")) {
            if (!(sender instanceof Player player)) {
                messages.error(sender, "Команда доступна только игрокам.");
                return true;
            }

            if (args.length != 2) {
                sender.sendMessage(Component.text("Использование: /gender set <male|female>", NamedTextColor.YELLOW));
                return true;
            }

            Gender selected = Gender.fromInput(args[1]);
            if (selected == null) {
                messages.errorPlain(sender, "Неизвестный пол: " + args[1] + ". Доступно: male, female.");
                return true;
            }

            Gender current = data.getGender(player.getUniqueId());

            if (current == selected) {
                messages.error(sender, "У вас уже выбран этот пол.");
                return true;
            }

            if (current == Gender.NONE) {
                data.setGender(player.getUniqueId(), selected);
                messages.success(sender, "Пол впервые установлен: <gold>" + selected.displayName() + "</gold>.");
                return true;
            }

            if (!economy.has(player, GENDER_CHANGE_COST)) {
                messages.error(sender, "Недостаточно денег. Смена пола стоит <gold>$100,000</gold>.");
                return true;
            }

            if (!economy.withdraw(player, GENDER_CHANGE_COST)) {
                messages.error(sender, "Не удалось списать деньги.");
                return true;
            }

            data.setGender(player.getUniqueId(), selected);
            messages.success(sender, "Пол изменён на <gold>" + selected.displayName() + "</gold>. Списано <gold>$100,000</gold>.");
            return true;
        }

        if (args[0].equalsIgnoreCase("setadmin")) {
            if (!sender.hasPermission("plugin.admin")) {
                messages.error(sender, "Нет прав.");
                return true;
            }

            if (args.length != 3) {
                sender.sendMessage(Component.text("Использование: /gender setadmin <player> <male|female>", NamedTextColor.YELLOW));
                return true;
            }

            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                messages.error(sender, "Игрок не найден.");
                return true;
            }

            Gender selected = Gender.fromInput(args[2]);
            if (selected == null) {
                messages.errorPlain(sender, "Неизвестный пол: " + args[2] + ". Доступно: male, female.");
                return true;
            }

            data.setGender(target.getUniqueId(), selected);
            messages.success(sender, "Игроку <gold>" + target.getName() + "</gold> установлен пол: <gold>" + selected.displayName() + "</gold>.");
            messages.send(target, "<gold>Администратор изменил ваш пол на: <yellow>" + selected.displayName() + "</yellow></gold>");
            return true;
        }

        sendGenderHelp(sender);
        return true;
    }

    private boolean handleSex(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            messages.error(sender, "Команда доступна только игрокам.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(Component.text("Использование: /sex <player|accept|deny>", NamedTextColor.YELLOW));
            return true;
        }

        if (args[0].equalsIgnoreCase("accept")) {
            acceptSex(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("deny")) {
            denySex(player);
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null || !target.isOnline()) {
            messages.error(player, "Игрок не найден.");
            return true;
        }

        if (target.getUniqueId().equals(player.getUniqueId())) {
            messages.error(player, "Нельзя отправить запрос самому себе.");
            return true;
        }

        if (!canHaveSex(player, target)) {
            messages.error(player, "Секс возможен только между игроками с выбранными противоположными полами (мужской + женский).");
            return true;
        }

        requests.sendSexRequest(player.getUniqueId(), target.getUniqueId());
        messages.success(player, "Запрос на секс отправлен игроку <gold>" + target.getName() + "</gold>.");
        messages.send(target, "<gold>" + player.getName() + "</gold> предлагает заняться сексом. <green>/sex accept</green> или <red>/sex deny</red> (30 сек).");
        return true;
    }

    private boolean canHaveSex(Player first, Player second) {
        Gender firstGender = data.getGender(first.getUniqueId());
        Gender secondGender = data.getGender(second.getUniqueId());

        if (firstGender == Gender.NONE || secondGender == Gender.NONE) {
            return false;
        }

        if (firstGender == secondGender) {
            return false;
        }

        return (firstGender == Gender.MALE && secondGender == Gender.FEMALE)
                || (firstGender == Gender.FEMALE && secondGender == Gender.MALE);
    }

    private void acceptSex(Player acceptor) {
        RequestManager.PendingRequest request = requests.getSexRequest(acceptor.getUniqueId());

        if (request == null) {
            messages.error(acceptor, "Нет активных запросов на секс.");
            return;
        }

        Player requester = Bukkit.getPlayer(request.getSender());

        if (requester == null || !requester.isOnline()) {
            requests.removeSexRequest(acceptor.getUniqueId());
            messages.error(acceptor, "Игрок недоступен.");
            return;
        }

        if (!canHaveSex(requester, acceptor)) {
            requests.removeSexRequest(acceptor.getUniqueId());
            messages.error(acceptor, "Секс невозможен: требуются выбранные противоположные полы.");
            messages.error(requester, "Секс отменён: неправильные полы.");
            return;
        }

        requests.removeSexRequest(acceptor.getUniqueId());
        performSex(requester, acceptor);

        messages.success(requester, "Вы занялись сексом с <gold>" + acceptor.getName() + "</gold>.");
        messages.success(acceptor, "Вы занялись сексом с <gold>" + requester.getName() + "</gold>.");
    }

    private void denySex(Player denier) {
        RequestManager.PendingRequest request = requests.getSexRequest(denier.getUniqueId());

        if (request == null) {
            messages.error(denier, "Нет активных запросов на секс.");
            return;
        }

        requests.removeSexRequest(denier.getUniqueId());

        Player requester = Bukkit.getPlayer(request.getSender());
        if (requester != null) {
            messages.error(requester, "Игрок <gold>" + denier.getName() + "</gold> отказал вам в сексе.");
        }

        messages.send(denier, "<yellow>Вы отказали в запросе на секс.</yellow>");
    }

    private void performSex(Player first, Player second) {
        Gender firstGender = data.getGender(first.getUniqueId());
        Gender secondGender = data.getGender(second.getUniqueId());

        Player male;
        Player female;

        if (firstGender == Gender.MALE && secondGender == Gender.FEMALE) {
            male = first;
            female = second;
        } else if (firstGender == Gender.FEMALE && secondGender == Gender.MALE) {
            male = second;
            female = first;
        } else {
            return;
        }

        shootLlamaSpit(male, female);
        playHorseDeath(female);
    }

    private void shootLlamaSpit(Player male, Player target) {
        Location spawnLocation = male.getEyeLocation().add(male.getLocation().getDirection().multiply(0.4D));
        Entity entity = male.getWorld().spawnEntity(spawnLocation, EntityType.LLAMA_SPIT);

        if (!(entity instanceof LlamaSpit spit)) {
            entity.remove();
            return;
        }

        spit.setShooter(male);

        Vector velocity = target.getEyeLocation().subtract(spawnLocation).toVector();

        if (velocity.lengthSquared() > 0.0D) {
            velocity.normalize().multiply(2.0D);
        } else {
            velocity = male.getLocation().getDirection().multiply(2.0D);
        }

        spit.setVelocity(velocity);

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (spit.isValid()) {
                spit.remove();
            }
        }, 60L);
    }

    private void playHorseDeath(Player female) {
        Location location = female.getLocation();

        for (Entity entity : female.getWorld().getNearbyEntities(location, 64.0D, 64.0D, 64.0D)) {
            if (entity instanceof Player near) {
                near.playSound(location, Sound.ENTITY_HORSE_DEATH, 1.0F, 1.0F);
            }
        }
    }

    private boolean handleMarry(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            messages.error(sender, "Команда доступна только игрокам.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(Component.text("Использование: /marry <player|accept|deny|divorce>", NamedTextColor.YELLOW));
            return true;
        }

        if (args[0].equalsIgnoreCase("accept")) {
            acceptMarry(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("deny")) {
            denyMarry(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("divorce")) {
            return handleDivorce(player);
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null || !target.isOnline()) {
            messages.error(player, "Игрок не найден.");
            return true;
        }

        if (target.getUniqueId().equals(player.getUniqueId())) {
            messages.error(player, "Нельзя предложить брак самому себе.");
            return true;
        }

        if (data.isMarried(player.getUniqueId())) {
            messages.error(player, "Вы уже состоите в браке.");
            return true;
        }

        if (data.isMarried(target.getUniqueId())) {
            messages.error(player, "Этот игрок уже состоит в браке.");
            return true;
        }

        requests.sendMarryRequest(player.getUniqueId(), target.getUniqueId());
        messages.success(player, "Предложение отправлено игроку <gold>" + target.getName() + "</gold>.");
        messages.send(target, "<gold>" + player.getName() + "</gold> предлагает вам брак. <green>/marry accept</green> или <red>/marry deny</red> (30 сек).");
        return true;
    }

    private void acceptMarry(Player acceptor) {
        RequestManager.PendingRequest request = requests.getMarryRequest(acceptor.getUniqueId());

        if (request == null) {
            messages.error(acceptor, "Нет активных предложений.");
            return;
        }

        Player requester = Bukkit.getPlayer(request.getSender());

        if (requester == null || !requester.isOnline()) {
            requests.removeMarryRequest(acceptor.getUniqueId());
            messages.error(acceptor, "Игрок недоступен.");
            return;
        }

        if (data.isMarried(requester.getUniqueId()) || data.isMarried(acceptor.getUniqueId())) {
            requests.removeMarryRequest(acceptor.getUniqueId());
            messages.error(requester, "Брак невозможен: кто-то из вас уже состоит в браке.");
            messages.error(acceptor, "Брак невозможен: кто-то из вас уже состоит в браке.");
            return;
        }

        requests.removeMarryRequest(acceptor.getUniqueId());
        data.marry(requester.getUniqueId(), acceptor.getUniqueId());

        Component broadcast = messages.marriageBroadcast(requester.getName(), acceptor.getName());

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            onlinePlayer.sendMessage(broadcast);
        }

        plugin.getServer().getConsoleSender().sendMessage(broadcast);

        messages.success(requester, "Вы поженились с <gold>" + acceptor.getName() + "</gold>.");
        messages.success(acceptor, "Вы поженились с <gold>" + requester.getName() + "</gold>.");
    }

    private void denyMarry(Player denier) {
        RequestManager.PendingRequest request = requests.getMarryRequest(denier.getUniqueId());

        if (request == null) {
            messages.error(denier, "Нет активных предложений.");
            return;
        }

        requests.removeMarryRequest(denier.getUniqueId());

        Player requester = Bukkit.getPlayer(request.getSender());

        if (requester != null) {
            requester.sendMessage(Component.text("Вам отказали! Вы лох и петух!", NamedTextColor.RED));
        }

        messages.send(denier, "<yellow>Вы отказали в предложении.</yellow>");
    }

    private boolean handleDivorce(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            messages.error(sender, "Команда доступна только игрокам.");
            return true;
        }

        UUID spouse = data.getSpouse(player.getUniqueId());

        if (spouse == null) {
            messages.error(player, "Вы не состоите в браке.");
            return true;
        }

        data.divorce(player.getUniqueId());
        messages.success(player, "Вы развелись.");

        Player spousePlayer = Bukkit.getPlayer(spouse);

        if (spousePlayer != null) {
            messages.send(spousePlayer, "<gold>" + player.getName() + "</gold> развёлся(лась) с вами.");
        }

        return true;
    }

    private void sendGenderHelp(CommandSender sender) {
        sender.sendMessage(Component.text("Команды:", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/gender set <male|female> - выбрать/сменить пол.", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("/gender setadmin <player> <male|female> - админ-смена пола.", NamedTextColor.GRAY));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);

        switch (name) {
            case "gender":
                return tabGender(sender, args);
            case "sex":
                return tabSex(sender, args);
            case "marry":
                return tabMarry(sender, args);
            default:
                return Collections.emptyList();
        }
    }

    private List<String> tabGender(CommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            options.add("set");

            if (sender.hasPermission("plugin.admin")) {
                options.add("setadmin");
            }

            return filter(options, args[0]);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
            return filter(Arrays.asList("male", "female"), args[1]);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("setadmin") && sender.hasPermission("plugin.admin")) {
            return filter(onlinePlayerNames(), args[1]);
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("setadmin") && sender.hasPermission("plugin.admin")) {
            return filter(Arrays.asList("male", "female"), args[2]);
        }

        return Collections.emptyList();
    }

    private List<String> tabSex(CommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>(Arrays.asList("accept", "deny"));
            options.addAll(onlinePlayerNames());
            return filter(options, args[0]);
        }

        return Collections.emptyList();
    }

    private List<String> tabMarry(CommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>(Arrays.asList("accept", "deny", "divorce"));
            options.addAll(onlinePlayerNames());
            return filter(options, args[0]);
        }

        return Collections.emptyList();
    }

    private List<String> onlinePlayerNames() {
        List<String> names = new ArrayList<>();

        for (Player player : Bukkit.getOnlinePlayers()) {
            names.add(player.getName());
        }

        return names;
    }

    private List<String> filter(List<String> options, String token) {
        String lowerToken = token.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();

        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(lowerToken)) {
                result.add(option);
            }
        }

        return result;
    }
}
