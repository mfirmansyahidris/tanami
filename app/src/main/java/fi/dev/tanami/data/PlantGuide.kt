package fi.dev.tanami.data

import androidx.annotation.StringRes
import fi.dev.tanami.R

data class PlantGuide(
    val id: String,
    @StringRes val name: Int,
    @StringRes val variety: Int,
    @StringRes val category: Int,
    val icon: String,
    @StringRes val harvestDays: Int,
    @StringRes val harvestBasis: Int,
    @StringRes val plantingMethod: Int,
    @StringRes val light: Int,
    @StringRes val medium: Int,
    @StringRes val watering: Int,
    @StringRes val nutrients: Int,
    @StringRes val care: List<Int>,
    @StringRes val problems: List<Int>,
    @StringRes val note: Int
)

object PlantCatalog {
    val plants = listOf(
        PlantGuide(
            id = "kangkung",
            name = R.string.plant_name_kangkung,
            variety = R.string.plant_variety_kangkung,
            category = R.string.plant_category_leafy,
            icon = "🥬",
            harvestDays = R.string.harvest_days_kangkung,
            harvestBasis = R.string.harvest_basis_kangkung,
            plantingMethod = R.string.planting_kangkung,
            light = R.string.light_kangkung,
            medium = R.string.medium_kangkung,
            watering = R.string.water_kangkung,
            nutrients = R.string.nutrients_kangkung,
            care = listOf(R.string.care_kangkung_1, R.string.care_kangkung_2),
            problems = listOf(R.string.pest_kangkung_1, R.string.pest_kangkung_2),
            note = R.string.note_kangkung
        ),
        PlantGuide(
            id = "bayam",
            name = R.string.plant_name_spinach,
            variety = R.string.plant_variety_spinach,
            category = R.string.plant_category_leafy,
            icon = "🌿",
            harvestDays = R.string.harvest_days_spinach,
            harvestBasis = R.string.harvest_basis_spinach,
            plantingMethod = R.string.planting_spinach,
            light = R.string.light_spinach,
            medium = R.string.medium_spinach,
            watering = R.string.water_spinach,
            nutrients = R.string.nutrients_spinach,
            care = listOf(R.string.care_spinach_1, R.string.care_spinach_2),
            problems = listOf(R.string.pest_spinach_1, R.string.pest_spinach_2),
            note = R.string.note_spinach
        ),
        PlantGuide(
            id = "pakcoy",
            name = R.string.plant_name_pakchoi,
            variety = R.string.plant_variety_pakchoi,
            category = R.string.plant_category_leafy,
            icon = "🥬",
            harvestDays = R.string.harvest_days_pakchoi,
            harvestBasis = R.string.harvest_basis_pakchoi,
            plantingMethod = R.string.planting_pakchoi,
            light = R.string.light_pakchoi,
            medium = R.string.medium_pakchoi,
            watering = R.string.water_pakchoi,
            nutrients = R.string.nutrients_pakchoi,
            care = listOf(R.string.care_pakchoi_1, R.string.care_pakchoi_2),
            problems = listOf(R.string.pest_pakchoi_1, R.string.pest_pakchoi_2),
            note = R.string.note_pakchoi
        ),
        PlantGuide(
            id = "timun",
            name = R.string.plant_name_cucumber,
            variety = R.string.plant_variety_cucumber,
            category = R.string.plant_category_fruiting,
            icon = "🥒",
            harvestDays = R.string.harvest_days_cucumber,
            harvestBasis = R.string.harvest_basis_cucumber,
            plantingMethod = R.string.planting_cucumber,
            light = R.string.light_cucumber,
            medium = R.string.medium_cucumber,
            watering = R.string.water_cucumber,
            nutrients = R.string.nutrients_cucumber,
            care = listOf(R.string.care_cucumber_1, R.string.care_cucumber_2),
            problems = listOf(R.string.pest_cucumber_1, R.string.pest_cucumber_2),
            note = R.string.note_cucumber
        ),
        PlantGuide(
            id = "kacang-panjang",
            name = R.string.plant_name_long_bean,
            variety = R.string.plant_variety_long_bean,
            category = R.string.plant_category_fruiting,
            icon = "🫛",
            harvestDays = R.string.harvest_days_long_bean,
            harvestBasis = R.string.harvest_basis_long_bean,
            plantingMethod = R.string.planting_long_bean,
            light = R.string.light_long_bean,
            medium = R.string.medium_long_bean,
            watering = R.string.water_long_bean,
            nutrients = R.string.nutrients_long_bean,
            care = listOf(R.string.care_long_bean_1, R.string.care_long_bean_2),
            problems = listOf(R.string.pest_long_bean_1, R.string.pest_long_bean_2),
            note = R.string.note_long_bean
        ),
        PlantGuide(
            id = "tomat-ceri",
            name = R.string.plant_name_cherry_tomato,
            variety = R.string.plant_variety_cherry_tomato,
            category = R.string.plant_category_fruiting,
            icon = "🍅",
            harvestDays = R.string.harvest_days_cherry_tomato,
            harvestBasis = R.string.harvest_basis_cherry_tomato,
            plantingMethod = R.string.planting_cherry_tomato,
            light = R.string.light_cherry_tomato,
            medium = R.string.medium_cherry_tomato,
            watering = R.string.water_cherry_tomato,
            nutrients = R.string.nutrients_cherry_tomato,
            care = listOf(R.string.care_cherry_tomato_1, R.string.care_cherry_tomato_2),
            problems = listOf(R.string.pest_cherry_tomato_1, R.string.pest_cherry_tomato_2),
            note = R.string.note_cherry_tomato
        )
    )
}

data class CountryOption(val code: String, @StringRes val label: Int)

object SoutheastAsianCountries {
    val options = listOf(
        CountryOption("BN", R.string.location_country_brunei),
        CountryOption("KH", R.string.location_country_cambodia),
        CountryOption("ID", R.string.location_country_indonesia),
        CountryOption("LA", R.string.location_country_laos),
        CountryOption("MY", R.string.location_country_malaysia),
        CountryOption("MM", R.string.location_country_myanmar),
        CountryOption("PH", R.string.location_country_philippines),
        CountryOption("SG", R.string.location_country_singapore),
        CountryOption("TH", R.string.location_country_thailand),
        CountryOption("TL", R.string.location_country_timor_leste),
        CountryOption("VN", R.string.location_country_vietnam)
    )

    fun byCode(code: String): CountryOption = options.firstOrNull { it.code == code } ?: options[2]
}
