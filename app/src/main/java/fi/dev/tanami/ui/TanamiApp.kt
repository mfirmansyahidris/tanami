package fi.dev.tanami.ui

import android.net.Uri
import androidx.appcompat.app.AppCompatDelegate
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import coil.compose.AsyncImage
import fi.dev.tanami.ai.PlantDiagnostician
import fi.dev.tanami.data.GardenEntry
import fi.dev.tanami.data.GardenStore
import fi.dev.tanami.data.PlantCatalog
import fi.dev.tanami.data.PlantGuide
import fi.dev.tanami.data.SoutheastAsianCountries
import fi.dev.tanami.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

private enum class AppTab(@androidx.annotation.StringRes val title: Int) {
    HOME(R.string.tab_home), PLANTS(R.string.tab_plants), GARDEN(R.string.tab_garden), DIAGNOSE(R.string.tab_analysis)
}

@Composable
fun TanamiApp() {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isEnglish = configuration.locales[0]?.language == "en"
    val store = remember { GardenStore(context.applicationContext) }
    var entries by remember { mutableStateOf(store.loadPlants()) }
    var country by remember { mutableStateOf(store.loadCountry()) }
    val countryName = stringResource(SoutheastAsianCountries.byCode(country).label)
    var activeTab by rememberSaveable { mutableStateOf(AppTab.HOME) }
    var selectedPlant by remember { mutableStateOf<PlantGuide?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                AppTab.entries.forEach { tab ->
                    val image = when (tab) {
                        AppTab.HOME -> Icons.Outlined.Home
                        AppTab.PLANTS -> Icons.Outlined.Explore
                        AppTab.GARDEN -> Icons.Outlined.Spa
                        AppTab.DIAGNOSE -> Icons.Outlined.BugReport
                    }
                    NavigationBarItem(
                        selected = activeTab == tab && selectedPlant == null,
                        onClick = { selectedPlant = null; activeTab = tab },
                        icon = { Icon(image, contentDescription = null) },
                        label = { Text(stringResource(tab.title)) }
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            val openPlant: (PlantGuide) -> Unit = { selectedPlant = it }
            when {
                selectedPlant != null -> PlantDetailScreen(
                    plant = selectedPlant!!,
                    isInGarden = entries.any { it.plantId == selectedPlant!!.id },
                    onBack = { selectedPlant = null },
                    onAdd = {
                        store.addPlant(selectedPlant!!.id)
                        entries = store.loadPlants()
                    }
                )
                activeTab == AppTab.HOME -> HomeScreen(
                    entries = entries,
                    country = countryName,
                    isEnglish = isEnglish,
                    onLanguageChange = {
                        val languageTag = if (isEnglish) "id" else "en"
                        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageTag))
                    },
                    onCountryChange = { selected -> store.saveCountry(selected); country = selected },
                    onExplore = { activeTab = AppTab.PLANTS },
                    onPlant = openPlant
                )
                activeTab == AppTab.PLANTS -> PlantCatalogScreen(onPlant = openPlant)
                activeTab == AppTab.GARDEN -> GardenScreen(
                    entries = entries,
                    onPlant = openPlant,
                    onExplore = { activeTab = AppTab.PLANTS },
                    onRemove = { id -> store.removePlant(id); entries = store.loadPlants() },
                    onCare = { id, care -> store.recordCare(id, care); entries = store.loadPlants() }
                )
                else -> DiagnoseScreen(country = countryName, isEnglish = isEnglish)
            }
        }
    }
}

