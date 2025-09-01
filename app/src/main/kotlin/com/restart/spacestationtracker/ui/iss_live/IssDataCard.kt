package com.restart.spacestationtracker.ui.iss_live

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.restart.spacestationtracker.domain.iss_live.model.IssLocation
import java.text.DecimalFormat

@Composable
fun IssDataCard(location: IssLocation) {
    val decimalFormat = DecimalFormat("0.###")

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.6f))
            .padding(16.dp)
    ) {
        val lat =
            if (location.latitude < 0) "${decimalFormat.format(location.latitude)}° S" else "${
                decimalFormat.format(location.latitude)
            }° N"
        val lon =
            if (location.longitude < 0) "${decimalFormat.format(location.longitude)}° W" else "${
                decimalFormat.format(location.longitude)
            }° E"

        Text(text = "$lat, $lon", color = Color.White, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.ArrowUpward,
                contentDescription = "Altitude",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = "Altitude: ${decimalFormat.format(location.altitude)} km",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.Speed,
                contentDescription = "Velocity",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = "Velocity: ${decimalFormat.format(location.velocity)} kph",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            val visibilityIcon = if (location.visibility.equals(
                    "eclipsed",
                    ignoreCase = true
                )
            ) Icons.Filled.Nightlight else Icons.Filled.WbSunny
            Icon(
                imageVector = visibilityIcon,
                contentDescription = "Visibility",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = "Visibility: ${location.visibility.replaceFirstChar { it.uppercase() }}",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}