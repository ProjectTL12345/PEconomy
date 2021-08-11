package net.projecttl.pbalance.commands

import net.projecttl.pbalance.PBalance
import net.projecttl.pbalance.api.Economy
import net.projecttl.pbalance.api.InitSQLDriver
import net.projecttl.pbalance.api.moneyUnit
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class MoneyCommand(private val plugin: PBalance): CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (command.name == "pbalance") {
            if (args.isEmpty()) {
                val moneySystem = Economy(sender as Player)
                sender.sendMessage("Your account balance: ${ChatColor.GREEN}${moneySystem.money}${moneyUnit()}")

                return false
            }

            return when (args[0]) {
                "rank" -> {
                    val moneySystem = Economy(sender as Player)
                    moneySystem.getRanking()
                    true
                }

                "send" -> {
                    val target = args[1]
                    val targetPlayer = Bukkit.getPlayer(target)
                    val amount = Integer.parseInt(args[2])

                    val senderMoney = Economy(sender as Player)
                    val targetMoney = Economy(targetPlayer!!)

                    if (senderMoney.money < amount) {
                        sender.sendMessage("<PEconomy> ${ChatColor.RED}You cannot send more than the amount you have.")
                    } else if (senderMoney.money <= 0 || amount <= 0) {
                        sender.sendMessage("<PEconomy> ${ChatColor.RED}Balance must not be less of 0${moneyUnit()}")
                    } else {
                        targetMoney.addMoney(amount)
                        senderMoney.removeMoney(amount)

                        sender.sendMessage("<PEconomy> You have successfully sent ${amount}${InitSQLDriver.moneyUnit} to ${targetPlayer.name}")
                        targetPlayer.sendMessage("<PEconomy> You received ${amount}${moneyUnit()} from ${sender.name}")
                    }

                    true
                }

                "balance" -> {
                    val moneySystem = Economy(sender as Player)
                    sender.sendMessage("Your account balance: ${ChatColor.GREEN}${moneySystem.money}${moneyUnit()}")

                    true
                }

                "account" -> {
                    val moneySystem = Economy(sender as Player)

                    sender.sendMessage("${ChatColor.GOLD}==========[${ChatColor.RESET}${sender.name}'s account${ChatColor.GOLD}]==========")
                    sender.sendMessage("${ChatColor.YELLOW}UUID${ChatColor.RESET}: ${sender.uniqueId}")
                    sender.sendMessage("${ChatColor.YELLOW}Balance${ChatColor.RESET}: ${moneySystem.money}${moneyUnit()}")

                    true
                }

                "set" -> {
                    if (sender.isOp) {
                        val target = args[1]
                        val amount = Integer.parseInt(args[2])

                        if (amount >= 0) {
                            val targetPlayer = Bukkit.getPlayer(target)
                            val moneySystem = Economy(targetPlayer!!)

                            if (!targetPlayer.isOnline) {
                                sender.sendMessage("<PEconomy> ${ChatColor.GOLD}${targetPlayer.name} is offline!")
                            } else {
                                moneySystem.money = amount
                                sender.sendMessage("<PEconomy> ${ChatColor.GREEN}Now ${targetPlayer.name}'s account balance is ${amount}${moneyUnit()}")
                            }
                        } else {
                            sender.sendMessage("<PEconomy> ${ChatColor.RED}Balance must not be less of 0${moneyUnit()}")
                        }

                        true
                    } else {
                        sender.sendMessage("<PEconomy> ${ChatColor.RED}You're not OP!")
                        true
                    }
                }

                "query" -> {
                    if (sender.isOp) {
                        val moneySystem = Economy(sender as Player)
                        moneySystem.queryList()
                        true
                    } else {
                        sender.sendMessage("<PEconomy> ${ChatColor.RED}You're not OP!")
                        true
                    }
                }

                "moneyunit" -> {
                    val unit = args[1]
                    if (sender.isOp) {
                        if (unit == "") {
                            sender.sendMessage("<PEconomy> ${ChatColor.RED}Money Unit must not be null!")
                        } else {
                            plugin.config.set("MONEY_UNIT", unit)
                            Bukkit.broadcastMessage("<PEconomy> ${ChatColor.GREEN}Now your server money unit is this: ${ChatColor.WHITE}$unit")
                            Bukkit.broadcastMessage("${ChatColor.GOLD}RELOADING SERVER...")
                            plugin.server.reload()
                            Bukkit.broadcastMessage("${ChatColor.GREEN}RELOAD COMPLETE!")
                        }
                    } else {
                        sender.sendMessage("<PEconomy> ${ChatColor.RED}You're not OP!")
                    }

                    true
                }

                else -> false
            }
        }

        return false
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): MutableList<String>? {
        if (command.name == "pbalance") {
            val commandList = mutableListOf<String>()

            when (args.size) {
                1 -> {
                    commandList.add("rank")
                    commandList.add("send")
                    commandList.add("balance")
                    commandList.add("account")

                    if (sender.isOp) {
                        commandList.add("set")
                        commandList.add("query")
                        commandList.add("moneyunit")
                    }

                    return commandList
                }

                2 -> {
                    if (args[0] == "set" && args[0] == "send") {
                        for (i in plugin.server.onlinePlayers) {
                            commandList.add(i.name)
                        }

                        return commandList
                    }
                }
            }
        }

        return null
    }
}