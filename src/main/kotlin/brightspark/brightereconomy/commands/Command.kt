package brightspark.brightereconomy.commands

import brightspark.brightereconomy.BrighterEconomy
import brightspark.brightereconomy.economy.EconomyState
import com.mojang.authlib.GameProfile
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.tree.LiteralCommandNode
import me.lucko.fabric.api.permissions.v0.Permissions
import net.minecraft.command.argument.GameProfileArgumentType
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text

abstract class Command(
	private val name: String,
	builderBlock: LiteralArgumentBuilder<ServerCommandSource>.() -> Unit
) {
	companion object {
		private const val COMMAND_PERM = "command.${BrighterEconomy.MOD_ID}"

		/**
		 * Returns a literal node that redirects its execution to the given [destination] node.
		 *
		 * [Based on the example in Velocity here](https://github.com/PaperMC/Velocity/blob/8abc9c80a69158ebae0121fda78b55c865c0abad/proxy/src/main/java/com/velocitypowered/proxy/util/BrigadierUtils.java#L38)
		 *
		 * [Brigadier issue](https://github.com/Mojang/brigadier/issues/46)
		 */
		fun buildRedirect(
			alias: String,
			destination: LiteralCommandNode<ServerCommandSource>
		): LiteralCommandNode<ServerCommandSource> =
			LiteralArgumentBuilder.literal<ServerCommandSource>(alias)
				.requires(destination.requirement)
				.forward(destination.redirect, destination.redirectModifier, destination.isFork)
				.executes(destination.command)
				.apply { destination.children.forEach { then(it) } }
				.build()

		fun <T : ArgumentBuilder<ServerCommandSource, T>> T.thenLiteral(
			name: String,
			block: LiteralArgumentBuilder<ServerCommandSource>.() -> Unit
		): T = this.then(CommandManager.literal(name).apply(block))

		fun <T : ArgumentBuilder<ServerCommandSource, T>, ARG> T.thenArgument(
			argumentName: String,
			argument: ArgumentType<ARG>,
			block: RequiredArgumentBuilder<ServerCommandSource, ARG>.() -> Unit
		): T = this.then(CommandManager.argument(argumentName, argument).apply(block))

		fun <T : ArgumentBuilder<ServerCommandSource, T>> T.thenCommand(command: Command) {
			val node = command.builder.build()
			this.then(node)
			command.aliases.forEach { this.then(buildRedirect(it, node)) }
		}

		fun <T : ArgumentBuilder<ServerCommandSource, T>> T.requiresPermission(level: Int): T =
			this.requires(Permissions.require(COMMAND_PERM, level))

		fun <T : ArgumentBuilder<ServerCommandSource, T>> T.requiresPermission(permission: String, level: Int): T =
			this.requires(Permissions.require("$COMMAND_PERM.$permission", level))

		fun CommandContext<ServerCommandSource>.getEconomyState(): EconomyState =
			EconomyState.get(this.source.server)
	}

	protected val aliases: MutableList<String> = mutableListOf()

	val builder: LiteralArgumentBuilder<ServerCommandSource> = CommandManager.literal(name).apply(builderBlock)

	protected fun alias(alias: String) {
		this.aliases.add(alias)
	}

	protected fun aliases(vararg aliases: String): Unit = aliases.forEach { this.aliases.add(it) }

	protected fun getPlayer(ctx: CommandContext<ServerCommandSource>): GameProfile? =
		GameProfileArgumentType.getProfileArgument(ctx, "player").singleOrNull() ?: run {
			ctx.source.sendError(Text.of("0 or many players found with that name."))
			null
		}
}
