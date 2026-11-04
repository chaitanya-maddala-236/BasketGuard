package com.basketguard.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.basketguard.android.presentation.AppUiState
import com.basketguard.android.presentation.AppViewModel
import com.basketguard.android.data.local.CartEntity
import com.basketguard.android.data.network.OrderDto
import com.basketguard.android.data.network.SubstitutionDto
import com.basketguard.shared.domain.BasketRescueItem
import com.basketguard.shared.domain.Product
import com.basketguard.shared.domain.ProductAlternative
import com.basketguard.shared.domain.SmartBasketPlan
import java.util.Locale

private enum class Tab(val title: String) { HOME("Home"), CART("Basket"), ORDERS("Orders"), PROFILE("Profile") }
private val Forest = Color(0xFF174B3D)
private val Mint = Color(0xFFDCEDE0)
private val Warm = Color(0xFFF7F6F0)
private val Ink = Color(0xFF1E2924)
private val Muted = Color(0xFF748078)
private val RescueWash = Color(0xFFFFF0E4)

@Composable
fun BasketGuardApp(viewModel: AppViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var tab by rememberSaveable { mutableStateOf(Tab.HOME) }
    var checkout by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.message) {
        state.message?.let {
            if (it.startsWith("Order placed")) {
                checkout = false
                tab = Tab.ORDERS
            }
            snackbar.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        containerColor = Warm,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (!checkout) NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) {
                Tab.entries.forEach { item ->
                    val icon = when (item) {
                        Tab.HOME -> Icons.Outlined.Home
                        Tab.CART -> Icons.Outlined.ShoppingBag
                        Tab.ORDERS -> Icons.Outlined.ReceiptLong
                        Tab.PROFILE -> Icons.Outlined.Person
                    }
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = {
                            tab = item
                            if (item == Tab.ORDERS) viewModel.loadOrders()
                        },
                        icon = { Icon(icon, contentDescription = item.title) },
                        label = { Text(item.title) },
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                checkout -> CheckoutScreen(
                    state = state,
                    onBack = { checkout = false },
                    onPlaceOrder = { viewModel.placeOrder(it) },
                    onSignIn = { checkout = false; tab = Tab.PROFILE },
                    onBudget = viewModel::setRescueBudget,
                    onAlternative = viewModel::applyRescue,
                    onKeepAvailable = viewModel::keepAvailable,
                    onRemoveUnavailable = viewModel::removeUnavailable,
                )
                tab == Tab.HOME -> HomeScreen(state, viewModel, onProfile = { tab = Tab.PROFILE })
                tab == Tab.CART -> CartScreen(state, viewModel, onCheckout = { checkout = true })
                tab == Tab.ORDERS -> OrdersScreen(state, viewModel)
                else -> ProfileScreen(state, viewModel)
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun HomeScreen(state: AppUiState, viewModel: AppViewModel, onProfile: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf(0) }
    var details by remember { mutableStateOf<Product?>(null) }
    var swapFor by remember { mutableStateOf<Product?>(null) }
    var plannerOpen by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(swapFor?.id, state.rescueBudgetPercent) {
        swapFor?.let { viewModel.loadAlternatives(it, quantity = 1) }
    }
    val cartQuantities = state.cart.associate { it.productId to it.quantity }
    val filtered = state.products.filter { product ->
        (selectedCategory == 0 || product.categoryId == selectedCategory) &&
            (query.isBlank() || product.name.contains(query, ignoreCase = true) ||
                product.category.contains(query, ignoreCase = true))
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item { StoreHeader(state, onProfile = onProfile) }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search avocado, coffee, eggs…", color = Muted) },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = Forest) },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
            )
        }
        item { BasketHero() }
        item {
            Card(
                onClick = { plannerOpen = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
            ) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(Mint), contentAlignment = Alignment.Center) {
                        Text("✨", fontSize = 24.sp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("AI Basket Planner", color = Ink, fontWeight = FontWeight.ExtraBold)
                        Text("Plan a meal with live stock and your budget.", color = Muted, style = MaterialTheme.typography.bodySmall)
                    }
                    Text("PLAN  ›", color = Forest, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Browse by aisle", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Ink)
                Spacer(Modifier.weight(1f))
                Text("See all", color = Forest, style = MaterialTheme.typography.labelLarge)
            }
        }
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.categories.forEach { category ->
                    FilterChip(
                        selected = selectedCategory == category.id,
                        onClick = { selectedCategory = category.id },
                        label = { Text("${category.icon}  ${category.name}") },
                    )
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.Bottom) {
                Column {
                    Text("Picked for your day", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Ink)
                    Text("Fresh finds, checked before checkout", color = Muted, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.weight(1f))
                if (!state.connected) Text("SAVED OFFLINE", color = Forest, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
        }
        if (filtered.isEmpty()) {
            item { EmptyCard("No products found", "Try another search or category.", "🔎") }
        } else {
            itemsIndexed(filtered.chunked(2)) { _, rowProducts ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    rowProducts.forEach { product ->
                        ProductCard(
                            product = product,
                            quantity = cartQuantities[product.id] ?: 0,
                            modifier = Modifier.weight(1f),
                            onAdd = { viewModel.add(product) },
                            onQuantity = { viewModel.changeQuantity(product.id, it) },
                            onDetails = { details = product },
                            onSwap = { swapFor = product },
                        )
                    }
                    if (rowProducts.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
        item { Spacer(Modifier.height(3.dp)) }
    }

    details?.let { product ->
        ProductDetailsSheet(product, onDismiss = { details = null }, onAdd = {
            viewModel.add(product)
            details = null
        })
    }
    swapFor?.let { product ->
        val options = state.alternatives[product.id].orEmpty()
        ModalBottomSheet(onDismissRequest = { swapFor = null }, containerColor = Warm) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp).padding(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Basket Rescue", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Ink)
                Text("${product.name} is out. Every swap below is in stock and within your cap.", color = Muted)
                RescueBudgetSelector(state.rescueBudgetPercent, viewModel::setRescueBudget)
                if (product.id in state.alternativesLoading) {
                    CircularProgressIndicator(Modifier.size(24.dp), color = Forest, strokeWidth = 2.dp)
                }
                options.forEach { alternative ->
                    RescueAlternativeRow(alternative, enabled = !state.busy, onChoose = {
                        viewModel.add(alternative.product)
                        swapFor = null
                        viewModel.showMessage("${alternative.product.name} added to your basket.")
                    })
                }
                if (options.isEmpty() && product.id !in state.alternativesLoading) {
                    Text("No swap fits this cap. Raise it a little, or leave this item out.", color = Muted)
                }
            }
        }
    }
    if (plannerOpen) {
        SmartBasketPlannerSheet(
            state = state,
            onDismiss = { plannerOpen = false },
            onSignIn = { plannerOpen = false; onProfile() },
            onCreatePlan = viewModel::requestSmartBasketPlan,
            onAddPlan = viewModel::addSmartPlanToBasket,
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SmartBasketPlannerSheet(
    state: AppUiState,
    onDismiss: () -> Unit,
    onSignIn: () -> Unit,
    onCreatePlan: (String, Double) -> Unit,
    onAddPlan: () -> Unit,
) {
    var prompt by rememberSaveable { mutableStateOf("Quick vegetarian dinner for two") }
    var budget by rememberSaveable { mutableStateOf("350") }
    val plan = state.smartBasketPlan
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Warm) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp).padding(bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            Text("AI Basket Planner", color = Ink, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
            Text("Describe a meal or shopping need. The planner can only choose from this store’s live catalog; the server checks stock and total before showing a plan.", color = Muted, style = MaterialTheme.typography.bodyMedium)
            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it.take(240) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("What do you need?") },
                minLines = 2,
                maxLines = 3,
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White),
            )
            OutlinedTextField(
                value = budget,
                onValueChange = { value -> if (value.all { it.isDigit() || it == '.' } && value.length <= 7) budget = value },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Budget (₹)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White),
            )
            if (state.accountName == null) {
                Card(shape = RoundedCornerShape(17.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF4E8))) {
                    Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Sign in to create a private, budget-checked plan.", color = Ink, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = onSignIn) { Text("Sign in", color = Forest) }
                    }
                }
            }
            Button(
                onClick = { budget.toDoubleOrNull()?.let { onCreatePlan(prompt, it) } },
                enabled = state.accountName != null && !state.planning && !state.busy && prompt.trim().length >= 8 && (budget.toDoubleOrNull() ?: 0.0) > 0,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                if (state.planning) CircularProgressIndicator(Modifier.size(19.dp), color = Color.White, strokeWidth = 2.dp)
                else Text("Build my basket", fontWeight = FontWeight.Bold)
            }
            plan?.let {
                SmartBasketPlanCard(it, enabled = !state.busy, onAdd = onAddPlan)
            }
            Text("AI suggestions are not allergen or medical advice. Check product labels before buying.", color = Muted, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun SmartBasketPlanCard(plan: SmartBasketPlan, enabled: Boolean, onAdd: () -> Unit) {
    Card(shape = RoundedCornerShape(21.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(plan.title, color = Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            Text(plan.summary, color = Muted, style = MaterialTheme.typography.bodySmall)
            plan.items.forEach { line ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(line.product.emoji, fontSize = 22.sp)
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text("${line.quantity} × ${line.product.name}", color = Ink, fontWeight = FontWeight.SemiBold)
                        Text(line.reason, color = Muted, style = MaterialTheme.typography.labelSmall)
                    }
                    Text(money(line.product.price * line.quantity), color = Ink, fontWeight = FontWeight.Bold)
                }
            }
            Card(shape = RoundedCornerShape(15.dp), colors = CardDefaults.cardColors(containerColor = Mint)) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    PriceLine("AI plan total", money(plan.estimatedTotal))
                    PriceLine("Remaining budget", money((plan.budget - plan.estimatedTotal).coerceAtLeast(0.0)))
                }
            }
            Button(onClick = onAdd, enabled = enabled, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(15.dp)) {
                Text("Add checked items to basket", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun StoreHeader(state: AppUiState, onProfile: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(42.dp).clip(RoundedCornerShape(15.dp)).background(Forest), contentAlignment = Alignment.Center) {
            Text("b", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = Forest, modifier = Modifier.size(16.dp))
                Text("DELIVERING TO", color = Muted, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
            Text("Indiranagar · 6 min", color = Ink, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
        IconButton(onClick = onProfile) {
            Icon(Icons.Outlined.AccountCircle, contentDescription = "Profile", tint = Forest, modifier = Modifier.size(29.dp))
        }
    }
}

@Composable
private fun BasketHero() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Forest),
    ) {
        Box(Modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(26.dp))) {
            Image(
                painter = painterResource(com.basketguard.android.R.drawable.basketguard_hero),
                contentDescription = "Fresh groceries in a reusable shopping tote",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            Box(
                Modifier.fillMaxSize().background(
                    Brush.horizontalGradient(listOf(Color(0xF011392F), Color(0xB011392F), Color.Transparent))
                )
            )
            Column(Modifier.align(Alignment.CenterStart).padding(horizontal = 20.dp, vertical = 18.dp).fillMaxWidth(0.84f)) {
                Surface(shape = CircleShape, color = Color(0xFF356B58)) {
                    Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.VerifiedUser, contentDescription = null, tint = Color(0xFFBCE4BF), modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("THE BASKETGUARD PROMISE", color = Color(0xFFD8F1D7), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("Your basket.\nYour call on swaps.", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, lineHeight = 29.sp)
                Spacer(Modifier.height(6.dp))
                Text("Set a price cap. Pick every replacement yourself.", color = Color(0xFFD5E6DC), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ProductCard(
    product: Product,
    quantity: Int,
    modifier: Modifier,
    onAdd: () -> Unit,
    onQuantity: (Int) -> Unit,
    onDetails: () -> Unit,
    onSwap: () -> Unit,
) {
    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(10.dp)) {
            Box(
                Modifier.fillMaxWidth().height(116.dp).clip(RoundedCornerShape(17.dp))
                    .background(productTint(product.categoryId)).clickable(onClick = onDetails),
                contentAlignment = Alignment.Center,
            ) {
                Text(product.emoji, fontSize = 54.sp)
                Surface(
                    Modifier.align(Alignment.TopEnd).padding(7.dp),
                    shape = RoundedCornerShape(99.dp),
                    color = if (product.available == 0) Color(0xFFFFE4DF) else Color.White.copy(alpha = .92f),
                ) {
                    Text(
                        if (product.available == 0) "OUT" else if (product.available <= 3) "${product.available} LEFT" else "FRESH",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = if (product.available == 0) Color(0xFF9E3329) else Forest,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(product.name, color = Ink, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.clickable(onClick = onDetails))
            Text(product.unit, color = Muted, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(7.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(money(product.price), color = Ink, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                when {
                    product.available <= 0 -> TextButton(onClick = onSwap, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 5.dp)) {
                        Text("Find swap", color = Forest, style = MaterialTheme.typography.labelMedium)
                    }
                    quantity == 0 -> Button(
                        onClick = onAdd,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 13.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Mint, contentColor = Forest),
                    ) { Text("ADD +", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.labelMedium) }
                    else -> QuantityControl(quantity = quantity, maxQuantity = product.available, onChange = onQuantity)
                }
            }
        }
    }
}

@Composable
private fun QuantityControl(quantity: Int, maxQuantity: Int? = null, onChange: (Int) -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(12.dp)).background(Forest).padding(horizontal = 1.dp, vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { onChange(quantity - 1) }, modifier = Modifier.size(29.dp)) {
            Icon(Icons.Outlined.Remove, contentDescription = "Remove one", tint = Color.White, modifier = Modifier.size(16.dp))
        }
        Text("$quantity", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 3.dp))
        IconButton(onClick = { onChange(quantity + 1) }, enabled = maxQuantity == null || quantity < maxQuantity, modifier = Modifier.size(29.dp)) {
            Icon(Icons.Outlined.Add, contentDescription = "Add one", tint = Color.White, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ProductDetailsSheet(product: Product, onDismiss: () -> Unit, onAdd: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Warm) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 30.dp), horizontalAlignment = Alignment.Start) {
            Box(Modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(24.dp)).background(productTint(product.categoryId)), contentAlignment = Alignment.Center) {
                Text(product.emoji, fontSize = 82.sp)
            }
            Spacer(Modifier.height(18.dp))
            Text(product.category.uppercase(Locale.getDefault()), color = Forest, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Text(product.name, color = Ink, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
            Text(product.unit, color = Muted)
            Spacer(Modifier.height(8.dp))
            Text(product.description, color = Ink, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(money(product.price), color = Ink, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(if (product.available > 0) "${product.available} available nearby" else "Currently unavailable", color = Muted)
                }
                if (product.available > 0) Button(onClick = onAdd, shape = RoundedCornerShape(14.dp)) { Text("Add to basket") }
            }
        }
    }
}

@Composable
private fun ReplacementRow(product: Product, onClick: () -> Unit) {
    Card(onClick = onClick, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(52.dp).clip(RoundedCornerShape(15.dp)).background(productTint(product.categoryId)), contentAlignment = Alignment.Center) { Text(product.emoji, fontSize = 29.sp) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(product.name, color = Ink, fontWeight = FontWeight.Bold)
                Text("${product.unit} · ${product.available} available", color = Muted, style = MaterialTheme.typography.bodySmall)
            }
            Text(money(product.price), color = Forest, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CartScreen(state: AppUiState, viewModel: AppViewModel, onCheckout: () -> Unit) {
    val subtotal = state.cart.sumOf { it.price * it.quantity }
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        ScreenTitle("Your basket", "${state.cart.sumOf { it.quantity }} thoughtfully picked items")
        if (state.cart.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                EmptyCard("Your basket is waiting", "Find something fresh for the day.", "🧺")
            }
        } else {
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 14.dp)) {
                items(state.cart, key = { it.productId }) { item ->
                    CartRow(item, onQuantity = { viewModel.changeQuantity(item.productId, it) })
                }
                item { InventoryNote() }
            }
            PriceSummary(subtotal)
            Spacer(Modifier.height(12.dp))
            Button(onClick = onCheckout, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(17.dp)) {
                Text("Review & check out  ·  ${money(subtotal)}", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(14.dp))
        }
    }
}

@Composable
private fun CartRow(item: CartEntity, onQuantity: (Int) -> Unit) {
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(66.dp).clip(RoundedCornerShape(17.dp)).background(Mint), contentAlignment = Alignment.Center) { Text(item.emoji, fontSize = 36.sp) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.name, color = Ink, fontWeight = FontWeight.Bold)
                Text("${item.unit} · ${money(item.price)}", color = Muted, style = MaterialTheme.typography.bodySmall)
                if (item.available < item.quantity) {
                    Text(
                        if (item.available <= 0) "Out of stock · choose a replacement" else "Only ${item.available} available now",
                        color = Color(0xFFB45C28), style = MaterialTheme.typography.labelSmall,
                    )
                }
                Spacer(Modifier.height(7.dp))
                    QuantityControl(item.quantity, item.available, onQuantity)
            }
            Text(money(item.price * item.quantity), color = Ink, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun InventoryNote() {
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Mint)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Shield, contentDescription = null, tint = Forest)
            Spacer(Modifier.width(10.dp))
            Column {
                Text("Stock checked before payment", color = Forest, fontWeight = FontWeight.Bold)
                Text("If a shelf changes, you’ll choose what happens next.", color = Forest, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun PriceSummary(subtotal: Double) {
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PriceLine("Basket subtotal", money(subtotal))
            PriceLine("Delivery", "Free")
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Total", color = Ink, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text(money(subtotal), color = Forest, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
private fun RescueBudgetSelector(selected: Int, onSelect: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("YOUR MAX PRICE INCREASE", color = Muted, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            listOf(0 to "₹0", 15 to "+15%", 30 to "+30%").forEach { (percent, label) ->
                FilterChip(
                    selected = selected == percent,
                    onClick = { onSelect(percent) },
                    label = { Text(if (percent == 0) "Strict · $label" else "${if (percent == 15) "Balanced" else "Flexible"} · $label") },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun RescueAlternativeRow(alternative: ProductAlternative, enabled: Boolean = true, onChoose: () -> Unit) {
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(15.dp)).background(productTint(alternative.product.categoryId)), contentAlignment = Alignment.Center) {
                Text(alternative.product.emoji, fontSize = 27.sp)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(alternative.product.name, color = Ink, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(alternative.reason, color = Muted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(priceDifference(alternative.priceDifference), color = Forest, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.width(6.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(money(alternative.product.price), color = Ink, fontWeight = FontWeight.ExtraBold)
                TextButton(onClick = onChoose, enabled = enabled, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 9.dp, vertical = 0.dp)) {
                    Text("Choose", color = Forest, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun BasketRescuePanel(
    items: List<BasketRescueItem>,
    budgetPercent: Int,
    busy: Boolean,
    onBudget: (Int) -> Unit,
    onAlternative: (Int, ProductAlternative, Int) -> Unit,
    onKeepAvailable: (Int, Int) -> Unit,
    onRemoveUnavailable: (Int) -> Unit,
) {
    Card(shape = RoundedCornerShape(23.dp), colors = CardDefaults.cardColors(containerColor = RescueWash)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(38.dp).clip(RoundedCornerShape(13.dp)).background(Color.White), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Shield, contentDescription = null, tint = Forest)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Basket Rescue", color = Ink, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                    Text("A shelf changed. You decide what happens.", color = Muted, style = MaterialTheme.typography.bodySmall)
                }
            }
            Text("We found ${items.size} item${if (items.size == 1) "" else "s"} that need your choice before checkout.", color = Ink, style = MaterialTheme.typography.bodyMedium)
            RescueBudgetSelector(budgetPercent, onBudget)
            items.forEach { item ->
                Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(item.source.emoji, fontSize = 27.sp)
                            Spacer(Modifier.width(9.dp))
                            Column(Modifier.weight(1f)) {
                                Text(item.source.name, color = Ink, fontWeight = FontWeight.Bold)
                                Text("Need ${item.quantity} · ${item.available} available now", color = Muted, style = MaterialTheme.typography.bodySmall)
                            }
                            Text(money(item.source.price), color = Ink, fontWeight = FontWeight.Bold)
                        }
                        if (item.loading) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                CircularProgressIndicator(Modifier.size(16.dp), color = Forest, strokeWidth = 2.dp)
                                Text("Finding in-stock alternatives…", color = Muted, style = MaterialTheme.typography.bodySmall)
                            }
                        } else if (item.alternatives.isNotEmpty()) {
                            Text("IN STOCK FOR YOUR FULL QUANTITY", color = Forest, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold)
                            item.alternatives.forEach { alternative ->
                                RescueAlternativeRow(alternative, enabled = !busy) {
                                    onAlternative(item.source.id, alternative, item.quantity)
                                }
                            }
                        } else {
                            Text("No same-use replacement fits this cap and quantity.", color = Muted, style = MaterialTheme.typography.bodySmall)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (item.available in 1 until item.quantity) {
                                OutlinedButton(
                                    onClick = { onKeepAvailable(item.source.id, item.available) },
                                    enabled = !busy,
                                    modifier = Modifier.weight(1f), shape = RoundedCornerShape(13.dp),
                                ) { Text("Keep ${item.available}") }
                            }
                            OutlinedButton(
                                onClick = { onRemoveUnavailable(item.source.id) },
                                enabled = !busy,
                                modifier = Modifier.weight(1f), shape = RoundedCornerShape(13.dp),
                            ) { Text("Remove item") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CheckoutScreen(
    state: AppUiState,
    onBack: () -> Unit,
    onPlaceOrder: (String) -> Unit,
    onSignIn: () -> Unit,
    onBudget: (Int) -> Unit,
    onAlternative: (Int, ProductAlternative, Int) -> Unit,
    onKeepAvailable: (Int, Int) -> Unit,
    onRemoveUnavailable: (Int) -> Unit,
) {
    var address by rememberSaveable { mutableStateOf("12 Green Street, Indiranagar, Bengaluru 560038") }
    val subtotal = state.cart.sumOf { it.price * it.quantity }
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Row(Modifier.padding(top = 16.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, contentDescription = "Back") }
            Column {
                Text("One last check", color = Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Your basket, confirmed fresh", color = Muted, style = MaterialTheme.typography.bodySmall)
            }
        }
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 16.dp)) {
            if (state.rescueItems.isNotEmpty()) {
                item {
                    BasketRescuePanel(
                        items = state.rescueItems,
                        budgetPercent = state.rescueBudgetPercent,
                        busy = state.busy,
                        onBudget = onBudget,
                        onAlternative = onAlternative,
                        onKeepAvailable = onKeepAvailable,
                        onRemoveUnavailable = onRemoveUnavailable,
                    )
                }
            }
            item {
                SectionLabel("DELIVERY ADDRESS")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = Forest) },
                    minLines = 2,
                    shape = RoundedCornerShape(18.dp),
                    colors = TextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White),
                )
            }
            item {
                SectionLabel("PAYMENT")
                Spacer(Modifier.height(8.dp))
                Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Lock, contentDescription = null, tint = Forest)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Pay on delivery", color = Ink, fontWeight = FontWeight.Bold)
                            Text("Demo checkout · no charge is made", color = Muted, style = MaterialTheme.typography.bodySmall)
                        }
                        Icon(Icons.Outlined.CheckCircle, contentDescription = "Selected", tint = Forest)
                    }
                }
            }
            item {
                SectionLabel("ORDER SUMMARY")
                Spacer(Modifier.height(8.dp))
                PriceSummary(subtotal)
            }
            item { InventoryNote() }
            if (state.accountName == null) {
                item {
                    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF4E8))) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Sign in to place your order", color = Ink, fontWeight = FontWeight.Bold)
                                Text("Your basket will be here when you return.", color = Muted, style = MaterialTheme.typography.bodySmall)
                            }
                            TextButton(onClick = onSignIn) { Text("Sign in", color = Forest) }
                        }
                    }
                }
            }
        }
        Button(
            onClick = { onPlaceOrder(address) },
            enabled = !state.busy && state.cart.isNotEmpty() && state.accountName != null && state.rescueItems.isEmpty(),
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(17.dp),
        ) {
            if (state.busy) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
            else Text(
                if (state.rescueItems.isNotEmpty()) "Resolve ${state.rescueItems.size} basket issue${if (state.rescueItems.size == 1) "" else "s"}"
                else "Place order  ·  ${money(subtotal)}",
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(14.dp))
    }
}

@Composable
private fun OrdersScreen(state: AppUiState, viewModel: AppViewModel) {
    LaunchedEffect(Unit) { viewModel.loadOrders() }
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        ScreenTitle("Your orders", "A little joy is on the way")
        if (state.accountName == null) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                EmptyCard("Orders live here", "Sign in to see tracking and any replacement choices.", "📦")
            }
        } else if (state.orders.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                EmptyCard("Nothing delivered yet", "Your first order will show up here.", "🛵")
            }
        } else {
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 20.dp)) {
                items(state.orders, key = { it.id }) { order ->
                    OrderCard(
                        order = order,
                        substitutions = state.substitutions[order.id].orEmpty(),
                        products = state.products,
                        onRefresh = { viewModel.refreshSubstitutions(order.id) },
                        onRequest = { itemId, productId -> viewModel.requestSubstitution(order.id, itemId, productId) },
                        onDecision = { sub, decision -> viewModel.decideSubstitution(order.id, sub, decision) },
                    )
                }
            }
        }
    }
}

