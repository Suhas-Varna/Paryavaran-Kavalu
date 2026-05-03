package com.example.paryavaran_kavalu.data

/**
 * Default rows for [redeem_items] — must match [AppDatabase] MIGRATION_5_6 insert (ids 1…6).
 */
object RedeemCatalogSeed {
    val all: List<RedeemItemEntity> = listOf(
        RedeemItemEntity(
            id = 1L,
            category = "Food & drink",
            title = "Neighbourhood café perk",
            subtitle = "Sample voucher - partner café",
            costPoints = 120,
            iconName = "LocalCafe",
        ),
        RedeemItemEntity(
            id = 2L,
            category = "Cleanup & gear",
            title = "Park cleanup kit",
            subtitle = "Gloves and bags for group drives",
            costPoints = 200,
            iconName = "Park",
        ),
        RedeemItemEntity(
            id = 3L,
            category = "Merch",
            title = "Green market tote",
            subtitle = "Reusable bag - pick-up location TBD",
            costPoints = 80,
            iconName = "ShoppingBag",
        ),
        RedeemItemEntity(
            id = 4L,
            category = "Retail partner",
            title = "Local store discount",
            subtitle = "10% off at a partner shop",
            costPoints = 150,
            iconName = "Storefront",
        ),
        RedeemItemEntity(
            id = 5L,
            category = "Community",
            title = "Community event pass",
            subtitle = "Entry to a local eco meet-up",
            costPoints = 100,
            iconName = "Groups",
        ),
        RedeemItemEntity(
            id = 6L,
            category = "Surprise",
            title = "Mystery reward",
            subtitle = "Rotating surprise from sponsors",
            costPoints = 300,
            iconName = "CardGiftcard",
        ),
    )
}
