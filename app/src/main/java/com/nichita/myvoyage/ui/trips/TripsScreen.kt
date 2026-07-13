package com.nichita.myvoyage.ui.trips

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.CurrencyExchange
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nichita.myvoyage.R
import com.nichita.myvoyage.ui.components.frostedGlass
import com.nichita.myvoyage.ui.theme.Marsala
import com.nichita.myvoyage.ui.theme.WineViolet
import com.nichita.myvoyage.util.Format
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

/**
 * Главный экран в стиле travel-приложения: крупный сворачивающийся заголовок,
 * плитки-шорткаты по активному рейсу, промо-карточка с фото, карусель последних
 * рейсов и полный список. Фоны промо/карусели — реальные фото (res/drawable).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripsScreen(
    onAddTrip: () -> Unit,
    onOpenTrip: (Long) -> Unit,
    onQuickExpense: (Long) -> Unit,
    onQuickFuel: (Long) -> Unit,
    onOpenRates: () -> Unit,
    bottomInset: Dp = 0.dp,
    viewModel: TripsViewModel = viewModel(factory = TripsViewModel.Factory)
) {
    val trips by viewModel.trips.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val topHaze = remember { HazeState() }

    // «Текущий» рейс: незавершённый и самый свежий; иначе — просто самый свежий.
    val active = trips.filter { it.trip.endDate == null }.maxByOrNull { it.trip.startDate }
        ?: trips.maxByOrNull { it.trip.startDate }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = Color.Transparent,
        topBar = {
            LargeTopAppBar(
                title = { Text("Мои рейсы") },
                modifier = Modifier.frostedGlass(topHaze),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                ),
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddTrip) {
                Text("+", style = MaterialTheme.typography.headlineSmall)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(topHaze),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 16.dp + bottomInset
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Плитка курса валют — доступна всегда (даже без рейсов).
            item { CurrencyTile(onClick = onOpenRates) }

            if (trips.isEmpty()) {
                item { PromoCard(onAddTrip) }
                item {
                    Text(
                        "Пока нет рейсов. Создайте первый — кнопкой «+» или картой выше.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Плитки-шорткаты по активному рейсу
                item {
                    QuickActions(
                        activeTripId = active!!.trip.id,
                        onOpenTrip = onOpenTrip,
                        onQuickExpense = onQuickExpense,
                        onQuickFuel = onQuickFuel
                    )
                }

                // Промо-карточка с фото и «матовой» кнопкой
                item { PromoCard(onAddTrip) }

                // Карусель последних рейсов
                item { SectionHeader("Последние рейсы") }
                item { RecentCarousel(trips = trips, onOpenTrip = onOpenTrip) }

                // Полный список
                item { SectionHeader("Все рейсы") }
                items(trips, key = { it.trip.id }) { item ->
                    TripCard(item = item, onClick = { onOpenTrip(item.trip.id) })
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface
    )
}

// --- Плитки-шорткаты (по активному рейсу) ---

@Composable
private fun QuickActions(
    activeTripId: Long,
    onOpenTrip: (Long) -> Unit,
    onQuickExpense: (Long) -> Unit,
    onQuickFuel: (Long) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ActionTile(
            modifier = Modifier.weight(1f),
            icon = Icons.Outlined.Explore,
            label = "Текущий рейс",
            gradient = AppGradients[0],
            onClick = { onOpenTrip(activeTripId) }
        )
        ActionTile(
            modifier = Modifier.weight(1f),
            icon = Icons.AutoMirrored.Outlined.ReceiptLong,
            label = "Расход",
            gradient = AppGradients[1],
            onClick = { onQuickExpense(activeTripId) }
        )
        ActionTile(
            modifier = Modifier.weight(1f),
            icon = Icons.Outlined.LocalGasStation,
            label = "Заправка",
            gradient = AppGradients[2],
            onClick = { onQuickFuel(activeTripId) }
        )
    }
}

/** Широкая плитка-шорткат на экран курса валют. */
@Composable
private fun CurrencyTile(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
            .clip(MaterialTheme.shapes.large)
            .background(AppGradients[4])
            .clickable(onClick = onClick)
            .padding(16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.CurrencyExchange,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    "Курс валют",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Text(
                    "Актуальные курсы НБМ",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}

@Composable
private fun ActionTile(
    modifier: Modifier,
    icon: ImageVector,
    label: String,
    gradient: Brush,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(100.dp)
            .clip(MaterialTheme.shapes.large)
            .background(gradient)
            .clickable(onClick = onClick)
            .padding(14.dp),
        contentAlignment = Alignment.BottomStart
    ) {
        Column {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                label,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// --- Промо-карточка с фото ---

@Composable
private fun PromoCard(onAddTrip: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(176.dp)
            .clip(MaterialTheme.shapes.large)
    ) {
        Image(
            painter = painterResource(TravelImages[0]),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )
        // Винный тон палитры поверх фото — для единства и читаемости текста.
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        listOf(WineViolet.copy(alpha = 0.45f), Marsala.copy(alpha = 0.8f))
                    )
                )
        )
        Column(
            Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "Новое путешествие",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Запланируйте рейс и следите за расходами в пути",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
            FrostedButton(label = "Создать рейс", onClick = onAddTrip)
        }
    }
}

