package net.projecttl.economy.plugin.utils

import net.projecttl.economy.plugin.instance
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.*

private val moneyUnit = InitSQL.moneyUnits.toString()

fun moneyUnit(): String {
    return if (moneyUnit.length > 3) {
        " $moneyUnit"
    } else {
        moneyUnit
    }
}

fun setMoneyUnit(unit: String) {
    instance.config.set("MONEY_UNIT", unit)
}

class Economy(private val player: Player) {

    private val statement = InitSQL.connection.createStatement()

    var money: Int
        get() = queryMoney()

        set(amount) {
            statement.executeUpdate(
                "update ${InitSQL.dbName}.account set amount = $amount where uuid = '${player.uniqueId}' or username = '${player.name}';")
        }

    fun addMoney(amount: Int) {
        money += amount
    }

    fun subtractMoney(amount: Int) {
        if (amount > money) {
            player.sendMessage(
                "${ChatColor.RED}Not enough balance in your account!\n" +
                        "${ChatColor.GREEN}Current balance${ChatColor.RESET}: ${money}${moneyUnit()}"
            )
        } else {
            money -= amount
        }
    }

    @Deprecated("changed for subtractMoney()")
    fun removeMoney(amount: Int) {
        if (amount > money) {
            player.sendMessage(
                "${ChatColor.RED}Not enough balance in your account!\n" +
                        "${ChatColor.GREEN}Current balance${ChatColor.RESET}: ${money}${moneyUnit()}"
            )
        } else {
            money -= amount
        }
    }

    private fun checkNullItemName(item: ItemStack, buy: Boolean) {
        if (item.itemMeta.displayName().toString() == "null") {
            if (!buy) {
                player.sendMessage(
                    "${ChatColor.GREEN}You're successful sell ${item.amount} ${item.type.name.lowercase()}!\n" +
                            "${ChatColor.GREEN}Current balance${ChatColor.RESET}: ${money}${moneyUnit()}"
                )
            } else {
                player.sendMessage(
                    "${ChatColor.GREEN}You're successful bought ${item.amount} ${item.type.name.lowercase()}!\n" +
                            "${ChatColor.GREEN}Current balance${ChatColor.RESET}: ${money}${moneyUnit()}"
                )
            }
        } else {
            if (!buy) {
                player.sendMessage(
                    "${ChatColor.GREEN}You're successful sell ${item.amount} ${item.itemMeta?.displayName().toString()}!\n" +
                            "${ChatColor.GREEN}Current balance${ChatColor.RESET}: ${money}${moneyUnit()}"
                )
            } else {
                player.sendMessage(
                    "${ChatColor.GREEN}You're successful bought ${item.amount} ${item.itemMeta?.displayName().toString()}!\n" +
                            "${ChatColor.GREEN}Current balance${ChatColor.RESET}: ${money}${moneyUnit()}"
                )
            }
        }
    }

    private fun checkFullInv(targetItem: ItemStack): Boolean {
        var fullInv = false
        for (i in 0..35) {
            if (player.inventory.getItem(i) != null) {
                if (player.inventory.getItem(i)!!.type == targetItem.type) {
                    if (player.inventory.getItem(i)!!.amount != 64) {
                        fullInv = false
                        break
                    } else {
                        continue
                    }
                }

                if (i == 35) {
                    fullInv = true
                }
            } else {
                fullInv = false
                break
            }
        }

        return fullInv
    }

    fun buy(item: ItemStack, amount: Int) {
        if (!checkFullInv(item)) {
            if (amount > money) {
                player.sendMessage(
                    "${ChatColor.RED}Not enough balance in your account!\n" +
                            "${ChatColor.GREEN}Current balance${ChatColor.RESET}: ${money}${moneyUnit()}")
            } else {
                subtractMoney(amount)
                player.inventory.addItem(item)

                checkNullItemName(item, true)
            }
        } else {
            player.sendMessage("${ChatColor.RED}Your inventory is full! try to empty your inventory first!")
        }
    }