@Composable
private fun OrderCard(
    order: OrderDto,
    substitutions: List<SubstitutionDto>,
    products: List<Product>,
    onRefresh: () -> Unit,
    onRequest: (Int, Int) -> Unit,
    onDecision: (String, String) -> Unit,
) {
    var selectingItem by remember(order.id) { mutableStateOf<Int?>(null) }
    LaunchedEffect(order.id) { onRefresh() }
    ElevatedCard(shape = RoundedCornerShape(23.dp), colors = CardDefaults.elevatedCardColors(containerColor = Color.White)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(14.dp)).background(Mint), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.LocalShipping, contentDescription = null, tint = Forest)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("BasketGuard Market", color = Ink, fontWeight = FontWeight.Bold)
                    Text("Order ${order.id.take(8).uppercase(Locale.getDefault())}", color = Muted, style = MaterialTheme.typography.bodySmall)
                }
                Surface(shape = CircleShape, color = Mint) {
                    Text(order.status.replaceFirstChar { it.uppercase() }, color = Forest, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
            order.items.forEach { item ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${item.quantity} ×", color = Muted, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.width(7.dp))
                    Text(item.product_name, color = Ink, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Text(money(item.final_price * item.quantity), color = Ink, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
                val product = products.firstOrNull { it.id == item.product_id }
                val options = products.filter { candidate ->
                    candidate.id != item.product_id && candidate.available > 0 &&
                        (product == null || candidate.categoryId == product.categoryId)
                }
                if (options.isNotEmpty()) {
                    TextButton(onClick = { selectingItem = if (selectingItem == item.id) null else item.id }) {
                        Icon(Icons.Outlined.SwapHoriz, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(if (selectingItem == item.id) "Close replacement choices" else "Request a replacement")
                    }
                    if (selectingItem == item.id) {
                        options.take(3).forEach { option ->
                            ReplacementRow(option) {
                                onRequest(item.id, option.id)
                                selectingItem = null
                            }
                        }
                    }
                }
            }
            substitutions.filter { it.status == "pending" }.forEach { sub ->
                Card(shape = RoundedCornerShape(17.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF4E8))) {
                    Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Your choice, always", color = Ink, fontWeight = FontWeight.Bold)
                        Text("Swap for ${sub.replacement_product_name} (${priceDifference(sub.price_difference)}). We’ll wait for your approval.", color = Muted, style = MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { onDecision(sub.id, "approve") }, shape = RoundedCornerShape(12.dp)) { Text("Approve") }
                            OutlinedButton(onClick = { onDecision(sub.id, "reject") }, shape = RoundedCornerShape(12.dp)) { Text("No thanks") }
                        }
                    }
                }
            }
            substitutions.filter { it.status != "pending" }.forEach { sub ->
                Text("Replacement ${sub.status} · ${sub.replacement_product_name}", color = Forest, style = MaterialTheme.typography.bodySmall)
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Delivery", color = Muted, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.weight(1f))
                Text("${money(order.total_amount)} · free delivery", color = Forest, fontWeight = FontWeight.ExtraBold)
            }
            Column {
                Text("ORDER PROGRESS", color = Muted, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(7.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    listOf("Confirmed", "Packing", "On the way", "Delivered").forEachIndexed { index, step ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Box(Modifier.size(12.dp).clip(CircleShape).background(if (index == 0) Forest else Color(0xFFDDE5DD)))
                            Spacer(Modifier.height(4.dp))
                            Text(step, color = if (index == 0) Forest else Muted, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileScreen(state: AppUiState, viewModel: AppViewModel) {
    var registering by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp).verticalScroll(rememberScrollState())) {
        ScreenTitle("Your profile", "Your account, your choices")
        if (state.accountName != null) {
            Card(shape = RoundedCornerShape(23.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(72.dp).clip(CircleShape).background(Mint), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Person, contentDescription = null, tint = Forest, modifier = Modifier.size(38.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Hey, ${state.accountName}", color = Ink, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Your basket is in good hands.", color = Muted)
                    Spacer(Modifier.height(18.dp))
                    ProfileInfoRow("📍", "Delivery address", "12 Green Street · Indiranagar")
                    ProfileInfoRow("🛡️", "BasketGuard promise", "Inventory checked at checkout")
                    ProfileInfoRow("💳", "Payment", "Mock payment · no charge")
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = { viewModel.signOut() }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) { Text("Sign out") }
                }
            }
        } else {
            Card(shape = RoundedCornerShape(23.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(if (registering) "Create your account" else "Welcome back", color = Ink, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Save orders and manage replacement choices.", color = Muted)
                    if (registering) AuthField(name, { name = it }, "Your name")
                    AuthField(email, { email = it }, "Email address", keyboard = KeyboardType.Email)
                    AuthField(password, { password = it }, "Password", password = true)
                    Button(
                        onClick = { viewModel.authenticate(name, email, password, registering) },
                        enabled = !state.busy,
                        modifier = Modifier.fillMaxWidth().height(51.dp),
                        shape = RoundedCornerShape(15.dp),
                    ) {
                        if (state.busy) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        else Text(if (registering) "Create account" else "Sign in", fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = { registering = !registering }) {
                        Text(if (registering) "Already have an account? Sign in" else "New here? Create an account", color = Forest)
                    }
                    Text("Passwords need at least 8 characters.", color = Muted, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Card(shape = RoundedCornerShape(21.dp), colors = CardDefaults.cardColors(containerColor = Mint)) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.VerifiedUser, contentDescription = null, tint = Forest, modifier = Modifier.size(26.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("The BasketGuard promise", color = Forest, fontWeight = FontWeight.Bold)
                    Text("We never swap an item without asking.", color = Forest, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun AuthField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboard: KeyboardType = KeyboardType.Text,
    password: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        visualTransformation = if (password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        shape = RoundedCornerShape(14.dp),
        colors = TextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White),
    )
}

@Composable
private fun ProfileInfoRow(emoji: String, title: String, detail: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(emoji, fontSize = 20.sp)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, color = Ink, fontWeight = FontWeight.SemiBold)
            Text(detail, color = Muted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ScreenTitle(title: String, subtitle: String) {
    Column(Modifier.padding(top = 19.dp, bottom = 17.dp)) {
        Text(title, color = Ink, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
        Text(subtitle, color = Muted, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, color = Forest, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold)
}

@Composable
private fun PriceLine(label: String, amount: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Muted, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.weight(1f))
        Text(amount, color = Ink, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun EmptyCard(title: String, description: String, emoji: String) {
    Card(shape = RoundedCornerShape(23.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 45.sp)
            Spacer(Modifier.height(8.dp))
            Text(title, color = Ink, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text(description, color = Muted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun money(value: Double): String = "₹" + String.format(Locale("en", "IN"), "%,.0f", value)

private fun priceDifference(value: Double): String = when {
    value > 0 -> "+${money(value)} more"
    value < 0 -> "${money(-value)} less"
    else -> "same price"
}

private fun productTint(categoryId: Int) = when (categoryId) {
    1 -> Color(0xFFE3F0DD)
    2 -> Color(0xFFF4E9D9)
    3 -> Color(0xFFF4E4D5)
    4 -> Color(0xFFE9E8F2)
    5 -> Color(0xFFF7E8D5)
    else -> Color(0xFFE1ECE8)
}