/** «Матовая» полупрозрачная кнопка поверх изображения. */
@Composable
private fun FrostedButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(MaterialTheme.shapes.large)
            .background(Color.White.copy(alpha = 0.25f))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = Color.White
        )
    }
}

// --- Карусель последних рейсов (с фото) ---

@Composable
private fun RecentCarousel(trips: List<TripListItem>, onOpenTrip: (Long) -> Unit) {
    val recent = trips.sortedByDescending { it.trip.startDate }.take(6)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(recent, key = { it.trip.id }) { item ->
            CarouselCard(item = item, onClick = { onOpenTrip(item.trip.id) })
        }
    }
}

@Composable
private fun CarouselCard(item: TripListItem, onClick: () -> Unit) {
    val trip = item.trip
    Box(
        modifier = Modifier
            .width(220.dp)
            .height(128.dp)
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
    ) {
        Image(
            painter = painterResource(imageFor(trip.id)),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        listOf(WineViolet.copy(alpha = 0.4f), Marsala.copy(alpha = 0.8f))
                    )
                )
        )
        Column(
            Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = trip.destination,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    Format.date(trip.startDate),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f)
                )
                Box(
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.small)
                        .background(Color.White.copy(alpha = 0.25f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        Format.money(item.spent, trip.currency),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

// --- Карточка-строка рейса ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TripCard(item: TripListItem, onClick: () -> Unit) {
    val trip = item.trip
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Круглый «аватар» направления с первой буквой.
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(gradientFor(trip.id)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = avatarChar(trip.destination),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = trip.destination,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val dates = if (trip.endDate != null)
                    "${Format.date(trip.startDate)} — ${Format.date(trip.endDate)}"
                else
                    Format.date(trip.startDate)
                Text(
                    text = dates,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.width(12.dp))

            // Потраченная сумма — «чипом» справа.
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = Format.money(item.spent, trip.currency),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1
                )
            }
        }
    }
}

/** Первая буква направления для аватара. */
private fun avatarChar(text: String): String =
    text.trim().firstOrNull()?.uppercase() ?: "•"

// --- Декоративные ресурсы ---

/** Травел-фото (res/drawable) для промо и карусели. */
private val TravelImages: List<Int> = listOf(
    R.drawable.travel_1,
    R.drawable.travel_2,
    R.drawable.travel_3,
    R.drawable.travel_4,
    R.drawable.travel_5
)

/** Стабильно выбирает фото по id рейса. */
private fun imageFor(seed: Long): Int = TravelImages[pickIndex(seed, TravelImages.size)]

/** Градиенты палитры — для плиток и аватаров. */
private val AppGradients: List<Brush> = listOf(
    Brush.linearGradient(listOf(Color(0xFF8F4A57), Color(0xFFB56B76))), // винный
    Brush.linearGradient(listOf(Color(0xFF6E5B86), Color(0xFF9385B0))), // фиолетовый
    Brush.linearGradient(listOf(Color(0xFF4F6477), Color(0xFF7D93A8))), // пыльно-голубой
    Brush.linearGradient(listOf(Color(0xFF7A5A6E), Color(0xFFA98598))), // сливово-розовый
    Brush.linearGradient(listOf(Color(0xFF566B8C), Color(0xFF8AA0BE)))  // стальной синий
)

/** Стабильно выбирает градиент по id рейса. */
private fun gradientFor(seed: Long): Brush = AppGradients[pickIndex(seed, AppGradients.size)]

private fun pickIndex(seed: Long, size: Int): Int = ((seed % size).toInt() + size) % size