    fun sell(item: ItemStack, amount: Int) {
        if (player.inventory.containsAtLeast(item, item.amount)) {
            player.inventory.removeItem(item)
            addMoney(amount)

            checkNullItemName(item, false)
        } else {
            player.sendMessage("${ChatColor.RED}Not enough item!")
        }
    }

    fun buySet(item: ItemStack) {
        buy(item, 64)
    }

    fun sellSet(item: ItemStack) {
        sell(item, 64)
    }

    fun sellAll(item: ItemStack) {
        var amount = 0
        for (i in 0..35) {
            if (player.inventory.getItem(i) == item) {
                amount += player.inventory.getItem(i)!!.amount
            }
        }

        sell(item, amount)
    }

    fun getRanking() {
        val resultSets = statement.executeQuery(
            "SELECT * FROM ${InitSQL.dbName}.account ORDER BY length(amount) desc, amount desc;"
        )
        var i = 1
        while (resultSets.next()) {
            val username = resultSets.getString("username")
            val amount   = resultSets.getInt("amount")

            when (i) {
                1 -> {
                    player.sendMessage("${ChatColor.GOLD}$i: ${ChatColor.WHITE}$username => $amount${moneyUnit()}")
                }

                2 -> {
                    player.sendMessage("${ChatColor.GRAY}${i}: ${ChatColor.WHITE}$username => $amount${moneyUnit()}")
                }

                3 -> {
                    player.sendMessage("${ChatColor.YELLOW}${i}: ${ChatColor.WHITE}$username => $amount${moneyUnit()}")
                }

                else -> {
                    player.sendMessage("${ChatColor.WHITE}${i}: ${ChatColor.WHITE}$username => $amount${moneyUnit()}")
                }
            }

            i++
        }
    }

    fun registerAccount() {
        statement.executeUpdate("insert ignore into ${InitSQL.dbName}.account values('${player.uniqueId}', 0, null);")
    }

    fun showAccount() {
        for (i in queryAccount().values) {
            if (i.uuid == player.uniqueId) {
                player.sendMessage(
                    "${ChatColor.GOLD}==========[${ChatColor.RESET}${Bukkit.getPlayer(i.uuid)}'s account${ChatColor.GOLD}]=========="
                )
                player.sendMessage("${ChatColor.YELLOW}UUID${ChatColor.RESET}: ${i.uuid}")
                player.sendMessage("${ChatColor.YELLOW}Balance${ChatColor.RESET}: ${i.amount}${moneyUnit()}")
            }
        }
    }

    fun queryList() {
        for (i in queryAccount().values) {
            player.sendMessage(
                "${ChatColor.GOLD}===============================================\n" +
                        "${ChatColor.GREEN}UUID: ${ChatColor.RESET}${i.uuid}\n" +
                        "${ChatColor.GREEN}NAME: ${ChatColor.RESET}${Bukkit.getPlayer(i.uuid)}\n" +
                        "${ChatColor.GREEN}BALANCE: ${ChatColor.RESET}${i.amount}${moneyUnit()}\n" +
                        "${ChatColor.GOLD}==============================================="
            )
        }
    }

    private fun queryMoney(): Int {
        val resultSet = statement.executeQuery(
            "select * from ${InitSQL.dbName}.account where username = '${player.name}' and uuid = '${player.uniqueId}'"
        )
        var result: Int? = null

        if (resultSet.next()) {
            result = resultSet.getInt("amount")
        }

        return result!!
    }

    fun queryAccount(): MutableMap<UUID, Account> {
        val resultSets = statement.executeQuery("SELECT * FROM ${InitSQL.dbName}.account;")
        val list = mutableMapOf<UUID, Account>()
        while (resultSets.next()) {
            list += UUID.fromString(resultSets.getString("uuid")) to Account(
                UUID.fromString(resultSets.getString("uuid")),
                resultSets.getInt("amount")
            )
        }

        return list
    }
}