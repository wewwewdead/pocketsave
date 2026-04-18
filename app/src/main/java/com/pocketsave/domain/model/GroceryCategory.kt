package com.pocketsave.domain.model

/**
 * Port of `GroceryCategory` from `PocketSave/Extensions/Grocery+Enums.swift`.
 *
 * `rawValue` stays identical to the Swift enum case so category identifiers travel
 * across iOS/Android identically in shared data (cart item category snapshots etc.).
 *
 * Android uses Material Icons Outlined instead of iOS's emoji. [defaultIconKey]
 * is a stable key for the [com.pocketsave.core.vault.icons.AppIcon] registry —
 * keeping it as a string rather than a Kotlin reference keeps the DB
 * serialisable and survives library upgrades / R8 renames.
 */
enum class GroceryCategory(
    val rawValue: String,
    val title: String,
    val defaultIconKey: String,
    val placeholder: String,
    val pastelHex: String,
) {
    FRESH_PRODUCE("freshProduce", "Fresh Produce", "fresh_produce", "e.g. Apples, Bananas", "AAFF72"),
    MEATS_SEAFOOD("meatsSeafood", "Meats & Seafood", "meats_seafood", "e.g. Chicken, Salmon", "FFBEBE"),
    DAIRY_EGGS("dairyEggs", "Dairy & Eggs", "dairy_eggs", "e.g. Milk, Eggs, Cheese", "FFE481"),
    FROZEN("frozen", "Frozen", "frozen", "e.g. Ice Cream, Pizza", "C5F9FF"),
    CONDIMENTS_INGREDIENTS("condimentsIngredients", "Condiments & Ingredients", "condiments_ingredients", "e.g. Olive Oil, Spices", "949494"),
    PANTRY("pantry", "Pantry", "pantry", "e.g. Rice, Pasta, Canned Goods", "FFF7AA"),
    BAKERY_BREAD("bakeryBread", "Bakery & Bread", "bakery_bread", "e.g. Bread, Bagels", "F5DEB3"),
    BEVERAGES("beverages", "Beverages", "beverages", "e.g. Water, Juice, Soda", "AAB3E0"),
    READY_MEALS("readyMeals", "Ready Meals", "ready_meals", "e.g. Salad Kit, Sushi", "FFDAB9"),
    PERSONAL_CARE("personalCare", "Personal Care", "personal_care", "e.g. Shampoo, Toothpaste", "FFC0CB"),
    HEALTH("health", "Health", "health", "e.g. Vitamins, Pain Relief", "CBCAFF"),
    CLEANING_HOUSEHOLD("cleaningHousehold", "Cleaning & Household", "cleaning_household", "e.g. Paper Towels, Detergent", "D8BFD8"),
    PETS("pets", "Pets", "pets", "e.g. Dog Food, Cat Litter", "CAA484"),
    BABY("baby", "Baby", "baby", "e.g. Diapers, Wipes", "B0E0E6"),
    HOME_GARDEN("homeGarden", "Home & Garden", "home_garden", "e.g. Light Bulbs, Batteries", "AED470"),
    ELECTRONICS_HOBBIES("electronicsHobbies", "Electronics & Hobbies", "electronics_hobbies", "e.g. Charger, Headphones", "FF96CA"),
    STATIONERY("stationery", "Stationery", "stationery", "e.g. Pens, Notebooks", "F3C7A3");

    companion object {
        fun fromTitle(title: String): GroceryCategory =
            entries.firstOrNull { it.title == title } ?: FRESH_PRODUCE

        fun fromRawValue(raw: String?): GroceryCategory? =
            raw?.let { entries.firstOrNull { cat -> cat.rawValue == it } }
    }
}
