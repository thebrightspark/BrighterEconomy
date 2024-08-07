package brightspark.brightereconomy.commands

import brightspark.brightereconomy.economy.TransactionExchangeResult
import brightspark.brightereconomy.util.Util
import com.mojang.authlib.GameProfile
import com.mojang.brigadier.arguments.LongArgumentType
import com.mojang.brigadier.context.CommandContext
import net.minecraft.command.argument.GameProfileArgumentType
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text
import net.minecraft.util.Formatting

object BalanceCommand : Command("balance", {
	requiresPermission("balance", 0)

	executes { ctx -> BalanceCommand.balance(ctx) }

	thenArgument("player", GameProfileArgumentType.gameProfile()) {
		requiresPermission("player", 2)

		executes { ctx ->
			val player = BalanceCommand.getPlayer(ctx) ?: return@executes 0
			BalanceCommand.balance(ctx, player)
		}

		thenLiteral("add") {
			thenArgument("amount", LongArgumentType.longArg(0)) {
				executes { ctx -> BalanceCommand.modifyBalance(ctx, true) }
			}
		}

		thenLiteral("remove") {
			thenArgument("amount", LongArgumentType.longArg(0)) {
				executes { ctx -> BalanceCommand.modifyBalance(ctx, false) }
			}
		}

		thenLiteral("set") {
			thenArgument("amount", LongArgumentType.longArg(0)) {
				executes { ctx -> BalanceCommand.setBalance(ctx) }
			}
		}
	}
}) {
	init {
		aliases("bal", "money")
	}

	private fun balance(ctx: CommandContext<ServerCommandSource>, player: GameProfile? = null): Int {
		val targetIsSelf = player?.id?.equals(ctx.source.player?.id) ?: true
		return ctx.source.player?.let { playerEntity ->
			val account = ctx.getEconomyState().getAccount(playerEntity.uuid)
			val money = Util.formatMoney(account.money)
			val textString = "${if (!targetIsSelf) "${player?.name}'s " else ""}Balance: $money"
			var text = Text.literal(textString)
			if (account.locked)
				text = text.append(Text.literal(" [locked]").styled { it.withColor(Formatting.RED) })

			ctx.source.sendMessage(text)
			1
		} ?: run {
			ctx.source.sendError(Text.of("Cannot get balance of non-player"))
			0
		}
	}

	private fun modifyBalance(ctx: CommandContext<ServerCommandSource>, add: Boolean): Int {
		val player = getPlayer(ctx) ?: return 0
		val amount = LongArgumentType.getLong(ctx, "amount")
		val result = ctx.getEconomyState().exchange(
			if (add) null else player.id,
			if (add) player.id else null,
			amount,
			ctx.source.name
		)

		val amountFormatted = Util.formatMoney(amount)
		return if (result == TransactionExchangeResult.SUCCESS) {
			if (add)
				ctx.source.sendMessage(Text.of("Added $amountFormatted to ${player.name}"))
			else
				ctx.source.sendMessage(Text.of("Removed $amountFormatted from ${player.name}"))
			1
		} else {
			if (add)
				ctx.source.sendMessage(Text.of("Failed to add $amountFormatted to ${player.name} due to $result"))
			else
				ctx.source.sendMessage(Text.of("Failed to remove $amountFormatted from ${player.name} due to $result"))
			0
		}
	}

	private fun setBalance(ctx: CommandContext<ServerCommandSource>): Int {
		val player = GameProfileArgumentType.getProfileArgument(ctx, "player").singleOrNull() ?: run {
			ctx.source.sendError(Text.of("0 or many players found with that name."))
			return 0
		}
		val amount = LongArgumentType.getLong(ctx, "amount")
		ctx.getEconomyState().setMoney(player.id, amount, ctx.source.name)
		ctx.source.sendMessage(Text.of("Set ${player.name} balance to ${Util.formatMoney(amount)}"))
		return 1
	}
}