@Composable
private fun HomeScreen(
    entries: List<GardenEntry>,
    country: String,
    isEnglish: Boolean,
    onLanguageChange: () -> Unit,
    onCountryChange: (String) -> Unit,
    onExplore: () -> Unit,
    onPlant: (PlantGuide) -> Unit
) {
    var showCountries by rememberSaveable { mutableStateOf(false) }
    val languageSwitchDescription = stringResource(R.string.language_switch_description)
    val firstPlants = PlantCatalog.plants.take(3)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                    Text("🌱", modifier = Modifier.padding(11.dp), style = MaterialTheme.typography.titleLarge)
                }
                Spacer(Modifier.width(11.dp))
                Column {
                    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.app_tagline), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onLanguageChange, contentPadding = PaddingValues(horizontal = 10.dp)) {
                    Text(
                        if (isEnglish) "ID" else "EN",
                        modifier = Modifier.semantics { contentDescription = languageSwitchDescription },
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        item {
            TextButton(onClick = { showCountries = true }, contentPadding = PaddingValues(horizontal = 0.dp)) {
                Text(stringResource(R.string.location_label, country), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
            }
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = MaterialTheme.shapes.large
            ) {
                Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.hero_title), style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)
                    Text(
                        stringResource(R.string.hero_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = .86f)
                    )
                    Button(onClick = onExplore, contentPadding = PaddingValues(horizontal = 18.dp, vertical = 11.dp)) {
                        Icon(Icons.Outlined.Explore, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.browse_plants))
                    }
                }
            }
        }
        item {
            SectionTitle(stringResource(R.string.garden_today), pluralStringResource(R.plurals.active_plants, entries.size, entries.size))
            Spacer(Modifier.height(10.dp))
            if (entries.isEmpty()) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("🪴", style = MaterialTheme.typography.headlineMedium)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.empty_garden_title), fontWeight = FontWeight.SemiBold)
                            Text(stringResource(R.string.empty_garden_hint), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            } else {
                entries.take(2).forEach { entry ->
                    val plant = PlantCatalog.plants.firstOrNull { it.id == entry.plantId } ?: return@forEach
                    GardenMiniCard(plant, entry, onClick = { onPlant(plant) })
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
        item { SectionTitle(stringResource(R.string.easy_start), stringResource(R.string.harvest_within_90)) }
        items(firstPlants) { plant -> PlantCard(plant = plant, onClick = { onPlant(plant) }) }
        item {
            Text(
                stringResource(R.string.harvest_estimate_disclaimer),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    if (showCountries) {
        AlertDialog(
            onDismissRequest = { showCountries = false },
            title = { Text(stringResource(R.string.choose_location)) },
            text = {
                LazyColumn(modifier = Modifier.height(360.dp)) {
                    items(SoutheastAsianCountries.options) { option ->
                        val name = stringResource(option.label)
                        TextButton(
                            onClick = { onCountryChange(option.code); showCountries = false },
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            Text(name, modifier = Modifier.weight(1f), color = if (name == country) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                            if (name == country) Text("✓", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showCountries = false }) { Text(stringResource(R.string.done)) } }
        )
    }
}

@Composable
private fun PlantCatalogScreen(onPlant: (PlantGuide) -> Unit) {
    var category by rememberSaveable { mutableIntStateOf(R.string.category_all) }
    val categories = listOf(R.string.category_all, R.string.category_leafy, R.string.category_fruiting)
    val plants = PlantCatalog.plants.filter { category == R.string.category_all || it.category == category }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(stringResource(R.string.catalog_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.catalog_description), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                categories.forEach { item ->
                    FilterChip(selected = category == item, onClick = { category = item }, label = { Text(stringResource(item)) })
                }
            }
        }
        items(plants) { plant -> PlantCard(plant = plant, onClick = { onPlant(plant) }) }
        item { Text(stringResource(R.string.catalog_note), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun PlantCard(plant: PlantGuide, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primaryContainer) {
                Text(plant.icon, modifier = Modifier.padding(14.dp), style = MaterialTheme.typography.headlineMedium)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(stringResource(plant.name), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(stringResource(plant.category), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(R.string.harvest_time_card, stringResource(plant.harvestDays)), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            Icon(Icons.Outlined.ArrowForward, contentDescription = stringResource(R.string.open_guide), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun PlantDetailScreen(
    plant: PlantGuide,
    isInGarden: Boolean,
    onBack: () -> Unit,
    onAdd: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            TextButton(onClick = onBack, contentPadding = PaddingValues(horizontal = 0.dp)) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.back))
            }
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), shape = MaterialTheme.shapes.large) {
                Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(plant.icon, style = MaterialTheme.typography.displaySmall)
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(stringResource(plant.name), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text(stringResource(plant.variety), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(stringResource(R.string.harvest_around, stringResource(plant.harvestDays)), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        item { InfoSection(stringResource(R.string.section_planting), stringResource(plant.plantingMethod)) }
        item { InfoSection(stringResource(R.string.section_light), stringResource(plant.light)) }
        item { InfoSection(stringResource(R.string.section_medium), stringResource(plant.medium)) }
        item { InfoSection(stringResource(R.string.section_water), stringResource(plant.watering)) }
        item { InfoSection(stringResource(R.string.section_nutrients), stringResource(plant.nutrients)) }
        item {
            DetailListSection(R.string.section_treatments, plant.care)
        }
        item {
            DetailListSection(R.string.section_pests, plant.problems)
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3D8))) {
                Text("ℹ️  ${stringResource(plant.harvestBasis)}\n\n${stringResource(plant.note)}", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodySmall)
            }
        }
        item {
            Button(onClick = onAdd, enabled = !isInGarden, modifier = Modifier.fillMaxWidth().height(54.dp)) {
                Icon(if (isInGarden) Icons.Outlined.Spa else Icons.Outlined.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(if (isInGarden) R.string.added_to_garden else R.string.start_growing))
            }
        }
    }
}

@Composable
private fun InfoSection(title: String, body: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DetailListSection(@androidx.annotation.StringRes title: Int, items: List<Int>) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            items.forEach { Text("•  ${stringResource(it)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
private fun GardenScreen(
    entries: List<GardenEntry>,
    onPlant: (PlantGuide) -> Unit,
    onExplore: () -> Unit,
    onRemove: (String) -> Unit,
    onCare: (String, String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(stringResource(R.string.garden_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.garden_subtitle), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (entries.isEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("🪴", style = MaterialTheme.typography.displaySmall)
                        Text(stringResource(R.string.garden_empty_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.garden_empty_description), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(onClick = onExplore) { Text(stringResource(R.string.explore_plants)) }
                    }
                }
            }
        } else {
            items(entries, key = { it.plantId }) { entry ->
                val plant = PlantCatalog.plants.firstOrNull { it.id == entry.plantId } ?: return@items
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(plant.icon, style = MaterialTheme.typography.headlineMedium)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(plant.name), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(stringResource(R.string.day_and_target, daysSince(entry.startedAt), stringResource(plant.harvestDays)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            TextButton(onClick = { onRemove(plant.id) }) {
                                Icon(Icons.Outlined.DeleteOutline, contentDescription = stringResource(R.string.remove_from_garden))
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { onPlant(plant) }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.guide_button)) }
                            OutlinedButton(onClick = { onCare(plant.id, "water") }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.watered_button)) }
                            OutlinedButton(onClick = { onCare(plant.id, "feed") }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.feed_button)) }
                        }
                        val wateredAt = entry.lastWateredAt?.let { elapsedSince(it) } ?: stringResource(R.string.not_logged)
                        val fedAt = entry.lastFedAt?.let { elapsedSince(it) } ?: stringResource(R.string.not_logged)
                        Text(
                            stringResource(R.string.care_last_logged, wateredAt, fedAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        item { Text(stringResource(R.string.care_local_note), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun GardenMiniCard(plant: PlantGuide, entry: GardenEntry, onClick: () -> Unit) {
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(plant.icon, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(plant.name), fontWeight = FontWeight.SemiBold)
                val careStatus = entry.lastWateredAt?.let { stringResource(R.string.watered_status, elapsedSince(it)) }
                    ?: stringResource(R.string.check_moisture)
                Text(stringResource(R.string.day_care_status, daysSince(entry.startedAt), careStatus), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Outlined.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun DiagnoseScreen(country: String, isEnglish: Boolean) {
    val context = LocalContext.current
    val diagnostician = remember { PlantDiagnostician(context.applicationContext) }
    val scope = rememberCoroutineScope()
    var imageUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var result by rememberSaveable { mutableStateOf<String?>(null) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var isLoading by rememberSaveable { mutableStateOf(false) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        imageUri = uri
        result = null
        error = null
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(stringResource(R.string.diagnosis_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.diagnosis_description), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), shape = MaterialTheme.shapes.large) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.BugReport, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(stringResource(R.string.ai_initial_check), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.ai_explanation), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            if (imageUri != null) {
                AsyncImage(
                    model = imageUri,
                    contentDescription = stringResource(R.string.selected_plant_photo),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(230.dp).clip(MaterialTheme.shapes.large)
                )
            } else {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.fillMaxWidth().height(190.dp).padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Icon(Icons.Outlined.CameraAlt, contentDescription = null, modifier = Modifier.size(42.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.photo_prompt_title), fontWeight = FontWeight.SemiBold)
                        Text(stringResource(R.string.photo_prompt_description), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        item {
            OutlinedButton(
                onClick = { imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Icon(Icons.Outlined.CameraAlt, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(if (imageUri == null) R.string.choose_photo else R.string.change_photo))
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3D8))) {
                Text(
                    stringResource(R.string.photo_privacy),
                    modifier = Modifier.padding(15.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        item {
            if (!diagnostician.isConfigured) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Text(stringResource(R.string.firebase_not_configured), modifier = Modifier.padding(15.dp), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        if (imageUri != null) {
            item {
                Button(
                    onClick = {
                        val photo = imageUri ?: return@Button
                        isLoading = true
                        result = null
                        error = null
                        scope.launch {
                            try {
                                result = diagnostician.analyze(photo, country, isEnglish)
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (exception: Exception) {
                                error = exception.message ?: context.getString(R.string.analysis_failed)
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    enabled = diagnostician.isConfigured && !isLoading,
                    modifier = Modifier.fillMaxWidth().height(54.dp)
                ) {
                    if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    else Text(stringResource(R.string.analyze_photo))
                }
            }
        }
        error?.let { message -> item { InfoSection(stringResource(R.string.analysis_error_title), message) } }
        result?.let { answer -> item { InfoSection(stringResource(R.string.analysis_result_title), answer) } }
        item {
            Text(stringResource(R.string.diagnosis_tip), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String? = null) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        subtitle?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

private fun daysSince(startedAt: Long): Int =
    (TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - startedAt) + 1).coerceAtLeast(1).toInt()

@Composable
private fun elapsedSince(timestamp: Long): String {
    val days = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - timestamp)
    return when {
        days <= 0 -> stringResource(R.string.today)
        days == 1L -> stringResource(R.string.yesterday)
        else -> stringResource(R.string.days_ago, days.toInt())
    }
}
