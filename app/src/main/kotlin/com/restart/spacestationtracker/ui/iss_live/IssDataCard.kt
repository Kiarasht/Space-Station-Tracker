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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.restart.spacestationtracker.R
import com.restart.spacestationtracker.domain.iss_live.model.IssLocation
import java.text.DecimalFormat
import java.util.Locale

@Composable
fun IssDataCard(location: IssLocation) {
    val decimalFormat = DecimalFormat("0.###")

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.6f))
            .padding(all = 16.dp)
    ) {
        val lat =
            if (location.latitude < 0) "${decimalFormat.format(location.latitude)}° S" else "${
                decimalFormat.format(location.latitude)
            }° N"
        val lon =
            if (location.longitude < 0) "${decimalFormat.format(location.longitude)}° W" else "${
                decimalFormat.format(location.longitude)
            }° E"
        val visibilityText = when (location.visibility.lowercase(Locale.ROOT)) {
            "eclipsed" -> stringResource(id = R.string.visibility_eclipsed)
            "daylight" -> stringResource(id = R.string.visibility_daylight)
            else -> location.visibility.replaceFirstChar { it.uppercase() }
        }

        Text(text = "$lat, $lon", color = Color.White, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.ArrowUpward,
                contentDescription = stringResource(id = R.string.altitude),
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = stringResource(
                    id = R.string.altitude_km_format,
                    decimalFormat.format(location.altitude)
                ),
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.Speed,
                contentDescription = stringResource(id = R.string.velocity),
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = stringResource(
                    id = R.string.velocity_kph_format,
                    decimalFormat.format(location.velocity)
                ),
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
                contentDescription = stringResource(id = R.string.visibility),
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = stringResource(id = R.string.visibility_format, visibilityText),
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}
