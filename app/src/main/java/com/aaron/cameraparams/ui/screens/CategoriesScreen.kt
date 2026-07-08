package com.aaron.cameraparams.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.aaron.cameraparams.R
import com.aaron.cameraparams.ui.*
import com.aaron.cameraparams.ui.theme.CameraParamsTheme

@Composable
fun CategoriesScreen(viewModel: CameraViewModel, onNavigateToDetail: (String) -> Unit) {
    val uiState by viewModel.uiState.collectAsState()

    CategoriesScreenContent(
        state = uiState.parameters,
        onIntent = { viewModel.handleIntent(it) },
        onNavigateToDetail = onNavigateToDetail
    )
}

@Composable
fun CategoriesScreenContent(
    state: CameraParametersState,
    onIntent: (CameraIntent) -> Unit,
    onNavigateToDetail: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            stringResource(R.string.screen_categories_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        SearchBar(state.searchQuery) { onIntent(CameraIntent.UpdateSearchQuery(it)) }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(state.filteredCategories) { category ->
                CategoryExpandableGroup(
                    category = category,
                    onToggle = { onIntent(CameraIntent.ToggleCategory(category.name)) },
                    onNavigateToDetail = onNavigateToDetail
                )
            }
        }
    }
}

@Composable
fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        placeholder = { Text(stringResource(R.string.search_placeholder)) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        shape = MaterialTheme.shapes.medium,
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
            unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
        )
    )
}

@Composable
fun CategoryExpandableGroup(
    category: ParameterCategory,
    onToggle: () -> Unit,
    onNavigateToDetail: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onToggle() }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    category.name,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium
                )
                Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                    Text(category.parameters.size.toString())
                }
                Spacer(Modifier.width(8.dp))
                Icon(
                    if (category.expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null
                )
            }

            if (category.expanded) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                category.parameters.forEach { parameter ->
                    ParameterRow(parameter, onNavigateToDetail)
                }
            }
        }
    }
}

@Composable
fun ParameterRow(parameter: CameraParameter, onClick: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().clickable { onClick(parameter.key) }.padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                parameter.key.substringAfterLast("."),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(16.dp))
        }
        Text(
            parameter.value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CategoriesScreenPreview() {
    val sampleParameters = listOf(
        CameraParameter(
            key = "android.sensor.info.pixelArraySize",
            value = "4000 x 3000",
            rawValue = "4000x3000",
            category = "Sensor",
            description = "The total number of pixels on the camera sensor."
        ),
        CameraParameter(
            key = "android.sensor.info.physicalSize",
            value = "6.40 x 4.80 mm",
            rawValue = "6.4x4.8",
            category = "Sensor",
            description = "The physical dimensions of the camera sensor."
        )
    )
    
    val sampleCategories = listOf(
        ParameterCategory(
            name = "Sensor",
            parameters = sampleParameters,
            expanded = true
        ),
        ParameterCategory(
            name = "Lens",
            parameters = listOf(
                CameraParameter(
                    key = "android.lens.facing",
                    value = "BACK",
                    rawValue = "1",
                    category = "Lens"
                )
            ),
            expanded = false
        )
    )

    CameraParamsTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            CategoriesScreenContent(
                state = CameraParametersState(
                    categories = sampleCategories,
                    filteredCategories = sampleCategories,
                    searchQuery = ""
                ),
                onIntent = {},
                onNavigateToDetail = {}
            )
        }
    }
}
