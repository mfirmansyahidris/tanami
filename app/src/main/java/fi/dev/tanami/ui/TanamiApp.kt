package fi.dev.tanami.ui

import android.net.Uri
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
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import fi.dev.tanami.ai.PlantDiagnostician
import fi.dev.tanami.data.GardenEntry
import fi.dev.tanami.data.GardenStore
import fi.dev.tanami.data.PlantCatalog
import fi.dev.tanami.data.PlantGuide
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

private enum class AppTab(val title: String) {
    HOME("Beranda"), PLANTS("Tanam"), GARDEN("Kebun"), DIAGNOSE("Analisis")
}

@Composable
fun TanamiApp() {
    val context = LocalContext.current
    val store = remember { GardenStore(context.applicationContext) }
    var entries by remember { mutableStateOf(store.loadPlants()) }
    var country by remember { mutableStateOf(store.loadCountry()) }
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
                        label = { Text(tab.title) }
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
                country = country,
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
                else -> DiagnoseScreen(country = country)
            }
        }
    }
}

@Composable
private fun HomeScreen(
    entries: List<GardenEntry>,
    country: String,
    onCountryChange: (String) -> Unit,
    onExplore: () -> Unit,
    onPlant: (PlantGuide) -> Unit
) {
    var showCountries by rememberSaveable { mutableStateOf(false) }
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
                    Text("Tanami", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Tumbuhkan sesuatu hari ini", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.weight(1f))
                Text("🌤️", style = MaterialTheme.typography.headlineSmall)
            }
        }
        item {
            TextButton(onClick = { showCountries = true }, contentPadding = PaddingValues(horizontal = 0.dp)) {
                Text("🌏  $country · Atur lokasi kebun  ▾", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
            }
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = MaterialTheme.shapes.large
            ) {
                Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Dari benih ke panen", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)
                    Text(
                        "Panduan menanam sayur dan buah yang cocok dimulai di rumah.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = .86f)
                    )
                    Button(onClick = onExplore, contentPadding = PaddingValues(horizontal = 18.dp, vertical = 11.dp)) {
                        Icon(Icons.Outlined.Explore, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Cari tanaman")
                    }
                }
            }
        }
        item {
            SectionTitle("Kebunmu hari ini", "${entries.size} tanaman aktif")
            Spacer(Modifier.height(10.dp))
            if (entries.isEmpty()) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("🪴", style = MaterialTheme.typography.headlineMedium)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Kebunmu masih kosong", fontWeight = FontWeight.SemiBold)
                            Text("Pilih tanaman untuk memulai perjalanan.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
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
        item { SectionTitle("Mulai dengan yang mudah", "Panen dalam sekitar 90 hari") }
        items(firstPlants) { plant -> PlantCard(plant = plant, onClick = { onPlant(plant) }) }
        item {
            Text(
                "Perkiraan panen dapat berubah menurut varietas, cuaca, dan cara hitung pada kemasan benih.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    if (showCountries) {
        val countries = listOf("Brunei", "Kamboja", "Indonesia", "Laos", "Malaysia", "Myanmar", "Filipina", "Singapura", "Thailand", "Timor-Leste", "Vietnam")
        AlertDialog(
            onDismissRequest = { showCountries = false },
            title = { Text("Pilih lokasimu") },
            text = {
                LazyColumn(modifier = Modifier.height(360.dp)) {
                    items(countries) { option ->
                        TextButton(
                            onClick = { onCountryChange(option); showCountries = false },
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            Text(option, modifier = Modifier.weight(1f), color = if (option == country) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                            if (option == country) Text("✓", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showCountries = false }) { Text("Selesai") } }
        )
    }
}

@Composable
private fun PlantCatalogScreen(onPlant: (PlantGuide) -> Unit) {
    var category by rememberSaveable { mutableStateOf("Semua") }
    val categories = listOf("Semua", "Daun", "Buah sayur")
    val plants = PlantCatalog.plants.filter { category == "Semua" || it.category == category }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Pilih tanaman", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Mulai dari tanaman yang cocok dengan ruang dan waktumu.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                categories.forEach { item ->
                    FilterChip(selected = category == item, onClick = { category = item }, label = { Text(item) })
                }
            }
        }
        items(plants) { plant -> PlantCard(plant = plant, onClick = { onPlant(plant) }) }
        item { Text("Katalog awal berisi panduan ringkas. Lokasi dan varietas akan dipakai untuk menyesuaikan rekomendasi berikutnya.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
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
                Text(plant.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(plant.category, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("⏱  ${plant.harvestDays} sampai panen", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            Icon(Icons.Outlined.ArrowForward, contentDescription = "Lihat panduan", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
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
                Text("Kembali")
            }
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), shape = MaterialTheme.shapes.large) {
                Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(plant.icon, style = MaterialTheme.typography.displaySmall)
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(plant.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text(plant.variety, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Panen sekitar ${plant.harvestDays}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        item { InfoSection("🌱  Cara mulai", plant.plantingMethod) }
        item { InfoSection("☀️  Cahaya", plant.light) }
        item { InfoSection("🪴  Media tanam", plant.medium) }
        item { InfoSection("💧  Air", plant.watering) }
        item { InfoSection("🌿  Nutrisi", plant.nutrients) }
        item {
            DetailListSection("Perlakuan tanaman", plant.care)
        }
        item {
            DetailListSection("Hama dan gangguan yang perlu dicek", plant.problems)
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3D8))) {
                Text("ℹ️  ${plant.harvestBasis}\n\n${plant.note}", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodySmall)
            }
        }
        item {
            Button(onClick = onAdd, enabled = !isInGarden, modifier = Modifier.fillMaxWidth().height(54.dp)) {
                Icon(if (isInGarden) Icons.Outlined.Spa else Icons.Outlined.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (isInGarden) "Sudah ada di kebunmu" else "Mulai tanam ini")
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
private fun DetailListSection(title: String, items: List<String>) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            items.forEach { Text("•  $it", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
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
            Text("Kebunku", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Catat perjalanan tanam dari hari pertama.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (entries.isEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("🪴", style = MaterialTheme.typography.displaySmall)
                        Text("Belum ada tanaman", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Tambahkan tanaman untuk mulai melihat panduan dan catatan pertumbuhannya.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(onClick = onExplore) { Text("Jelajahi tanaman") }
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
                                Text(plant.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Hari ke-${daysSince(entry.startedAt)} · Target ${plant.harvestDays}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            TextButton(onClick = { onRemove(plant.id) }) {
                                Icon(Icons.Outlined.DeleteOutline, contentDescription = "Hapus dari kebun")
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { onPlant(plant) }, modifier = Modifier.weight(1f)) { Text("Panduan") }
                            OutlinedButton(onClick = { onCare(plant.id, "water") }, modifier = Modifier.weight(1f)) { Text("Sudah siram") }
                            OutlinedButton(onClick = { onCare(plant.id, "feed") }, modifier = Modifier.weight(1f)) { Text("Catat pupuk") }
                        }
                        Text(
                            "Terakhir disiram: ${entry.lastWateredAt?.let(::elapsedSince) ?: "belum dicatat"} · Pupuk: ${entry.lastFedAt?.let(::elapsedSince) ?: "belum dicatat"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        item { Text("Catatan perawatan disimpan di perangkat ini. Tombol mencatat tindakan yang kamu lakukan; cek media sebelum menyiram.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun GardenMiniCard(plant: PlantGuide, entry: GardenEntry, onClick: () -> Unit) {
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(plant.icon, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(plant.name, fontWeight = FontWeight.SemiBold)
                val careStatus = entry.lastWateredAt?.let(::elapsedSince)?.let { "Disiram $it" } ?: "Cek kelembapan media"
                Text("Hari ke-${daysSince(entry.startedAt)} · $careStatus", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Outlined.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun DiagnoseScreen(country: String) {
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
            Text("Analisis tanaman", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Foto gejala untuk mendapat kemungkinan penyebab dan langkah pemeriksaan.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), shape = MaterialTheme.shapes.large) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.BugReport, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Pemeriksaan awal dengan AI", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Gemini akan melihat foto dan membantu menyaring kemungkinan masalah. Hasilnya bukan diagnosis pasti.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            if (imageUri != null) {
                AsyncImage(
                    model = imageUri,
                    contentDescription = "Foto tanaman yang dipilih",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(230.dp).clip(MaterialTheme.shapes.large)
                )
            } else {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.fillMaxWidth().height(190.dp).padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Icon(Icons.Outlined.CameraAlt, contentDescription = null, modifier = Modifier.size(42.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(8.dp))
                        Text("Pilih foto yang terang dan fokus", fontWeight = FontWeight.SemiBold)
                        Text("Foto daun dari atas dan bawah membantu pemeriksaan.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
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
                Text(if (imageUri == null) "Pilih foto tanaman" else "Ganti foto")
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3D8))) {
                Text(
                    "Privasi: foto akan dikirim ke Google Gemini hanya setelah kamu menekan Analisa. Jangan sertakan wajah atau informasi pribadi.",
                    modifier = Modifier.padding(15.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        item {
            if (!diagnostician.isConfigured) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Text("Analisis foto belum aktif. Tambahkan konfigurasi Firebase untuk fi.dev.tanami; fitur kebun dan panduan tetap tersedia offline.", modifier = Modifier.padding(15.dp), style = MaterialTheme.typography.bodySmall)
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
                                result = diagnostician.analyze(photo, country)
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (exception: Exception) {
                                error = exception.message ?: "Analisis gagal. Periksa koneksi dan coba lagi."
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    enabled = diagnostician.isConfigured && !isLoading,
                    modifier = Modifier.fillMaxWidth().height(54.dp)
                ) {
                    if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    else Text("Analisa foto dengan Gemini")
                }
            }
        }
        error?.let { message -> item { InfoSection("Belum berhasil", message) } }
        result?.let { answer -> item { InfoSection("Hasil pemeriksaan awal", answer) } }
        item {
            Text("Tip: bila gejalanya berubah cepat, menjalar, atau seluruh tanaman layu, ambil foto ulang dan minta bantuan penyuluh pertanian setempat.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

private fun daysSince(startedAt: Long): Long =
    (TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - startedAt) + 1).coerceAtLeast(1)

private fun elapsedSince(timestamp: Long): String {
    val days = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - timestamp)
    return when {
        days <= 0 -> "hari ini"
        days == 1L -> "kemarin"
        else -> "$days hari lalu"
    }
}
