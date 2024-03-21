package brightspark.brightereconomy

import io.wispforest.owo.config.annotation.*

@Suppress("unused")
@Config(name = BrighterEconomy.MOD_ID, wrapperName = "ModConfig")
@Modmenu(modId = BrighterEconomy.MOD_ID)
class ModConfigModel {
	@JvmField
	@RestartRequired
	var apiEnabled: Boolean = true

	@JvmField
	@RangeConstraint(min = 0.0, max = 65535.0)
	@RestartRequired
	var apiPort: Int = 25570

	@JvmField
	@RestartRequired
	var commandAliases: List<String> = listOf("be")

	@JvmField
	@RegexConstraint("^[^\\s]{1,5}$")
	var currencySymbol: String = "💩"

	@JvmField
	var whitelist: List<String> = emptyList()

	@JvmField
	var blacklist: List<String> = emptyList()

	@JvmField
	@RangeConstraint(min = 0.0, max = 0.99)
	var transferTax: Float = 0F
}
