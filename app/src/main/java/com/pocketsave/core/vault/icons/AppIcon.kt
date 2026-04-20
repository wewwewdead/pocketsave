package com.pocketsave.core.vault.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.DinnerDining
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.EggAlt
import androidx.compose.material.icons.filled.EmojiFoodBeverage
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.Icecream
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Liquor
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.LocalPizza
import androidx.compose.material.icons.filled.LunchDining
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Recycling
import androidx.compose.material.icons.filled.RiceBowl
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.SetMeal
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material.icons.filled.SoupKitchen
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.WineBar
import androidx.compose.material.icons.filled.Yard
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Curated Material Icons (Filled) exposed to category icon pickers.
 *
 * Uses the Filled variant because every icon in `material-icons-extended`
 * ships a Filled version, while Outlined coverage is patchy (e.g. Ramen has
 * no outlined variant in some releases). Tinting a Filled vector with a soft
 * colour gives it an outlined-ish feel without risking a compile break.
 *
 * Rendering code stores a stable string key — not an [ImageVector] — so:
 *  - The Room database never holds a Kotlin reference that would break after
 *    R8 rename or a library upgrade.
 *  - The key set can grow without a schema migration.
 *  - Any caller that resolves an unknown key gets a sensible [DEFAULT] fallback
 *    instead of a crash.
 */
object AppIcon {
    /** Fallback when a stored `iconKey` isn't in the registry. */
    val DEFAULT: ImageVector = Icons.Filled.Label

    /**
     * Key → icon map. Keys are lowercase snake_case and kept stable over time
     * because they ship in user data. New keys are fine; renames are not.
     */
    val registry: Map<String, ImageVector> = mapOf(
        // Default GroceryCategory coverage
        "fresh_produce" to Icons.Filled.Eco,
        "meats_seafood" to Icons.Filled.SetMeal,
        "dairy_eggs" to Icons.Filled.EggAlt,
        "frozen" to Icons.Filled.AcUnit,
        "condiments_ingredients" to Icons.Filled.SoupKitchen,
        "pantry" to Icons.Filled.Inventory2,
        "bakery_bread" to Icons.Filled.BakeryDining,
        "beverages" to Icons.Filled.LocalDrink,
        "ready_meals" to Icons.Filled.DinnerDining,
        "personal_care" to Icons.Filled.Spa,
        "health" to Icons.Filled.LocalPharmacy,
        "cleaning_household" to Icons.Filled.CleaningServices,
        "pets" to Icons.Filled.Pets,
        "baby" to Icons.Filled.ChildCare,
        "home_garden" to Icons.Filled.Yard,
        "electronics_hobbies" to Icons.Filled.Devices,
        "stationery" to Icons.Filled.EditNote,

        // Extras offered in the picker grid for custom categories
        "cake" to Icons.Filled.Cake,
        "coffee" to Icons.Filled.Coffee,
        "cookie" to Icons.Filled.Cookie,
        "emoji_food_beverage" to Icons.Filled.EmojiFoodBeverage,
        "fastfood" to Icons.Filled.Fastfood,
        "grass" to Icons.Filled.Grass,
        "healing" to Icons.Filled.Healing,
        "icecream" to Icons.Filled.Icecream,
        "liquor" to Icons.Filled.Liquor,
        "local_cafe" to Icons.Filled.LocalCafe,
        "local_pizza" to Icons.Filled.LocalPizza,
        "lunch_dining" to Icons.Filled.LunchDining,
        "rice_bowl" to Icons.Filled.RiceBowl,
        "recycling" to Icons.Filled.Recycling,
        "restaurant_menu" to Icons.Filled.RestaurantMenu,
        "shopping_basket" to Icons.Filled.ShoppingBasket,
        "wine_bar" to Icons.Filled.WineBar,
        "label" to Icons.Filled.Label,
    )

    /** Resolve to a concrete vector, falling back to [DEFAULT] on miss. */
    fun resolveIcon(key: String?): ImageVector = key?.let { registry[it] } ?: DEFAULT

    /**
     * Ordered list the picker grid renders. Mirrors the default-category order
     * first (so custom categories can pick the same icons as the built-ins),
     * then the extras alphabetically.
     */
    val pickableIcons: List<Pair<String, ImageVector>> = listOf(
        "fresh_produce",
        "meats_seafood",
        "dairy_eggs",
        "frozen",
        "condiments_ingredients",
        "pantry",
        "bakery_bread",
        "beverages",
        "ready_meals",
        "personal_care",
        "health",
        "cleaning_household",
        "pets",
        "baby",
        "home_garden",
        "electronics_hobbies",
        "stationery",
        "cake",
        "coffee",
        "cookie",
        "emoji_food_beverage",
        "fastfood",
        "grass",
        "healing",
        "icecream",
        "liquor",
        "local_cafe",
        "local_pizza",
        "lunch_dining",
        "rice_bowl",
        "recycling",
        "restaurant_menu",
        "shopping_basket",
        "wine_bar",
        "label",
    ).map { key -> key to (registry[key] ?: DEFAULT) }
}
